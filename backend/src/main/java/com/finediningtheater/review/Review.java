package com.finediningtheater.review;

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
 * 리뷰(요구서상 커뮤니티, 기능7). accountId는 지금은 일반 회원 로그인(카카오)이 없어 FK가 아니라
 * 값으로만 둔다 — {@code account} 테이블 자체가 아직 없다. 로그인이 붙으면 실제 FK로 승격한다
 * (CLAUDE.md §13.4 expand-contract). 지금 단계에서는 작성 경로가 없고 관리자 모더레이션(숨김·
 * 복구·삭제·원문수정)만 지원한다 — 시드 데이터로 화면을 검증한다(2026-08-28 결정).
 */
@Entity
@Getter
@Table(name = "review")
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PUBLISHED;

    protected Review() {}

    public Review(Long accountId, String title, String body) {
        this.accountId = accountId;
        this.title = title;
        this.body = body;
    }

    public void hide() {
        this.status = ReviewStatus.HIDDEN;
    }

    public void restore() {
        this.status = ReviewStatus.PUBLISHED;
    }

    public void softDelete() {
        this.status = ReviewStatus.DELETED;
    }

    /** 관리자는 남의 글을 숨김·삭제뿐 아니라 원문 수정까지 할 수 있다 (2026-08-26 결정, §3.6). */
    public void adminEditContent(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public boolean isPublished() {
        return status == ReviewStatus.PUBLISHED;
    }

    public boolean isHidden() {
        return status == ReviewStatus.HIDDEN;
    }
}
