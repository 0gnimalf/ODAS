//package Ogni.ODAS.iminfin.service;
//
//import Ogni.ODAS.application.port.out.ExternalPopulationCollectorPort;
//import Ogni.ODAS.domain.enumtype.PeriodType;
//import Ogni.ODAS.domain.model.PopulationStat;
//import Ogni.ODAS.domain.model.Period;
//import Ogni.ODAS.iminfin.config.IminfinPassportPage;
//import Ogni.ODAS.iminfin.model.IminfinLoadedData;
//import Ogni.ODAS.iminfin.model.IminfinReportDefinition;
//import Ogni.ODAS.iminfin.util.IminfinJsonTableHelper;
//import Ogni.ODAS.iminfin.util.IminfinPeriodFormatter;
//import com.fasterxml.jackson.databind.JsonNode;
//import org.springframework.stereotype.Service;
//
//import java.util.Map;
//import java.util.Optional;
//
//@Service
//public class IminfinPopulationCollector implements ExternalPopulationCollectorPort {
//
//    private static final String POPULATION_LABEL = "численность населения (чел.)";
//
//    private final IminfinReportDiscoveryService discoveryService;
//    private final IminfinReportDataLoader reportDataLoader;
//
//    public IminfinPopulationCollector(
//            IminfinReportDiscoveryService discoveryService,
//            IminfinReportDataLoader reportDataLoader
//    ) {
//        this.discoveryService = discoveryService;
//        this.reportDataLoader = reportDataLoader;
//    }
//
//    @Override
//    public Optional<PopulationStat> collect(String regionCode, Integer year) {
//        if (regionCode == null || regionCode.isBlank() || year == null) {
//            return Optional.empty();
//        }
//
//        IminfinReportDefinition reportDefinition = discoveryService.discover(IminfinPassportPage.PASSPORT_ROOT);
//        IminfinLoadedData loadedData = reportDataLoader.loadData(
//                reportDefinition,
//                reportDefinition.resolvePopulationDataSource(),
//                Map.of(
//                        "territory", regionCode,
//                        "paramPeriod", IminfinPeriodFormatter.format(year, 1)
//                )
//        );
//
//        if (!loadedData.dataRows().isArray() || loadedData.dataRows().isEmpty()) {
//            return Optional.empty();
//        }
//
//        Long population = extractPopulationValue(loadedData);
//        if (population == null || population <= 0) {
//            return Optional.empty();
//        }
//
//        return Optional.of(new PopulationStat(
//                null,
//                regionCode,
//                new Period(
//                        null,
//                        PeriodType.YEAR,
//                        year,
//                        null,
//                        null,
//                        "На 01.01." + year
//                ),
//                population
//        ));
//    }
//
//    private Long extractPopulationValue(IminfinLoadedData loadedData) {
//        Map<String, Integer> columns = IminfinJsonTableHelper.columnIndexes(loadedData.dataSource().columnNames());
//        Integer nameIndex = columns.get("name");
//        Integer populationIndex = columns.get("prevYearFact");
//
//        for (JsonNode row : loadedData.dataRows()) {
//            if (!row.isArray() || row.isEmpty()) {
//                continue;
//            }
//
//            String label = IminfinJsonTableHelper.textCell(row, nameIndex);
//            if (!POPULATION_LABEL.equals(Ogni.ODAS.application.support.TextNormalizer.normalize(label))) {
//                continue;
//            }
//
//            Long value = IminfinJsonTableHelper.longCell(row, populationIndex);
//            if (value != null) {
//                return value;
//            }
//        }
//        return null;
//    }
//}
