package Ogni.ODAS.application.iminfin.port.out;

import Ogni.ODAS.application.iminfin.model.IminfinParameterOption;
import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;

import java.util.List;

public interface IminfinParameterOptionRepositoryPort {

    List<IminfinParameterOption> findByProfileKeyAndParameterName(
            IminfinReportProfileKey profileKey,
            String parameterName
    );

    IminfinParameterOption save(IminfinParameterOption option);
}
