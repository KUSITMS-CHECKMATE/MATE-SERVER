package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.TreeTest;

import java.util.List;

public record TreeTestCreateResponse(
        Long questionId,
        List<Long> rootIds
) {
    public static TreeTestCreateResponse from(Question question, List<TreeTest> roots) {
        return new TreeTestCreateResponse(
                question.getId(),
                roots.stream().map(TreeTest::getId).toList()
        );
    }
}
