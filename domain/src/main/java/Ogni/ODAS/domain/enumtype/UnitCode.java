package Ogni.ODAS.domain.enumtype;

import lombok.Getter;

@Getter
public enum UnitCode {
    RUB("руб."),
    THOUSAND_RUB("тыс. руб."),
    MILLION_RUB("млн. руб."),
    BILLION_RUB("млрд. руб."),
    PERCENT("%"),
    PERSON("чел.")
    ;

    private final String label;

    UnitCode(String label) {
        this.label = label;
    }
}
