package com.kb.tangtang.common.storage;

import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageProcessorTest {

    private final ImageProcessor processor = new ImageProcessor();

    private static byte[] png(int width, int height, Color color, boolean withAlpha) throws Exception {
        BufferedImage image = new BufferedImage(width, height,
                withAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static BufferedImage read(byte[] bytes) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    @Test
    @DisplayName("가로로 긴 이미지를 256x256 정사각으로 만든다")
    void cropsWideImage() throws Exception {
        BufferedImage result = read(processor.toSquareJpeg(png(600, 200, Color.RED, false)));

        assertEquals(256, result.getWidth());
        assertEquals(256, result.getHeight());
    }

    @Test
    @DisplayName("세로로 긴 이미지도 256x256 정사각으로 만든다")
    void cropsTallImage() throws Exception {
        BufferedImage result = read(processor.toSquareJpeg(png(200, 600, Color.BLUE, false)));

        assertEquals(256, result.getWidth());
        assertEquals(256, result.getHeight());
    }

    @Test
    @DisplayName("256보다 작은 이미지도 256으로 맞춘다 — 화면 크기가 일정해야 한다")
    void upscalesSmallImage() throws Exception {
        BufferedImage result = read(processor.toSquareJpeg(png(40, 40, Color.GREEN, false)));

        assertEquals(256, result.getWidth());
    }

    @Test
    @DisplayName("투명 PNG 는 흰 배경으로 깔아 JPEG 로 만든다 — 안 그러면 검게 나온다")
    void fillsTransparentBackground() throws Exception {
        /* 알파만 있고 색을 칠하지 않은 완전 투명 이미지 */
        BufferedImage transparent = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(transparent, "png", out);

        BufferedImage result = read(processor.toSquareJpeg(out.toByteArray()));

        Color center = new Color(result.getRGB(128, 128));
        assertTrue(center.getRed() > 240 && center.getGreen() > 240 && center.getBlue() > 240,
                "투명 영역이 흰색이어야 한다. 실제=" + center);
    }

    @Test
    @DisplayName("이미지가 아닌 바이트는 INVALID_IMAGE 로 거부한다")
    void rejectsNonImage() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> processor.toSquareJpeg("이건 이미지가 아니다".getBytes()));

        assertEquals("INVALID_IMAGE", e.getCode());
    }

    @Test
    @DisplayName("빈 파일은 IMAGE_REQUIRED 로 거부한다")
    void rejectsEmpty() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> processor.toSquareJpeg(new byte[0]));

        assertEquals("IMAGE_REQUIRED", e.getCode());
    }

    @Test
    @DisplayName("5MB 를 넘으면 IMAGE_TOO_LARGE 로 거부한다")
    void rejectsTooLarge() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> processor.toSquareJpeg(new byte[5 * 1024 * 1024 + 1]));

        assertEquals("IMAGE_TOO_LARGE", e.getCode());
    }

    @Test
    @DisplayName("requireWithinLimit 은 5MB 이하면 통과한다 — 본문을 만들지 않고 크기만 본다")
    void requireWithinLimitAllowsUpToFiveMb() {
        processor.requireWithinLimit(5 * 1024 * 1024L);
    }

    @Test
    @DisplayName("requireWithinLimit 은 5MB 를 넘으면 IMAGE_TOO_LARGE — 컨트롤러가 getBytes() 전에 부른다")
    void requireWithinLimitRejectsTooLarge() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> processor.requireWithinLimit(5 * 1024 * 1024L + 1));

        assertEquals("IMAGE_TOO_LARGE", e.getCode());
    }
}
