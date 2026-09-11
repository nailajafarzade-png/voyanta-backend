package com.voyanta.survey.dto.shared;

import com.voyanta.survey.enums.DurationType;

import java.time.LocalDate;

public record TravelDates(
        LocalDate startDate,
        LocalDate endDate,
        DurationType approximateDuration
) {
    public TravelDates {
        boolean hasExactDates = startDate != null && endDate != null;
        boolean hasApproxDuration = approximateDuration != null;

        if (hasExactDates == hasApproxDuration) {
            throw new IllegalArgumentException(
                    "Ya dəqiq tarixlər, ya da təxmini müddət seçilməlidir — ikisi eyni vaxtda ola bilməz"
            );
        }
    }
}