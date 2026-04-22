package Ogni.ODAS.application.port.in;

import Ogni.ODAS.application.command.ReadObservationsCommand;
import Ogni.ODAS.application.dto.read.IndicatorGroupReadDto;
import Ogni.ODAS.application.dto.read.IndicatorTreeNodeReadDto;
import Ogni.ODAS.application.dto.read.ObservationReadResultDto;
import Ogni.ODAS.application.dto.read.RegionReadDto;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

import java.util.List;

public interface StoredDataReadUseCase {

    List<IndicatorGroupReadDto> getIndicatorGroups();

    List<RegionReadDto> getRegions();

    List<IndicatorTreeNodeReadDto> getIndicatorTree(IndicatorGroupCode groupCode, int year);

    ObservationReadResultDto getObservations(ReadObservationsCommand command);
}
