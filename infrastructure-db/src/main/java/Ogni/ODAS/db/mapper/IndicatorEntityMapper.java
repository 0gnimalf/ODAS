package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.IndicatorEntity;
import Ogni.ODAS.domain.model.Indicator;
import org.springframework.stereotype.Component;

@Component
public class IndicatorEntityMapper {

    public Indicator toDomain(IndicatorEntity entity) {
        return new Indicator(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getIndicatorGroupCode(),
                entity.getParent() == null ? null : entity.getParent().getId(),
                entity.getLevel(),
                entity.getSortOrder(),
                entity.isSection()
        );
    }

    public IndicatorEntity toNewEntity(Indicator domain, IndicatorEntity parent) {
        IndicatorEntity entity = new IndicatorEntity();
        copyToEntity(domain, parent, entity);
        return entity;
    }

    public void copyToEntity(Indicator domain, IndicatorEntity parent, IndicatorEntity entity) {
        entity.setCode(domain.code());
        entity.setName(domain.name());
        entity.setIndicatorGroupCode(domain.groupCode());
        entity.setParent(parent);
        entity.setLevel(domain.level());
        entity.setSortOrder(domain.sortOrder());
        entity.setSection(domain.section());
    }
}
