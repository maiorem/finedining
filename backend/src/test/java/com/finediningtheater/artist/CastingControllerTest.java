package com.finediningtheater.artist;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.global.support.SiteLocale;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CastingController.class)
@AutoConfigureMockMvc(addFilters = false)
class CastingControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CastingService castingService;
    @MockitoBean private JwtProvider jwtProvider;

    @Test
    void 발행된_모집_공고_목록을_반환한다() throws Exception {
        Casting casting = new Casting();
        casting.addTranslation(SiteLocale.KO, "배우 모집", "지원은 이메일로");
        when(castingService.listPublished()).thenReturn(List.of(casting));

        mockMvc.perform(get("/api/castings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("배우 모집"));
    }
}
