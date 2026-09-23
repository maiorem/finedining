package com.finediningtheater.production;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.finediningtheater.media.MediaAsset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 대표 이미지 지정이 상세 응답의 이미지 목록을 어떻게 바꾸는지 검증한다 — 프론트는 첫 번째
 * 이미지를 그대로 히어로로 쓰므로(§ProductionDetailPage.tsx) 순서가 핵심이고, 목록 대표사진
 * (원래 첫 번째)은 다른 사진이 히어로로 뽑히면 상세에서 완전히 빠져야 한다 — 그대로 두면
 * 히어로 자리에서 밀려나 본문 아래 갤러리에 다시 나타난다(2026-09-24).
 */
class ProductionControllerHeroOrderTest {

    private MediaAsset assetWithId(long id) {
        MediaAsset asset = mock(MediaAsset.class);
        when(asset.getId()).thenReturn(id);
        return asset;
    }

    @Test
    void 대표_이미지로_지정하지_않았으면_원래_순서_그대로다() {
        List<MediaAsset> assets = List.of(assetWithId(1), assetWithId(2), assetWithId(3));

        List<MediaAsset> result = ProductionController.orderedWithHeroFirst(assets, null);

        assertThat(result).containsExactly(assets.get(0), assets.get(1), assets.get(2));
    }

    @Test
    void 대표_이미지로_지정한_사진을_맨_앞으로_보내고_원래_대표사진은_뺀다() {
        MediaAsset first = assetWithId(1);
        MediaAsset second = assetWithId(2);
        MediaAsset third = assetWithId(3);
        List<MediaAsset> assets = List.of(first, second, third);

        List<MediaAsset> result = ProductionController.orderedWithHeroFirst(assets, 3L);

        assertThat(result).containsExactly(third, second);
    }

    @Test
    void 이미_맨_앞이면_그대로다() {
        List<MediaAsset> assets = List.of(assetWithId(1), assetWithId(2));

        List<MediaAsset> result = ProductionController.orderedWithHeroFirst(assets, 1L);

        assertThat(result).containsExactly(assets.get(0), assets.get(1));
    }

    @Test
    void 대표_이미지와_원래_대표사진뿐이면_대표_이미지_하나만_남는다() {
        List<MediaAsset> assets = List.of(assetWithId(1), assetWithId(2));

        List<MediaAsset> result = ProductionController.orderedWithHeroFirst(assets, 2L);

        assertThat(result).containsExactly(assets.get(1));
    }

    @Test
    void 지정한_이미지가_삭제됐거나_아직_발행되지_않아_목록에_없으면_원래_순서로_되돌아간다() {
        List<MediaAsset> assets = List.of(assetWithId(1), assetWithId(2));

        List<MediaAsset> result = ProductionController.orderedWithHeroFirst(assets, 999L);

        assertThat(result).containsExactly(assets.get(0), assets.get(1));
    }
}
