package Ogni.ODAS.application.service;

import Ogni.ODAS.application.command.ReadObservationsCommand;
import Ogni.ODAS.application.dto.read.*;
import Ogni.ODAS.application.port.in.StoredDataReadUseCase;
import Ogni.ODAS.application.port.out.persistence.PeriodPersistencePort;
import Ogni.ODAS.application.port.out.persistence.StoredDataQueryPort;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.PeriodType;
import Ogni.ODAS.domain.model.Period;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StoredDataReadService implements StoredDataReadUseCase {

    private final PeriodPersistencePort periodPersistence;
    private final StoredDataQueryPort storedDataQuery;

    public StoredDataReadService(PeriodPersistencePort periodPersistence, StoredDataQueryPort storedDataQuery) {
        this.periodPersistence = Objects.requireNonNull(periodPersistence);
        this.storedDataQuery = Objects.requireNonNull(storedDataQuery);
    }

    private static int compareEntries(IndicatorEntryReadDto left, IndicatorEntryReadDto right) {
        int sort = Comparator.nullsLast(Integer::compareTo).compare(left.sortOrder(), right.sortOrder());
        if (sort != 0) {
            return sort;
        }
        return Comparator.nullsLast(String::compareTo).compare(left.name(), right.name());
    }

    private static List<Long> safeList(List<Long> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).distinct().toList();
    }

    @Override
    public List<IndicatorGroupReadDto> getIndicatorGroups() {
        return Arrays.stream(IndicatorGroupCode.values())
                .map(code -> new IndicatorGroupReadDto(code, code.getLabel()))
                .collect(Collectors.toList());
    }

    @Override
    public List<RegionReadDto> getRegions() {
        // по-другому
        return storedDataQuery.findRegions();
    }

    @Override
    public List<IndicatorTreeNodeReadDto> getIndicatorTree(IndicatorGroupCode groupCode, int year) {
        Objects.requireNonNull(groupCode, "groupCode must not be null");
        Optional<Period> yearPeriod = periodPersistence.findByIdentity(PeriodType.YEAR, year, null, null);
        return yearPeriod.map(period ->
                buildTree(
                        // по-другому
                        storedDataQuery.findIndicatorEntries(groupCode, period.id()))
        ).orElseGet(List::of);
    }

    @Override
    public ObservationReadResultDto getObservations(ReadObservationsCommand command) {
        validate(command);
        Optional<Period> period = periodPersistence.findByIdentity(PeriodType.MONTH, command.year(), command.month(), null);
        if (period.isEmpty()) {
            return new ObservationReadResultDto(command.groupCode(), command.year(), command.month(), null,
                    safeList(command.regionIds()), safeList(command.indicatorYearEntryIds()), 0, List.of());
        }

        Collection<Long> indicatorIds = resolveIndicatorFilter(command);
        List<ObservationReadDto> observations = storedDataQuery.findObservations(
                command.groupCode(),
                period.get().id(),
                command.regionIds(),
                indicatorIds,
                command.valueKinds()
        );
        return new ObservationReadResultDto(command.groupCode(), command.year(), command.month(), period.get().id(),
                safeList(command.regionIds()), new ArrayList<>(indicatorIds), observations.size(), observations);
    }

    private void validate(ReadObservationsCommand command) {
        Objects.requireNonNull(command, "Read observations command must not be null");
        Objects.requireNonNull(command.groupCode(), "groupCode must not be null");
        Objects.requireNonNull(command.year(), "year must not be null");
        Objects.requireNonNull(command.month(), "month must not be null");
        if (command.month() < 1 || command.month() > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
    }

    private Collection<Long> resolveIndicatorFilter(ReadObservationsCommand command) {
        List<Long> requested = safeList(command.indicatorYearEntryIds());
        if (requested.isEmpty() || !command.includeChildren()) {
            return requested;
        }

        Optional<Period> yearPeriod = periodPersistence.findByIdentity(PeriodType.YEAR, command.year(), null, null);
        if (yearPeriod.isEmpty()) {
            return requested;
        }

        List<IndicatorEntryReadDto> allEntries = storedDataQuery.findIndicatorEntries(command.groupCode(), yearPeriod.get().id());
        Map<Long, List<Long>> childrenByParent = allEntries.stream()
                .filter(entry -> entry.parentIndicatorYearEntryId() != null)
                .collect(Collectors.groupingBy(
                        IndicatorEntryReadDto::parentIndicatorYearEntryId,
                        LinkedHashMap::new,
                        Collectors.mapping(IndicatorEntryReadDto::id, Collectors.toList())
                ));

        LinkedHashSet<Long> result = new LinkedHashSet<>(requested);
        Deque<Long> queue = new ArrayDeque<>(requested);
        while (!queue.isEmpty()) {
            Long current = queue.removeFirst();
            for (Long child : childrenByParent.getOrDefault(current, List.of())) {
                if (result.add(child)) {
                    queue.addLast(child);
                }
            }
        }
        return result;
    }

    private List<IndicatorTreeNodeReadDto> buildTree(List<IndicatorEntryReadDto> entries) {
        Map<Long, List<IndicatorEntryReadDto>> childrenByParent = entries.stream()
                .filter(entry -> entry.parentIndicatorYearEntryId() != null)
                .collect(Collectors.groupingBy(
                        IndicatorEntryReadDto::parentIndicatorYearEntryId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<Long, IndicatorEntryReadDto> byId = entries.stream()
                .collect(Collectors.toMap(IndicatorEntryReadDto::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        return entries.stream()
                .filter(entry -> entry.parentIndicatorYearEntryId() == null || !byId.containsKey(entry.parentIndicatorYearEntryId()))
                .sorted(StoredDataReadService::compareEntries)
                .map(root -> toNode(root, childrenByParent))
                .toList();
    }

    private IndicatorTreeNodeReadDto toNode(IndicatorEntryReadDto entry, Map<Long, List<IndicatorEntryReadDto>> childrenByParent) {
        List<IndicatorTreeNodeReadDto> children = childrenByParent.getOrDefault(entry.id(), List.of()).stream()
                .sorted(StoredDataReadService::compareEntries)
                .map(child -> toNode(child, childrenByParent))
                .toList();
        return new IndicatorTreeNodeReadDto(entry.id(), entry.indicatorId(), entry.name(), entry.groupCode(),
                entry.parentIndicatorYearEntryId(), entry.level(), entry.sortOrder(), entry.hasChildren(), children);
    }
}
