package server.MATE.domain.test.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;

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
    @CollectionTable(name = "test_image", joinColumns = @JoinColumn(name = "test_id"))
    @Column(name = "image_key")
    private List<String> imageKeys = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestStatus status;

    @Column(nullable = false)
    private Integer goalPpl;

    @Column(nullable = false)
    private Integer reward;

    @Column(nullable = false)
    private Long pplCount;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
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

    @Builder
    public Test(Long makerId, String title, String description, String serviceName,
                String serviceDescription, List<String> imageKeys) {
        this.makerId = makerId;
        this.title = title;
        this.description = description;
        this.serviceName = serviceName;
        this.serviceDescription = serviceDescription;
        if (imageKeys != null) {
            this.imageKeys.addAll(imageKeys);
        }
        this.status = TestStatus.IN_PROGRESS;
        this.goalPpl = 100;
        this.reward = 300;
        this.pplCount = 0L;
    }
}
