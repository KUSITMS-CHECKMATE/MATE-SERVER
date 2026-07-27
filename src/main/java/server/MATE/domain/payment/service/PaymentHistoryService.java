package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.dto.response.PaymentHistoryResponse;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentHistoryService {

    private final PaymentRepository paymentRepository;
    private final TestRepository testRepository;

    public List<PaymentHistoryResponse> getHistory(Long makerId) {
        List<Payment> payments = paymentRepository.findByMakerIdOrderByCreatedAtDesc(makerId);

        Set<Long> testIds = payments.stream()
                .map(Payment::getTestId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, String> testTitleMap = testRepository.findAllById(testIds).stream()
                .collect(Collectors.toMap(Test::getId, Test::getTitle));

        return payments.stream()
                .map(p -> PaymentHistoryResponse.of(p, testTitleMap.get(p.getTestId())))
                .toList();
    }
}
