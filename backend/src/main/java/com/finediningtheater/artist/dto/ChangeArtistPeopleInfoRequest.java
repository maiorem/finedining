package com.finediningtheater.artist.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 발행을 거치지 않고 즉시 반영되는 필드들. 빈 문자열은 "없음"이다. */
public record ChangeArtistPeopleInfoRequest(
        @Email @Size(max = 200) String email,
        @Min(0) @Max(9999) int displayOrder,
        @Size(max = 500)
                @Pattern(
                        regexp = "^(https://((www|m)\\.)?(youtube\\.com|youtu\\.be)/\\S+)?$",
                        message = "유튜브 주소(https://www.youtube.com/... 또는 https://youtu.be/...)를 입력해 주세요.")
                String interviewUrl) {}
