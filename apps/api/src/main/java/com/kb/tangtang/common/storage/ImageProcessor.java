package com.kb.tangtang.common.storage;

import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * 업로드된 이미지를 화면용 규격으로 다시 굽는다.
 *
 * **재인코딩이 곧 검증이다.** ImageIO 가 읽지 못하면 이미지가 아니므로, 확장자나
 * Content-Type 을 믿지 않고 디코딩 성공 여부로 판정한다. 이름만 .jpg 인 파일은 여기서 걸린다.
 *
 * 목록 화면(그룹 멤버·랭킹)에 아바타가 여러 개 깔리므로 원본을 그대로 두지 않는다.
 * 결과는 항상 256x256 JPEG 한 장이고 원본은 보관하지 않는다.
 */
@Component
public class ImageProcessor {

    /** 서블릿 상한(WebConfig 10MB)보다 낮게 서비스에서 한 번 더 막는다. */
    private static final int MAX_BYTES = 5 * 1024 * 1024;

    /** 아바타는 가장 큰 자리도 100px 남짓이다. 2배 화면을 감안해 256 으로 둔다. */
    private static final int SIZE = 256;

    private static final float QUALITY = 0.85f;

    public byte[] toSquareJpeg(byte[] source) {
        if (source == null || source.length == 0) {
            throw new BusinessException("IMAGE_REQUIRED", "올릴 이미지를 선택해주세요.");
        }
        requireWithinLimit(source.length);

        BufferedImage original = decode(source);
        BufferedImage square = cropCenterSquare(original);
        return encodeJpeg(square);
    }

    /**
     * 크기만 검사한다 — 본문을 메모리에 올리기 전에 컨트롤러가 먼저 부른다.
     *
     * {@code toSquareJpeg} 는 바이트 배열을 받으므로 이미 메모리에 올라온 뒤에야 크기를 본다.
     * 그래서는 5MB 상한이 할당 자체를 막지 못한다(실제 상한은 서블릿 컨테이너의 파일 10MB
     * 설정이 잡는다). 컨트롤러가 {@code MultipartFile#getSize()} 로 여기부터 부르면
     * {@code getBytes()} 호출 전에 막을 수 있다. 상한 상수는 이 클래스 한 곳에만 둔다.
     */
    public void requireWithinLimit(long sizeInBytes) {
        if (sizeInBytes > MAX_BYTES) {
            throw new BusinessException("IMAGE_TOO_LARGE", "5MB 이하 이미지만 올릴 수 있어요.");
        }
    }

    /** ImageIO 가 null 을 돌려주면 지원하지 않는 형식이거나 이미지가 아니다. */
    private static BufferedImage decode(byte[] source) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(source));
            if (image == null) {
                throw new BusinessException("INVALID_IMAGE", "이미지 파일만 올릴 수 있어요.");
            }
            return image;
        } catch (IOException e) {
            throw new BusinessException("INVALID_IMAGE", "이미지 파일만 올릴 수 있어요.");
        }
    }

    /**
     * 짧은 변 기준으로 가운데를 잘라 SIZE 정사각으로 그린다.
     *
     * TYPE_INT_RGB 로 만들고 흰색을 먼저 칠한다 — 투명 PNG 를 그대로 JPEG 로 쓰면
     * 알파 채널이 사라지면서 배경이 검게 나온다.
     */
    private static BufferedImage cropCenterSquare(BufferedImage original) {
        int side = Math.min(original.getWidth(), original.getHeight());
        int x = (original.getWidth() - side) / 2;
        int y = (original.getHeight() - side) / 2;

        BufferedImage target = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, SIZE, SIZE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, SIZE, SIZE, x, y, x + side, y + side, null);
        g.dispose();
        return target;
    }

    private static byte[] encodeJpeg(BufferedImage image) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(QUALITY);
            try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(stream);
                writer.write(null, new IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("이미지를 변환하지 못했습니다.", e);
        }
        return out.toByteArray();
    }
}
