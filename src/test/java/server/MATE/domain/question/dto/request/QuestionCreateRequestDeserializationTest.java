package server.MATE.domain.question.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    @DisplayName("각 질문 타입의 최소 유효 payload는 해당 DTO로 역직렬화된다")
    void deserializeMinimumValidPayloadForEachQuestionType() throws Exception {
        String json = """
                {
                  "questions": [
                    {
                      "type": "OBJECTIVE",
                      "title": "객관식",
                      "isDuplicate": false,
                      "isOther": false,
                      "options": [
                        { "content": "A", "imageKey": null },
                        { "content": "B", "imageKey": null }
                      ]
                    },
                    {
                      "type": "SUBJECTIVE",
                      "title": "주관식"
                    },
                    {
                      "type": "FIVE_SECOND",
                      "title": "5초",
                      "imageKey": "five-second-image-key",
                      "imageRatio": "9:16",
                      "isObjective": false,
                      "isOther": null
                    },
                    {
                      "type": "SCALE",
                      "title": "척도",
                      "range": 5
                    },
                    {
                      "type": "AB_TEST",
                      "title": "AB 테스트",
                      "aImageKey": "image-a",
                      "bImageKey": "image-b",
                      "imageRatio": "9:16"
                    },
                    {
                      "type": "CARD_SORTING",
                      "title": "카드소팅",
                      "cards": ["카드1", "카드2", "카드3", "카드4"],
                      "categories": ["카테고리1"]
                    },
                    {
                      "type": "TREE_TEST",
                      "title": "트리 테스트",
                      "features": [
                        {
                          "label": "루트1",
                          "children": []
                        }
                      ]
                    }
                  ]
                }
                """;

        QuestionCreateRequest request = objectMapper.readValue(json, QuestionCreateRequest.class);

        assertThat(request.questions()).hasSize(7);
        assertThat(request.questions().get(0)).isInstanceOf(ObjectiveCreateRequest.class);
        assertThat(request.questions().get(1)).isInstanceOf(SubjectiveCreateRequest.class);
        assertThat(request.questions().get(2)).isInstanceOf(FiveSecondCreateRequest.class);
        assertThat(request.questions().get(3)).isInstanceOf(ScaleCreateRequest.class);
        assertThat(request.questions().get(4)).isInstanceOf(AbTestCreateRequest.class);
        assertThat(request.questions().get(5)).isInstanceOf(CardSortingCreateRequest.class);
        assertThat(request.questions().get(6)).isInstanceOf(TreeTestCreateRequest.class);
    }

    @Test
    @DisplayName("통합 질문 생성 요청에서 type이 없으면 역직렬화에 실패한다")
    void failsWhenQuestionTypeIsMissing() {
        String json = """
                {
                  "questions": [
                    {
                      "title": "주 기능은?",
                      "description": "객관식 질문",
                      "isDuplicate": false,
                      "isOther": true,
                      "options": [
                        { "content": "검색", "imageKey": null },
                        { "content": "결제", "imageKey": null }
                      ]
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, QuestionCreateRequest.class))
                .isInstanceOf(InvalidTypeIdException.class);
    }

    @Test
    @DisplayName("통합 질문 생성 요청에서 알 수 없는 type이면 역직렬화에 실패한다")
    void failsWhenQuestionTypeIsUnknown() {
        String json = """
                {
                  "questions": [
                    {
                      "type": "UNKNOWN",
                      "title": "주 기능은?",
                      "description": "객관식 질문"
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, QuestionCreateRequest.class))
                .isInstanceOf(InvalidTypeIdException.class);
    }

    @Test
    @DisplayName("통합 질문 생성 요청에서 type이 소문자면 역직렬화에 실패한다")
    void failsWhenQuestionTypeIsLowercase() {
        String json = """
                {
                  "questions": [
                    {
                      "type": "objective",
                      "title": "주 기능은?"
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, QuestionCreateRequest.class))
                .isInstanceOf(InvalidTypeIdException.class);
    }

    @Test
    @DisplayName("숫자 필드에 문자열이 들어오면 역직렬화에 실패한다")
    void failsWhenNumericFieldContainsStringValue() {
        String json = """
                {
                  "questions": [
                    {
                      "type": "SCALE",
                      "title": "척도 질문",
                      "range": "five"
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, QuestionCreateRequest.class))
                .isInstanceOf(InvalidFormatException.class);
    }

    @Test
    @DisplayName("타입은 맞지만 다른 타입 필드가 섞여 들어오면 현재는 역직렬화에 실패한다")
    void failsWhenFieldsFromAnotherTypeAreMixedIn() {
        String json = """
                {
                  "questions": [
                    {
                      "type": "OBJECTIVE",
                      "title": "객관식 질문",
                      "isDuplicate": false,
                      "isOther": true,
                      "options": [
                        { "content": "A", "imageKey": null },
                        { "content": "B", "imageKey": null }
                      ],
                      "range": 5
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, QuestionCreateRequest.class))
                .isInstanceOf(UnrecognizedPropertyException.class);
    }

    @Test
    @DisplayName("알 수 없는 필드가 들어오면 현재는 역직렬화에 실패한다")
    void failsWhenUnknownFieldIsProvided() {
        String json = """
                {
                  "questions": [
                    {
                      "type": "SUBJECTIVE",
                      "title": "주관식 질문",
                      "unexpectedField": "unexpected"
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, QuestionCreateRequest.class))
                .isInstanceOf(UnrecognizedPropertyException.class);
    }

    @Test
    @DisplayName("깊이 초과 트리 테스트 JSON도 역직렬화 자체는 성공한다")
    void deserializeDeepTreeTestPayload() throws Exception {
        String json = """
                {
                  "questions": [
                    {
                      "type": "TREE_TEST",
                      "title": "깊이 초과 테스트",
                      "description": "5단계까지 내려가므로 실패해야 합니다.",
                      "features": [
                        {
                          "label": "송금",
                          "children": [
                            {
                              "label": "은행 송금",
                              "children": [
                                {
                                  "label": "자동이체",
                                  "children": [
                                    {
                                      "label": "자동이체 설정",
                                      "children": [
                                        {
                                          "label": "자동이체 상세 설정",
                                          "children": []
                                        }
                                      ]
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        QuestionCreateRequest request = objectMapper.readValue(json, QuestionCreateRequest.class);

        assertThat(request.questions()).hasSize(1);
        assertThat(request.questions().get(0)).isInstanceOf(TreeTestCreateRequest.class);
    }
}
