package com.finediningtheater.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ExifOrientationTest {

    private static final int RED = 0xFF0000;

    /** 4x2 이미지의 왼쪽 위 픽셀만 빨강이다 — 돌린 뒤 빨강이 어디로 갔는지로 방향을 검증한다. */
    private BufferedImage marker() {
        BufferedImage image = new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, RED);
        return image;
    }

    private boolean isRed(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0xFFFFFF) == RED;
    }

    @Test
    void 방향_1은_원본을_그대로_돌려준다() {
        BufferedImage image = marker();

        assertThat(ExifOrientation.apply(image, 1)).isSameAs(image);
    }

    @Test
    void 방향_6은_시계_방향_90도로_세운다() {
        BufferedImage result = ExifOrientation.apply(marker(), 6);

        assertThat(result.getWidth()).isEqualTo(2);
        assertThat(result.getHeight()).isEqualTo(4);
        assertThat(isRed(result, 1, 0)).isTrue(); // 왼쪽 위 → 오른쪽 위
    }

    @Test
    void 방향_8은_반시계_방향_90도로_세운다() {
        BufferedImage result = ExifOrientation.apply(marker(), 8);

        assertThat(result.getWidth()).isEqualTo(2);
        assertThat(result.getHeight()).isEqualTo(4);
        assertThat(isRed(result, 0, 3)).isTrue(); // 왼쪽 위 → 왼쪽 아래
    }

    @Test
    void 방향_3은_180도_돌린다() {
        BufferedImage result = ExifOrientation.apply(marker(), 3);

        assertThat(result.getWidth()).isEqualTo(4);
        assertThat(result.getHeight()).isEqualTo(2);
        assertThat(isRed(result, 3, 1)).isTrue();
    }

    @Test
    void 방향_2와_4는_반전하고_5_7은_가로세로가_바뀐다() {
        assertThat(isRed(ExifOrientation.apply(marker(), 2), 3, 0)).isTrue();
        assertThat(isRed(ExifOrientation.apply(marker(), 4), 0, 1)).isTrue();
        assertThat(ExifOrientation.apply(marker(), 5).getWidth()).isEqualTo(2);
        assertThat(ExifOrientation.apply(marker(), 7).getWidth()).isEqualTo(2);
    }

    @Test
    void 리틀엔디안과_빅엔디안_EXIF에서_방향을_읽는다() throws IOException {
        assertThat(ExifOrientation.read(withExif(jpeg(), 6, true))).isEqualTo(6);
        assertThat(ExifOrientation.read(withExif(jpeg(), 8, false))).isEqualTo(8);
    }

    @Test
    void EXIF가_없거나_깨졌거나_JPEG가_아니면_1이다() throws IOException {
        assertThat(ExifOrientation.read(jpeg())).isEqualTo(1);
        assertThat(ExifOrientation.read(new byte[] {1, 2, 3})).isEqualTo(1);
        assertThat(ExifOrientation.read(new byte[0])).isEqualTo(1);
        byte[] truncated = withExif(jpeg(), 6, true);
        assertThat(ExifOrientation.read(java.util.Arrays.copyOf(truncated, 20))).isEqualTo(1);
    }

    @Test
    void 범위_밖_방향값은_무시한다() throws IOException {
        assertThat(ExifOrientation.read(withExif(jpeg(), 9, true))).isEqualTo(1);
    }

    @Test
    void 방향_6_사진을_처리하면_가로세로가_바뀌어_저장된다() throws IOException {
        BufferedImage source = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream plain = new ByteArrayOutputStream();
        ImageIO.write(source, "jpg", plain);

        ImageProcessor.ProcessedImage processed = new ImageProcessor().process(withExif(plain.toByteArray(), 6, true));

        assertThat(processed.width()).isEqualTo(50);
        assertThat(processed.height()).isEqualTo(100);
    }

    private byte[] jpeg() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 4, BufferedImage.TYPE_INT_RGB), "jpg", out);
        return out.toByteArray();
    }

    /** SOI 바로 뒤에 Orientation 태그 하나만 가진 APP1(Exif) 세그먼트를 끼워 넣는다. */
    private byte[] withExif(byte[] jpeg, int orientation, boolean little) {
        byte[] tiff = new byte[26];
        tiff[0] = (byte) (little ? 'I' : 'M');
        tiff[1] = tiff[0];
        put16(tiff, 2, 0x002A, little);
        put32(tiff, 4, 8, little); // IFD0 위치
        put16(tiff, 8, 1, little); // 항목 수
        put16(tiff, 10, 0x0112, little); // Orientation
        put16(tiff, 12, 3, little); // SHORT
        put32(tiff, 14, 1, little); // count
        put16(tiff, 18, orientation, little);

        int segmentLength = 2 + 6 + tiff.length;
        byte[] app1 = new byte[2 + segmentLength];
        app1[0] = (byte) 0xFF;
        app1[1] = (byte) 0xE1;
        app1[2] = (byte) (segmentLength >> 8);
        app1[3] = (byte) segmentLength;
        byte[] header = {'E', 'x', 'i', 'f', 0, 0};
        System.arraycopy(header, 0, app1, 4, 6);
        System.arraycopy(tiff, 0, app1, 10, tiff.length);

        byte[] result = new byte[jpeg.length + app1.length];
        System.arraycopy(jpeg, 0, result, 0, 2);
        System.arraycopy(app1, 0, result, 2, app1.length);
        System.arraycopy(jpeg, 2, result, 2 + app1.length, jpeg.length - 2);
        return result;
    }

    private void put16(byte[] d, int at, int value, boolean little) {
        d[at] = (byte) (little ? value : value >> 8);
        d[at + 1] = (byte) (little ? value >> 8 : value);
    }

    private void put32(byte[] d, int at, int value, boolean little) {
        put16(d, little ? at : at + 2, value & 0xFFFF, little);
        put16(d, little ? at + 2 : at, value >>> 16, little);
    }
}
