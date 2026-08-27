package com.finediningtheater.media;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ImageMagicBytesTest {

    @Test
    void JPEG_매직바이트를_인식한다() {
        byte[] header = new byte[16];
        header[0] = (byte) 0xFF;
        header[1] = (byte) 0xD8;
        header[2] = (byte) 0xFF;

        assertThat(ImageMagicBytes.isValidImage(header)).isTrue();
    }

    @Test
    void PNG_매직바이트를_인식한다() {
        byte[] header = new byte[16];
        header[0] = (byte) 0x89;
        header[1] = 0x50;
        header[2] = 0x4E;
        header[3] = 0x47;

        assertThat(ImageMagicBytes.isValidImage(header)).isTrue();
    }

    @Test
    void WEBP_매직바이트를_인식한다() {
        byte[] header = new byte[16];
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[8] = 'W';
        header[9] = 'E';
        header[10] = 'B';
        header[11] = 'P';

        assertThat(ImageMagicBytes.isValidImage(header)).isTrue();
    }

    @Test
    void 확장자만_바꾼_텍스트_파일은_거부한다() {
        byte[] header = "not an image, just text padding".getBytes();

        assertThat(ImageMagicBytes.isValidImage(header)).isFalse();
    }

    @Test
    void 너무_짧은_바이트는_거부한다() {
        assertThat(ImageMagicBytes.isValidImage(new byte[]{1, 2, 3})).isFalse();
    }
}
