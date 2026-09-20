package com.finediningtheater.media;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * 휴대폰 사진은 픽셀이 눕혀진 채 저장되고 "이만큼 돌려서 보라"는 방향 값(EXIF Orientation)이 따로
 * 들어 있다. ImageIO는 이 값을 무시하고 파생본을 만들 때 EXIF도 버려서, 그대로 두면 웹에서 사진이
 * 눕거나 기울어 보인다. JPEG의 APP1(Exif) 세그먼트에서 방향 값(1~8)만 읽어 파생본을 만들기 전에
 * 픽셀을 똑바로 돌린다. 별도 라이브러리 없이 필요한 태그 하나만 읽는다.
 */
final class ExifOrientation {

    static final int NORMAL = 1;

    private ExifOrientation() {}

    /** 읽을 수 없거나 JPEG가 아니면 NORMAL(1)이다 — 잘못된 EXIF 때문에 업로드가 실패하면 안 된다. */
    static int read(byte[] jpeg) {
        try {
            return parse(jpeg);
        } catch (RuntimeException e) {
            return NORMAL;
        }
    }

    private static int parse(byte[] d) {
        if (d.length < 4 || (d[0] & 0xFF) != 0xFF || (d[1] & 0xFF) != 0xD8) {
            return NORMAL;
        }
        int pos = 2;
        while (pos + 4 <= d.length) {
            if ((d[pos] & 0xFF) != 0xFF) {
                return NORMAL;
            }
            int marker = d[pos + 1] & 0xFF;
            if (marker == 0xD9 || marker == 0xDA) { // EOI, SOS — 이후에는 EXIF가 없다
                return NORMAL;
            }
            int length = ((d[pos + 2] & 0xFF) << 8) | (d[pos + 3] & 0xFF);
            int segmentStart = pos + 4;
            if (length < 2 || pos + 2 + length > d.length) {
                return NORMAL;
            }
            if (marker == 0xE1 && length >= 8 && isExifHeader(d, segmentStart)) {
                return readTiffOrientation(d, segmentStart + 6, pos + 2 + length);
            }
            pos += 2 + length;
        }
        return NORMAL;
    }

    private static boolean isExifHeader(byte[] d, int at) {
        return d[at] == 'E' && d[at + 1] == 'x' && d[at + 2] == 'i' && d[at + 3] == 'f' && d[at + 4] == 0 && d[at + 5] == 0;
    }

    private static int readTiffOrientation(byte[] d, int tiff, int end) {
        boolean little = d[tiff] == 'I' && d[tiff + 1] == 'I';
        boolean big = d[tiff] == 'M' && d[tiff + 1] == 'M';
        if (!little && !big) {
            return NORMAL;
        }
        int ifd = tiff + (int) u32(d, tiff + 4, little);
        if (ifd < tiff || ifd + 2 > end) {
            return NORMAL;
        }
        int entries = u16(d, ifd, little);
        for (int i = 0; i < entries; i++) {
            int entry = ifd + 2 + i * 12;
            if (entry + 12 > end) {
                return NORMAL;
            }
            if (u16(d, entry, little) == 0x0112) {
                int value = u16(d, entry + 8, little);
                return value >= 1 && value <= 8 ? value : NORMAL;
            }
        }
        return NORMAL;
    }

    private static int u16(byte[] d, int at, boolean little) {
        int a = d[at] & 0xFF;
        int b = d[at + 1] & 0xFF;
        return little ? (b << 8) | a : (a << 8) | b;
    }

    private static long u32(byte[] d, int at, boolean little) {
        long lo = u16(d, little ? at : at + 2, little);
        long hi = u16(d, little ? at + 2 : at, little);
        return (hi << 16) | lo;
    }

    /** 방향 값대로 픽셀을 돌려 똑바로 세운 이미지를 돌려준다. NORMAL이면 원본을 그대로 돌려준다. */
    static BufferedImage apply(BufferedImage source, int orientation) {
        if (orientation == NORMAL) {
            return source;
        }
        int w = source.getWidth();
        int h = source.getHeight();
        boolean swap = orientation >= 5;
        AffineTransform transform =
                switch (orientation) {
                    case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0); // 좌우 반전
                    case 3 -> new AffineTransform(-1, 0, 0, -1, w, h); // 180도
                    case 4 -> new AffineTransform(1, 0, 0, -1, 0, h); // 상하 반전
                    case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0); // 전치
                    case 6 -> new AffineTransform(0, 1, -1, 0, h, 0); // 시계 방향 90도
                    case 7 -> new AffineTransform(0, -1, -1, 0, h, w); // 반대 전치
                    case 8 -> new AffineTransform(0, -1, 1, 0, 0, w); // 반시계 방향 90도
                    default -> new AffineTransform();
                };
        BufferedImage rotated = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rotated.createGraphics();
        g.drawImage(source, transform, null);
        g.dispose();
        return rotated;
    }
}
