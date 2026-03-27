package Ogni.ODAS.iminfin.session.catalog;

import Ogni.ODAS.application.iminfin.model.IminfinOptionVerificationStatus;
import Ogni.ODAS.application.iminfin.model.IminfinParameterCatalogItem;
import Ogni.ODAS.application.iminfin.model.IminfinParameterOption;
import Ogni.ODAS.application.iminfin.model.IminfinParameterRole;
import Ogni.ODAS.iminfin.profile.IminfinReportProfile;
import Ogni.ODAS.iminfin.session.model.IminfinReportModelParameterDescriptor;
import Ogni.ODAS.iminfin.session.model.IminfinReportModels;
import Ogni.ODAS.iminfin.session.parser.IminfinReportModelParser;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class IminfinParameterCatalogHarvester {

    private final IminfinReportModelParser reportModelParser;

    public IminfinParameterCatalogHarvester(IminfinReportModelParser reportModelParser) {
        this.reportModelParser = reportModelParser;
    }

    public List<IminfinParameterCatalogItem> harvestCatalog(
            IminfinReportProfile profile,
            IminfinReportModels reportModels
    ) {
        Map<String, IminfinParameterCatalogItem> resultByName = new LinkedHashMap<>();

        for (IminfinParameterCatalogItem seed : profile.seedCatalogItems()) {
            if (seed == null || isBlank(seed.parameterName())) {
                continue;
            }
            resultByName.put(seed.parameterName(), normalizeSeed(profile, seed));
        }

        mergeExtracted(resultByName, profile, reportModelParser.extractParameters(reportModels.primary()));
        mergeExtracted(resultByName, profile, reportModelParser.extractParameters(reportModels.meta()));

        return resultByName.values().stream()
                .sorted(Comparator.comparing(IminfinParameterCatalogItem::parameterName))
                .toList();
    }

    public List<IminfinParameterOption> harvestDefaultOptions(
            IminfinReportProfile profile,
            List<IminfinParameterCatalogItem> catalogItems
    ) {
        OffsetDateTime discoveredAt = OffsetDateTime.now();
        Map<String, IminfinParameterOption> unique = new LinkedHashMap<>();

        for (IminfinParameterCatalogItem item : catalogItems) {
            if (item == null) {
                continue;
            }
            if (isBlank(item.defaultValue())) {
                continue;
            }
            if (item.role() != IminfinParameterRole.BUSINESS
                    && item.role() != IminfinParameterRole.PROFILE_DEFAULT) {
                continue;
            }

            String key = item.parameterName() + "::" + item.defaultValue();
            unique.put(key, new IminfinParameterOption(
                    null,
                    profile.key(),
                    item.parameterName(),
                    item.defaultValue(),
                    null,
                    null,
                    IminfinOptionVerificationStatus.OBSERVED,
                    discoveredAt
            ));
        }

        return unique.values().stream().toList();
    }

    private void mergeExtracted(
            Map<String, IminfinParameterCatalogItem> resultByName,
            IminfinReportProfile profile,
            Map<String, IminfinReportModelParameterDescriptor> extractedByName
    ) {
        for (Map.Entry<String, IminfinReportModelParameterDescriptor> entry : extractedByName.entrySet()) {
            String parameterName = entry.getKey();
            IminfinReportModelParameterDescriptor extracted = entry.getValue();

            if (isBlank(parameterName) || extracted == null) {
                continue;
            }

            IminfinParameterCatalogItem existing = resultByName.get(parameterName);
            if (existing != null) {
                resultByName.put(parameterName, new IminfinParameterCatalogItem(
                        existing.id(),
                        profile.key(),
                        parameterName,
                        existing.role(),
                        coalesce(extracted.valueType(), existing.valueType()),
                        extracted.required() || existing.required(),
                        pickDefaultValue(extracted.defaultValue(), existing.defaultValue())
                ));
            } else {
                resultByName.put(parameterName, new IminfinParameterCatalogItem(
                        null,
                        profile.key(),
                        parameterName,
                        IminfinParameterRole.SERVICE,
                        extracted.valueType(),
                        extracted.required(),
                        extracted.defaultValue()
                ));
            }
        }
    }

    private IminfinParameterCatalogItem normalizeSeed(
            IminfinReportProfile profile,
            IminfinParameterCatalogItem seed
    ) {
        return new IminfinParameterCatalogItem(
                null,
                profile.key(),
                seed.parameterName(),
                seed.role(),
                seed.valueType(),
                seed.required(),
                seed.defaultValue()
        );
    }

    private String pickDefaultValue(String extractedDefault, String currentDefault) {
        if (!isBlank(extractedDefault)) {
            return extractedDefault;
        }
        return currentDefault;
    }

    private String coalesce(String first, String second) {
        return !isBlank(first) ? first : second;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}