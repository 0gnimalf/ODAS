package Ogni.ODAS.iminfin.discovery;

import Ogni.ODAS.application.iminfin.port.in.RunIminfinDiscoveryUseCase;
import Ogni.ODAS.iminfin.config.IminfinDiscoveryProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class IminfinDiscoveryInitializer {

    private final IminfinDiscoveryProperties properties;
    private final RunIminfinDiscoveryUseCase discoveryUseCase;

    public IminfinDiscoveryInitializer(
            IminfinDiscoveryProperties properties,
            RunIminfinDiscoveryUseCase discoveryUseCase
    ) {
        this.properties = properties;
        this.discoveryUseCase = discoveryUseCase;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!properties.isEnabled() || !properties.isRunOnStartup()) {
            return;
        }

        discoveryUseCase.runStartupDiscovery();
    }
}
