package com.finediningtheater.production;

import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.MediaOwnerType;
import com.finediningtheater.media.MediaService;
import com.finediningtheater.media.dto.MediaAssetResponse;
import com.finediningtheater.production.dto.ProductionDetailResponse;
import com.finediningtheater.production.dto.ProductionSummaryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 공개 조회 전용. 쓰기는 ProductionEditController(2순위: 관리자 로그인)에서 추가한다. */
@RestController
@RequestMapping("/api/productions")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;
    private final MediaService mediaService;

    @GetMapping
    public ApiResponse<List<ProductionSummaryResponse>> list(
            @RequestParam(defaultValue = "KO") SiteLocale lang) {
        List<ProductionSummaryResponse> body =
                productionService.listPublished().stream()
                        .map(production -> ProductionSummaryResponse.from(production, lang, thumbnailFor(production)))
                        .toList();
        return ApiResponse.success(body);
    }

    /**
     * preview=true는 인증된 관리자에게만 적용된다 — 익명 요청은 서버에서 무시한다(CLAUDE.md
     * §3.9). 방금 만든 초안 작품처럼 아직 발행되지 않은 작품도 관리자는 같은 URL에서 편집 패널에
     * 붙을 수 있어야 한다.
     */
    @GetMapping("/{slug}")
    public ApiResponse<ProductionDetailResponse> detail(
            @PathVariable String slug,
            @RequestParam(defaultValue = "KO") SiteLocale lang,
            @RequestParam(defaultValue = "false") boolean preview,
            @AuthenticationPrincipal AdminPrincipal principal) {
        Production production =
                (preview && principal != null) ? productionService.getForPreview(slug) : productionService.getPublished(slug);
        List<MediaAssetResponse> images = imagesFor(production);
        return ApiResponse.success(ProductionDetailResponse.from(production, lang, images));
    }

    private MediaAssetResponse thumbnailFor(Production production) {
        return mediaService.listPublished(MediaOwnerType.PRODUCTION, production.getId()).stream()
                .findFirst()
                .map(asset -> MediaAssetResponse.from(asset, mediaService))
                .orElse(null);
    }

    private List<MediaAssetResponse> imagesFor(Production production) {
        return mediaService.listPublished(MediaOwnerType.PRODUCTION, production.getId()).stream()
                .map(asset -> MediaAssetResponse.from(asset, mediaService))
                .toList();
    }
}
