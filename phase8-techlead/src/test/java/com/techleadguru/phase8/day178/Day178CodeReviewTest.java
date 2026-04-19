package com.techleadguru.phase8.day178;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Day178CodeReviewTest {

    @Test
    void codeReviewChecklistIsNonEmpty() {
        var items = Day178CodeReview.codeReviewChecklist();
        assertFalse(items.isEmpty());
        items.forEach(item -> {
            assertNotNull(item.category());
            assertNotNull(item.item());
            assertNotNull(item.antiPattern());
            assertNotNull(item.fix());
        });
    }

    @Test
    void codeReviewChecklistCoversAllCategories() {
        var items = Day178CodeReview.codeReviewChecklist();
        long categories = items.stream()
                .map(Day178CodeReview.ChecklistItem::category)
                .distinct()
                .count();
        assertEquals(Day178CodeReview.ReviewCategory.values().length, categories);
    }

    @Test
    void securityChecklistContainsInjectionEntry() {
        var list = Day178CodeReview.securityChecklist();
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(s -> s.toLowerCase().contains("injection")));
    }

    @Test
    void performanceChecklistContainsN1Entry() {
        var list = Day178CodeReview.performanceChecklist();
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(s -> s.toLowerCase().contains("n+1")));
    }

    @Test
    void reviewCommentCanBeCreated() {
        var comment = new Day178CodeReview.ReviewComment(
                "OrderService.java", 42,
                Day178CodeReview.ReviewCategory.SECURITY,
                Day178CodeReview.ReviewSeverity.BLOCKER,
                "SQL injection risk",
                "Use parameterised query"
        );
        assertEquals("OrderService.java", comment.file());
        assertEquals(42, comment.line());
        assertEquals(Day178CodeReview.ReviewSeverity.BLOCKER, comment.severity());
    }
}
