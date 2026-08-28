package com.finediningtheater.media;

/** 이미지가 어느 도메인 엔티티에 붙는지. media 패키지는 도메인에 묶이지 않는다(CLAUDE.md §6). */
public enum MediaOwnerType {
    PRODUCTION,
    ARTIST
}
