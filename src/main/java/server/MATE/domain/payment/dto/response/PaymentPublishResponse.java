package server.MATE.domain.payment.dto.response;

import server.MATE.domain.payment.entity.PublishStatus;

public record PaymentPublishResponse(
        PublishStatus status
) {
}
