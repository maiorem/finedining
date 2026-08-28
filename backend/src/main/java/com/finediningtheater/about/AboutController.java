package com.finediningtheater.about;

import com.finediningtheater.about.dto.AboutResponse;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.support.SiteLocale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 공개 조회 전용 — 로그인 없이 전부 볼 수 있다(§1). */
@RestController
@RequiredArgsConstructor
public class AboutController {

    private final AboutService aboutService;

    /**
     * preview=true는 인증된 관리자에게만 적용된다 — 익명 요청은 서버에서 무시한다(CLAUDE.md
     * §3.9). 발행 전 소개문도 관리자는 같은 URL에서 편집 패널에 붙을 수 있어야 한다.
     */
    @GetMapping("/api/about")
    public ApiResponse<AboutResponse> get(
            @RequestParam(defaultValue = "KO") SiteLocale lang,
            @RequestParam(defaultValue = "false") boolean preview,
            @AuthenticationPrincipal AdminPrincipal principal) {
        AboutContent about =
                (preview && principal != null) ? aboutService.getForPreview() : aboutService.getPublished();
        return ApiResponse.success(AboutResponse.from(about, lang));
    }
}
