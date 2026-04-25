package Ogni.ODAS.analysis.service;

import Ogni.ODAS.application.dto.analysis.MatrixCellDto;
import Ogni.ODAS.application.dto.analysis.RegionIndicatorMatrixColumnDto;
import Ogni.ODAS.application.dto.analysis.RegionIndicatorMatrixResultDto;
import Ogni.ODAS.application.dto.analysis.RegionIndicatorMatrixRowDto;
import Ogni.ODAS.application.dto.read.IndicatorEntryReadDto;
import Ogni.ODAS.application.dto.read.ObservationReadDto;
import Ogni.ODAS.application.dto.read.RegionReadDto;
import Ogni.ODAS.application.port.out.analysis.RegionIndicatorMatrixPort;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.UnitCode;

import java.util.*;

public class RegionIndicatorMatrixCalculator implements RegionIndicatorMatrixPort {

    @Override
    public RegionIndicatorMatrixResultDto calculate(
            IndicatorGroupCode groupCode,
            int year,
            int month,
            ObservationValueKind valueKind,
            UnitCode unitCode,
            List<RegionReadDto> rows,
            List<IndicatorEntryReadDto> columns,
            List<ObservationReadDto> observations
    ) {
        Map<CellKey, ObservationReadDto> observationByCell = new LinkedHashMap<>();
        for (ObservationReadDto observation : observations) {
            observationByCell.putIfAbsent(new CellKey(observation.regionId(), observation.indicatorYearEntryId()), observation);
        }

        List<RegionIndicatorMatrixRowDto> rowDtos = rows.stream()
                .sorted(Comparator.comparing(RegionReadDto::name))
                .map(region -> new RegionIndicatorMatrixRowDto(region.id(), region.name()))
                .toList();
        List<RegionIndicatorMatrixColumnDto> columnDtos = columns.stream()
                .map(entry -> new RegionIndicatorMatrixColumnDto(entry.id(), entry.indicatorId(), entry.name(), entry.level(), entry.sortOrder()))
                .toList();

        List<MatrixCellDto> cells = new ArrayList<>(rowDtos.size() * columnDtos.size());
        for (RegionIndicatorMatrixRowDto row : rowDtos) {
            for (RegionIndicatorMatrixColumnDto column : columnDtos) {
                ObservationReadDto observation = observationByCell.get(new CellKey(row.regionId(), column.indicatorYearEntryId()));
                cells.add(new MatrixCellDto(
                        row.regionId(),
                        column.indicatorYearEntryId(),
                        observation == null ? null : observation.value(),
                        observation == null
                ));
            }
        }

        return new RegionIndicatorMatrixResultDto(
                groupCode,
                year,
                month,
                valueKind,
                valueKind.getLabel(),
                unitCode,
                unitCode.getLabel(),
                rowDtos,
                columnDtos,
                List.copyOf(cells)
        );
    }

    private record CellKey(Long regionId, Long indicatorYearEntryId) {
    }
}
