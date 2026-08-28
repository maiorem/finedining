package com.finediningtheater.showing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeBookingUrlRequest(@NotBlank @Size(max = 500) String bookingUrl) {}
