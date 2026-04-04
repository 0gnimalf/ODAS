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
                entity.getIndicatorGroupCode()
        );
    }

    public IndicatorEntity toNewEntity(Indicator domain) {
        IndicatorEntity entity = new IndicatorEntity();
        copyToEntity(domain, entity);
        return entity;
    }

    public void copyToEntity(Indicator domain, IndicatorEntity entity) {
        entity.setCode(domain.code());
        entity.setIndicatorGroupCode(domain.groupCode());
    }
}
