package com.finediningtheater.media;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * 리사이즈 + JPEG 파생본만 만든다 — 진짜 딥줌(DZI 타일)·WebP는 다음 단계로 미뤘다
 * (2026-08-27 결정, CLAUDE.md §7.5·§15). {@code java.awt}/{@code ImageIO}만 쓰고 별도
 * 이미지 라이브러리(Thumbnailator 등)는 들이지 않는다.
 */
@Component
public class ImageProcessor {

    private static final int[] DERIVATIVE_WIDTHS = {640, 960, 1600};
    private static final int LQIP_WIDTH = 24;

    public record ProcessedImage(
            int width, int height, Map<Integer, byte[]> jpegDerivativesByWidth, String lqipBase64) {}

    public ProcessedImage process(byte[] originalBytes) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (original == null) {
            throw new IOException("이미지를 디코딩할 수 없습니다.");
        }

        int width = original.getWidth();
        int height = original.getHeight();

        Map<Integer, byte[]> derivatives = new LinkedHashMap<>();
        for (int targetWidth : DERIVATIVE_WIDTHS) {
            int boundedWidth = Math.min(targetWidth, width); // 원본보다 키우지 않는다
            derivatives.put(targetWidth, toJpegBytes(resize(original, boundedWidth)));
        }

        String lqipBase64 = Base64.getEncoder().encodeToString(toJpegBytes(resize(original, Math.min(LQIP_WIDTH, width))));

        return new ProcessedImage(width, height, derivatives, lqipBase64);
    }

    private BufferedImage resize(BufferedImage source, int targetWidth) {
        int targetHeight = Math.max(1, Math.round(targetWidth * ((float) source.getHeight() / source.getWidth())));
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resized;
    }

    private byte[] toJpegBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
}
