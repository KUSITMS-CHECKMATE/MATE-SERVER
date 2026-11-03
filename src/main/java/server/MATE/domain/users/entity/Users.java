package server.MATE.domain.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Users extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, unique = true)
    private String ci;

    // 연결 해제(UNLINK) 후에도 유지해 재가입한 동일인을 기존 계정으로 연결하는 데 쓴다.
    @Column(nullable = true, unique = true, length = 64)
    private String ciHash;

    @Column
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder
    public Users(String ci, String ciHash, String name, Role role) {
        this.ci = ci;
        this.ciHash = ciHash;
        this.name = name;
        this.role = role == null ? Role.USER : role;
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        this.name = name;
    }

    public void assignCiHashIfAbsent(String ciHash) {
        if (this.ciHash == null) {
            this.ciHash = ciHash;
        }
    }

    public void relinkCi(String ci) {
        this.ci = ci;
    }

    // 연결만 끊은 경우 ciHash를 남겨 재가입 시 기존 계정으로 연결하고, 탈퇴는 ciHash까지 지워 완전히 익명화한다.
    public void anonymizeForTossUnlink(boolean retainCiHash) {
        this.ci = null;
        if (!retainCiHash) {
            this.ciHash = null;
        }
    }
}
