package com.finediningtheater.production;

import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.dto.ProductionDetailResponse;
import com.finediningtheater.production.dto.ProductionSummaryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public ApiResponse<List<ProductionSummaryResponse>> list(
            @RequestParam(defaultValue = "KO") SiteLocale lang) {
        List<ProductionSummaryResponse> body =
                productionService.listPublished().stream()
                        .map(production -> ProductionSummaryResponse.from(production, lang))
                        .toList();
        return ApiResponse.success(body);
    }

    @GetMapping("/{slug}")
    public ApiResponse<ProductionDetailResponse> detail(
            @PathVariable String slug, @RequestParam(defaultValue = "KO") SiteLocale lang) {
        Production production = productionService.getPublished(slug);
        return ApiResponse.success(ProductionDetailResponse.from(production, lang));
    }
}
