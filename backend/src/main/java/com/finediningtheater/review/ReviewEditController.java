package com.finediningtheater.review;

import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.MemberPrincipal;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.review.dto.ReviewAdminResponse;
import com.finediningtheater.review.dto.ReviewCommentResponse;
import com.finediningtheater.review.dto.ReviewContentRequest;
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
 * 리뷰 모더레이션(관리자) + 작성·본인 수정·삭제(회원, 2026-09-03 — 카카오 로그인이 붙은 뒤 연
 * 경로, §3.6). 작성자 본인과 관리자가 같은 컨트롤러를 쓴다(§6 패키지 설계) — 글 작성·수정·삭제는
 * {@code @PreAuthorize("isAuthenticated()")}로 클래스 레벨의 {@code hasRole('EDITOR')}를
 * 메서드에서 덮어써 회원도 통과시키고, "누구의 글을 건드릴 수 있는가"는 관리자 여부에 따라
 * 서비스 계층에서 갈린다(소유권 검사, §3.3) — 관리자는 그 검사를 우회하는 유일한 경로다. 숨김·
 * 복구·댓글삭제는 여전히 관리자 전용이라 클래스 레벨 hasRole('EDITOR')를 그대로 받는다. 전부
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

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReviewAdminResponse> create(
            @Valid @RequestBody ReviewContentRequest request,
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            HttpServletRequest httpRequest) {
        if (memberPrincipal == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Review review = reviewService.create(memberPrincipal.id(), request.title(), request.body());

        auditLogger.record(
                memberPrincipal.id(),
                "REVIEW_CREATE",
                "Review",
                review.getId(),
                null,
                Map.of("title", review.getTitle(), "body", review.getBody()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(review));
    }

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
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReviewAdminResponse> updateContent(
            @PathVariable Long id,
            @Valid @RequestBody ReviewContentRequest request,
            @AuthenticationPrincipal AdminPrincipal adminPrincipal,
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            HttpServletRequest httpRequest) {
        Review before = reviewService.getForAdmin(id);
        Map<String, String> beforeSnapshot = Map.of("title", before.getTitle(), "body", before.getBody());

        if (adminPrincipal != null) {
            Review after = reviewService.adminEditContent(id, request.title(), request.body());
            auditLogger.record(
                    adminPrincipal.id(),
                    "REVIEW_ADMIN_EDIT",
                    "Review",
                    id,
                    beforeSnapshot,
                    Map.of("title", after.getTitle(), "body", after.getBody()),
                    ClientIp.resolve(httpRequest));
            return ApiResponse.success(toAdminResponse(after));
        }

        if (memberPrincipal != null) {
            Review after = reviewService.editOwnContent(id, memberPrincipal.id(), request.title(), request.body());
            auditLogger.record(
                    memberPrincipal.id(),
                    "REVIEW_SELF_EDIT",
                    "Review",
                    id,
                    beforeSnapshot,
                    Map.of("title", after.getTitle(), "body", after.getBody()),
                    ClientIp.resolve(httpRequest));
            return ApiResponse.success(toAdminResponse(after));
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
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
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReviewAdminResponse> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal adminPrincipal,
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            HttpServletRequest httpRequest) {
        String beforeStatus = reviewService.getForAdmin(id).getStatus().name();

        if (adminPrincipal != null) {
            Review after = reviewService.softDelete(id);
            auditLogger.record(
                    adminPrincipal.id(),
                    "REVIEW_DELETE",
                    "Review",
                    id,
                    Map.of("status", beforeStatus),
                    Map.of("status", after.getStatus().name()),
                    ClientIp.resolve(httpRequest));
            return ApiResponse.success(toAdminResponse(after));
        }

        if (memberPrincipal != null) {
            Review after = reviewService.softDeleteOwn(id, memberPrincipal.id());
            auditLogger.record(
                    memberPrincipal.id(),
                    "REVIEW_SELF_DELETE",
                    "Review",
                    id,
                    Map.of("status", beforeStatus),
                    Map.of("status", after.getStatus().name()),
                    ClientIp.resolve(httpRequest));
            return ApiResponse.success(toAdminResponse(after));
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
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
