package com.finediningtheater.artist.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 이 아티스트가 참여한 작품 전체를 이 목록으로 교체한다(추가/삭제가 아니라 치환). */
public record LinkProductionsRequest(@NotNull List<Long> productionIds) {}
