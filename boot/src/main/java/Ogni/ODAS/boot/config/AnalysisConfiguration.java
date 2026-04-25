package Ogni.ODAS.boot.config;

import Ogni.ODAS.analysis.service.*;
import Ogni.ODAS.application.port.in.AnalysisUseCase;
import Ogni.ODAS.application.port.in.ObservationCollectionUseCase;
import Ogni.ODAS.application.port.out.analysis.*;
import Ogni.ODAS.application.port.out.persistence.AnalysisQueryPort;
import Ogni.ODAS.application.port.out.persistence.PeriodPersistencePort;
import Ogni.ODAS.application.port.out.persistence.StoredDataQueryPort;
import Ogni.ODAS.application.service.AnalysisService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnalysisConfiguration {

    @Bean
    public NonCumulativeValuePort nonCumulativeValuePort() {
        return new NonCumulativeValueCalculator();
    }

    @Bean
    public QuarterAggregationPort quarterAggregationPort() {
        return new QuarterAggregationCalculator();
    }

    @Bean
    public PeriodGrowthMetricsPort periodGrowthMetricsPort() {
        return new PeriodGrowthMetricsCalculator();
    }

    @Bean
    public RegionComparisonPort regionComparisonPort() {
        return new RegionComparisonCalculator();
    }

    @Bean
    public SubtreeSlicePort subtreeSlicePort() {
        return new SubtreeSliceCalculator();
    }

    @Bean
    public RegionIndicatorMatrixPort regionIndicatorMatrixPort() {
        return new RegionIndicatorMatrixCalculator();
    }

    @Bean
    public AnalysisUseCase analysisUseCase(
            ObservationCollectionUseCase observationCollectionUseCase,
            PeriodPersistencePort periodPersistencePort,
            StoredDataQueryPort storedDataQueryPort,
            AnalysisQueryPort analysisQueryPort,
            NonCumulativeValuePort nonCumulativeValuePort,
            QuarterAggregationPort quarterAggregationPort,
            PeriodGrowthMetricsPort periodGrowthMetricsPort,
            RegionComparisonPort regionComparisonPort,
            SubtreeSlicePort subtreeSlicePort,
            RegionIndicatorMatrixPort regionIndicatorMatrixPort
    ) {
        return new AnalysisService(
                observationCollectionUseCase,
                periodPersistencePort,
                storedDataQueryPort,
                analysisQueryPort,
                nonCumulativeValuePort,
                quarterAggregationPort,
                periodGrowthMetricsPort,
                regionComparisonPort,
                subtreeSlicePort,
                regionIndicatorMatrixPort
        );
    }
}
