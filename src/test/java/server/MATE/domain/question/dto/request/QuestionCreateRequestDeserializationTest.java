package server.MATE.domain.question.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionCreateRequestDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("통합 질문 생성 요청은 type에 따라 기존 Request DTO로 역직렬화된다")
    void deserializeQuestionCreateRequestByType() throws Exception {
        String json = """
                {
                  "questions": [
                    {
                      "type": "OBJECTIVE",
                      "title": "주 기능은?",
                      "description": "객관식 질문",
                      "isDuplicate": false,
                      "isOther": true,
                      "options": [
                        { "content": "검색", "imageKey": null },
                        { "content": "결제", "imageKey": null }
                      ]
                    },
                    {
                      "type": "SCALE",
                      "title": "만족도는?",
                      "description": "척도 질문",
                      "imageKey": null,
                      "minLabel": "낮음",
                      "maxLabel": "높음",
                      "range": 5
                    },
                    {
                      "type": "CARD_SORTING",
                      "title": "알맞게 분류해주세요",
                      "description": "비슷한 항목끼리 묶어주세요.",
                      "cards": ["티셔츠", "청바지", "운동화", "코트"],
                      "categories": ["상의", "하의", "신발"]
                    }
                  ]
                }
                """;

        QuestionCreateRequest request = objectMapper.readValue(json, QuestionCreateRequest.class);

        assertThat(request.questions()).hasSize(3);
        assertThat(request.questions().get(0)).isInstanceOf(ObjectiveCreateRequest.class);
        assertThat(request.questions().get(1)).isInstanceOf(ScaleCreateRequest.class);
        assertThat(request.questions().get(2)).isInstanceOf(CardSortingCreateRequest.class);

        ObjectiveCreateRequest objective = (ObjectiveCreateRequest) request.questions().get(0);
        ScaleCreateRequest scale = (ScaleCreateRequest) request.questions().get(1);
        CardSortingCreateRequest cardSorting = (CardSortingCreateRequest) request.questions().get(2);

        assertThat(objective.type().name()).isEqualTo("OBJECTIVE");
        assertThat(objective.options()).hasSize(2);
        assertThat(scale.type().name()).isEqualTo("SCALE");
        assertThat(scale.range()).isEqualTo(5);
        assertThat(cardSorting.type().name()).isEqualTo("CARD_SORTING");
        assertThat(cardSorting.cards()).containsExactly("티셔츠", "청바지", "운동화", "코트");
        assertThat(cardSorting.categories()).containsExactly("상의", "하의", "신발");
    }
}
