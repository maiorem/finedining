package com.finediningtheater.account;

/**
 * 로그인/재발급 성공 시 서비스가 돌려주는 값. HTTP로 그대로 나가지 않는다. 관리자의 AdminSession과
 * 달리 public이다 — OAuth2LoginSuccessHandler(global.security 패키지)가 로그인 직후 이 값을
 * 받아 리다이렉트를 만들어야 한다.
 */
public record MemberSession(String accessToken, String refreshToken, String nickname) {}
