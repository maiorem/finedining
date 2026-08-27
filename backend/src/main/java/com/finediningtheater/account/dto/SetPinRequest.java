package com.finediningtheater.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetPinRequest(
        @NotBlank String currentPassword,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "6자리 숫자로 입력해 주세요.") String newPin) {}
