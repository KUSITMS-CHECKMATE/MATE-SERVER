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
        if (questionsPayload != null) {
            this.questionsPayload = questionsPayload;
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
        this.questionsPayload = questionsPayload;
        this.status = status == null ? TestDraftStatus.DRAFT : status;
        this.publishedTestId = publishedTestId;
        this.orderNo = orderNo;
        this.expectedAmount = expectedAmount;
        this.payToken = payToken;
    }
}
