package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.command.AnalyzeBudgetDataCommand;
import Ogni.ODAS.application.dto.CollectedDatasetDto;
import Ogni.ODAS.application.dto.CollectedObservationDto;
import Ogni.ODAS.application.port.out.ExternalSourceCollectorPort;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.iminfin.config.IminfinCollectorProperties;
import Ogni.ODAS.iminfin.config.IminfinPassportPage;
import Ogni.ODAS.iminfin.model.IminfinLoadedData;
import Ogni.ODAS.iminfin.model.IminfinReportDefinition;
import Ogni.ODAS.iminfin.util.IminfinPeriodFormatter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class IminfinDirectCollector implements ExternalSourceCollectorPort {

    private static final String CREDIT_DATA_SOURCE = "PassportFK_001_005_creditGridData";
    private static final String CREDIT_PARAMETER_NAME = "PassportFK_001_005_paramCredits";

    private final IminfinReportDiscoveryService discoveryService;
    private final IminfinTerritoryResolver territoryResolver;
    private final IminfinReportDataLoader reportDataLoader;
    private final IminfinObservationMapper observationMapper;
    private final IminfinCollectorProperties properties;

    public IminfinDirectCollector(
            IminfinReportDiscoveryService discoveryService,
            IminfinTerritoryResolver territoryResolver,
            IminfinReportDataLoader reportDataLoader,
            IminfinObservationMapper observationMapper,
            IminfinCollectorProperties properties
    ) {
        this.discoveryService = discoveryService;
        this.territoryResolver = territoryResolver;
        this.reportDataLoader = reportDataLoader;
        this.observationMapper = observationMapper;
        this.properties = properties;
    }

    @Override
    public CollectedDatasetDto collect(AnalyzeBudgetDataCommand command) {
        validate(command);

        return switch (command.indicatorGroupCode()) {
            case INCOME, OUTCOME, FIN_SOURCE -> collectDetail(command, resolveDetailSpec(command));
            case CREDIT -> collectCredit(command);
            default ->
                    throw new IllegalStateException("Unsupported indicatorGroupCode: " + command.indicatorGroupCode());
        };
    }

    private CollectedDatasetDto collectDetail(AnalyzeBudgetDataCommand command, DetailCollectionSpec spec) {
        IminfinReportDefinition reportDefinition = discoveryService.discover(spec.page());
        String territoryCode = territoryResolver.resolve(command.regionCode());
        String period = IminfinPeriodFormatter.format(command.year(), command.month());

        IminfinLoadedData loadedData = reportDataLoader.loadDetailData(
                reportDefinition,
                territoryCode,
                period,
                spec.outcomesType()
        );

        List<CollectedObservationDto> observations = observationMapper.mapDetailObservationsForRegion(
                territoryCode,
                spec.indicatorGroupCode(),
                command.year(),
                command.month(),
                spec.rootPrefix(),
                loadedData.dataSource(),
                loadedData.dataRows()
        );

        return buildDataset(reportDefinition, observations,
                "No observations found for indicatorCode=" + command.indicatorCode());
    }

    private CollectedDatasetDto collectCredit(AnalyzeBudgetDataCommand command) {
        territoryResolver.resolve(command.regionCode());

        IminfinReportDefinition reportDefinition = discoveryService.discover(IminfinPassportPage.CREDITS_COMPARE);
        String period = IminfinPeriodFormatter.format(command.year(), command.month());
        IminfinLoadedData loadedData = reportDataLoader.loadData(
                reportDefinition,
                CREDIT_DATA_SOURCE,
                Map.of(
                        CREDIT_PARAMETER_NAME, toCreditSourceCode(command.indicatorCode()),
                        "paramPeriod", period
                )
        );

        List<CollectedObservationDto> observations = observationMapper.mapCreditObservationsForIndicator(
                command.indicatorCode(),
                command.year(),
                command.month(),
                loadedData.dataSource(),
                loadedData.dataRows(),
                territoryResolver.regionCodeByNormalizedName()
        );

        return buildDataset(
                reportDefinition,
                observations,
                "No credit observations found for region=" + command.regionCode() + ", indicatorCode=" + command.indicatorCode()
        );
    }

    private CollectedDatasetDto buildDataset(
            IminfinReportDefinition reportDefinition,
            List<CollectedObservationDto> observations,
            String emptyMessage
    ) {
        if (observations.isEmpty()) {
            throw new IllegalStateException(emptyMessage);
        }

        return new CollectedDatasetDto(
                reportDefinition.page().name().toLowerCase(),
                reportDefinition.dataVersion(),
                SourceSystemCode.IMINFIN,
                observations
        );
    }

    private DetailCollectionSpec resolveDetailSpec(AnalyzeBudgetDataCommand command) {
        return switch (command.indicatorGroupCode()) {
            case INCOME -> new DetailCollectionSpec(
                    IminfinPassportPage.INCOMES_DETAIL,
                    IndicatorGroupCode.INCOME,
                    "income",
                    null
            );
            case FIN_SOURCE -> new DetailCollectionSpec(
                    IminfinPassportPage.FIN_SOURCES_DETAIL,
                    IndicatorGroupCode.FIN_SOURCE,
                    "fin-source",
                    null
            );
            case OUTCOME -> resolveOutcomeSpec(command.indicatorCode());
            default ->
                    throw new IllegalStateException("Detail collection is not supported for " + command.indicatorGroupCode());
        };
    }

    private DetailCollectionSpec resolveOutcomeSpec(String indicatorCode) {
        if (indicatorCode.startsWith("outcome/rzpr/")) {
            return new DetailCollectionSpec(
                    IminfinPassportPage.OUTCOMES_DETAIL,
                    IndicatorGroupCode.OUTCOME,
                    "outcome/rzpr",
                    2
            );
        }
        if (indicatorCode.startsWith("outcome/kvr/")) {
            return new DetailCollectionSpec(
                    IminfinPassportPage.OUTCOMES_DETAIL,
                    IndicatorGroupCode.OUTCOME,
                    "outcome/kvr",
                    3
            );
        }
        throw new IllegalStateException("Outcome indicatorCode must start with 'outcome/rzpr/' or 'outcome/kvr/'");
    }

    private String toCreditSourceCode(String indicatorCode) {
        if (indicatorCode == null || !indicatorCode.startsWith("credit/")) {
            throw new IllegalStateException("Credit indicatorCode must start with 'credit/'");
        }
        return indicatorCode.substring("credit/".length());
    }

    private void validate(AnalyzeBudgetDataCommand command) {
        if (command.regionCode() == null || command.regionCode().isBlank()) {
            throw new IllegalStateException("regionCode is required");
        }
        if (command.indicatorGroupCode() == null) {
            throw new IllegalStateException("indicatorGroupCode is required");
        }
        if (command.indicatorCode() == null || command.indicatorCode().isBlank()) {
            throw new IllegalStateException("indicatorCode is required");
        }
        if (command.year() == null || command.month() == null) {
            throw new IllegalStateException("year and month are required");
        }
        if (command.month() < 1 || command.month() > 12) {
            throw new IllegalStateException("month must be between 1 and 12");
        }
        if (command.year() < properties.getMinYearToCollect() || command.year() > properties.getMaxYearToCollect()) {
            throw new IllegalStateException("year must be valid");
        }
    }

    private record DetailCollectionSpec(
            IminfinPassportPage page,
            IndicatorGroupCode indicatorGroupCode,
            String rootPrefix,
            Integer outcomesType
    ) {
    }
}
