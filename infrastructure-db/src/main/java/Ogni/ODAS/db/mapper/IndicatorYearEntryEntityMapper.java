package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.IndicatorEntity;
import Ogni.ODAS.db.entity.IndicatorYearEntryEntity;
import Ogni.ODAS.domain.model.IndicatorYearEntry;
import org.springframework.stereotype.Component;

@Component
public class IndicatorYearEntryEntityMapper {

    public IndicatorYearEntry toDomain(IndicatorYearEntryEntity entity) {
        return new IndicatorYearEntry(
                entity.getId(),
                entity.getIndicator().getId(),
                entity.getIndicator().getCode(),
                entity.getIndicator().getIndicatorGroupCode(),
                entity.getYearValue(),
                entity.getName(),
                entity.getParentIndicator() == null ? null : entity.getParentIndicator().getId(),
                entity.getLevel(),
                entity.getSortOrder(),
                entity.isSection()
        );
    }

    public IndicatorYearEntryEntity toNewEntity(
            IndicatorYearEntry domain,
            IndicatorEntity indicator,
            IndicatorEntity parentIndicator
    ) {
        IndicatorYearEntryEntity entity = new IndicatorYearEntryEntity();
        copyToEntity(domain, indicator, parentIndicator, entity);
        return entity;
    }

    public void copyToEntity(
            IndicatorYearEntry domain,
            IndicatorEntity indicator,
            IndicatorEntity parentIndicator,
            IndicatorYearEntryEntity entity
    ) {
        entity.setIndicator(indicator);
        entity.setYearValue(domain.year());
        entity.setName(domain.name());
        entity.setParentIndicator(parentIndicator);
        entity.setLevel(domain.level());
        entity.setSortOrder(domain.sortOrder());
        entity.setSection(domain.section());
    }
}
