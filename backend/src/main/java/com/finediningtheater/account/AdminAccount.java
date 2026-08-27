package com.finediningtheater.account;

import com.finediningtheater.global.support.BaseTimeEntity;
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
 * 관리자 계정. 카카오로 가입하는 일반 회원 계정(아직 구현 전 — 카카오 로그인은 우선순위 마지막)과
 * 완전히 별개인 엔티티다. 회원가입 절차가 없고 SUPER_ADMIN이 직접 발급한다 (CLAUDE.md §3.1,
 * 2026-08-27 결정).
 */
@Entity
@Getter
@Table(name = "admin_account")
public class AdminAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminAccountStatus status = AdminAccountStatus.ACTIVE;

    @Column(length = 100)
    private String pinHash;

    protected AdminAccount() {}

    public AdminAccount(String username, String passwordHash, AdminRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void changePin(String newPinHash) {
        this.pinHash = newPinHash;
    }

    public void disable() {
        this.status = AdminAccountStatus.DISABLED;
    }

    public boolean isActive() {
        return status == AdminAccountStatus.ACTIVE;
    }
}
