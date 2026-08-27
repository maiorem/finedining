package com.finediningtheater.global.support;

import jakarta.servlet.http.HttpServletRequest;

/**
 * CloudFront/nginx 뒤에 있으므로 X-Forwarded-For를 우선 본다 (CLAUDE.md §13.3).
 */
public final class ClientIp {

    private ClientIp() {}

    public static String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
