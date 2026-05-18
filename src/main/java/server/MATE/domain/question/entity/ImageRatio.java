package server.MATE.domain.question.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ImageRatio {
    RATIO_9_16("9:16"),
    RATIO_1_1("1:1"),
    RATIO_4_3("4:3");

    private final String value;

    ImageRatio(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ImageRatio from(String value) {
        return Arrays.stream(values())
                .filter(ratio -> ratio.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown image ratio: " + value));
    }
}
