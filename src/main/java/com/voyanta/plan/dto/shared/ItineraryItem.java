package com.voyanta.plan.dto.shared;

// category bilərəkdən String-dir (enum yox) — AI-nin sərbəst kateqoriya adları
// qaytarmasına imkan verir, frontend rənglənməni buna görə edir
public record ItineraryItem(
        String time,
        String title,
        String description,
        String category
) {}