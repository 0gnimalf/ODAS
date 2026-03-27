package Ogni.ODAS.iminfin.profile;

import Ogni.ODAS.application.iminfin.model.IminfinParameterCatalogItem;
import Ogni.ODAS.application.iminfin.model.IminfinParameterRole;
import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;

import java.util.List;

public final class IminfinProfiles {

    private IminfinProfiles() {
    }

    public static List<IminfinReportProfile> all() {
        return List.of(
                new IminfinReportProfile(
                        IminfinReportProfileKey.INCOMES_COMPARE,
                        "https://www.iminfin.ru/areas-of-analysis/budget/finansoviy-pasport-subjecta-rf/dokhody-sravnenie-po-regionam",
                        "PassportFK_001_003_incomesGridData",
                        List.of("ds_FK_Passport_MONTH_Periods"),
                        List.of(
                                parameter(IminfinReportProfileKey.INCOMES_COMPARE, "paramPeriod", IminfinParameterRole.BUSINESS, "DATE", true, null),
                                parameter(IminfinReportProfileKey.INCOMES_COMPARE, "PassportFK_001_003_paramIncomes", IminfinParameterRole.BUSINESS, "STRING", true, null),
                                parameter(IminfinReportProfileKey.INCOMES_COMPARE, "PassportFK_001_paramMeasure", IminfinParameterRole.PROFILE_DEFAULT, "STRING", true, "abs"),
                                parameter(IminfinReportProfileKey.INCOMES_COMPARE, "territoryMap", IminfinParameterRole.SERVICE, "STRING", false, null)
                        )
                ),
                new IminfinReportProfile(
                        IminfinReportProfileKey.INCOMES_DETAIL,
                        "https://www.iminfin.ru/areas-of-analysis/budget/finansoviy-pasport-subjecta-rf/dokhody-detalno",
                        "PassportFK_002_001_incomesDataAfter01052019",
                        List.of("TerritoryOnlySubject", "ds_FK_Passport_MONTH_Periods", "periodHelperData"),
                        List.of(
                                parameter(IminfinReportProfileKey.INCOMES_DETAIL, "territory", IminfinParameterRole.BUSINESS, "STRING", true, null),
                                parameter(IminfinReportProfileKey.INCOMES_DETAIL, "paramPeriod", IminfinParameterRole.BUSINESS, "DATE", true, null),
                                parameter(IminfinReportProfileKey.INCOMES_DETAIL, "TERRITORIES_paramPeriod", IminfinParameterRole.REFERENCE_SELECTOR, "DATE", false, null),
                                parameter(IminfinReportProfileKey.INCOMES_DETAIL, "helperPeriod", IminfinParameterRole.SERVICE, "STRING", false, null)
                        )
                ),
                new IminfinReportProfile(
                        IminfinReportProfileKey.EXPENSES_COMPARE,
                        "https://www.iminfin.ru/areas-of-analysis/budget/finansoviy-pasport-subjecta-rf/raskhody-sravnenie-po-regionam",
                        "PassportFK_001_004_outcomesGridData",
                        List.of("ds_FK_Passport_MONTH_Periods"),
                        List.of(
                                parameter(IminfinReportProfileKey.EXPENSES_COMPARE, "paramPeriod", IminfinParameterRole.BUSINESS, "DATE", true, null),
                                parameter(IminfinReportProfileKey.EXPENSES_COMPARE, "PassportFK_001_004_paramOutcomes", IminfinParameterRole.BUSINESS, "STRING", true, null),
                                parameter(IminfinReportProfileKey.EXPENSES_COMPARE, "PassportFK_001_paramMeasure", IminfinParameterRole.PROFILE_DEFAULT, "STRING", true, "abs"),
                                parameter(IminfinReportProfileKey.EXPENSES_COMPARE, "territoryMap", IminfinParameterRole.SERVICE, "STRING", false, null)
                        )
                ),
                new IminfinReportProfile(
                        IminfinReportProfileKey.EXPENSES_DETAIL,
                        "https://www.iminfin.ru/areas-of-analysis/budget/finansoviy-pasport-subjecta-rf/raskhody-detalno",
                        "PassportFK_002_002_outcomesDataAfter01052019",
                        List.of("TerritoryOnlySubject", "PassportFK_002_002_outcomesTypesFix", "ds_FK_Passport_MONTH_Periods", "periodHelperData"),
                        List.of(
                                parameter(IminfinReportProfileKey.EXPENSES_DETAIL, "territory", IminfinParameterRole.BUSINESS, "STRING", true, null),
                                parameter(IminfinReportProfileKey.EXPENSES_DETAIL, "paramPeriod", IminfinParameterRole.BUSINESS, "DATE", true, null),
                                parameter(IminfinReportProfileKey.EXPENSES_DETAIL, "PassportFK_002_002_outcomesType", IminfinParameterRole.BUSINESS, "STRING", true, null),
                                parameter(IminfinReportProfileKey.EXPENSES_DETAIL, "TERRITORIES_paramPeriod", IminfinParameterRole.REFERENCE_SELECTOR, "DATE", false, null),
                                parameter(IminfinReportProfileKey.EXPENSES_DETAIL, "helperPeriod", IminfinParameterRole.SERVICE, "STRING", false, null)
                        )
                ),
                new IminfinReportProfile(
                        IminfinReportProfileKey.CREDITS_COMPARE,
                        "https://www.iminfin.ru/areas-of-analysis/budget/finansoviy-pasport-subjecta-rf/kredity-sravnenie-po-regionam",
                        "PassportFK_001_005_creditGridData",
                        List.of("ds_FK_Passport_MONTH_Periods"),
                        List.of(
                                parameter(IminfinReportProfileKey.CREDITS_COMPARE, "paramPeriod", IminfinParameterRole.BUSINESS, "DATE", true, null),
                                parameter(IminfinReportProfileKey.CREDITS_COMPARE, "PassportFK_001_005_paramCredits", IminfinParameterRole.BUSINESS, "STRING", true, null),
                                parameter(IminfinReportProfileKey.CREDITS_COMPARE, "territoryMap", IminfinParameterRole.SERVICE, "STRING", false, null)
                        )
                ),
                new IminfinReportProfile(
                        IminfinReportProfileKey.FIN_SOURCES_DETAIL,
                        "https://www.iminfin.ru/areas-of-analysis/budget/finansoviy-pasport-subjecta-rf/istochniki-finansirovaniya-detalno",
                        "PassportFK_002_003_finSourcesDataAfter01052019",
                        List.of("TerritoryOnlySubject", "ds_FK_Passport_MONTH_Periods", "periodHelperData"),
                        List.of(
                                parameter(IminfinReportProfileKey.FIN_SOURCES_DETAIL, "territory", IminfinParameterRole.BUSINESS, "STRING", true, null),
                                parameter(IminfinReportProfileKey.FIN_SOURCES_DETAIL, "paramPeriod", IminfinParameterRole.BUSINESS, "DATE", true, null),
                                parameter(IminfinReportProfileKey.FIN_SOURCES_DETAIL, "TERRITORIES_paramPeriod", IminfinParameterRole.REFERENCE_SELECTOR, "DATE", false, null),
                                parameter(IminfinReportProfileKey.FIN_SOURCES_DETAIL, "helperPeriod", IminfinParameterRole.SERVICE, "STRING", false, null)
                        )
                )
        );
    }

    private static IminfinParameterCatalogItem parameter(
            IminfinReportProfileKey profileKey,
            String parameterName,
            IminfinParameterRole role,
            String valueType,
            boolean required,
            String defaultValue
    ) {
        return new IminfinParameterCatalogItem(
                null,
                profileKey,
                parameterName,
                role,
                valueType,
                required,
                defaultValue
        );
    }
}
