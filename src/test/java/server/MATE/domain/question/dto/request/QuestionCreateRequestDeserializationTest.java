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
                    }
                  ]
                }
                """;

        QuestionCreateRequest request = objectMapper.readValue(json, QuestionCreateRequest.class);

        assertThat(request.questions()).hasSize(2);
        assertThat(request.questions().get(0)).isInstanceOf(ObjectiveCreateRequest.class);
        assertThat(request.questions().get(1)).isInstanceOf(ScaleCreateRequest.class);

        ObjectiveCreateRequest objective = (ObjectiveCreateRequest) request.questions().get(0);
        ScaleCreateRequest scale = (ScaleCreateRequest) request.questions().get(1);

        assertThat(objective.type().name()).isEqualTo("OBJECTIVE");
        assertThat(objective.options()).hasSize(2);
        assertThat(scale.type().name()).isEqualTo("SCALE");
        assertThat(scale.range()).isEqualTo(5);
    }
}
