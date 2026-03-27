package Ogni.ODAS.application.iminfin.port.out;

import Ogni.ODAS.application.iminfin.model.IminfinParameterCatalogItem;
import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;

import java.util.List;

public interface IminfinParameterCatalogRepositoryPort {

    List<IminfinParameterCatalogItem> findByProfileKey(IminfinReportProfileKey profileKey);

    void replaceForProfile(IminfinReportProfileKey profileKey, List<IminfinParameterCatalogItem> items);
}
