package Ogni.ODAS.application.dto;

public record ReferenceSyncResultDto(
        int regionsProcessed,
        int indicatorsProcessed,
        int incomeIndicators,
        int outcomeIndicators,
        int creditIndicators,
        int finSourceIndicators
) {
}
