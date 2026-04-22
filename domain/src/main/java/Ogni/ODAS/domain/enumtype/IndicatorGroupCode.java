package Ogni.ODAS.domain.enumtype;

import lombok.Getter;

@Getter
public enum IndicatorGroupCode {
    INCOME("Доходы"),
    OUTCOME("Расходы"),
    CREDIT("Кредиты"),
    FIN_SOURCE("Источники финансирования"),
    OTHER("Другое")
    ;

    private final String label;

    IndicatorGroupCode(String label) {
        this.label = label;
    }
}
