package com.ditto.news.crawler.strategy;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.news.pipeline.model.CrawledNewsArticle;
import com.ditto.news.pipeline.model.NewsArticleCandidate;
import com.ditto.news.service.collector.RssDateParser;

import lombok.extern.slf4j.Slf4j;

/**
 * 뉴스 사이트별 기사 파서 구현체를 위한 공통 추상 클래스.
 * 호스트 기반 supports 검증, 본문 문단 정제, 메타데이터 추출 및 fallback 로직을 제공합니다.
 */
@Slf4j
public abstract class AbstractNewsArticleParser implements NewsArticleParserStrategy {

    protected abstract String[] getSupportedDomains();

    protected abstract String getDefaultSource();

    protected abstract String[] getTitleSelectors();

    protected abstract String[] getBodyContainerSelectors();

    protected abstract String[] getDateSelectors();

    protected String[] getUnwantedSelectors() {
        return new String[] {
                "script", "style", "iframe", "button", "noscript",
                ".sns_share", ".share", ".sns-area", ".tag", ".tags",
                ".article-ad", ".ad", ".banner", ".reporter_profile",
                ".byline", ".copyright", "figure figcaption", ".photo_caption",
                ".comment", "#comments", ".related-news"
        };
    }

    @Override
    public boolean supports(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }

            String lowerHost = host.toLowerCase(Locale.ROOT);
            for (String domain : getSupportedDomains()) {
                String lowerDomain = domain.toLowerCase(Locale.ROOT);
                if (lowerHost.equals(lowerDomain) || lowerHost.endsWith("." + lowerDomain)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("URL 호스트 검증 실패 (supports=false): url={}, cause={}", url, e.getMessage());
            return false;
        }
    }

    @Override
    public CrawledNewsArticle parse(NewsArticleCandidate candidate, Document document) {
        if (document == null) {
            log.warn("파싱 대상 HTML Document가 null입니다.");
            throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);
        }

        String candidateTitle = candidate != null ? candidate.getTitle() : null;
        String title = extractTitle(document, candidateTitle);

        String body = extractBody(document);
        if (body == null || body.isBlank()) {
            log.warn("기사 본문 추출 실패: url={}", candidate != null ? candidate.getUrl() : "unknown");
            throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);
        }

        LocalDateTime candidateDate = candidate != null ? candidate.getPublishedAt() : null;
        LocalDateTime publishedAt = extractPublishedAt(document, candidateDate);

        String imageUrl = extractImageUrl(document);
        String source = extractSource(candidate);
        String finalUrl = extractCanonicalUrl(document, candidate != null ? candidate.getUrl() : null);

        return CrawledNewsArticle.builder()
                .title(title)
                .body(body)
                .url(finalUrl)
                .source(source)
                .publishedAt(publishedAt)
                .imageUrl(imageUrl)
                .build();
    }

    protected String extractTitle(Document doc, String candidateTitle) {
        // 1. 사이트 전용 selector
        for (String selector : getTitleSelectors()) {
            Element elem = doc.selectFirst(selector);
            if (elem != null && !elem.text().isBlank()) {
                return cleanText(elem.text());
            }
        }

        // 2. og:title
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null && ogTitle.hasAttr("content") && !ogTitle.attr("content").isBlank()) {
            return cleanText(ogTitle.attr("content"));
        }

        // 3. document.title()
        if (doc.title() != null && !doc.title().isBlank()) {
            return cleanText(doc.title());
        }

        // 4. candidateTitle fallback
        if (candidateTitle != null && !candidateTitle.isBlank()) {
            return cleanText(candidateTitle);
        }

        throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);
    }

    protected String extractBody(Document doc) {
        Element container = null;
        for (String selector : getBodyContainerSelectors()) {
            container = doc.selectFirst(selector);
            if (container != null) {
                break;
            }
        }

        if (container == null) {
            container = doc.body();
        }

        if (container == null) {
            return null;
        }

        // 본문 컨테이너 복제본에서 불필요한 요소 제거
        Element containerClone = container.clone();
        for (String unwanted : getUnwantedSelectors()) {
            containerClone.select(unwanted).remove();
        }

        List<String> paragraphs = new ArrayList<>();

        // 1. p 태그 기반 문단 추출 시도
        Elements pTags = containerClone.select("p");
        if (!pTags.isEmpty()) {
            for (Element p : pTags) {
                String pText = cleanParagraph(p.text());
                if (!pText.isBlank() && !isBoilerplateText(pText)) {
                    paragraphs.add(pText);
                }
            }
        }

        // 2. p 태그가 없거나 추출된 문단이 없을 경우 줄바꿈 기반 추출
        if (paragraphs.isEmpty()) {
            String fullText = containerClone.wholeText();
            String[] lines = fullText.split("\\r?\\n");
            for (String line : lines) {
                String cleanLine = cleanParagraph(line);
                if (!cleanLine.isBlank() && !isBoilerplateText(cleanLine)) {
                    paragraphs.add(cleanLine);
                }
            }
        }

        if (paragraphs.isEmpty()) {
            return null;
        }

        return String.join("\n\n", paragraphs);
    }

    protected LocalDateTime extractPublishedAt(Document doc, LocalDateTime candidateDate) {
        // 1. 사이트 전용 date selector
        for (String selector : getDateSelectors()) {
            Element elem = doc.selectFirst(selector);
            if (elem != null) {
                String dateText = elem.hasAttr("datetime") ? elem.attr("datetime") : elem.text();
                LocalDateTime parsed = RssDateParser.parse(dateText);
                if (parsed != null) {
                    return parsed;
                }
            }
        }

        // 2. meta[property=article:published_time]
        Element metaDate = doc.selectFirst("meta[property=article:published_time]");
        if (metaDate != null && metaDate.hasAttr("content")) {
            LocalDateTime parsed = RssDateParser.parse(metaDate.attr("content"));
            if (parsed != null) {
                return parsed;
            }
        }

        // 3. meta[name=pubdate] / meta[name=date]
        Element pubdate = doc.selectFirst("meta[name=pubdate], meta[name=date]");
        if (pubdate != null && pubdate.hasAttr("content")) {
            LocalDateTime parsed = RssDateParser.parse(pubdate.attr("content"));
            if (parsed != null) {
                return parsed;
            }
        }

        // 4. Candidate fallback
        return candidateDate;
    }

    protected String extractImageUrl(Document doc) {
        // 1. og:image
        Element ogImg = doc.selectFirst("meta[property=og:image]");
        if (ogImg != null && ogImg.hasAttr("content")) {
            String imgUrl = ogImg.attr("content").trim();
            if (isValidImageUrl(imgUrl)) {
                return imgUrl;
            }
        }

        // 2. twitter:image
        Element twitterImg = doc.selectFirst("meta[name=twitter:image]");
        if (twitterImg != null && twitterImg.hasAttr("content")) {
            String imgUrl = twitterImg.attr("content").trim();
            if (isValidImageUrl(imgUrl)) {
                return imgUrl;
            }
        }

        return null;
    }

    protected String extractSource(NewsArticleCandidate candidate) {
        if (candidate != null && candidate.getSource() != null && !candidate.getSource().isBlank()) {
            return candidate.getSource().trim();
        }
        return getDefaultSource();
    }

    protected String extractCanonicalUrl(Document doc, String candidateUrl) {
        Element canonical = doc.selectFirst("link[rel=canonical]");
        if (canonical != null && canonical.hasAttr("href")) {
            String href = canonical.attr("href").trim();
            if (href.startsWith("http://") || href.startsWith("https://")) {
                return href;
            }
        }
        return candidateUrl != null ? candidateUrl.trim() : null;
    }

    protected String cleanParagraph(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\t\\p{Z}]+", " ")
                .replaceAll("^\\s+|\\s+$", "")
                .trim();
    }

    protected String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .trim();
    }

    protected boolean isBoilerplateText(String text) {
        if (text.length() < 3) {
            return true;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.matches("^[\\(c\\)\\[\\]\\s]*all rights reserved.*")
                || lower.startsWith("(c) yonhap news")
                || lower.startsWith("copyright (c)")
                || lower.contains("무단 전재 및 재배포 금지")
                || lower.contains("무단전재 및 재배포 금지")
                || lower.matches("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}\\s*$");
    }

    private boolean isValidImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String trimmed = url.trim();
        return trimmed.startsWith("http://") || trimmed.startsWith("https://");
    }
}
