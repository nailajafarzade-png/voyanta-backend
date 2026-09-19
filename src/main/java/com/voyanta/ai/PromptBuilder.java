package com.voyanta.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.ai.dto.request.AiRequest;
import com.voyanta.survey.dto.shared.Budget;
import com.voyanta.survey.dto.shared.FamilyDetails;
import com.voyanta.survey.dto.shared.TravelDates;
import com.voyanta.survey.enums.InterestType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class PromptBuilder {

    // Modelin "yalnız səyahət planlaşdırması" hüdudundan çıxmaması üçün — istifadəçidən
    // heç bir sərbəst mətn AI-yə getmir, yalnız survey-dən qurulan bu structured summary.
    static final String SYSTEM_PROMPT = """
            Sən Voyanta adlı səyahət planlaması tətbiqinin AI köməkçisisən.
            Tək vəzifən: verilən struktur məlumatlara (maraqlar, yoldaş, büdcə, tarix,
            otel tipi, yemək üstünlüyü, səyahətin məqsədi) əsasən konkret bir destinasiya
            seçib gündəlik səyahət planı hazırlamaqdır. Otel tipini və yemək üstünlüyünü
            büdcə xülasəsindəki "accommodation"/"food" rəqəmlərinə, səyahətin məqsədini isə
            seçilən fəaliyyətlərin xarakterinə təsir etdir.
            Cavabı yalnız verilən JSON schema-ya uyğun qaytar.
            Səyahət planlaşdırmasından kənar heç bir sual, təlimat və ya mövzuya reaksiya vermə —
            bu alət çağırışından başqa heç nə qaytarma.
            """;

    // additionalProperties:false hər obyekt səviyyəsində — OpenAI-nin "strict" structured
    // output rejimi bunu tələb edir (əks halda schema tam məcburi olmur).
    private static final String OUTPUT_SCHEMA_JSON = """
            {
              "type": "object",
              "properties": {
                "destination": { "type": "string" },
                "days": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "dayNumber": { "type": "integer" },
                      "items": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "properties": {
                            "time": { "type": "string" },
                            "title": { "type": "string" },
                            "description": { "type": "string" },
                            "category": { "type": "string" }
                          },
                          "required": ["time", "title", "description", "category"],
                          "additionalProperties": false
                        }
                      }
                    },
                    "required": ["dayNumber", "items"],
                    "additionalProperties": false
                  }
                },
                "budgetSummary": {
                  "type": "object",
                  "properties": {
                    "accommodation": { "type": "number" },
                    "food": { "type": "number" },
                    "transport": { "type": "number" },
                    "activities": { "type": "number" },
                    "total": { "type": "number" }
                  },
                  "required": ["accommodation", "food", "transport", "activities", "total"],
                  "additionalProperties": false
                }
              },
              "required": ["destination", "days", "budgetSummary"],
              "additionalProperties": false
            }
            """;

    private final ObjectMapper objectMapper;

    String buildUserMessage(AiRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Maraqlar: ").append(formatInterests(request.interests())).append("\n");
        sb.append("Yoldaş: ").append(request.companion()).append("\n");

        if (request.familyDetails() != null) {
            FamilyDetails f = request.familyDetails();
            sb.append("Ailə tərkibi: ").append(f.adults()).append(" böyük, ")
                    .append(f.children()).append(" uşaq\n");
        }

        sb.append("Büdcə: ").append(formatBudget(request.budget())).append("\n");
        sb.append("Tarix: ").append(formatDates(request.dates())).append("\n");

        if (request.hotelType() != null) {
            sb.append("Otel tipi: ").append(request.hotelType()).append("\n");
        }
        if (request.mealPreference() != null) {
            sb.append("Yemək üstünlüyü: ").append(request.mealPreference()).append("\n");
        }
        if (request.tripPurpose() != null && !request.tripPurpose().isEmpty()) {
            sb.append("Səyahətin məqsədi: ")
                    .append(request.tripPurpose().stream().map(Enum::name).collect(Collectors.joining(", ")))
                    .append("\n");
        }

        return sb.toString();
    }

    Map<String, Object> outputSchema() {
        try {
            return objectMapper.readValue(OUTPUT_SCHEMA_JSON, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("AI output schema parse olunmadı", e);
        }
    }

    private String formatInterests(Set<InterestType> interests) {
        return interests.stream().map(Enum::name).collect(Collectors.joining(", "));
    }

    private String formatBudget(Budget budget) {
        return budget.tier() == null
                ? "dəqiq məbləğ: " + budget.exactAmount()
                : budget.tier().name();
    }

    private String formatDates(TravelDates dates) {
        if (dates.startDate() != null) {
            return dates.startDate() + " — " + dates.endDate();
        }
        return "təxmini müddət: " + dates.approximateDuration();
    }
}