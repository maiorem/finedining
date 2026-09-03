package com.finediningtheater.account;

/**
 * 로그인/재발급 성공 시 서비스가 돌려주는 값. HTTP로 그대로 나가지 않는다. 관리자의 AdminSession과
 * 달리 public이다 — OAuth2LoginSuccessHandler(global.security 패키지)가 로그인 직후 이 값을
 * 받아 리다이렉트를 만들어야 한다.
 *
 * <p>accountId를 담는 이유: 프론트가 리뷰의 accountId와 비교해 "내 글인가"를 판단하고 수정·삭제
 * 버튼을 보여줄지 정해야 한다(서버는 이 값과 무관하게 매 요청마다 소유권을 다시 검사한다 — §3.3).
 */
public record MemberSession(Long accountId, String accessToken, String refreshToken, String nickname) {}
