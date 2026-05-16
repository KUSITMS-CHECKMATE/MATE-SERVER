package server.MATE.domain.answer.service;

import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.Scale;
import server.MATE.domain.question.entity.TreeTest;

import java.util.List;
import java.util.Map;

public record AnswerCreateContext(
        Map<Long, Objective> objectives,
        Map<Long, FiveSecond> fiveSeconds,
        Map<Long, Scale> scales,
        Map<Long, CardSorting> cardSortings,
        Map<Long, List<TreeTest>> treeNodes
) {}
