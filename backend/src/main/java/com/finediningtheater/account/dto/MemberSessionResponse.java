package com.finediningtheater.account.dto;

public record MemberSessionResponse(Long accountId, String accessToken, String nickname) {}
