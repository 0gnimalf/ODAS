package Ogni.ODAS.application.command;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

import java.util.Objects;

public record SyncIndicatorsCommand(
        IndicatorGroupCode groupCode,
        Integer year
) {
    public SyncIndicatorsCommand {
        Objects.requireNonNull(year, "year must not be null");
    }
}
