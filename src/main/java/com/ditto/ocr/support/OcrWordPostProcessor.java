package com.ditto.ocr.support;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ditto.ocr.client.ClovaOcrResult;
import com.ditto.ocr.client.RecognizedWord;

import lombok.extern.slf4j.Slf4j;

/**
 * CLOVA 인식 결과의 OCR 후처리.
 *
 * <p>층수·가격·할인율·시각처럼 상호가 될 수 없는 형태와 너무 작은 글자만 버린다.
 * SALE·세일중 은 여기서 지우지 않는다. 같은 줄에서 맞닿은 단어(POP + MART)는
 * bbox 기준으로 붙인 뒤 면적 순으로 다시 정렬한다.
 */
@Slf4j
@Component
public class OcrWordPostProcessor {

    /** 같은 줄로 볼 세로 겹침 비율. */
    private static final double SAME_LINE_OVERLAP = 0.4;

    /** 붙여 쓸 최대 가로 간격(평균 글자 높이 대비). */
    private static final double MAX_GAP_RATIO = 0.9;

    /** 약간 겹친 글자까지 허용하는 음수 간격. */
    private static final double MIN_GAP_RATIO = -0.15;

    /** 가장 큰 남은 글자 대비 이보다 작으면 가격·캡션으로 본다. */
    private static final double MIN_AREA_RATIO = 0.05;

    public ClovaOcrResult process(ClovaOcrResult raw) {
        if (raw == null || raw.isEmpty()) {
            return ClovaOcrResult.empty();
        }

        List<RecognizedWord> kept = new ArrayList<>();
        for (RecognizedWord word : raw.getWords()) {
            if (OcrStopwords.isStructuralNoise(word.getText())) {
                continue;
            }
            kept.add(word);
        }
        if (kept.isEmpty()) {
            log.debug("OCR 후처리 후 남은 단어 없음. rawCount={}", raw.getWords().size());
            return ClovaOcrResult.empty();
        }

        // 병합을 먼저 한다. POP+MART 처럼 쪼개진 상호는 붙여 놓아야 면적이 합쳐져,
        // 대형 세일 배너가 maxArea 를 끌어올려도 dropTiny 에 걸리지 않는다.
        List<RecognizedWord> merged = mergeAdjacent(kept);
        dropTiny(merged);
        merged.sort(Comparator.comparingDouble(RecognizedWord::getArea).reversed());
        log.debug("OCR 후처리. raw={} kept={} merged={}",
                raw.getWords().size(), kept.size(), merged.size());
        return new ClovaOcrResult(merged);
    }

    private void dropTiny(List<RecognizedWord> words) {
        double maxArea = 0;
        for (RecognizedWord word : words) {
            maxArea = Math.max(maxArea, word.getArea());
        }
        if (maxArea <= 0) {
            return;
        }
        double minArea = maxArea * MIN_AREA_RATIO;
        words.removeIf(word -> word.getArea() < minArea);
    }

    private List<RecognizedWord> mergeAdjacent(List<RecognizedWord> words) {
        List<RecognizedWord> sorted = new ArrayList<>(words);
        sorted.sort(Comparator
                .comparingDouble(RecognizedWord::centerY)
                .thenComparingDouble(RecognizedWord::getMinX));

        boolean[] used = new boolean[sorted.size()];
        List<RecognizedWord> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            if (used[i]) {
                continue;
            }
            List<RecognizedWord> group = new ArrayList<>();
            group.add(sorted.get(i));
            used[i] = true;
            RecognizedWord current = sorted.get(i);
            while (true) {
                int next = findNextOnLine(sorted, used, current);
                if (next < 0) {
                    break;
                }
                used[next] = true;
                group.add(sorted.get(next));
                current = mergeGroup(group);
            }
            result.add(group.size() == 1 ? group.get(0) : mergeGroup(group));
        }
        return result;
    }

    private int findNextOnLine(List<RecognizedWord> words, boolean[] used, RecognizedWord current) {
        int best = -1;
        double bestMinX = Double.MAX_VALUE;
        for (int i = 0; i < words.size(); i++) {
            if (used[i]) {
                continue;
            }
            RecognizedWord candidate = words.get(i);
            if (!sameLine(current, candidate) || !adjacent(current, candidate)) {
                continue;
            }
            if (candidate.getMinX() < bestMinX) {
                bestMinX = candidate.getMinX();
                best = i;
            }
        }
        return best;
    }

    private boolean sameLine(RecognizedWord a, RecognizedWord b) {
        if (!a.hasBoundingBox() || !b.hasBoundingBox()) {
            return false;
        }
        double overlap = Math.min(a.getMaxY(), b.getMaxY()) - Math.max(a.getMinY(), b.getMinY());
        double minHeight = Math.min(a.height(), b.height());
        if (minHeight <= 0) {
            return false;
        }
        return overlap / minHeight >= SAME_LINE_OVERLAP;
    }

    private boolean adjacent(RecognizedWord left, RecognizedWord right) {
        if (!left.hasBoundingBox() || !right.hasBoundingBox()) {
            return false;
        }
        if (right.getMinX() < left.getMinX()) {
            return false;
        }
        double gap = right.getMinX() - left.getMaxX();
        double avgHeight = (left.height() + right.height()) / 2.0;
        if (avgHeight <= 0) {
            return false;
        }
        double ratio = gap / avgHeight;
        return ratio >= MIN_GAP_RATIO && ratio <= MAX_GAP_RATIO;
    }

    private RecognizedWord mergeGroup(List<RecognizedWord> group) {
        group.sort(Comparator.comparingDouble(RecognizedWord::getMinX));
        StringBuilder text = new StringBuilder();
        double confidenceSum = 0;
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (RecognizedWord word : group) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(word.getText());
            confidenceSum += word.getConfidence();
            minX = Math.min(minX, word.getMinX());
            minY = Math.min(minY, word.getMinY());
            maxX = Math.max(maxX, word.getMaxX());
            maxY = Math.max(maxY, word.getMaxY());
        }
        return new RecognizedWord(text.toString(), confidenceSum / group.size(), minX, minY, maxX, maxY);
    }
}
