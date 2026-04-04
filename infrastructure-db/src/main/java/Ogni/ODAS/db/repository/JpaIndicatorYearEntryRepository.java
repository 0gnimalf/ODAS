package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.IndicatorYearEntryEntity;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaIndicatorYearEntryRepository extends JpaRepository<IndicatorYearEntryEntity, Long> {

    List<IndicatorYearEntryEntity> findAllByIndicatorIndicatorGroupCodeAndYearValue(
            IndicatorGroupCode groupCode,
            Integer yearValue
    );

    List<IndicatorYearEntryEntity> findAllByIndicatorIndicatorGroupCodeAndYearValueOrderBySortOrderAscNameAsc(
            IndicatorGroupCode groupCode,
            Integer yearValue
    );
}
