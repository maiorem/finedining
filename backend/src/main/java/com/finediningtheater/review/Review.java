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
 * 리뷰(요구서상 커뮤니티, 기능7). accountId는 카카오 로그인(계정 테이블)이 붙은 뒤에도 여전히
 * FK가 아니라 값으로만 둔다 — V7 시드 데이터가 실제 account 테이블과 무관한 placeholder id를
 * 이미 갖고 있어, DB 레벨 FK를 지금 추가하면 그 행들이 제약을 위반한다. 새로 작성되는 리뷰는
 * 실제 {@code Account.id}를 담지만, FK 승격은 별도 마이그레이션으로 시드 데이터를 정리한 뒤
 * 한다(CLAUDE.md §13.4 expand-contract).
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

    /**
     * 본인 글 수정과 관리자의 원문 수정(2026-08-26 결정, §3.6)이 공유하는 상태 변경이다 —
     * "누가" 부를 수 있는지(소유권 검사)는 호출부(ReviewService)의 책임이지 이 메서드의 관심사가
     * 아니다.
     */
    public void editContent(String title, String body) {
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
