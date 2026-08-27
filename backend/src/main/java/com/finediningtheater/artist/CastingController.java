package com.finediningtheater.artist;

import com.finediningtheater.artist.dto.CastingResponse;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.support.SiteLocale;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 공개 조회 전용 — 열람만 가능하다(§3.8). */
@RestController
@RequestMapping("/api/castings")
@RequiredArgsConstructor
public class CastingController {

    private final CastingService castingService;

    @GetMapping
    public ApiResponse<List<CastingResponse>> list(@RequestParam(defaultValue = "KO") SiteLocale lang) {
        List<CastingResponse> body =
                castingService.listPublished().stream().map(c -> CastingResponse.from(c, lang)).toList();
        return ApiResponse.success(body);
    }
}
