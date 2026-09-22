package com.finediningtheater.production.dto;

import jakarta.validation.constraints.Size;

public record ChangeProductionNolBookingUrlRequest(@Size(max = 500) String nolBookingUrl) {}
