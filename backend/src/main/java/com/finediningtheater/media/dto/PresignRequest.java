package com.finediningtheater.media.dto;

import com.finediningtheater.media.MediaOwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PresignRequest(
        @NotNull MediaOwnerType ownerType,
        @NotNull Long ownerId,
        @NotBlank String contentType,
        @Positive long contentLengthBytes) {}
