package com.finediningtheater.site.dto;

import jakarta.validation.constraints.NotNull;

// 원시 boolean이면 필드를 빼먹은 요청이 조용히 false(비공개)로 처리되므로 NotNull Boolean으로 받는다.
public record SiteVisibilityRequest(@NotNull Boolean open) {}
