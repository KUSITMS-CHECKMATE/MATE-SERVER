package server.MATE.test.entity;

import jakarta.persistence.*;
import lombok.Getter;
import server.MATE.test.enums.TestStatus;

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

    @Column(name = "description")
    private String description;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "service_description")
    private String serviceDescription;

    @Column(name = "image_key", nullable = false)
    private String imageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TestStatus status = TestStatus.IN_PROGRESS;

    @Column(name = "ppl_count", nullable = false)
    private Long pplCount = 0L;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
