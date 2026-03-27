package Ogni.ODAS.iminfin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "odas.iminfin.discovery")
public class IminfinDiscoveryProperties {

    private boolean enabled = true;
    private boolean runOnStartup = true;
    private boolean failFast = false;
    private boolean smokeValidationEnabled = false;
    private Duration refreshIfOlderThan = Duration.ofHours(24);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRunOnStartup() {
        return runOnStartup;
    }

    public void setRunOnStartup(boolean runOnStartup) {
        this.runOnStartup = runOnStartup;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public boolean isSmokeValidationEnabled() {
        return smokeValidationEnabled;
    }

    public void setSmokeValidationEnabled(boolean smokeValidationEnabled) {
        this.smokeValidationEnabled = smokeValidationEnabled;
    }

    public Duration getRefreshIfOlderThan() {
        return refreshIfOlderThan;
    }

    public void setRefreshIfOlderThan(Duration refreshIfOlderThan) {
        this.refreshIfOlderThan = refreshIfOlderThan;
    }
}
