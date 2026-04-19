package Ogni.ODAS.application.dto;

public record ObservationCollectionResultDto(
        int datasetCollections,
        int receivedObservations,
        int savedObservations,
        int skippedObservations
) {
}
