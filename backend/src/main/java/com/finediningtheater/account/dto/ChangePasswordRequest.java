package com.finediningtheater.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 10, max = 100, message = "비밀번호는 10자 이상이어야 합니다.") String newPassword) {}
