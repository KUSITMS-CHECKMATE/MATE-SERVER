package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.dto.response.PaymentHistoryResponse;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.storage.service.FileStorageService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentHistoryService {

    private final PaymentRepository paymentRepository;
    private final TestRepository testRepository;
    private final FileStorageService fileStorageService;

    public List<PaymentHistoryResponse> getHistory(Long makerId) {
        List<Payment> payments = paymentRepository.findByMakerIdOrderByCreatedAtDesc(makerId);

        Set<Long> testIds = payments.stream()
                .map(Payment::getTestId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, Test> testMap = testRepository.findAllById(testIds).stream()
                .collect(Collectors.toMap(Test::getId, Function.identity()));

        return payments.stream()
                .map(p -> {
                    Test test = testMap.get(p.getTestId());
                    String thumbnailUrl = toThumbnailUrl(test);
                    return PaymentHistoryResponse.of(p, test, thumbnailUrl);
                })
                .toList();
    }

    private String toThumbnailUrl(Test test) {
        if (test == null || test.getImageKeys().isEmpty()) return null;
        return fileStorageService.generateDownloadUrl(test.getImageKeys().getFirst());
    }
}
