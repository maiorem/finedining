package com.finediningtheater.inquiry;

import com.finediningtheater.global.support.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/**
 * 협업 제안(기능4). 카카오 로그인이 붙기 전까지는 로그인 없이 받는다 — accountId는 항상
 * null이다(2026-08-27 결정, CLAUDE.md §3.7). 카카오가 붙으면 accountId를 필수로 되돌린다.
 */
@Entity
@Getter
@Table(name = "proposal")
public class Proposal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 회원 연결용. 지금은 로그인 자체가 없어 항상 null이다. */
    @Column
    private Long accountId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String contactEmail;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProposalStatus status = ProposalStatus.SENT;

    /** 개인정보 수집 동의 시각 — 폼에서 동의하지 않으면 애초에 이 엔티티가 만들어지지 않는다(§7.7). */
    @Column(nullable = false)
    private Instant consentAgreedAt;

    protected Proposal() {}

    public Proposal(String name, String contactEmail, String title, String body) {
        this.name = name;
        this.contactEmail = contactEmail;
        this.title = title;
        this.body = body;
        this.consentAgreedAt = Instant.now();
    }

    public void markRead() {
        if (status == ProposalStatus.SENT) {
            this.status = ProposalStatus.READ;
        }
    }

    public boolean isOpen() {
        return status == ProposalStatus.SENT || status == ProposalStatus.READ;
    }

    public void accept() {
        this.status = ProposalStatus.ACCEPTED;
    }

    public void decline() {
        this.status = ProposalStatus.DECLINED;
    }
}
