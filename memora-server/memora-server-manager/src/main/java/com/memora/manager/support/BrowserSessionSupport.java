package com.memora.manager.support;

import com.memora.manager.vo.AuthSessionVO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BrowserSessionSupport {
    public static final String CLIENT_HEADER = "X-Memora-Client";
    public static final String WEB_APP_CLIENT = "memora-web-app";

    @Value("${memora.auth.session-cookie.name:MEMORA_SESSION}")
    private String cookieName;

    @Value("${memora.auth.session-cookie.path:/}")
    private String cookiePath;

    @Value("${memora.auth.session-cookie.same-site:Lax}")
    private String sameSite;

    @Value("${memora.auth.session-cookie.secure:false}")
    private boolean secure;

    @Value("${memora.auth.session-cookie.max-age-seconds:2592000}")
    private long cookieMaxAgeSeconds;

    public boolean isWebAppClient(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        return WEB_APP_CLIENT.equalsIgnoreCase(request.getHeader(CLIENT_HEADER));
    }

    public boolean hasSessionCookie(HttpServletRequest request) {
        return StringUtils.hasText(resolveSessionCookie(request));
    }

    public boolean shouldClearSessionCookie(HttpServletRequest request) {
        return isWebAppClient(request) || hasSessionCookie(request);
    }

    public String resolveSessionCookie(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookie != null && cookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue().trim();
            }
        }
        return null;
    }

    public void writeSessionCookie(HttpServletResponse response, String accessToken) {
        if (response == null || !StringUtils.hasText(accessToken)) {
            return;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(accessToken.trim(), cookieMaxAgeSeconds).toString());
    }

    public void clearSessionCookie(HttpServletResponse response) {
        if (response == null) {
            return;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", 0).toString());
    }

    public AuthSessionVO sanitizeBrowserSession(AuthSessionVO session) {
        if (session == null) {
            return null;
        }
        session.setAccessToken(null);
        return session;
    }

    private ResponseCookie buildCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(cookieName, value)
            .path(StringUtils.hasText(cookiePath) ? cookiePath : "/")
            .httpOnly(true)
            .secure(secure)
            .sameSite(StringUtils.hasText(sameSite) ? sameSite : "Lax")
            .maxAge(maxAgeSeconds)
            .build();
    }
}
