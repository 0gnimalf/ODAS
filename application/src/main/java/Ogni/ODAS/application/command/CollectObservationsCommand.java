package Ogni.ODAS.application.command;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

import java.util.Objects;

public record CollectObservationsCommand(
        IndicatorGroupCode groupCode,
        Integer year,
        Integer month
        // сюда добавить список регионов
) {
    public CollectObservationsCommand {
        Objects.requireNonNull(groupCode, "groupCode must not be null");
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(month, "month must not be null");
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
    }
}
