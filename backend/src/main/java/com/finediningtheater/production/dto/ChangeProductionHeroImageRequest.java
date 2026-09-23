package com.finediningtheater.production.dto;

/** heroImageId가 null이면 대표 이미지 지정을 해제한다(목록 대표사진으로 되돌아간다). */
public record ChangeProductionHeroImageRequest(Long heroImageId) {}
