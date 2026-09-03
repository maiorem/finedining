package com.finediningtheater.press;

import static org.assertj.core.api.Assertions.assertThat;

import com.finediningtheater.global.support.ContentStatus;
import org.junit.jupiter.api.Test;

class PressClippingTest {

    private PressClipping clipping() {
        return new PressClipping("조선일보 - 파인다이닝 씨어터", "https://example.com/article");
    }

    @Test
    void 생성하면_DRAFT_상태다() {
        PressClipping clipping = clipping();

        assertThat(clipping.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(clipping.isPublished()).isFalse();
    }

    @Test
    void updateContent은_제목과_링크를_즉시_바꾼다() {
        PressClipping clipping = clipping();

        clipping.updateContent("새 제목", "https://example.com/new");

        assertThat(clipping.getTitle()).isEqualTo("새 제목");
        assertThat(clipping.getExternalUrl()).isEqualTo("https://example.com/new");
    }

    @Test
    void publish하면_PUBLISHED가_된다() {
        PressClipping clipping = clipping();

        clipping.publish(1L);

        assertThat(clipping.isPublished()).isTrue();
        assertThat(clipping.getPublishedBy()).isEqualTo(1L);
    }

    @Test
    void unpublish하면_DRAFT로_돌아간다() {
        PressClipping clipping = clipping();
        clipping.publish(1L);

        clipping.unpublish();

        assertThat(clipping.isPublished()).isFalse();
        assertThat(clipping.getPublishedBy()).isNull();
    }
}
