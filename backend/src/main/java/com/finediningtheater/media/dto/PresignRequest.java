package com.finediningtheater.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PresignRequest(
        @NotNull Long productionId, @NotBlank String contentType, @Positive long contentLengthBytes) {}
