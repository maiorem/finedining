package com.finediningtheater.global.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.finediningtheater.site.SiteVisibilityService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IndexHtmlFilterTest {

    @TempDir Path tempDir;

    private final SiteVisibilityService siteVisibilityService = mock(SiteVisibilityService.class);
    private IndexHtmlFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        Path index = tempDir.resolve("index.html");
        Files.writeString(index, "<html><head><title>t</title></head><body></body></html>");
        filter = new IndexHtmlFilter(index.toString(), siteVisibilityService);
    }

    private MockHttpServletResponse serve() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/proposal"), response, new MockFilterChain());
        return response;
    }

    @Test
    void 공개_상태면_noindex를_넣지_않고_짧게_캐시한다() throws Exception {
        when(siteVisibilityService.isPublic()).thenReturn(true);

        MockHttpServletResponse response = serve();

        assertThat(response.getContentAsString()).doesNotContain("noindex");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("public, max-age=60");
        assertThat(response.getHeader("X-Robots-Tag")).isNull();
    }

    @Test
    void 비공개_상태면_noindex를_넣고_캐시하지_않는다() throws Exception {
        when(siteVisibilityService.isPublic()).thenReturn(false);

        MockHttpServletResponse response = serve();

        assertThat(response.getContentAsString()).contains("<meta name=\"robots\" content=\"noindex, nofollow\" /></head>");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("X-Robots-Tag")).isEqualTo("noindex, nofollow");
    }

    @Test
    void 공개_여부를_읽지_못해도_HTML은_서빙하고_비공개로_취급한다() throws Exception {
        when(siteVisibilityService.isPublic()).thenThrow(new IllegalStateException("db down"));

        MockHttpServletResponse response = serve();

        assertThat(response.getContentAsString()).contains("noindex");
    }
}
