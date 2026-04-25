package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.in.ObservationCollectionUseCase;
import Ogni.ODAS.application.port.in.ReferenceSyncUseCase;
import Ogni.ODAS.application.port.in.StoredDataReadUseCase;
import Ogni.ODAS.application.port.out.collector.ExternalIndicatorCollectorPort;
import Ogni.ODAS.application.port.out.collector.ExternalObservationCollectorPort;
import Ogni.ODAS.application.port.out.collector.ExternalRegionCollectorPort;
import Ogni.ODAS.application.port.out.persistence.*;
import Ogni.ODAS.application.service.ObservationCollectionService;
import Ogni.ODAS.application.service.ReferenceSyncService;
import Ogni.ODAS.application.service.StoredDataReadService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationUseCaseConfiguration {

    @Bean
    public StoredDataReadUseCase storedDataReadUseCase(
            PeriodPersistencePort periodPersistence,
            StoredDataQueryPort storedDataQuery,
            ObservationCollectionUseCase observationCollectionUseCase
    ) {
        return new StoredDataReadService(periodPersistence, storedDataQuery, observationCollectionUseCase);
    }

    @Bean
    public ReferenceSyncUseCase referenceSyncUseCase(
            ExternalRegionCollectorPort regionCollector,
            ExternalIndicatorCollectorPort indicatorCollector,
            RegionPersistencePort regionPersistence,
            PeriodPersistencePort periodPersistence,
            IndicatorPersistencePort indicatorPersistence,
            IndicatorYearEntryPersistencePort indicatorYearEntryPersistence
    ) {
        return new ReferenceSyncService(
                regionCollector,
                indicatorCollector,
                regionPersistence,
                periodPersistence,
                indicatorPersistence,
                indicatorYearEntryPersistence
        );
    }

    @Bean
    public ObservationCollectionUseCase observationCollectionUseCase(
            ReferenceSyncUseCase referenceSyncUseCase,
            ExternalObservationCollectorPort observationCollector,
            RegionPersistencePort regionPersistence,
            PeriodPersistencePort periodPersistence,
            IndicatorPersistencePort indicatorPersistence,
            IndicatorYearEntryPersistencePort indicatorYearEntryPersistence,
            DatasetVersionPersistencePort datasetVersionPersistence,
            DatasetCollectionPersistencePort datasetCollectionPersistence,
            ObservationPersistencePort observationPersistence
    ) {
        return new ObservationCollectionService(
                referenceSyncUseCase,
                observationCollector,
                regionPersistence,
                periodPersistence,
                indicatorPersistence,
                indicatorYearEntryPersistence,
                datasetVersionPersistence,
                datasetCollectionPersistence,
                observationPersistence
        );
    }
}
