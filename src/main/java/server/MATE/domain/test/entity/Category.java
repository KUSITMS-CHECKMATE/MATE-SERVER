package server.MATE.domain.test.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    DAILY("일상"),
    FINANCE("금융"),
    HEALTH("건강"),
    SHOPPING("쇼핑"),
    FOOD("음식"),
    GAME("게임"),
    CONTENT("콘텐츠"),
    COMMUNITY("커뮤니티"),
    AI("AI"),
    EDUCATION("교육"),
    TRAVEL("여행"),
    SOCIAL("소셜"),
    CONVENIENCE("편의"),
    INFORMATION("정보"),
    BUSINESS("비즈니스"),
    TRANSPORT("교통"),
    PUBLIC_ADMIN("공공·행정");

    private final String displayName;
}
