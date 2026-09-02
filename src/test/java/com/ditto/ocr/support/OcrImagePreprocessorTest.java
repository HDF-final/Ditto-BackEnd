package com.ditto.ocr.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.ocr.config.OcrProperties;

class OcrImagePreprocessorTest {

    private OcrImagePreprocessor preprocessor(OcrProperties properties) {
        return new OcrImagePreprocessor(properties);
    }

    private byte[] jpeg(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    @Test
    @DisplayName("긴 변이 기준을 넘으면 JPEG 로 줄여 CLOVA 페이로드를 작게 만든다")
    void downscalesLargeImage() throws Exception {
        byte[] original = jpeg(4000, 3000);
        PreprocessedImage processed = preprocessor(new OcrProperties()).process(original, "jpg");

        assertThat(processed.isTransformed()).isTrue();
        assertThat(processed.getFormat()).isEqualTo("jpg");
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(processed.getBytes()));
        assertThat(Math.max(out.getWidth(), out.getHeight())).isEqualTo(1600);
        assertThat(processed.getBytes().length).isLessThan(original.length);
    }

    @Test
    @DisplayName("이미 작은 이미지는 재인코딩하지 않고 원본을 보낸다")
    void passesThroughSmallImage() throws Exception {
        byte[] original = jpeg(800, 600);
        PreprocessedImage processed = preprocessor(new OcrProperties()).process(original, "jpg");

        assertThat(processed.isTransformed()).isFalse();
        assertThat(processed.getBytes()).isEqualTo(original);
        assertThat(processed.getFormat()).isEqualTo("jpg");
    }

    @Test
    @DisplayName("전처리를 끄면 큰 이미지도 원본 그대로 보낸다")
    void disabledKeepsOriginal() throws Exception {
        OcrProperties properties = new OcrProperties();
        properties.getPreprocess().setEnabled(false);
        byte[] original = jpeg(4000, 3000);

        PreprocessedImage processed = preprocessor(properties).process(original, "png");

        assertThat(processed.isTransformed()).isFalse();
        assertThat(processed.getBytes()).isEqualTo(original);
        assertThat(processed.getFormat()).isEqualTo("png");
    }

    @Test
    @DisplayName("픽셀 수가 상한을 넘으면 디코딩하지 않고 거절한다 (디컴프레션 밤 방어)")
    void rejectsImageExceedingPixelLimit() throws Exception {
        OcrProperties properties = new OcrProperties();
        properties.getPreprocess().setMaxDecodePixels(1_000_000L); // 1MP 상한
        byte[] original = jpeg(2000, 2000); // 4MP → 초과

        assertThatThrownBy(() -> preprocessor(properties).process(original, "jpg"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_SIZE_EXCEEDED);
    }

    @Test
    @DisplayName("픽셀 상한 이내면 정상 처리한다")
    void allowsImageWithinPixelLimit() throws Exception {
        OcrProperties properties = new OcrProperties();
        properties.getPreprocess().setMaxDecodePixels(10_000_000L); // 10MP 상한
        byte[] original = jpeg(2000, 2000); // 4MP → 통과

        PreprocessedImage processed = preprocessor(properties).process(original, "jpg");

        assertThat(processed.isTransformed()).isTrue();
    }

    @Test
    @DisplayName("디코딩할 수 없으면 원본을 그대로 보낸다")
    void undecodableBytesPassThrough() {
        byte[] original = new byte[] {1, 2, 3, 4, 5};
        PreprocessedImage processed = preprocessor(new OcrProperties()).process(original, "heic");

        assertThat(processed.isTransformed()).isFalse();
        assertThat(processed.getBytes()).isEqualTo(original);
        assertThat(processed.getFormat()).isEqualTo("heic");
    }
}
