package com.finediningtheater.account;

import com.finediningtheater.global.support.BaseTimeEntity;
import com.finediningtheater.global.support.SiteLocale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * 카카오로 로그인하는 일반 회원 신원. 관리자 계정(AdminAccount)과 완전히 별개인 엔티티다
 * (CLAUDE.md §3.1, 2026-08-27 확정) — 가입 절차·인증 수단·테이블이 전부 다르다. role 컬럼이
 * 없다 — 로그인 자체가 곧 리뷰 작성 자격이다.
 *
 * <p>식별은 email이 아니라 provider+providerUserId로 한다 — 카카오 이메일은 사용자가 제공에
 * 동의하지 않으면 null로 온다(§7.4). 탈퇴 시 provider_user_id·email을 익명화한다(§3.2) —
 * 재로그인하면 새 계정으로 취급된다.
 */
@Entity
@Getter
@Table(name = "account")
public class Account extends BaseTimeEntity {

    public static final String WITHDRAWN_NICKNAME = "탈퇴한 회원";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_user_id", length = 191)
    private String providerUserId;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SiteLocale locale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    protected Account() {}

    public Account(String provider, String providerUserId, String email, String nickname, SiteLocale locale) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.nickname = nickname;
        this.locale = locale;
    }

    /**
     * 탈퇴(§3.2) — 물리 삭제가 아니라 상태 전환이다. 작성한 글의 account_id 참조가 끊기지 않도록
     * 행은 남기고 개인정보만 지운다. provider_user_id를 지우므로 같은 카카오 계정으로 다시 로그인하면
     * 새 계정으로 취급된다.
     */
    public void withdraw() {
        this.status = AccountStatus.WITHDRAWN;
        this.nickname = WITHDRAWN_NICKNAME;
        this.email = null;
        this.providerUserId = null;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }
}
