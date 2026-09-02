package com.finediningtheater.program;

import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.MediaOwnerType;
import com.finediningtheater.media.MediaService;
import com.finediningtheater.media.dto.MediaAssetResponse;
import com.finediningtheater.program.dto.ProgramDetailResponse;
import com.finediningtheater.program.dto.ProgramResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 공개 조회 전용 — 로그인 없이 전부 볼 수 있다(§1). */
@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;
    private final MediaService mediaService;

    @GetMapping
    public ApiResponse<List<ProgramResponse>> list(@RequestParam(defaultValue = "KO") SiteLocale lang) {
        List<ProgramResponse> body =
                programService.listPublished().stream()
                        .map(p -> ProgramResponse.from(p, lang, thumbnailFor(p)))
                        .toList();
        return ApiResponse.success(body);
    }

    /**
     * preview=true는 인증된 관리자에게만 적용된다 — 익명 요청은 서버에서 무시한다(CLAUDE.md
     * §3.9). 방금 만든 초안 프로그램도 관리자는 같은 URL에서 편집 패널에 붙을 수 있어야 한다.
     */
    @GetMapping("/{slug}")
    public ApiResponse<ProgramDetailResponse> detail(
            @PathVariable String slug,
            @RequestParam(defaultValue = "KO") SiteLocale lang,
            @RequestParam(defaultValue = "false") boolean preview,
            @AuthenticationPrincipal AdminPrincipal principal) {
        Program program =
                (preview && principal != null) ? programService.getForPreview(slug) : programService.getPublished(slug);
        return ApiResponse.success(ProgramDetailResponse.from(program, lang, imagesFor(program)));
    }

    private MediaAssetResponse thumbnailFor(Program program) {
        return mediaService.listPublished(MediaOwnerType.PROGRAM, program.getId()).stream()
                .findFirst()
                .map(asset -> MediaAssetResponse.from(asset, mediaService))
                .orElse(null);
    }

    private List<MediaAssetResponse> imagesFor(Program program) {
        return mediaService.listPublished(MediaOwnerType.PROGRAM, program.getId()).stream()
                .map(asset -> MediaAssetResponse.from(asset, mediaService))
                .toList();
    }
}
