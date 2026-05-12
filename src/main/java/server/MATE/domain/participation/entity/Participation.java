package server.MATE.domain.participation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "participation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"test_id", "tester_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long testId;

    @Column(nullable = false, updatable = false)
    private Long testerId;

    private LocalDateTime deletedAt;

    @Builder
    public Participation(Long testId, Long testerId) {
        this.testId = testId;
        this.testerId = testerId;
    }

    public void validateTester(Long testerId) {
        if (!this.testerId.equals(testerId)) {
            throw new BaseException(BaseErrorCode.COMMON_009);
        }
    }
}
