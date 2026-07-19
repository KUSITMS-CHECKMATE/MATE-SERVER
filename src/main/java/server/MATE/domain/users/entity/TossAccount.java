package server.MATE.domain.users.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Column(unique = true)
    private Long tossUserKey;

    @Column(length = 2000)
    private String encryptedTossRefreshToken;

    @Column(length = 1000)
    private String scope;

    @Column(nullable = false)
    private boolean isLinked;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TossUnlinkReferrer unlinkReferrer;

    @Column
    private LocalDateTime unlinkedAt;

    @Column(nullable = false)
    private LocalDateTime lastLoginAt;

    @Column(nullable = false)
    private LocalDateTime lastTokenRefreshedAt;

    @Builder
    public TossAccount(
            Users user,
            Long tossUserKey,
            String encryptedTossRefreshToken,
            String scope,
            boolean isLinked,
            TossUnlinkReferrer unlinkReferrer,
            LocalDateTime unlinkedAt,
            LocalDateTime lastLoginAt,
            LocalDateTime lastTokenRefreshedAt
    ) {
        this.user = user;
        this.tossUserKey = tossUserKey;
        this.encryptedTossRefreshToken = encryptedTossRefreshToken;
        this.scope = scope;
        this.isLinked = isLinked;
        this.unlinkReferrer = unlinkReferrer;
        this.unlinkedAt = unlinkedAt;
        this.lastLoginAt = lastLoginAt;
        this.lastTokenRefreshedAt = lastTokenRefreshedAt;
    }

    public void syncLoginState(
            Long tossUserKey,
            String encryptedTossRefreshToken,
            String scope,
            LocalDateTime lastLoginAt,
            LocalDateTime lastTokenRefreshedAt
    ) {
        this.tossUserKey = tossUserKey;
        this.encryptedTossRefreshToken = encryptedTossRefreshToken;
        this.scope = scope;
        this.isLinked = true;
        this.unlinkReferrer = null;
        this.unlinkedAt = null;
        this.lastLoginAt = lastLoginAt;
        this.lastTokenRefreshedAt = lastTokenRefreshedAt;
    }

    public void refreshLoginState(
            String encryptedTossRefreshToken,
            String scope,
            LocalDateTime lastTokenRefreshedAt
    ) {
        this.encryptedTossRefreshToken = encryptedTossRefreshToken;
        this.scope = scope;
        this.isLinked = true;
        this.unlinkReferrer = null;
        this.unlinkedAt = null;
        this.lastTokenRefreshedAt = lastTokenRefreshedAt;
    }

    public void markUnlinked(TossUnlinkReferrer unlinkReferrer, LocalDateTime unlinkedAt) {
        this.isLinked = false;
        this.tossUserKey = null;
        this.encryptedTossRefreshToken = null;
        this.scope = null;
        this.unlinkReferrer = unlinkReferrer;
        this.unlinkedAt = unlinkedAt;
    }
}
