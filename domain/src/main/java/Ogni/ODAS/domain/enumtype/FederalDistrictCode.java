package Ogni.ODAS.domain.enumtype;

import lombok.Getter;

@Getter
public enum FederalDistrictCode {
    CFO(
            "Центральный", "Центральный федеральный округ", "ЦФО"),
    SZFO(
            "Северо-Западный", "Северо-Западный федеральный округ", "СЗФО"),
    YFO(
            "Южный", "Южный федеральный округ", "ЮФО"),
    SKFO(
            "Северо-Кавказский", "Северо-Кавказский федеральный округ", "СКФО"),
    PFO(
            "Приволжский", "Приволжский федеральный округ", "ПФО"),
    UFO(
            "Уральский", "Уральский федеральный округ", "УФО"),
    SFO(
            "Сибирский", "Сибирский федеральный округ", "СФО"),
    DFO(
            "Дальневосточный", "Дальневосточный федеральный округ", "ДФО"),
    NONE(
            "Нет", "Нет федерального округа", "—")
    ;

    private final String name;
    private final String fullName;
    private final String shortName;

    FederalDistrictCode(String name, String fullName, String shortName) {
        this.name = name;
        this.fullName = fullName;
        this.shortName = shortName;
    }
}
