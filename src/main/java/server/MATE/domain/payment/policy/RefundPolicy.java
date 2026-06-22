package server.MATE.domain.payment.policy;

import org.springframework.stereotype.Component;
import server.MATE.domain.test.entity.Test;

@Component
public class RefundPolicy {

    private static final double REFUND_THRESHOLD = 0.2;

    /**
     * 환불 대상 여부를 판정한다.
     * 100% 환불이 가능한 경우에만 true를 반환한다.
     *
     * <ul>
     *   <li>메이커 수동 종료 → 환불 불가</li>
     *   <li>메이커가 "현재 인원으로 진행" 선택 → 환불 불가</li>
     *   <li>결과 데이터 열람/다운로드 → 환불 불가</li>
     *   <li>달성률 20% 이상 → 환불 불가</li>
     *   <li>달성률 20% 미만 + 위 조건 없음 → 100% 환불</li>
     * </ul>
     */
    public boolean isEligibleForRefund(Test test) {
        if (test.isClosedByMaker()) {
            return false;
        }
        if (test.isRefundWaived()) {
            return false;
        }
        if (test.isDataViewed()) {
            return false;
        }
        return test.getAchievementRate() < REFUND_THRESHOLD;
    }
}
