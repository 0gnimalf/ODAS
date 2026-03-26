package Ogni.ODAS.domain.enumtype;

public enum ObservationValueKind {
    // доходы | расходы | кредиты (сравнение по регионам) | источники финансирования (детально)
    PLAN(
            "План",
            UnitCode.RUB, ObservationValueType.ABSOLUTE),
    REFINED_PLAN_CONSOLIDATED_SUBJECT_BUDGET(
            "Уточненный план; конс.бюджет субъекта РФ",
            UnitCode.RUB, ObservationValueType.ABSOLUTE),
    REFINED_PLAN_SUBJECT_BUDGET(
            "Уточненный план; в т.ч.бюджет субъекта",
            UnitCode.RUB, ObservationValueType.ABSOLUTE),
    REFINED_PLAN_RATE_TO_PREVIOUS_PERIOD_EXECUTION(
            "Темп уточненного плана к исполнению пред.года,%",
            UnitCode.PERCENT, ObservationValueType.RATIO),
    ACTUAL_CONSOLIDATED_SUBJECT_BUDGET(
            "Исполнено;  конс.бюджет субъекта РФ",
            UnitCode.RUB, ObservationValueType.ABSOLUTE),
    ACTUAL_SUBJECT_BUDGET(
            "Исполнено; в т.ч.бюджет субъекта",
            UnitCode.RUB, ObservationValueType.ABSOLUTE),
    // (доходы | расходы) сравнение по регионам
    ACTUAL_RATE_TO_PREVIOUS_PERIOD(
            "Исполнено; Темп к соотв.периоду пред.года,%",
            UnitCode.PERCENT, ObservationValueType.RATIO),
    // (доходы | расходы) детально
    GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_SUBJECT(
            "Темп роста к соотв.периоду прошл.года,%; по субъекту",
            UnitCode.PERCENT, ObservationValueType.RATIO),
    GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_FEDERAL_DISTRICT(
            "Темп роста к соотв.периоду прошл.года,%; по ФО",
            UnitCode.PERCENT, ObservationValueType.RATIO),
    GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_RUSSIAN_FEDERATION(
            "Темп роста к соотв.периоду прошл.года,%; по РФ", UnitCode.PERCENT,
            ObservationValueType.RATIO)
    ;

    private final String label;
    private final UnitCode unitCode;
    private final ObservationValueType observationValueType;
    ObservationValueKind(String label,UnitCode unitCode, ObservationValueType observationValueType) {
        this.label = label;
        this.unitCode = unitCode;
        this.observationValueType = observationValueType;
    }
}
