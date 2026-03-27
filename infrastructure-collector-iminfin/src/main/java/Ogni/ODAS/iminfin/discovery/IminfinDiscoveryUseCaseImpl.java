package Ogni.ODAS.iminfin.discovery;

import Ogni.ODAS.application.iminfin.model.IminfinDiscoveryState;
import Ogni.ODAS.application.iminfin.model.IminfinDiscoveryStatus;
import Ogni.ODAS.application.iminfin.model.IminfinParameterCatalogItem;
import Ogni.ODAS.application.iminfin.model.IminfinReportSession;
import Ogni.ODAS.application.iminfin.port.in.RunIminfinDiscoveryUseCase;
import Ogni.ODAS.application.iminfin.port.out.IminfinDiscoveryStateRepositoryPort;
import Ogni.ODAS.application.iminfin.port.out.IminfinParameterCatalogRepositoryPort;
import Ogni.ODAS.application.iminfin.port.out.IminfinParameterOptionRepositoryPort;
import Ogni.ODAS.application.iminfin.port.out.IminfinReportSessionRepositoryPort;
import Ogni.ODAS.iminfin.config.IminfinDiscoveryProperties;
import Ogni.ODAS.iminfin.profile.IminfinReportProfile;
import Ogni.ODAS.iminfin.profile.IminfinReportProfileRegistry;
import Ogni.ODAS.iminfin.session.catalog.IminfinParameterCatalogHarvester;
import Ogni.ODAS.iminfin.session.client.IminfinPageSessionResolver;
import Ogni.ODAS.iminfin.session.client.IminfinReportModelClient;
import Ogni.ODAS.iminfin.session.model.IminfinReportModels;
import Ogni.ODAS.iminfin.session.model.IminfinResolvedReportSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class IminfinDiscoveryUseCaseImpl implements RunIminfinDiscoveryUseCase {

    private static final Logger log = LoggerFactory.getLogger(IminfinDiscoveryUseCaseImpl.class);

    private final IminfinDiscoveryProperties properties;
    private final IminfinReportProfileRegistry profileRegistry;
    private final IminfinParameterCatalogRepositoryPort parameterCatalogRepositoryPort;
    private final IminfinParameterOptionRepositoryPort parameterOptionRepositoryPort;
    private final IminfinDiscoveryStateRepositoryPort discoveryStateRepositoryPort;
    private final IminfinReportSessionRepositoryPort reportSessionRepositoryPort;
    private final IminfinPageSessionResolver pageSessionResolver;
    private final IminfinReportModelClient reportModelClient;
    private final IminfinParameterCatalogHarvester parameterCatalogHarvester;

    public IminfinDiscoveryUseCaseImpl(
            IminfinDiscoveryProperties properties,
            IminfinReportProfileRegistry profileRegistry,
            IminfinParameterCatalogRepositoryPort parameterCatalogRepositoryPort,
            IminfinParameterOptionRepositoryPort parameterOptionRepositoryPort,
            IminfinDiscoveryStateRepositoryPort discoveryStateRepositoryPort,
            IminfinReportSessionRepositoryPort reportSessionRepositoryPort,
            IminfinPageSessionResolver pageSessionResolver,
            IminfinReportModelClient reportModelClient,
            IminfinParameterCatalogHarvester parameterCatalogHarvester
    ) {
        this.properties = properties;
        this.profileRegistry = profileRegistry;
        this.parameterCatalogRepositoryPort = parameterCatalogRepositoryPort;
        this.parameterOptionRepositoryPort = parameterOptionRepositoryPort;
        this.discoveryStateRepositoryPort = discoveryStateRepositoryPort;
        this.reportSessionRepositoryPort = reportSessionRepositoryPort;
        this.pageSessionResolver = pageSessionResolver;
        this.reportModelClient = reportModelClient;
        this.parameterCatalogHarvester = parameterCatalogHarvester;
    }

    @Override
    public void runStartupDiscovery() {
        if (!properties.isEnabled()) {
            log.info("iMinfin discovery is disabled by configuration");
            return;
        }

        for (IminfinReportProfile profile : profileRegistry.getAll()) {
            discoverProfile(profile);
        }
    }

    private void discoverProfile(IminfinReportProfile profile) {
        if (isFresh(profile)) {
            log.info("Skipping iMinfin profile {} because report session is still fresh", profile.key());
            return;
        }

        savePendingState(profile);
        IminfinResolvedReportSession resolvedSession = null;

        try {
            resolvedSession = pageSessionResolver.resolve(profile);
            IminfinReportModels reportModels = reportModelClient.load(resolvedSession);

            List<IminfinParameterCatalogItem> harvestedCatalog = parameterCatalogHarvester.harvestCatalog(profile, reportModels);
            parameterCatalogRepositoryPort.replaceForProfile(profile.key(), harvestedCatalog);
            parameterCatalogHarvester.harvestDefaultOptions(profile, harvestedCatalog)
                    .forEach(parameterOptionRepositoryPort::save);

            IminfinReportSession session = reportSessionRepositoryPort.save(
                    resolvedSession.toApplicationSession(IminfinDiscoveryStatus.SUCCESS)
            );

            discoveryStateRepositoryPort.save(new IminfinDiscoveryState(
                    null,
                    profile.key(),
                    IminfinDiscoveryStatus.SUCCESS,
                    session.resolvedAt(),
                    null,
                    null
            ));

            log.info(
                    "Resolved iMinfin profile {} -> reportId={}, dataVersion={}, catalogItems={}",
                    profile.key(),
                    session.reportId(),
                    session.dataVersion(),
                    harvestedCatalog.size()
            );
        } catch (Exception ex) {
            if (resolvedSession != null) {
                reportSessionRepositoryPort.save(resolvedSession.toApplicationSession(IminfinDiscoveryStatus.DEGRADED));
            }

            discoveryStateRepositoryPort.save(new IminfinDiscoveryState(
                    null,
                    profile.key(),
                    IminfinDiscoveryStatus.FAILED,
                    null,
                    OffsetDateTime.now(),
                    ex.getMessage()
            ));

            if (properties.isFailFast()) {
                throw ex;
            }

            log.error("Failed to resolve iMinfin profile {}", profile.key(), ex);
        }
    }

    private boolean isFresh(IminfinReportProfile profile) {
        return reportSessionRepositoryPort.findLatestByProfileKey(profile.key())
                .filter(session -> session.status() == IminfinDiscoveryStatus.SUCCESS)
                .filter(session -> session.resolvedAt() != null)
                .filter(session -> session.resolvedAt().isAfter(OffsetDateTime.now().minus(properties.getRefreshIfOlderThan())))
                .isPresent();
    }

    private void savePendingState(IminfinReportProfile profile) {
        discoveryStateRepositoryPort.save(new IminfinDiscoveryState(
                null,
                profile.key(),
                IminfinDiscoveryStatus.PENDING,
                null,
                null,
                null
        ));
    }
}
