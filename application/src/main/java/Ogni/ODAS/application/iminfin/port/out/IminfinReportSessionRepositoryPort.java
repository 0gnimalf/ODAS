package Ogni.ODAS.application.iminfin.port.out;

import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.application.iminfin.model.IminfinReportSession;

import java.util.Optional;

public interface IminfinReportSessionRepositoryPort {

    Optional<IminfinReportSession> findLatestByProfileKey(IminfinReportProfileKey profileKey);

    IminfinReportSession save(IminfinReportSession session);
}
