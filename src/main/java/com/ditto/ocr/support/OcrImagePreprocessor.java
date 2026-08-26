package com.ditto.ocr.support;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Component;

import com.ditto.ocr.config.OcrProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * CLOVA OCR 호출 전 대용량 이미지를 줄인다.
 *
 * <p>스마트폰 원본은 긴 변이 4000px 을 넘고 수 MB 인 경우가 많다. 글자 인식에는 1600px
 * 전후면 충분하고, 바이트가 작을수록 업로드·CLOVA 처리가 짧아진다. 디코딩에 실패하면
 * 원본을 그대로 보낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcrImagePreprocessor {

    private static final float FALLBACK_JPEG_QUALITY = 0.6f;

    private final OcrProperties properties;

    public PreprocessedImage process(byte[] original, String format) {
        OcrProperties.Preprocess cfg = properties.getPreprocess();
        if (!cfg.isEnabled() || original == null || original.length == 0) {
            return PreprocessedImage.passthrough(original, format);
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(original));
            if (image == null) {
                return PreprocessedImage.passthrough(original, format);
            }

            int width = image.getWidth();
            int height = image.getHeight();
            int longSide = Math.max(width, height);
            boolean tooManyPixels = longSide > cfg.getMaxLongSidePx();
            boolean tooManyBytes = original.length > cfg.getMaxBytes();
            if (!tooManyPixels && !tooManyBytes) {
                return PreprocessedImage.passthrough(original, format);
            }

            BufferedImage scaled = tooManyPixels
                    ? resize(image, cfg.getMaxLongSidePx())
                    : toRgb(image);
            byte[] jpeg = writeJpeg(scaled, cfg.getJpegQuality());
            if (jpeg.length > cfg.getMaxBytes() && cfg.getJpegQuality() > FALLBACK_JPEG_QUALITY) {
                jpeg = writeJpeg(scaled, FALLBACK_JPEG_QUALITY);
            }

            log.debug("OCR 이미지 전처리. originalBytes={} {}x{} → {} bytes {}x{}",
                    original.length, width, height, jpeg.length, scaled.getWidth(), scaled.getHeight());
            return new PreprocessedImage(jpeg, "jpg", true, original.length, width, height);
        } catch (Exception e) {
            log.warn("OCR 이미지 전처리 실패, 원본 전송. cause={}", e.getMessage());
            return PreprocessedImage.passthrough(original, format);
        }
    }

    private BufferedImage resize(BufferedImage src, int maxLongSide) {
        int width = src.getWidth();
        int height = src.getHeight();
        double scale = (double) maxLongSide / Math.max(width, height);
        int nextWidth = Math.max(1, (int) Math.round(width * scale));
        int nextHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage dst = new BufferedImage(nextWidth, nextHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = dst.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, nextWidth, nextHeight);
            graphics.drawImage(src, 0, 0, nextWidth, nextHeight, null);
        } finally {
            graphics.dispose();
        }
        return dst;
    }

    private BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, src.getWidth(), src.getHeight());
            graphics.drawImage(src, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgb;
    }

    private byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ByteArrayOutputStream fallback = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", fallback);
            return fallback.toByteArray();
        }

        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
            return out.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
