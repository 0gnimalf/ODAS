package Ogni.ODAS.iminfin.discovery;

import Ogni.ODAS.application.iminfin.model.IminfinDiscoveryState;
import Ogni.ODAS.application.iminfin.model.IminfinDiscoveryStatus;
import Ogni.ODAS.application.iminfin.port.in.RunIminfinDiscoveryUseCase;
import Ogni.ODAS.application.iminfin.port.out.IminfinDiscoveryStateRepositoryPort;
import Ogni.ODAS.application.iminfin.port.out.IminfinParameterCatalogRepositoryPort;
import Ogni.ODAS.iminfin.config.IminfinDiscoveryProperties;
import Ogni.ODAS.iminfin.profile.IminfinReportProfile;
import Ogni.ODAS.iminfin.profile.IminfinReportProfileRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class IminfinDiscoveryUseCaseImpl implements RunIminfinDiscoveryUseCase {

    private static final Logger log = LoggerFactory.getLogger(IminfinDiscoveryUseCaseImpl.class);

    private final IminfinDiscoveryProperties properties;
    private final IminfinReportProfileRegistry profileRegistry;
    private final IminfinParameterCatalogRepositoryPort parameterCatalogRepositoryPort;
    private final IminfinDiscoveryStateRepositoryPort discoveryStateRepositoryPort;

    public IminfinDiscoveryUseCaseImpl(
            IminfinDiscoveryProperties properties,
            IminfinReportProfileRegistry profileRegistry,
            IminfinParameterCatalogRepositoryPort parameterCatalogRepositoryPort,
            IminfinDiscoveryStateRepositoryPort discoveryStateRepositoryPort
    ) {
        this.properties = properties;
        this.profileRegistry = profileRegistry;
        this.parameterCatalogRepositoryPort = parameterCatalogRepositoryPort;
        this.discoveryStateRepositoryPort = discoveryStateRepositoryPort;
    }

    @Override
    public void runStartupDiscovery() {
        if (!properties.isEnabled()) {
            log.info("iMinfin discovery is disabled by configuration");
            return;
        }

        for (IminfinReportProfile profile : profileRegistry.getAll()) {
            seedProfileCatalog(profile);
        }
    }

    private void seedProfileCatalog(IminfinReportProfile profile) {
        try {
            parameterCatalogRepositoryPort.replaceForProfile(profile.key(), profile.seedCatalogItems());

            discoveryStateRepositoryPort.save(new IminfinDiscoveryState(
                    null,
                    profile.key(),
                    IminfinDiscoveryStatus.SEEDED,
                    OffsetDateTime.now(),
                    null,
                    null
            ));

            log.info("Seeded iMinfin profile {} with {} catalog items",
                    profile.key(), profile.seedCatalogItems().size());
        } catch (Exception ex) {
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

            log.error("Failed to seed iMinfin profile {}", profile.key(), ex);
        }
    }
}
