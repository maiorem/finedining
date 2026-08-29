package com.finediningtheater.account;

import com.finediningtheater.global.support.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * INVITE_ONLY 정책에서만 참조되는 허용 목록. provider+providerUserId 또는 email로 매칭한다
 * (CLAUDE.md §3.2). 관리자(admin_account)와는 무관한 별도 테이블이다 — 예전 account_invite와
 * 헷갈리지 않게 이름을 다시 쓰지 않는다.
 */
@Entity
@Getter
@Table(name = "signup_allowlist")
public class SignupAllowlistEntry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String provider;

    @Column(name = "provider_user_id", length = 191)
    private String providerUserId;

    @Column(length = 255)
    private String email;

    protected SignupAllowlistEntry() {}

    public SignupAllowlistEntry(String provider, String providerUserId, String email) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
    }
}
