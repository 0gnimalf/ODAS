package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.iminfin.config.IminfinPassportPage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IminfinIndicatorSelectorTest {

    private final IminfinIndicatorSelector selector = new IminfinIndicatorSelector();

    @Test
    void parsesExplicitPrefix() {
        var selection = selector.parse("income-detail: Налоговые доходы");

        assertEquals(IminfinPassportPage.INCOMES_DETAIL, selection.candidatePages().getFirst());
        assertEquals("Налоговые доходы", selection.indicatorName());
        assertFalse(selection.expensesBySection());
    }

    @Test
    void parsesOutcomeSectionMode() {
        var selection = selector.parse("outcome-detail-rzpr: Образование");

        assertTrue(selection.expensesBySection());
        assertEquals(IminfinPassportPage.OUTCOMES_DETAIL, selection.candidatePages().getFirst());
    }

    @Test
    void fallsBackToDefaultPagesWhenPrefixMissing() {
        var selection = selector.parse("Налоговые доходы");

        assertEquals(4, selection.candidatePages().size());
        assertEquals("Налоговые доходы", selection.indicatorName());
    }

    @Test
    void matchesByNormalizedCaptionOrSlug() {
        assertTrue(selector.matches("Налоговые доходы", "  налоговые   доходы "));
        assertTrue(selector.matches("Налоговые доходы", "Налоговые-доходы"));
        assertFalse(selector.matches("Налоговые доходы", "Расходы"));
    }
}

