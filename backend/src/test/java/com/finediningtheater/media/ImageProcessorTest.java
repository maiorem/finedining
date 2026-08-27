package com.finediningtheater.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageProcessorTest {

    private final ImageProcessor processor = new ImageProcessor();

    @Test
    void 원본보다_큰_파생본_너비는_원본_너비로_제한한다() throws IOException {
        byte[] tinyJpeg = jpegBytes(100, 50);

        ImageProcessor.ProcessedImage processed = processor.process(tinyJpeg);

        assertThat(processed.width()).isEqualTo(100);
        assertThat(processed.height()).isEqualTo(50);
        assertThat(processed.jpegDerivativesByWidth()).containsKeys(640, 960, 1600);
        assertThat(processed.jpegDerivativesByWidth().get(640)).isNotEmpty();
        assertThat(processed.lqipBase64()).isNotBlank();
    }

    @Test
    void 원본보다_작은_타겟너비는_그대로_축소한다() throws IOException {
        byte[] jpeg = jpegBytes(2000, 1000);

        ImageProcessor.ProcessedImage processed = processor.process(jpeg);

        assertThat(processed.width()).isEqualTo(2000);
        assertThat(processed.height()).isEqualTo(1000);
        assertThat(processed.jpegDerivativesByWidth().get(640)).isNotEmpty();
        assertThat(processed.jpegDerivativesByWidth().get(1600)).isNotEmpty();
    }

    private byte[] jpegBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
}
