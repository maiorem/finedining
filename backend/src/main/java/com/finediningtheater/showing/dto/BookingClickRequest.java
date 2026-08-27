package com.finediningtheater.showing.dto;

import jakarta.validation.constraints.Size;

/**
 * navigator.sendBeacon으로 인증 없이 들어오므로 어떤 값도 신뢰하지 않는다 — 전부 nullable이고
 * 길이만 제한한다 (CLAUDE.md §7.7).
 */
public record BookingClickRequest(
        @Size(max = 40) String channel,
        @Size(max = 10) String locale,
        @Size(max = 100) String utmSource,
        @Size(max = 100) String utmMedium,
        @Size(max = 100) String utmCampaign) {}
