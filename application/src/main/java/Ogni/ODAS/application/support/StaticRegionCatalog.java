package Ogni.ODAS.application.support;

import Ogni.ODAS.domain.enumtype.FederalDistrictCode;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class StaticRegionCatalog {
    private static final List<Entry> ENTRIES = List.of(
            new Entry("Российская Федерация", FederalDistrictCode.NONE),

            new Entry("Центральный федеральный округ", FederalDistrictCode.CFO),
            new Entry("Белгородская область", FederalDistrictCode.CFO),
            new Entry("Брянская область", FederalDistrictCode.CFO),
            new Entry("Владимирская область", FederalDistrictCode.CFO),
            new Entry("Воронежская область", FederalDistrictCode.CFO),
            new Entry("Ивановская область", FederalDistrictCode.CFO),
            new Entry("Калужская область", FederalDistrictCode.CFO),
            new Entry("Костромская область", FederalDistrictCode.CFO),
            new Entry("Курская область", FederalDistrictCode.CFO),
            new Entry("Липецкая область", FederalDistrictCode.CFO),
            new Entry("Московская область", FederalDistrictCode.CFO),
            new Entry("Орловская область", FederalDistrictCode.CFO),
            new Entry("Рязанская область", FederalDistrictCode.CFO),
            new Entry("Смоленская область", FederalDistrictCode.CFO),
            new Entry("Тамбовская область", FederalDistrictCode.CFO),
            new Entry("Тверская область", FederalDistrictCode.CFO),
            new Entry("Тульская область", FederalDistrictCode.CFO),
            new Entry("Ярославская область", FederalDistrictCode.CFO),
            new Entry("г. Москва", FederalDistrictCode.CFO),

            new Entry("Северо-Западный федеральный округ", FederalDistrictCode.SZFO),
            new Entry("Республика Карелия", FederalDistrictCode.SZFO),
            new Entry("Республика Коми", FederalDistrictCode.SZFO),
            new Entry("Архангельская область", FederalDistrictCode.SZFO),
            new Entry("Вологодская область", FederalDistrictCode.SZFO),
            new Entry("Калининградская область", FederalDistrictCode.SZFO),
            new Entry("Ленинградская область", FederalDistrictCode.SZFO),
            new Entry("Мурманская область", FederalDistrictCode.SZFO),
            new Entry("Новгородская область", FederalDistrictCode.SZFO),
            new Entry("Псковская область", FederalDistrictCode.SZFO),
            new Entry("г. Санкт-Петербург", FederalDistrictCode.SZFO),
            new Entry("Ненецкий автономный округ", FederalDistrictCode.SZFO),

            new Entry("Приволжский федеральный округ", FederalDistrictCode.PFO),
            new Entry("Республика Башкортостан", FederalDistrictCode.PFO),
            new Entry("Республика Марий Эл", FederalDistrictCode.PFO),
            new Entry("Республика Мордовия", FederalDistrictCode.PFO),
            new Entry("Республика Татарстан (Татарстан)", FederalDistrictCode.PFO), // Республика Татарстан
            new Entry("Удмуртская Республика", FederalDistrictCode.PFO),
            new Entry("Чувашская Республика - Чувашия", FederalDistrictCode.PFO), // Чувашская Республика
            new Entry("Кировская область", FederalDistrictCode.PFO),
            new Entry("Нижегородская область", FederalDistrictCode.PFO),
            new Entry("Оренбургская область", FederalDistrictCode.PFO),
            new Entry("Пензенская область", FederalDistrictCode.PFO),
            new Entry("Пермский край", FederalDistrictCode.PFO), // Пермская область
            new Entry("Самарская область", FederalDistrictCode.PFO),
            new Entry("Саратовская область", FederalDistrictCode.PFO),
            new Entry("Ульяновская область", FederalDistrictCode.PFO),
            // Коми-Пермяцкий автономный округ

            new Entry("Уральский федеральный округ", FederalDistrictCode.UFO),
            new Entry("Курганская область", FederalDistrictCode.UFO),
            new Entry("Свердловская область", FederalDistrictCode.UFO),
            new Entry("Тюменская область", FederalDistrictCode.UFO),
            new Entry("Челябинская область", FederalDistrictCode.UFO),
            new Entry("Ханты-Мансийский автономный округ - Югра", FederalDistrictCode.UFO), // Ханты-Мансийский автономный округ
            new Entry("Ямало-Ненецкий автономный округ", FederalDistrictCode.UFO),

            new Entry("Северо-Кавказский федеральный округ", FederalDistrictCode.SKFO),
            new Entry("Республика Дагестан", FederalDistrictCode.SKFO),
            new Entry("Республика Ингушетия", FederalDistrictCode.SKFO),
            new Entry("Кабардино-Балкарская Республика", FederalDistrictCode.SKFO),
            new Entry("Карачаево-Черкесская Республика", FederalDistrictCode.SKFO),
            new Entry("Республика Северная Осетия - Алания", FederalDistrictCode.SKFO),
            new Entry("Чеченская Республика", FederalDistrictCode.SKFO),
            new Entry("Ставропольский край", FederalDistrictCode.SKFO),

            new Entry("Южный федеральный округ", FederalDistrictCode.YFO),
            new Entry("Республика Адыгея (Адыгея)", FederalDistrictCode.YFO),
            new Entry("Республика Калмыкия", FederalDistrictCode.YFO),
            new Entry("Республика Крым", FederalDistrictCode.YFO),
            new Entry("Краснодарский край", FederalDistrictCode.YFO),
            new Entry("Астраханская область", FederalDistrictCode.YFO),
            new Entry("Волгоградская область", FederalDistrictCode.YFO),
            new Entry("Ростовская область", FederalDistrictCode.YFO),
            new Entry("г. Севастополь", FederalDistrictCode.YFO),

            new Entry("Сибирский федеральный округ", FederalDistrictCode.SFO),
            new Entry("Республика Алтай", FederalDistrictCode.SFO),
            new Entry("Республика Тыва", FederalDistrictCode.SFO),
            new Entry("Республика Хакасия", FederalDistrictCode.SFO),
            new Entry("Алтайский край", FederalDistrictCode.SFO),
            new Entry("Красноярский край", FederalDistrictCode.SFO),
            new Entry("Иркутская область", FederalDistrictCode.SFO),
            new Entry("Кемеровская область - Кузбасс", FederalDistrictCode.SFO),  // Кемеровская область
            new Entry("Новосибирская область", FederalDistrictCode.SFO),
            new Entry("Омская область", FederalDistrictCode.SFO),
            new Entry("Томская область", FederalDistrictCode.SFO),

            new Entry("Дальневосточный федеральный округ", FederalDistrictCode.DFO),
            new Entry("Республика Бурятия", FederalDistrictCode.DFO),
            new Entry("Республика Саха (Якутия)", FederalDistrictCode.DFO),
            new Entry("Забайкальский край", FederalDistrictCode.DFO),
            new Entry("Камчатский край", FederalDistrictCode.DFO),
            new Entry("Приморский край", FederalDistrictCode.DFO),
            new Entry("Хабаровский край", FederalDistrictCode.DFO),
            new Entry("Амурская область", FederalDistrictCode.DFO),
            new Entry("Магаданская область", FederalDistrictCode.DFO),
            new Entry("Сахалинская область", FederalDistrictCode.DFO),
            new Entry("Еврейская автономная область", FederalDistrictCode.DFO),
            new Entry("Чукотский автономный округ", FederalDistrictCode.DFO),

            new Entry("Нет федерального округа", FederalDistrictCode.NONE),
            new Entry("г. Байконур", FederalDistrictCode.NONE),
            new Entry("Сириус", FederalDistrictCode.NONE),

            new Entry("Донецкая Народная Республика", FederalDistrictCode.NONE),
            new Entry("Запорожская область", FederalDistrictCode.NONE),
            new Entry("Луганская Народная Республика", FederalDistrictCode.NONE),
            new Entry("Херсонская область", FederalDistrictCode.NONE)
    );

    private static final Map<String, Entry> BY_NORMALIZED_NAME = ENTRIES.stream()
            .collect(Collectors.toUnmodifiableMap(
                    entry -> TextNormalizer.normalize(entry.name()),
                    entry -> entry,
                    (left, right) -> left
            ));

    private StaticRegionCatalog() {
    }

    public static Optional<Entry> findByName(String rawName) {
        return Optional.ofNullable(BY_NORMALIZED_NAME.get(TextNormalizer.normalize(rawName)));
    }

    public record Entry(String name, FederalDistrictCode federalDistrictCode) {
    }
}

