package com.finediningtheater.artist;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.global.support.SiteLocale;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArtistController.class)
@AutoConfigureMockMvc(addFilters = false)
class ArtistControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ArtistService artistService;
    @MockitoBean private JwtProvider jwtProvider;

    @Test
    void 발행된_아티스트_목록을_반환한다() throws Exception {
        Artist artist = new Artist("kim-artist");
        artist.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개");
        when(artistService.listPublished()).thenReturn(List.of(artist));

        mockMvc.perform(get("/api/artists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("kim-artist"))
                .andExpect(jsonPath("$.data[0].name").value("김아무개"));
    }

    @Test
    void 존재하지_않는_슬러그는_404를_반환한다() throws Exception {
        when(artistService.getPublished("unknown")).thenThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        mockMvc.perform(get("/api/artists/unknown")).andExpect(status().isNotFound());
    }
}
