package com.finediningtheater.artist;

import com.finediningtheater.artist.dto.ArtistDetailResponse;
import com.finediningtheater.artist.dto.ArtistSummaryResponse;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.MediaOwnerType;
import com.finediningtheater.media.MediaService;
import com.finediningtheater.media.dto.MediaAssetResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 공개 조회 전용. 로그인 없이 전부 볼 수 있다(§1) — 하트를 뺐으므로 뷰어 종속 데이터가 없다. */
@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;
    private final MediaService mediaService;

    @GetMapping
    public ApiResponse<List<ArtistSummaryResponse>> list(@RequestParam(defaultValue = "KO") SiteLocale lang) {
        List<ArtistSummaryResponse> body =
                artistService.listPublished().stream()
                        .map(a -> ArtistSummaryResponse.from(a, lang, photoFor(a)))
                        .toList();
        return ApiResponse.success(body);
    }

    /**
     * preview=true는 인증된 관리자에게만 적용된다 — 익명 요청은 서버에서 무시한다(CLAUDE.md
     * §3.9). 방금 만든 초안 아티스트도 관리자는 같은 URL에서 편집 패널에 붙을 수 있어야 한다.
     */
    @GetMapping("/{slug}")
    public ApiResponse<ArtistDetailResponse> detail(
            @PathVariable String slug,
            @RequestParam(defaultValue = "KO") SiteLocale lang,
            @RequestParam(defaultValue = "false") boolean preview,
            @AuthenticationPrincipal AdminPrincipal principal) {
        Artist artist =
                (preview && principal != null) ? artistService.getForPreview(slug) : artistService.getPublished(slug);
        return ApiResponse.success(ArtistDetailResponse.from(artist, lang, photoFor(artist)));
    }

    private MediaAssetResponse photoFor(Artist artist) {
        return mediaService.listPublished(MediaOwnerType.ARTIST, artist.getId()).stream()
                .findFirst()
                .map(asset -> MediaAssetResponse.from(asset, mediaService))
                .orElse(null);
    }
}
