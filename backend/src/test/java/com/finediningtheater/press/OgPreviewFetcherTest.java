package com.finediningtheater.press;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.finediningtheater.global.security.SafeUrlFetcher;
import java.net.URI;
import org.junit.jupiter.api.Test;

/** 네트워크 없이 순수 파싱 로직만 검증한다 — 실제 요청은 SafeUrlFetcher가 이미 별도로 다룬다. */
class OgPreviewFetcherTest {

    private final OgPreviewFetcher fetcher = new OgPreviewFetcher(new SafeUrlFetcher());

    @Test
    void og_태그를_전부_뽑는다() {
        String html =
                """
                <html><head>
                <meta property="og:title" content="기사 제목입니다">
                <meta property="og:description" content="기사 요약">
                <meta property="og:image" content="https://cdn.example.com/a.jpg">
                </head></html>
                """;

        OgPreviewFetcher.OgPreview preview = fetcher.parse(html, URI.create("https://news.example.com/article"));

        assertThat(preview.title()).isEqualTo("기사 제목입니다");
        assertThat(preview.description()).isEqualTo("기사 요약");
        assertThat(preview.imageUrl()).isEqualTo("https://cdn.example.com/a.jpg");
    }

    @Test
    void og_태그가_없으면_twitter_카드로_폴백한다() {
        String html =
                """
                <meta name="twitter:title" content="트위터 제목">
                <meta name="twitter:image" content="/relative/a.jpg">
                """;

        OgPreviewFetcher.OgPreview preview = fetcher.parse(html, URI.create("https://news.example.com/article"));

        assertThat(preview.title()).isEqualTo("트위터 제목");
        assertThat(preview.imageUrl()).isEqualTo("https://news.example.com/relative/a.jpg");
    }

    @Test
    void 상대경로_이미지는_페이지_URL_기준으로_절대경로가_된다() {
        String html = "<meta property=\"og:image\" content=\"images/thumb.jpg\">";

        OgPreviewFetcher.OgPreview preview =
                fetcher.parse(html, URI.create("https://news.example.com/section/article.html"));

        assertThat(preview.imageUrl()).isEqualTo("https://news.example.com/section/images/thumb.jpg");
    }

    @Test
    void 메타태그가_전혀_없으면_전부_null이다() {
        OgPreviewFetcher.OgPreview preview = fetcher.parse("<html><body>본문</body></html>", URI.create("https://x.com"));

        assertNull(preview.title());
        assertNull(preview.description());
        assertNull(preview.imageUrl());
    }

    @Test
    void HTML_엔티티를_풀어서_반환한다() {
        String html = "<meta property=\"og:title\" content=\"김&amp;박 &quot;맛집&quot; 후기\">";

        OgPreviewFetcher.OgPreview preview = fetcher.parse(html, URI.create("https://x.com"));

        assertThat(preview.title()).isEqualTo("김&박 \"맛집\" 후기");
    }
}
