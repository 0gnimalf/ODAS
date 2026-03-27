package Ogni.ODAS.domain.value;

import Ogni.ODAS.domain.enumtype.UnitCode;

import java.math.BigDecimal;
import java.util.Objects;

public record MoneyValue(
        BigDecimal amount,
        UnitCode unit
) {
    public MoneyValue {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
    }
}
