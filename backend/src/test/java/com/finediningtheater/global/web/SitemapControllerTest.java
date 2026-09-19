package com.finediningtheater.global.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.artist.Artist;
import com.finediningtheater.artist.ArtistService;
import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.production.Production;
import com.finediningtheater.production.ProductionService;
import com.finediningtheater.program.Program;
import com.finediningtheater.program.ProgramService;
import com.finediningtheater.site.SiteVisibilityService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SitemapController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "app.frontend-url=https://example.com/")
class SitemapControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ProductionService productionService;
    @MockitoBean private ProgramService programService;
    @MockitoBean private ArtistService artistService;
    @MockitoBean private SiteVisibilityService siteVisibilityService;
    @MockitoBean private JwtProvider jwtProvider;

    @Test
    void 공개_상태면_고정_페이지와_발행된_콘텐츠를_나열한다() throws Exception {
        when(siteVisibilityService.isPublic()).thenReturn(true);
        when(productionService.listPublished()).thenReturn(List.of(new Production("father-table")));
        when(programService.listPublished()).thenReturn(List.of(new Program("fun-brunch")));
        when(artistService.listPublished()).thenReturn(List.of(new Artist("kim-miran")));

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://example.com/</loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://example.com/about</loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://example.com/productions/father-table</loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://example.com/programs/fun-brunch</loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://example.com/artists/kim-miran</loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/login"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/reviews"))));
    }

    @Test
    void 비공개_상태면_빈_urlset을_돌려주고_콘텐츠를_조회하지_않는다() throws Exception {
        when(siteVisibilityService.isPublic()).thenReturn(false);

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<url>"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<urlset")));

        verifyNoInteractions(productionService, programService, artistService);
    }

    @Test
    void 슬러그의_특수문자는_이스케이프한다() throws Exception {
        when(siteVisibilityService.isPublic()).thenReturn(true);
        when(productionService.listPublished()).thenReturn(List.of(new Production("a&b")));

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/productions/a&amp;b")));
        verify(productionService).listPublished();
    }
}
