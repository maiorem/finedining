package com.finediningtheater.press;

import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.media.MediaAsset;
import com.finediningtheater.media.MediaOwnerType;
import com.finediningtheater.media.MediaService;
import com.finediningtheater.press.dto.PressClippingResponse;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 조회 전용. 등록·수정·발행은 PressClippingEditController가 맡는다(§3.5). */
@RestController
@RequestMapping("/api/press-clippings")
@RequiredArgsConstructor
public class PressClippingController {

    private final PressClippingService pressClippingService;
    private final MediaService mediaService;

    @GetMapping
    public ApiResponse<List<PressClippingResponse>> list() {
        List<PressClippingResponse> body =
                pressClippingService.listPublished().stream()
                        .map(
                                clipping -> {
                                    Optional<MediaAsset> image =
                                            mediaService
                                                    .listPublished(MediaOwnerType.PRESS_CLIPPING, clipping.getId())
                                                    .stream()
                                                    .findFirst();
                                    return PressClippingResponse.from(clipping, image.orElse(null), mediaService);
                                })
                        .toList();
        return ApiResponse.success(body);
    }
}
