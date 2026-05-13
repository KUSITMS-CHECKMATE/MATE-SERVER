package server.MATE.domain.users.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;

@Getter
@Entity
@Table(name = "toss_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TossAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users user;

    @Column(nullable = false, unique = true)
    private Long tossUserKey;

    @Column(length = 2000)
    private String tossAccessToken;

    @Column(length = 2000)
    private String tossRefreshToken;

    @Column
    private LocalDateTime tossAccessTokenExpiresAt;

    @Column(length = 1000)
    private String scope;

    @Builder
    public TossAccount(
            Users user,
            Long tossUserKey,
            String tossAccessToken,
            String tossRefreshToken,
            LocalDateTime tossAccessTokenExpiresAt,
            String scope
    ) {
        this.user = user;
        this.tossUserKey = tossUserKey;
        this.tossAccessToken = tossAccessToken;
        this.tossRefreshToken = tossRefreshToken;
        this.tossAccessTokenExpiresAt = tossAccessTokenExpiresAt;
        this.scope = scope;
    }

    public void syncTokenState(
            Long tossUserKey,
            String tossAccessToken,
            String tossRefreshToken,
            LocalDateTime tossAccessTokenExpiresAt,
            String scope
    ) {
        this.tossUserKey = tossUserKey;
        this.tossAccessToken = tossAccessToken;
        this.tossRefreshToken = tossRefreshToken;
        this.tossAccessTokenExpiresAt = tossAccessTokenExpiresAt;
        this.scope = scope;
    }
}
