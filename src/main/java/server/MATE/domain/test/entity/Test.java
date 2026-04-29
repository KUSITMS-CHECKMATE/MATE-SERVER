package server.MATE.domain.test.entity;

import jakarta.persistence.*;
import lombok.Getter;
import server.MATE.domain.test.enums.TestStatus;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "test")
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "maker_id", nullable = false)
    private Long makerId;

    @Column(name = "title", nullable = false)
    private String title;


    private String description;
    private String serviceName;
    private String serviceDescription;

    @Column(name = "image_key", nullable = false)
    private String imageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TestStatus status = TestStatus.IN_PROGRESS;

    @Column(name = "ppl_count", nullable = false)
    private Long pplCount = 0L;

    private LocalDateTime deletedAt;
}
