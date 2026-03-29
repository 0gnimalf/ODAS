package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.iminfin.config.IminfinPassportPage;
import Ogni.ODAS.iminfin.util.IminfinTextNormalizer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IminfinIndicatorSelector {

    public Selection parse(String rawIndicatorCode) {
        String value = rawIndicatorCode == null ? "" : rawIndicatorCode.trim();
        if (value.startsWith("income-detail:")) {
            return new Selection(List.of(IminfinPassportPage.INCOMES_DETAIL),
                    value.substring("income-detail:".length()).trim(), false);
        }
        if (value.startsWith("outcome-detail:")) {
            return new Selection(List.of(IminfinPassportPage.OUTCOMES_DETAIL),
                    value.substring("outcome-detail:".length()).trim(), false);
        }
        if (value.startsWith("outcome-detail-rzpr:")) {
            return new Selection(List.of(IminfinPassportPage.OUTCOMES_DETAIL),
                    value.substring("outcome-detail-rzpr:".length()).trim(), true);
        }
        if (value.startsWith("income-compare:")) {
            return new Selection(List.of(IminfinPassportPage.INCOMES_COMPARE),
                    value.substring("income-compare:".length()).trim(), false);
        }
        if (value.startsWith("outcome-compare:")) {
            return new Selection(List.of(IminfinPassportPage.OUTCOMES_COMPARE),
                    value.substring("outcome-compare:".length()).trim(), false);
        }
        if (value.startsWith("credit:")) {
            return new Selection(List.of(IminfinPassportPage.CREDITS_COMPARE),
                    value.substring("credit:".length()).trim(), false);
        }
        if (value.startsWith("fin-source:")) {
            return new Selection(List.of(IminfinPassportPage.FIN_SOURCES_DETAIL),
                    value.substring("fin-source:".length()).trim(), false);
        }

        return new Selection(
                List.of(
                        IminfinPassportPage.INCOMES_DETAIL,
                        IminfinPassportPage.OUTCOMES_DETAIL,
                        IminfinPassportPage.CREDITS_COMPARE,
                        IminfinPassportPage.FIN_SOURCES_DETAIL
                ),
                value,
                false
        );
    }

    public boolean matches(String requestedIndicator, String rowCaption) {
        String requestedNormalized = IminfinTextNormalizer.normalize(requestedIndicator);
        String captionNormalized = IminfinTextNormalizer.normalize(rowCaption);
        if (requestedNormalized.equals(captionNormalized)) {
            return true;
        }
        return IminfinTextNormalizer.slugify(requestedIndicator)
                .equals(IminfinTextNormalizer.slugify(rowCaption));
    }

    public record Selection(
            List<IminfinPassportPage> candidatePages,
            String indicatorName,
            boolean expensesBySection
    ) {
    }
}
