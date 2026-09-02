package com.finediningtheater.global.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 확장자 없는 문서 요청(SPA 라우트 진입점)에 index.html을 서빙한다. 프론트 배포는
 * 공유 볼륨(web-dist)에 새 index.html을 덮어쓰는 방식이라(§13.6), 부팅 시 한 번만 읽어
 * 메모리에 고정하면 프론트 재배포 후에도 옛 에셋 해시를 계속 내려준다 — 그래서 매 요청
 * mtime을 확인하고 바뀐 경우에만 다시 읽는다(§10).
 *
 * <p>라우트별 메타 태그 주입(제목·설명·OG 이미지·hreflang)은 여기 없다 — 경로별로 어떤
 * Production/Program을 참조하는지 DB에서 조회해야 하는 별도 기능이라 MetaInjectionFilter로
 * 이어서 만든다(§10). 지금은 정적 서빙 + 캐시 무효화까지만 한다.
 */
@Component
public class IndexHtmlFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IndexHtmlFilter.class);

    private final Path indexPath;
    private volatile CachedIndex cached;

    public IndexHtmlFilter(@Value("${app.web.index-html-path:/web-dist/index.html}") String indexHtmlPath) {
        this.indexPath = Path.of(indexHtmlPath);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean hasExtension = uri.substring(uri.lastIndexOf('/') + 1).contains(".");
        return !HttpMethod.GET.matches(request.getMethod()) || uri.startsWith("/api/") || hasExtension;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String html = readIndexHtml();
        if (html == null) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "public, max-age=60");
        response.getWriter().write(html);
    }

    private String readIndexHtml() {
        try {
            Instant mtime = Files.getLastModifiedTime(indexPath).toInstant();
            CachedIndex current = cached;
            if (current != null && current.mtime().equals(mtime)) {
                return current.content();
            }
            String content = Files.readString(indexPath, StandardCharsets.UTF_8);
            cached = new CachedIndex(content, mtime);
            return content;
        } catch (IOException e) {
            log.warn("index.html을 읽을 수 없습니다 ({}): {}", indexPath, e.getMessage());
            return null;
        }
    }

    private record CachedIndex(String content, Instant mtime) {}
}
