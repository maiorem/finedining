package com.finediningtheater.site;

import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.site.dto.SiteStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 프론트가 "준비 중" 화면을 보여줄지 정하려고 부팅 때 묻는다. 비공개여도 누구나 호출할 수 있다. */
@RestController
@RequestMapping("/api/site")
@RequiredArgsConstructor
public class SiteController {

    private final SiteVisibilityService siteVisibilityService;

    @GetMapping("/status")
    public ApiResponse<SiteStatusResponse> status() {
        return ApiResponse.success(new SiteStatusResponse(siteVisibilityService.isPublic()));
    }
}
