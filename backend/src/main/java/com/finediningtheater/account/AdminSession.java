package com.finediningtheater.account;

/** 로그인/재발급 성공 시 서비스가 컨트롤러에 돌려주는 내부 값. HTTP로 그대로 나가지 않는다. */
record AdminSession(String accessToken, String refreshToken, String username, AdminRole role) {}
