package com.finediningtheater.site.dto;

/** open=true면 누구나 볼 수 있는 공개 상태, false면 관리자만 볼 수 있는 비공개 상태다. */
public record SiteStatusResponse(boolean open) {}
