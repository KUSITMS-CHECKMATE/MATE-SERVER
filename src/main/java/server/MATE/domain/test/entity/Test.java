package server.MATE.domain.test.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import server.MATE.global.common.entity.BaseEntity;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "test")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Test extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long makerId;

    @Column(nullable = false, length = 17)
    private String title;

    @Column(length = 60)
    private String description;

    @Column(length = 17)
    private String serviceName;

    @Column(length = 70)
    private String serviceDescription;

    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "test_image", joinColumns = @JoinColumn(name = "test_id"))
    @Column(name = "image_key")
    private List<String> imageKeys = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestStatus testStatus;

    @Enumerated(EnumType.STRING)
    private ReportStatus reportStatus = ReportStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus;

    @Column(nullable = false)
    private Integer goalPpl;

    @Column(nullable = false)
    private Integer reward;

    @Column(nullable = false)
    private Long pplCount;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Long likeCount = 0L;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<TestCategory> categories = new ArrayList<>();

    public void addCategories(List<Category> categories) {
        categories.forEach(category -> {
            TestCategory testCategory = TestCategory.builder()
                    .test(this)
                    .category(category)
                    .build();
            this.categories.add(testCategory);
        });
    }

    public void update(String title, String description, List<Category> categories,
                       String serviceName, String serviceDescription, List<String> imageKeys) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (categories != null) {
            this.categories.clear();
            addCategories(categories);
        }
        if (serviceName != null) this.serviceName = serviceName;
        if (serviceDescription != null) this.serviceDescription = serviceDescription;
        if (imageKeys != null) {
            this.imageKeys.clear();
            this.imageKeys.addAll(imageKeys);
        }
    }

    public void validateCanParticipate() {
        if (this.approvalStatus != ApprovalStatus.ACCEPTED || this.testStatus != TestStatus.IN_PROGRESS) {
            throw new BaseException(BaseErrorCode.PARTICIPATION_002);
        }
        if (this.pplCount >= this.goalPpl.longValue()) {
            throw new BaseException(BaseErrorCode.PARTICIPATION_004);
        }
    }

    public void incrementPplCount() {
        this.pplCount++;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void complete() {
        this.testStatus = TestStatus.COMPLETED;
    }

    public void startReportAggregation() {
        this.reportStatus = ReportStatus.IN_PROGRESS;
    }

    public void completeReportAggregation() {
        this.reportStatus = ReportStatus.COMPLETED;
    }

    public void failReportAggregation() {
        this.reportStatus = ReportStatus.FAILED;
    }

    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Builder
    public Test(Long makerId, String title, String description, String serviceName,
                String serviceDescription, List<String> imageKeys) {
        this.makerId = makerId;
        this.title = title;
        this.description = description;
        this.serviceName = serviceName;
        this.serviceDescription = serviceDescription;
        if (imageKeys != null) this.imageKeys.addAll(imageKeys);
        this.testStatus = TestStatus.IN_PROGRESS;
        this.approvalStatus = ApprovalStatus.ACCEPTED; // Todo. 관리자 api 개발 후 WAITING으로 수정
        this.goalPpl = 100;
        this.reward = 300;
        this.pplCount = 0L;
        this.likeCount = 0L;
    }
}
