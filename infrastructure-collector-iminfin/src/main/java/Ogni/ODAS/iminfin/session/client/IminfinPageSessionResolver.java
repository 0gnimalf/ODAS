package Ogni.ODAS.iminfin.session.client;

import Ogni.ODAS.iminfin.http.IminfinHttpClient;
import Ogni.ODAS.iminfin.profile.IminfinReportProfile;
import Ogni.ODAS.iminfin.session.model.IminfinResolvedReportSession;
import Ogni.ODAS.iminfin.session.parser.IminfinPageSessionParser;
import org.springframework.stereotype.Component;

@Component
public class IminfinPageSessionResolver {

    private final IminfinHttpClient httpClient;
    private final IminfinPageSessionParser parser;

    public IminfinPageSessionResolver(IminfinHttpClient httpClient, IminfinPageSessionParser parser) {
        this.httpClient = httpClient;
        this.parser = parser;
    }

    public IminfinResolvedReportSession resolve(IminfinReportProfile profile) {
        String html = httpClient.getString(profile.pageUrl());
        return parser.parse(profile, html);
    }
}
