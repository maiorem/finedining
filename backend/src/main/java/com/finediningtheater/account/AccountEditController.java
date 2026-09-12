package com.finediningtheater.account;

import com.finediningtheater.account.dto.AccountAdminResponse;
import com.finediningtheater.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자용 회원(카카오 로그인) 조회 화면(2026-09-12). 조회 전용이다 — 정지·삭제 같은 쓰기
 * 기능은 요청받지 않아 이번 범위에 넣지 않는다. 감사 로그는 상태를 바꾸지 않는 조회라
 * 남기지 않는다(다른 *EditController의 GET /manage와 동일한 취급, 예: PressClippingEditController).
 */
@RestController
@RequestMapping("/api/accounts")
@PreAuthorize("hasRole('EDITOR')")
@RequiredArgsConstructor
public class AccountEditController {

    private final AccountService accountService;

    @GetMapping("/manage")
    public ApiResponse<List<AccountAdminResponse>> listForAdmin() {
        List<AccountAdminResponse> body =
                accountService.listForAdmin().stream().map(AccountAdminResponse::from).toList();
        return ApiResponse.success(body);
    }
}
