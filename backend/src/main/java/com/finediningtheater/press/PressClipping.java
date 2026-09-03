package com.finediningtheater.press;

import com.finediningtheater.global.support.Publishable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * 보도자료(소개 페이지의 "보도자료" 탭). 언론 기사 이미지 + 외부 링크로 구성된다(2026-09-04
 * 결정). Casting과 같은 열람 전용·슬러그 없음 패턴이지만 번역은 두지 않는다 — 실제 기사
 * 제목은 원문 그대로(대개 한국어 언론사 기사) 노출하는 게 맞아서 로케일별 제목을 따로 관리할
 * 이유가 없다. 이미지는 media 패키지가 ownerType=PRESS_CLIPPING으로 붙는다(§6).
 */
@Entity
@Getter
@Table(name = "press_clipping")
public class PressClipping extends Publishable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String externalUrl;

    protected PressClipping() {}

    public PressClipping(String title, String externalUrl) {
        this.title = title;
        this.externalUrl = externalUrl;
    }

    /** 제목·링크는 draft 없이 즉시 반영된다 — Artist.linkUrl과 같은 취급이다(§3.9). */
    public void updateContent(String title, String externalUrl) {
        this.title = title;
        this.externalUrl = externalUrl;
    }
}
