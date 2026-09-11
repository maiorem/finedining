package com.finediningtheater.press;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.security.SafeUrlFetcher;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 기사 링크에서 Open Graph 메타태그(og:title/og:description/og:image, 없으면 twitter: 카드로
 * 폴백)를 뽑아 관리자 화면의 "미리보기"에 쓴다(2026-09-12). Jsoup 같은 HTML 파서를 새로
 * 들이지 않기로 했으므로(CLAUDE.md §12 "새 의존성은 먼저 물어본다") 정규식으로 meta 태그만
 * 뽑는다 — DOM 전체를 이해할 필요 없이 이 목적에는 충분하고, 실패해도 관리자가 제목·이미지를
 * 수동 입력하는 기존 경로가 그대로 남아 있다.
 */
@Component
@RequiredArgsConstructor
public class OgPreviewFetcher {

    private static final int MAX_HTML_BYTES = 2 * 1024 * 1024;

    private static final Pattern META_TAG =
            Pattern.compile("<meta\\s+[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROPERTY_ATTR =
            Pattern.compile("(?:property|name)\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT_ATTR =
            Pattern.compile("content\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);

    private final SafeUrlFetcher safeUrlFetcher;

    public record OgPreview(String title, String description, String imageUrl) {}

    public OgPreview fetch(String pageUrl) {
        try {
            SafeUrlFetcher.FetchResult result = safeUrlFetcher.fetch(pageUrl, MAX_HTML_BYTES);
            if (!result.contentType().toLowerCase().contains("html")) {
                throw new IOException("HTML 문서가 아닙니다.");
            }
            String html = new String(result.body(), StandardCharsets.UTF_8);
            return parse(html, URI.create(pageUrl));
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException(ErrorCode.EXTERNAL_FETCH_FAILED);
        }
    }

    /** 네트워크 I/O 없이 순수하게 파싱만 한다 — 테스트가 실제 요청 없이 이 메서드만 검증한다. */
    OgPreview parse(String html, URI baseUri) {
        String title = firstOf(html, "og:title", "twitter:title");
        String description = firstOf(html, "og:description", "twitter:description");
        String image = firstOf(html, "og:image", "twitter:image");

        String resolvedImage = image == null ? null : resolveUrl(baseUri, image);
        return new OgPreview(title, description, resolvedImage);
    }

    private String firstOf(String html, String... metaNames) {
        Matcher tagMatcher = META_TAG.matcher(html);
        while (tagMatcher.find()) {
            String tag = tagMatcher.group();
            Matcher propertyMatcher = PROPERTY_ATTR.matcher(tag);
            if (!propertyMatcher.find()) {
                continue;
            }
            String property = propertyMatcher.group(1);
            for (String name : metaNames) {
                if (property.equalsIgnoreCase(name)) {
                    Matcher contentMatcher = CONTENT_ATTR.matcher(tag);
                    if (contentMatcher.find()) {
                        return unescapeHtml(contentMatcher.group(1));
                    }
                }
            }
        }
        return null;
    }

    private String resolveUrl(URI baseUri, String maybeRelative) {
        try {
            return baseUri.resolve(maybeRelative).toString();
        } catch (IllegalArgumentException e) {
            return maybeRelative;
        }
    }

    private String unescapeHtml(String value) {
        return value.replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }
}
