package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.IndicatorRepositoryPort;
import Ogni.ODAS.db.entity.IndicatorEntity;
import Ogni.ODAS.db.entity.IndicatorYearEntryEntity;
import Ogni.ODAS.db.mapper.IndicatorEntityMapper;
import Ogni.ODAS.db.mapper.IndicatorYearEntryEntityMapper;
import Ogni.ODAS.db.repository.JpaIndicatorRepository;
import Ogni.ODAS.db.repository.JpaIndicatorYearEntryRepository;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Indicator;
import Ogni.ODAS.domain.model.IndicatorYearEntry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class IndicatorRepositoryAdapter implements IndicatorRepositoryPort {

    private final JpaIndicatorRepository repository;
    private final JpaIndicatorYearEntryRepository yearEntryRepository;
    private final IndicatorEntityMapper mapper;
    private final IndicatorYearEntryEntityMapper yearEntryMapper;

    public IndicatorRepositoryAdapter(
            JpaIndicatorRepository repository,
            JpaIndicatorYearEntryRepository yearEntryRepository,
            IndicatorEntityMapper mapper,
            IndicatorYearEntryEntityMapper yearEntryMapper
    ) {
        this.repository = repository;
        this.yearEntryRepository = yearEntryRepository;
        this.mapper = mapper;
        this.yearEntryMapper = yearEntryMapper;
    }

    @Override
    @Transactional
    public Indicator save(Indicator indicator) {
        IndicatorEntity entity = repository.findByCodeAndIndicatorGroupCode(indicator.code(), indicator.groupCode())
                .orElseGet(() -> indicator.id() == null
                        ? mapper.toNewEntity(indicator)
                        : repository.findById(indicator.id()).orElseGet(() -> mapper.toNewEntity(indicator)));

        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Indicator> findByCodeAndGroupCode(String code, IndicatorGroupCode groupCode) {
        return repository.findByCodeAndIndicatorGroupCode(code, groupCode)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void replaceYearEntries(IndicatorGroupCode groupCode, Integer year, List<IndicatorYearEntry> entries) {
        List<IndicatorYearEntryEntity> existing = yearEntryRepository.findAllByIndicatorIndicatorGroupCodeAndYearValue(groupCode, year);
        if (!existing.isEmpty()) {
            yearEntryRepository.deleteAllInBatch(existing);
        }
        if (entries.isEmpty()) {
            return;
        }

        Map<Long, IndicatorEntity> indicatorsById = loadIndicators(entries);
        List<IndicatorYearEntryEntity> entities = entries.stream()
                .map(entry -> yearEntryMapper.toNewEntity(
                        entry,
                        requireIndicator(indicatorsById, entry.indicatorId()),
                        entry.parentIndicatorId() == null ? null : requireIndicator(indicatorsById, entry.parentIndicatorId())
                ))
                .toList();

        yearEntryRepository.saveAll(entities);
    }

    @Override
    public List<IndicatorYearEntry> findAllByGroupCodeAndYear(IndicatorGroupCode groupCode, Integer year) {
        return yearEntryRepository.findAllByIndicatorIndicatorGroupCodeAndYearValueOrderBySortOrderAscNameAsc(groupCode, year).stream()
                .map(yearEntryMapper::toDomain)
                .toList();
    }

    private Map<Long, IndicatorEntity> loadIndicators(List<IndicatorYearEntry> entries) {
        Set<Long> ids = entries.stream()
                .flatMap(entry -> entry.parentIndicatorId() == null
                        ? java.util.stream.Stream.of(entry.indicatorId())
                        : java.util.stream.Stream.of(entry.indicatorId(), entry.parentIndicatorId()))
                .collect(Collectors.toSet());

        return repository.findAllByIdIn(ids).stream()
                .collect(Collectors.toMap(
                        IndicatorEntity::getId,
                        entity -> entity,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private IndicatorEntity requireIndicator(Map<Long, IndicatorEntity> indicatorsById, Long indicatorId) {
        IndicatorEntity entity = indicatorsById.get(indicatorId);
        if (entity == null) {
            throw new IllegalStateException("Indicator entity not found: " + indicatorId);
        }
        return entity;
    }
}
