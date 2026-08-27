package com.finediningtheater.artist;

import com.finediningtheater.artist.dto.ArtistDetailResponse;
import com.finediningtheater.artist.dto.ArtistSummaryResponse;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.support.SiteLocale;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public ApiResponse<List<ArtistSummaryResponse>> list(@RequestParam(defaultValue = "KO") SiteLocale lang) {
        List<ArtistSummaryResponse> body =
                artistService.listPublished().stream().map(a -> ArtistSummaryResponse.from(a, lang)).toList();
        return ApiResponse.success(body);
    }

    @GetMapping("/{slug}")
    public ApiResponse<ArtistDetailResponse> detail(
            @PathVariable String slug, @RequestParam(defaultValue = "KO") SiteLocale lang) {
        Artist artist = artistService.getPublished(slug);
        return ApiResponse.success(ArtistDetailResponse.from(artist, lang));
    }
}
