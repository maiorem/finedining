package com.finediningtheater.production.dto;

import jakarta.validation.constraints.Size;

public record ChangeProductionTrailerUrlRequest(@Size(max = 500) String trailerUrl) {}
