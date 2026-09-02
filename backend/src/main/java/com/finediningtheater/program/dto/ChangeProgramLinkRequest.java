package com.finediningtheater.program.dto;

import jakarta.validation.constraints.Size;

/** applyUrl(참가하기)·locationUrl(위치보기) 변경 공용 요청 — ChangeArtistLinkRequest와 같은 패턴. */
public record ChangeProgramLinkRequest(@Size(max = 500) String url) {}
