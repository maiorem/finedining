package com.finediningtheater.review;

import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.review.dto.AdminEditReviewRequest;
import com.finediningtheater.review.dto.ReviewAdminResponse;
import com.finediningtheater.review.dto.ReviewCommentResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 리뷰 모더레이션(2026-08-28, 우선순위 5단계). 본인 글 수정·삭제(소유권 검사)는 아직 없다 —
 * 로그인 회원 신원이 없어 "내 글인가"를 확인할 방법이 없기 때문이다. 숨김·복구·삭제·원문수정은
 * §3.4의 PIN 필수 목록에 없으므로 sudo 모드를 요구하지 않는다(ProposalEditController와 동일한
 * 판단). 모든 쓰기에 감사 로그를 남긴다(§7.7).
 */
@RestController
@RequestMapping("/api/reviews")
@PreAuthorize("hasRole('EDITOR')")
@RequiredArgsConstructor
public class ReviewEditController {

    private final ReviewService reviewService;
    private final AuditLogger auditLogger;

    @GetMapping("/manage")
    public ApiResponse<List<ReviewAdminResponse>> listForAdmin() {
        // 목록에서는 리뷰마다 댓글을 함께 불러올 필요가 없다 — N+1을 피하려고 빈 목록으로 둔다.
        List<ReviewAdminResponse> body =
                reviewService.listForAdmin().stream()
                        .map(review -> ReviewAdminResponse.from(review, List.of()))
                        .toList();
        return ApiResponse.success(body);
    }

    @GetMapping("/manage/{id}")
    public ApiResponse<ReviewAdminResponse> getForAdmin(@PathVariable Long id) {
        return ApiResponse.success(toAdminResponse(reviewService.getForAdmin(id)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReviewAdminResponse> updateContent(
            @PathVariable Long id,
            @Valid @RequestBody AdminEditReviewRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        Review before = reviewService.getForAdmin(id);
        Map<String, String> beforeSnapshot = Map.of("title", before.getTitle(), "body", before.getBody());

        Review after = reviewService.adminEditContent(id, request.title(), request.body());

        auditLogger.record(
                principal.id(),
                "REVIEW_ADMIN_EDIT",
                "Review",
                id,
                beforeSnapshot,
                Map.of("title", after.getTitle(), "body", after.getBody()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(after));
    }

    @PostMapping("/{id}/hide")
    public ApiResponse<ReviewAdminResponse> hide(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        String beforeStatus = reviewService.getForAdmin(id).getStatus().name();
        Review after = reviewService.hide(id);

        auditLogger.record(
                principal.id(),
                "REVIEW_HIDE",
                "Review",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(after));
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<ReviewAdminResponse> restore(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        String beforeStatus = reviewService.getForAdmin(id).getStatus().name();
        Review after = reviewService.restore(id);

        auditLogger.record(
                principal.id(),
                "REVIEW_RESTORE",
                "Review",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(after));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<ReviewAdminResponse> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        String beforeStatus = reviewService.getForAdmin(id).getStatus().name();
        Review after = reviewService.softDelete(id);

        auditLogger.record(
                principal.id(),
                "REVIEW_DELETE",
                "Review",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(after));
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        reviewService.softDeleteComment(commentId);

        auditLogger.record(
                principal.id(), "REVIEW_COMMENT_DELETE", "ReviewComment", commentId, null, null, ClientIp.resolve(httpRequest));

        return ApiResponse.ok();
    }

    private ReviewAdminResponse toAdminResponse(Review review) {
        List<ReviewCommentResponse> comments =
                reviewService.listActiveComments(review.getId()).stream().map(ReviewCommentResponse::from).toList();
        return ReviewAdminResponse.from(review, comments);
    }
}
