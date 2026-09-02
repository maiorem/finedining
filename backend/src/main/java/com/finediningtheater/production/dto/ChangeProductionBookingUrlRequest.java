package com.finediningtheater.production.dto;

import jakarta.validation.constraints.Size;

public record ChangeProductionBookingUrlRequest(@Size(max = 500) String bookingUrl) {}
