package server.MATE.domain.test.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;

@Getter
@Entity
@Table(
        name = "test_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "test_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "test_id", nullable = false, updatable = false)
    private Long testId;

    @Builder
    public TestLike(Long userId, Long testId) {
        this.userId = userId;
        this.testId = testId;
    }
}
