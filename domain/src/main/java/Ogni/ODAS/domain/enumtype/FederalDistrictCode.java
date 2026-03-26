package Ogni.ODAS.domain.enumtype;

public enum FederalDistrictCode {
    CFO(
            "Центральный", "Центральный федеральный округ", "ЦФО"),
    SZFO(
            "Северо-Западный", "Северо-Западный федеральный округ", "С-ЗФО"),
    YFO(
            "Южный", "Южный федеральный округ", "ЮФО"),
    SKFO(
            "Северо-Кавказский", "Северо-Кавказский федеральный округ", "С-КФО"),
    PFO(
            "Приволжский", "Приволжский федеральный округ", "ПФО"),
    UFO(
            "Уральский", "Уральский федеральный округ", "УФО"),
    SFO(
            "Сибирский", "Сибирский федеральный округ", "СФО"),
    DFO(
            "Дальневосточный", "Дальневосточный федеральный округ", "ДФО"),
    NONE(
            "Нет", "Нет федерального округа", "нет фо")
    ;

    private final String name;
    private final String fullName;
    private final String shortName;
    private FederalDistrictCode(String name, String fullName, String shortName) {
        this.name = name;
        this.fullName = fullName;
        this.shortName = shortName;
    }

}
