package Ogni.ODAS.iminfin.session.client;

import Ogni.ODAS.iminfin.http.IminfinHttpClient;
import Ogni.ODAS.iminfin.session.model.IminfinReportModels;
import Ogni.ODAS.iminfin.session.model.IminfinResolvedReportSession;
import org.springframework.stereotype.Component;

@Component
public class IminfinReportModelClient {

    private final IminfinHttpClient httpClient;

    public IminfinReportModelClient(IminfinHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public IminfinReportModels load(IminfinResolvedReportSession session) {
        return new IminfinReportModels(
                httpClient.getJson(session.primaryReportModelUrl()),
                httpClient.getJson(session.metaReportModelUrl())
        );
    }
}
