package Ogni.ODAS.application.dto;

public record ReferenceSyncResultDto(
        int received,
        int created,
        int updated,
        int skipped
) {
    public static ReferenceSyncResultDto empty() {
        return new ReferenceSyncResultDto(0, 0, 0, 0);
    }

    public ReferenceSyncResultDto plus(ReferenceSyncResultDto other) {
        if (other == null) {
            return this;
        }
        return new ReferenceSyncResultDto(
                received + other.received,
                created + other.created,
                updated + other.updated,
                skipped + other.skipped
        );
    }
}
