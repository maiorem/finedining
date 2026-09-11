package com.finediningtheater.global.security;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * 관리자가 입력한 외부 URL을 서버가 대신 요청하는 모든 기능(보도자료 링크 미리보기, 기사 이미지
 * 다운로드)이 공유하는 SSRF 방어 게이트. 인증된 EDITOR만 호출할 수 있지만, 서버가 임의 URL을
 * 대신 열어주는 이상 사설망·AWS 인스턴스 메타데이터(169.254.169.254, §13.7의 인스턴스 역할이
 * 여기 노출되면 자격증명 탈취로 이어진다)로 향하는 요청은 여기서 끊어야 한다.
 */
@Component
public class SafeUrlFetcher {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_REDIRECTS = 2;

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NEVER) // 리다이렉트마다 재검증하려고 수동으로 따라간다
                    .build();

    public record FetchResult(byte[] body, String contentType) {}

    /** SSRF 검증을 통과한 URL만 GET하고, 응답 바이트 수를 {@code maxBytes}에서 자른다. */
    public FetchResult fetch(String url, int maxBytes) throws IOException, InterruptedException {
        URI uri = URI.create(url);
        for (int redirects = 0; ; redirects++) {
            requireSafe(uri);

            HttpRequest request =
                    HttpRequest.newBuilder(uri)
                            .timeout(READ_TIMEOUT)
                            .header("User-Agent", "FineDiningTheaterBot/1.0")
                            .GET()
                            .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                if (redirects >= MAX_REDIRECTS) {
                    throw new IOException("리다이렉트가 너무 많습니다.");
                }
                String location =
                        response.headers().firstValue("Location").orElseThrow(() -> new IOException("잘못된 리다이렉트 응답입니다."));
                uri = uri.resolve(location);
                continue;
            }
            if (status != 200) {
                throw new IOException("응답 상태 코드가 올바르지 않습니다: " + status);
            }

            String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
            byte[] body = readCapped(response.body(), maxBytes);
            return new FetchResult(body, contentType);
        }
    }

    private byte[] readCapped(InputStream in, int maxBytes) throws IOException {
        try (in) {
            byte[] buffer = new byte[8192];
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            int read;
            while ((read = in.read(buffer)) != -1) {
                if (out.size() + read > maxBytes) {
                    throw new IOException("응답 크기가 상한을 넘었습니다.");
                }
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    /** http(s) 스킴 + 사설/루프백/링크로컬 대역이 아닌 호스트만 통과시킨다. */
    private void requireSafe(URI uri) throws IOException {
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
            throw new IOException("http(s) URL만 허용됩니다.");
        }
        if (uri.getHost() == null) {
            throw new IOException("호스트가 없는 URL입니다.");
        }

        InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
        for (InetAddress address : addresses) {
            if (address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isAnyLocalAddress()
                    || address.isMulticastAddress()) {
                throw new IOException("사설·내부 네트워크로는 요청할 수 없습니다.");
            }
        }
    }
}
