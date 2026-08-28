package com.finediningtheater.artist;

import com.finediningtheater.artist.dto.ArtistAdminResponse;
import com.finediningtheater.artist.dto.ChangeArtistLinkRequest;
import com.finediningtheater.artist.dto.CreateArtistRequest;
import com.finediningtheater.artist.dto.UpsertArtistTranslationRequest;
import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.SudoMode;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.MediaOwnerType;
import com.finediningtheater.media.MediaService;
import com.finediningtheater.media.dto.MediaAssetResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 아티스트 편집(기능6). Production과 같은 패턴이다(§3.8) — 발행/발행취소는 파괴적·공개적
 * 동작이라 sudo 모드를 요구한다(§3.4). 모든 쓰기에 감사 로그를 남긴다(§7.7).
 */
@RestController
@RequestMapping("/api/artists")
@PreAuthorize("hasRole('EDITOR')")
@RequiredArgsConstructor
public class ArtistEditController {

    private final ArtistService artistService;
    private final MediaService mediaService;
    private final AuditLogger auditLogger;
    private final SudoMode sudoMode;

    @GetMapping("/manage")
    public ApiResponse<List<ArtistAdminResponse>> listForAdmin() {
        // 목록에서는 아티스트마다 이미지를 함께 불러올 필요가 없다 — N+1을 피하려고 빈 목록으로 둔다.
        List<ArtistAdminResponse> body =
                artistService.listForAdmin().stream().map(a -> ArtistAdminResponse.from(a, List.of())).toList();
        return ApiResponse.success(body);
    }

    @GetMapping("/manage/{id}")
    public ApiResponse<ArtistAdminResponse> getForAdmin(@PathVariable Long id) {
        return ApiResponse.success(toAdminResponse(artistService.getForAdmin(id)));
    }

    @PostMapping
    public ApiResponse<ArtistAdminResponse> create(
            @Valid @RequestBody CreateArtistRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        Artist artist = artistService.create(request.slug());

        auditLogger.record(
                principal.id(),
                "ARTIST_CREATE",
                "Artist",
                artist.getId(),
                null,
                Map.of("slug", request.slug()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(artist));
    }

    /** "임시저장" — 공개본에는 영향이 없다(§3.9). */
    @PutMapping("/{id}/translations/{locale}")
    public ApiResponse<ArtistAdminResponse> saveDraftTranslation(
            @PathVariable Long id,
            @PathVariable SiteLocale locale,
            @Valid @RequestBody UpsertArtistTranslationRequest request) {
        artistService.saveDraftTranslation(
                id, locale, request.name(), request.role(), request.bio(), request.credits());
        return ApiResponse.success(toAdminResponse(artistService.getForAdmin(id)));
    }

    @PutMapping("/{id}/link")
    public ApiResponse<ArtistAdminResponse> changeLink(
            @PathVariable Long id, @Valid @RequestBody ChangeArtistLinkRequest request) {
        return ApiResponse.success(toAdminResponse(artistService.changeLinkUrl(id, request.linkUrl())));
    }

    /** 파괴적·공개적 동작 — PIN sudo 모드가 열려 있어야 한다(§3.4). */
    @PostMapping("/{id}/publish")
    public ApiResponse<ArtistAdminResponse> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeStatus = artistService.getForAdmin(id).getStatus().name();
        Artist after = artistService.publish(id, principal.id());

        auditLogger.record(
                principal.id(),
                "ARTIST_PUBLISH",
                "Artist",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(after));
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<ArtistAdminResponse> unpublish(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeStatus = artistService.getForAdmin(id).getStatus().name();
        Artist after = artistService.unpublish(id);

        auditLogger.record(
                principal.id(),
                "ARTIST_UNPUBLISH",
                "Artist",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(after));
    }

    // 관리자는 PENDING·FAILED 이미지도 봐야 재시도·삭제할 수 있으므로 listForAdmin(전체)을 쓴다.
    private ArtistAdminResponse toAdminResponse(Artist artist) {
        List<MediaAssetResponse> images =
                mediaService.listForAdmin(MediaOwnerType.ARTIST, artist.getId()).stream()
                        .map(asset -> MediaAssetResponse.from(asset, mediaService))
                        .toList();
        return ArtistAdminResponse.from(artist, images);
    }
}
