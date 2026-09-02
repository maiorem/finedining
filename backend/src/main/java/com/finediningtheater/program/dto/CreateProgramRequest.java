package com.finediningtheater.program.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProgramRequest(
        @NotBlank
                @Size(max = 191)
                @Pattern(regexp = "^[a-z0-9-]+$", message = "소문자·숫자·하이픈만 사용해 주세요.")
                String slug) {}
