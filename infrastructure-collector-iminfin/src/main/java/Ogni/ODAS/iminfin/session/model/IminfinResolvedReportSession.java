package Ogni.ODAS.iminfin.session.model;

import Ogni.ODAS.application.iminfin.model.IminfinDiscoveryStatus;
import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.application.iminfin.model.IminfinReportSession;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

public record IminfinResolvedReportSession(
        IminfinReportProfileKey profileKey,
        String pageUrl,
        String redirectBaseUrl,
        String reportId,
        String uuid,
        String versionLabel,
        String dataVersion,
        OffsetDateTime resolvedAt
) {

    public String primaryReportModelUrl() {
        return redirectBaseUrl
                + "/Data/Primary/ReportModel.json?reportId=" + encode(reportId)
                + "&version=" + encode(versionLabel);
    }

    public String metaReportModelUrl() {
        return redirectBaseUrl
                + "/Data/Meta/ReportModel.json?reportId=" + encode(reportId)
                + "&version=" + encode(versionLabel);
    }

    public IminfinReportSession toApplicationSession(IminfinDiscoveryStatus status) {
        return new IminfinReportSession(
                null,
                profileKey,
                reportId,
                uuid,
                versionLabel,
                dataVersion,
                resolvedAt,
                status
        );
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
