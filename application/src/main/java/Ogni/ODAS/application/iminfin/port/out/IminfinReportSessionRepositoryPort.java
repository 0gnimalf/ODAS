package Ogni.ODAS.application.iminfin.port.out;

import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.application.iminfin.model.IminfinReportSession;

import java.util.List;
import java.util.Optional;

public interface IminfinReportSessionRepositoryPort {

    IminfinReportSession save(IminfinReportSession session);

    Optional<IminfinReportSession> findLatestByProfileKey(IminfinReportProfileKey profileKey);

    List<IminfinReportSession> findByProfileKey(IminfinReportProfileKey profileKey);
}