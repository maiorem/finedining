package com.finediningtheater.global.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PublishableTest {

    private static class SampleContent extends Publishable {}

    @Test
    void 기본_상태는_DRAFT다() {
        SampleContent content = new SampleContent();

        assertThat(content.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(content.isPublished()).isFalse();
    }

    @Test
    void publish하면_PUBLISHED로_바뀌고_발행자와_시각이_기록된다() {
        SampleContent content = new SampleContent();

        content.publish(42L);

        assertThat(content.isPublished()).isTrue();
        assertThat(content.getPublishedBy()).isEqualTo(42L);
        assertThat(content.getPublishedAt()).isNotNull();
    }

    @Test
    void unpublish하면_DRAFT로_되돌아가고_발행_정보가_지워진다() {
        SampleContent content = new SampleContent();
        content.publish(42L);

        content.unpublish();

        assertThat(content.isPublished()).isFalse();
        assertThat(content.getPublishedBy()).isNull();
        assertThat(content.getPublishedAt()).isNull();
    }
}
