package com.finediningtheater.program;

import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.program.dto.ProgramResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 공개 조회 전용 — 로그인 없이 전부 볼 수 있다(§1). */
@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    @GetMapping
    public ApiResponse<List<ProgramResponse>> list(@RequestParam(defaultValue = "KO") SiteLocale lang) {
        List<ProgramResponse> body =
                programService.listPublished().stream().map(p -> ProgramResponse.from(p, lang)).toList();
        return ApiResponse.success(body);
    }
}
