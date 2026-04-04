package Ogni.ODAS.application.command;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

public record SyncIndicatorsCommand(
        IndicatorGroupCode groupCode,
        Integer year
) {
}
