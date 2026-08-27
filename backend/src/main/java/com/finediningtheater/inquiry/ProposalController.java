package com.finediningtheater.inquiry;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.ratelimit.RateLimiter;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.inquiry.dto.CreateProposalRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 협업 제안 접수 — 공개 API의 두 예외 중 하나(§7.7). 로그인 없이 받는다(2026-08-27, §3.7).
 * IP당 분당 제한은 여기서, 일 3건 제한과 허니팟은 서비스에서 처리한다.
 */
@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private static final int SUBMIT_LIMIT_PER_MINUTE = 3;

    private final ProposalService proposalService;
    private final RateLimiter rateLimiter;

    @PostMapping
    public ApiResponse<Void> create(
            @Valid @RequestBody CreateProposalRequest request, HttpServletRequest httpRequest) {
        String ip = ClientIp.resolve(httpRequest);

        if (!rateLimiter.tryAcquire("proposal:" + ip, SUBMIT_LIMIT_PER_MINUTE)) {
            throw new BusinessException(ErrorCode.RATE_LIMITED);
        }

        proposalService.create(request, ip);
        return ApiResponse.ok();
    }
}
