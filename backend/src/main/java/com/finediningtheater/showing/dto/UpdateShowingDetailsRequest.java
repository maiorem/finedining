package com.finediningtheater.showing.dto;

import com.finediningtheater.global.support.SiteLocale;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateShowingDetailsRequest(
        @NotNull Instant startsAt,
        @Positive int durationMinutes,
        @NotBlank @Size(max = 200) String venueName,
        @Size(max = 300) String venueAddress,
        @NotNull SiteLocale spokenLanguage,
        boolean interpretationAvailable) {}
