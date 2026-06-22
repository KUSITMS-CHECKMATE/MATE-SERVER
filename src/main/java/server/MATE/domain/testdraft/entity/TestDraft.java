package server.MATE.domain.testdraft.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Entity
@Table(
        name = "test_draft",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "order_no")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestDraft extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long makerId;

    @Column(length = 17)
    private String title;

    @Column(length = 60)
    private String description;

    @Column(length = 17)
    private String serviceName;

    @Column(length = 70)
    private String serviceDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> imageKeys = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> categories = new ArrayList<>();

    private Integer goalPpl;

    private Integer reward;

    private LocalDateTime closedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> questionsPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TestDraftStatus status;

    private Long publishedTestId;

    @Column(name = "order_no", length = 50)
    private String orderNo;

    private Integer expectedAmount;

    private String payToken;

    public void update(String title,
                       String description,
                       String serviceName,
                       String serviceDescription,
                       List<String> imageKeys,
                       List<String> categories,
                       Integer goalPpl,
                       Integer reward,
                       LocalDateTime closedAt,
                       Map<String, Object> questionsPayload) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (serviceName != null) {
            this.serviceName = serviceName;
        }
        if (serviceDescription != null) {
            this.serviceDescription = serviceDescription;
        }
        if (imageKeys != null) {
            this.imageKeys = new ArrayList<>(imageKeys);
        }
        if (categories != null) {
            this.categories = new ArrayList<>(categories);
        }
        if (goalPpl != null) {
            this.goalPpl = goalPpl;
        }
        if (reward != null) {
            this.reward = reward;
        }
        if (closedAt != null) {
            this.closedAt = closedAt;
        }
        if (questionsPayload != null) {
            this.questionsPayload = questionsPayload;
        }
    }

    public void markPublishing() {
        this.status = TestDraftStatus.PUBLISHING;
    }

    public void markPublished(Long testId) {
        this.publishedTestId = testId;
        this.status = TestDraftStatus.PUBLISHED;
    }

    public void markPublishFailed() {
        this.status = TestDraftStatus.PUBLISH_FAILED;
    }

    public void validatePaymentState() {
        if (this.status == TestDraftStatus.PUBLISHED || this.status == TestDraftStatus.PUBLISHING) {
            throw new BaseException(BaseErrorCode.DRAFT_003);
        }
    }

    public void validateAmountFields() {
        if (this.goalPpl == null || this.reward == null || this.closedAt == null) {
            throw new BaseException(BaseErrorCode.DRAFT_005);
        }
        if (this.goalPpl <= 0 || this.reward < 0) {
            throw new BaseException(BaseErrorCode.DRAFT_005);
        }
    }

    public void validatePublishableFields() {
        if (isBlank(this.title) || isBlank(this.description)) {
            throw new BaseException(BaseErrorCode.DRAFT_006);
        }
        if (this.categories == null || this.categories.isEmpty()) {
            throw new BaseException(BaseErrorCode.DRAFT_006);
        }
        boolean hasInvalidCategory = this.categories.stream()
                .anyMatch(category -> !isValidCategory(category));
        if (hasInvalidCategory) {
            throw new BaseException(BaseErrorCode.DRAFT_006);
        }
        if (this.questionsPayload == null
                || !(this.questionsPayload.get("questions") instanceof List<?> questions)
                || questions.isEmpty()) {
            throw new BaseException(BaseErrorCode.DRAFT_006);
        }
    }

    public void validatePublishState() {
        if (this.publishedTestId != null && this.status == TestDraftStatus.PUBLISHED) {
            return;
        }
        if (this.status != TestDraftStatus.PAYMENT_CREATED && this.status != TestDraftStatus.PUBLISH_FAILED) {
            throw new BaseException(BaseErrorCode.DRAFT_004);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isValidCategory(String category) {
        if (category == null) {
            return false;
        }
        try {
            server.MATE.domain.test.entity.Category.valueOf(category);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Builder
    public TestDraft(Long makerId,
                     String title,
                     String description,
                     String serviceName,
                     String serviceDescription,
                     List<String> imageKeys,
                     List<String> categories,
                     Integer goalPpl,
                     Integer reward,
                     LocalDateTime closedAt,
                     Map<String, Object> questionsPayload,
                     TestDraftStatus status,
                     Long publishedTestId,
                     String orderNo,
                     Integer expectedAmount,
                     String payToken) {
        this.makerId = makerId;
        this.title = title;
        this.description = description;
        this.serviceName = serviceName;
        this.serviceDescription = serviceDescription;
        if (imageKeys != null) {
            this.imageKeys = new ArrayList<>(imageKeys);
        }
        if (categories != null) {
            this.categories = new ArrayList<>(categories);
        }
        this.goalPpl = goalPpl;
        this.reward = reward;
        this.closedAt = closedAt;
        this.questionsPayload = questionsPayload;
        this.status = status == null ? TestDraftStatus.DRAFT : status;
        this.publishedTestId = publishedTestId;
        this.orderNo = orderNo;
        this.expectedAmount = expectedAmount;
        this.payToken = payToken;
    }
}
