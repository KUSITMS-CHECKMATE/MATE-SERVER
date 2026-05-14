package server.MATE.domain.users.entity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum TossUnlinkReferrer {
    UNLINK,
    WITHDRAWAL_TERMS,
    WITHDRAWAL_TOSS,
    UNKNOWN;

    public static TossUnlinkReferrer from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }

        try {
            return TossUnlinkReferrer.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown Toss unlink referrer received: {}", value);
            return UNKNOWN;
        }
    }
}
