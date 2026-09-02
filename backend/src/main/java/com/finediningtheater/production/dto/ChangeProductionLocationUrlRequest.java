package com.finediningtheater.production.dto;

import jakarta.validation.constraints.Size;

public record ChangeProductionLocationUrlRequest(@Size(max = 500) String locationUrl) {}
