package com.voyanta.plan.dto.response;

import com.voyanta.plan.dto.shared.ItineraryItem;

import java.util.List;

public record ItineraryDayResponse(
        int dayNumber,
        boolean locked,
        List<ItineraryItem> items   // locked = true olanda null qaytarılır
) {}