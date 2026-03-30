package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import Ogni.ODAS.iminfin.util.IminfinTextNormalizer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class IminfinFederalDistrictResolver {

    private static final Map<String, FederalDistrictCode> DISTRICT_BY_REGION_NAME = Map.<String, FederalDistrictCode>ofEntries(
            Map.entry(norm("Белгородская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Брянская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Владимирская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Воронежская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Ивановская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Калужская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Костромская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Курская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Липецкая область"), FederalDistrictCode.CFO),
            Map.entry(norm("Московская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Орловская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Рязанская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Смоленская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Тамбовская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Тверская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Тульская область"), FederalDistrictCode.CFO),
            Map.entry(norm("Ярославская область"), FederalDistrictCode.CFO),
            Map.entry(norm("г. Москва"), FederalDistrictCode.CFO),
            Map.entry(norm("Республика Карелия"), FederalDistrictCode.SZFO),
            Map.entry(norm("Республика Коми"), FederalDistrictCode.SZFO),
            Map.entry(norm("Архангельская область"), FederalDistrictCode.SZFO),
            Map.entry(norm("Вологодская область"), FederalDistrictCode.SZFO),
            Map.entry(norm("Калининградская область"), FederalDistrictCode.SZFO),
            Map.entry(norm("Ленинградская область"), FederalDistrictCode.SZFO),
            Map.entry(norm("Мурманская область"), FederalDistrictCode.SZFO),
            Map.entry(norm("Новгородская область"), FederalDistrictCode.SZFO),
            Map.entry(norm("Псковская область"), FederalDistrictCode.SZFO),
            Map.entry(norm("г. Санкт-Петербург"), FederalDistrictCode.SZFO),
            Map.entry(norm("Ненецкий автономный округ"), FederalDistrictCode.SZFO),
            Map.entry(norm("Республика Адыгея"), FederalDistrictCode.YFO),
            Map.entry(norm("Астраханская область"), FederalDistrictCode.YFO),
            Map.entry(norm("Волгоградская область"), FederalDistrictCode.YFO),
            Map.entry(norm("Республика Калмыкия"), FederalDistrictCode.YFO),
            Map.entry(norm("Краснодарский край"), FederalDistrictCode.YFO),
            Map.entry(norm("Республика Крым"), FederalDistrictCode.YFO),
            Map.entry(norm("Ростовская область"), FederalDistrictCode.YFO),
            Map.entry(norm("г. Севастополь"), FederalDistrictCode.YFO),
            Map.entry(norm("Республика Дагестан"), FederalDistrictCode.SKFO),
            Map.entry(norm("Республика Ингушетия"), FederalDistrictCode.SKFO),
            Map.entry(norm("Кабардино-Балкарская Республика"), FederalDistrictCode.SKFO),
            Map.entry(norm("Карачаево-Черкесская Республика"), FederalDistrictCode.SKFO),
            Map.entry(norm("Республика Северная Осетия-Алания"), FederalDistrictCode.SKFO),
            Map.entry(norm("Чеченская Республика"), FederalDistrictCode.SKFO),
            Map.entry(norm("Ставропольский край"), FederalDistrictCode.SKFO),
            Map.entry(norm("Республика Башкортостан"), FederalDistrictCode.PFO),
            Map.entry(norm("Республика Марий Эл"), FederalDistrictCode.PFO),
            Map.entry(norm("Республика Мордовия"), FederalDistrictCode.PFO),
            Map.entry(norm("Республика Татарстан"), FederalDistrictCode.PFO),
            Map.entry(norm("Удмуртская Республика"), FederalDistrictCode.PFO),
            Map.entry(norm("Чувашская Республика"), FederalDistrictCode.PFO),
            Map.entry(norm("Пермский край"), FederalDistrictCode.PFO),
            Map.entry(norm("Кировская область"), FederalDistrictCode.PFO),
            Map.entry(norm("Нижегородская область"), FederalDistrictCode.PFO),
            Map.entry(norm("Оренбургская область"), FederalDistrictCode.PFO),
            Map.entry(norm("Пензенская область"), FederalDistrictCode.PFO),
            Map.entry(norm("Самарская область"), FederalDistrictCode.PFO),
            Map.entry(norm("Саратовская область"), FederalDistrictCode.PFO),
            Map.entry(norm("Ульяновская область"), FederalDistrictCode.PFO),
            Map.entry(norm("Курганская область"), FederalDistrictCode.UFO),
            Map.entry(norm("Свердловская область"), FederalDistrictCode.UFO),
            Map.entry(norm("Тюменская область"), FederalDistrictCode.UFO),
            Map.entry(norm("Челябинская область"), FederalDistrictCode.UFO),
            Map.entry(norm("Ханты-Мансийский автономный округ"), FederalDistrictCode.UFO),
            Map.entry(norm("Ямало-Ненецкий автономный округ"), FederalDistrictCode.UFO),
            Map.entry(norm("Республика Алтай"), FederalDistrictCode.SFO),
            Map.entry(norm("Республика Тыва"), FederalDistrictCode.SFO),
            Map.entry(norm("Республика Хакасия"), FederalDistrictCode.SFO),
            Map.entry(norm("Алтайский край"), FederalDistrictCode.SFO),
            Map.entry(norm("Красноярский край"), FederalDistrictCode.SFO),
            Map.entry(norm("Иркутская область"), FederalDistrictCode.SFO),
            Map.entry(norm("Кемеровская область"), FederalDistrictCode.SFO),
            Map.entry(norm("Новосибирская область"), FederalDistrictCode.SFO),
            Map.entry(norm("Омская область"), FederalDistrictCode.SFO),
            Map.entry(norm("Томская область"), FederalDistrictCode.SFO),
            Map.entry(norm("Амурская область"), FederalDistrictCode.DFO),
            Map.entry(norm("Республика Бурятия"), FederalDistrictCode.DFO),
            Map.entry(norm("Республика Саха (Якутия)"), FederalDistrictCode.DFO),
            Map.entry(norm("Еврейская автономная область"), FederalDistrictCode.DFO),
            Map.entry(norm("Забайкальский край"), FederalDistrictCode.DFO),
            Map.entry(norm("Камчатский край"), FederalDistrictCode.DFO),
            Map.entry(norm("Магаданская область"), FederalDistrictCode.DFO),
            Map.entry(norm("Приморский край"), FederalDistrictCode.DFO),
            Map.entry(norm("Сахалинская область"), FederalDistrictCode.DFO),
            Map.entry(norm("Хабаровский край"), FederalDistrictCode.DFO),
            Map.entry(norm("Чукотский автономный округ"), FederalDistrictCode.DFO),
            Map.entry(norm("г. Байконур"), FederalDistrictCode.NONE),
            Map.entry(norm("Донецкая Народная Республика"), FederalDistrictCode.NONE),
            Map.entry(norm("Запорожская область"), FederalDistrictCode.NONE),
            Map.entry(norm("Луганская Народная Республика"), FederalDistrictCode.NONE),
            Map.entry(norm("Федеральная территория «Сириус»"), FederalDistrictCode.NONE),
            Map.entry(norm("Сириус"), FederalDistrictCode.NONE),
            Map.entry(norm("Херсонская область"), FederalDistrictCode.NONE),
            Map.entry(norm("Российская Федерация"), FederalDistrictCode.NONE)
    );

    public FederalDistrictCode resolve(String regionName) {
        return DISTRICT_BY_REGION_NAME.getOrDefault(norm(regionName), FederalDistrictCode.NONE);
    }

    private static String norm(String value) {
        return IminfinTextNormalizer.normalize(value);
    }
}
