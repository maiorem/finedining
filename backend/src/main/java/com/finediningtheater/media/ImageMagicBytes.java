package com.finediningtheater.media;

/**
 * 파일 검증은 확장자가 아니라 매직 바이트로 한다 (CLAUDE.md §7.5). 확장자·Content-Type은
 * 클라이언트가 얼마든지 속일 수 있지만 파일 앞부분 바이트는 속이려면 실제로 그 포맷이어야 한다.
 */
public final class ImageMagicBytes {

    private ImageMagicBytes() {}

    public static boolean isValidImage(byte[] header) {
        if (header.length < 12) {
            return false;
        }
        return isJpeg(header) || isPng(header) || isWebp(header);
    }

    private static boolean isJpeg(byte[] h) {
        return unsigned(h[0]) == 0xFF && unsigned(h[1]) == 0xD8 && unsigned(h[2]) == 0xFF;
    }

    private static boolean isPng(byte[] h) {
        return unsigned(h[0]) == 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47;
    }

    private static boolean isWebp(byte[] h) {
        return h[0] == 'R'
                && h[1] == 'I'
                && h[2] == 'F'
                && h[3] == 'F'
                && h[8] == 'W'
                && h[9] == 'E'
                && h[10] == 'B'
                && h[11] == 'P';
    }

    private static int unsigned(byte b) {
        return b & 0xFF;
    }
}
