package Ogni.ODAS.iminfin.profile;

import Ogni.ODAS.application.iminfin.model.IminfinParameterCatalogItem;
import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;

import java.util.List;

public record IminfinReportProfile(
        IminfinReportProfileKey key,
        String pageUrl,
        String mainDatasetCode,
        List<String> referenceDatasetCodes,
        List<IminfinParameterCatalogItem> seedCatalogItems
) {
}
