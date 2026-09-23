package com.finediningtheater.production;

import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.MediaAsset;
import com.finediningtheater.media.MediaOwnerType;
import com.finediningtheater.media.MediaService;
import com.finediningtheater.media.dto.MediaAssetResponse;
import com.finediningtheater.production.dto.ProductionDetailResponse;
import com.finediningtheater.production.dto.ProductionSummaryResponse;
import java.util.ArrayList;
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

    /**
     * 관리자가 대표 이미지로 지정한 사진이 있으면 맨 앞으로 보낸다 — 프론트는 첫 번째 사진을
     * 그대로 제목 옆 큰 이미지(히어로)로 쓰고 나머지를 아래 갤러리로 쓴다({@code
     * ProductionDetailPage.tsx}). 지정한 사진이 삭제됐거나 아직 발행 전이라 목록에 없으면
     * 조용히 원래 순서(=목록 대표사진이 맨 앞)로 되돌아간다.
     */
    private List<MediaAssetResponse> imagesFor(Production production) {
        List<MediaAsset> assets = mediaService.listPublished(MediaOwnerType.PRODUCTION, production.getId());
        return orderedWithHeroFirst(assets, production.getHeroImageId()).stream()
                .map(asset -> MediaAssetResponse.from(asset, mediaService))
                .toList();
    }

    // static + package-private: 순수 정렬 로직이라 MediaAssetResponse 변환 없이 바로 단위 테스트한다.
    static List<MediaAsset> orderedWithHeroFirst(List<MediaAsset> assets, Long heroImageId) {
        if (heroImageId == null) {
            return assets;
        }
        int heroIndex = -1;
        for (int i = 0; i < assets.size(); i++) {
            if (assets.get(i).getId().equals(heroImageId)) {
                heroIndex = i;
                break;
            }
        }
        if (heroIndex <= 0) {
            return assets;
        }
        List<MediaAsset> reordered = new ArrayList<>(assets);
        MediaAsset hero = reordered.remove(heroIndex);
        reordered.add(0, hero);
        return reordered;
    }
}
