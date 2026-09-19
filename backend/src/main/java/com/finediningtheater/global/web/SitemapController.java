package com.finediningtheater.global.web;

import com.finediningtheater.artist.ArtistService;
import com.finediningtheater.global.support.BaseTimeEntity;
import com.finediningtheater.production.ProductionService;
import com.finediningtheater.program.ProgramService;
import com.finediningtheater.site.SiteVisibilityService;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * sitemap.xml(CLAUDE.md §10) — 발행된 작품·프로그램·사람들만 동적으로 나열한다. 프리렌더가 없으므로
 * 발행 즉시 반영돼야 해서 정적 파일이 아니라 요청 시 만든다(앞단 CDN이 짧게 캐시한다).
 *
 * <p>오픈 전 비공개(SiteVisibilityService) 동안은 빈 목록을 돌려준다 — 검색엔진에 주소를 알려주지
 * 않는다. 이야기(리뷰)·로그인·내 계정은 noindex라 뺐다(§3.6·§3.5). 초안(DRAFT)은 리포지토리가
 * PUBLISHED만 돌려주므로 여기에 오지 않는다(§7.3).
 */
@RestController
public class SitemapController {

    private static final List<String> STATIC_PATHS =
            List.of("/", "/about", "/productions", "/programs", "/artists", "/proposal", "/privacy", "/terms");

    private final ProductionService productionService;
    private final ProgramService programService;
    private final ArtistService artistService;
    private final SiteVisibilityService siteVisibilityService;
    private final String baseUrl;

    public SitemapController(
            ProductionService productionService,
            ProgramService programService,
            ArtistService artistService,
            SiteVisibilityService siteVisibilityService,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.productionService = productionService;
        this.programService = programService;
        this.artistService = artistService;
        this.siteVisibilityService = siteVisibilityService;
        this.baseUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        List<String> entries = new ArrayList<>();
        if (siteVisibilityService.isPublic()) {
            STATIC_PATHS.forEach(path -> entries.add(entry(path, null)));
            productionService.listPublished().forEach(p -> entries.add(entry("/productions/" + p.getSlug(), p)));
            programService.listPublished().forEach(p -> entries.add(entry("/programs/" + p.getSlug(), p)));
            artistService.listPublished().forEach(a -> entries.add(entry("/artists/" + a.getSlug(), a)));
        }

        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        entries.forEach(xml::append);
        xml.append("</urlset>\n");
        return xml.toString();
    }

    private String entry(String path, BaseTimeEntity source) {
        StringBuilder sb = new StringBuilder("  <url><loc>").append(escape(baseUrl + path)).append("</loc>");
        if (source != null && source.getUpdatedAt() != null) {
            sb.append("<lastmod>").append(source.getUpdatedAt().atZone(ZoneOffset.UTC).toLocalDate()).append("</lastmod>");
        }
        return sb.append("</url>\n").toString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
