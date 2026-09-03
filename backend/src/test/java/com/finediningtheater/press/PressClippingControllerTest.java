package com.finediningtheater.press;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.media.MediaOwnerType;
import com.finediningtheater.media.MediaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PressClippingController.class)
@AutoConfigureMockMvc(addFilters = false)
class PressClippingControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PressClippingService pressClippingService;
    @MockitoBean private MediaService mediaService;
    @MockitoBean private JwtProvider jwtProvider;

    @Test
    void 공개된_보도자료_목록을_반환한다() throws Exception {
        when(pressClippingService.listPublished())
                .thenReturn(List.of(new PressClipping("조선일보 기사", "https://example.com/a")));
        when(mediaService.listPublished(any(MediaOwnerType.class), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/press-clippings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("조선일보 기사"))
                .andExpect(jsonPath("$.data[0].externalUrl").value("https://example.com/a"));
    }

    @Test
    void 보도자료가_없으면_빈_목록을_반환한다() throws Exception {
        when(pressClippingService.listPublished()).thenReturn(List.of());

        mockMvc.perform(get("/api/press-clippings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(0)));
    }
}
