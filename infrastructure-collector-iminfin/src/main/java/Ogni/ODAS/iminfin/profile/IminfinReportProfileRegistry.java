package Ogni.ODAS.iminfin.profile;

import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IminfinReportProfileRegistry {

    private final List<IminfinReportProfile> profiles = IminfinProfiles.all();

    public List<IminfinReportProfile> getAll() {
        return profiles;
    }

    public IminfinReportProfile getRequired(IminfinReportProfileKey profileKey) {
        return profiles.stream()
                .filter(profile -> profile.key() == profileKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileKey));
    }
}
