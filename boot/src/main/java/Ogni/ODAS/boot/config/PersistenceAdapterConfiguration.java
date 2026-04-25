package Ogni.ODAS.boot.config;

import Ogni.ODAS.application.port.out.persistence.*;
import Ogni.ODAS.db.adapter.*;
import Ogni.ODAS.db.repository.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class PersistenceAdapterConfiguration {

    @Bean
    public RegionPersistencePort regionPersistencePort(RegionJpaRepository repository) {
        return new RegionPersistenceAdapter(repository);
    }

    @Bean
    public PeriodPersistencePort periodPersistencePort(PeriodJpaRepository repository) {
        return new PeriodPersistenceAdapter(repository);
    }

    @Bean
    public IndicatorPersistencePort indicatorPersistencePort(IndicatorJpaRepository repository) {
        return new IndicatorPersistenceAdapter(repository);
    }

    @Bean
    public IndicatorYearEntryPersistencePort indicatorYearEntryPersistencePort(IndicatorYearEntryJpaRepository repository) {
        return new IndicatorYearEntryPersistenceAdapter(repository);
    }

    @Bean
    public DatasetVersionPersistencePort datasetVersionPersistencePort(DatasetVersionJpaRepository repository) {
        return new DatasetVersionPersistenceAdapter(repository);
    }

    @Bean
    public DatasetCollectionPersistencePort datasetCollectionPersistencePort(DatasetCollectionJpaRepository repository) {
        return new DatasetCollectionPersistenceAdapter(repository);
    }

    @Bean
    public ObservationPersistencePort observationPersistencePort(ObservationJpaRepository repository, JdbcTemplate jdbcTemplate) {
        return new ObservationPersistenceAdapter(repository, jdbcTemplate);
    }

    @Bean
    public StoredDataQueryPort storedDataQueryPort(NamedParameterJdbcTemplate jdbcTemplate) {
        return new StoredDataQueryAdapter(jdbcTemplate);
    }

    @Bean
    public AnalysisQueryPort analysisQueryPort(NamedParameterJdbcTemplate jdbcTemplate) {
        return new AnalysisQueryAdapter(jdbcTemplate);
    }
}
