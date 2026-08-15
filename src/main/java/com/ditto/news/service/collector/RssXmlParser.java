package com.ditto.news.service.collector;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ditto.news.pipeline.model.NewsArticleCandidate;

import lombok.extern.slf4j.Slf4j;

/**
 * RSS 2.0 및 Atom 피드 XML 문서를 파싱하여 {@link NewsArticleCandidate} 목록으로 변환하는 컴포넌트.
 * XXE 취약점 방지 및 불완전/오염된 아이템에 대한 안전 처리를 내장합니다.
 */
@Slf4j
@Component
public class RssXmlParser {

    /**
     * XML 본문을 파싱하여 기사 후보 목록을 반환합니다.
     *
     * @param xmlContent        RSS/Atom XML 본문
     * @param defaultSourceName 피드 소스 기본명 (출처 태그가 없을 경우 대체용)
     * @return 파싱된 기사 후보 목록 (파싱 실패 시 빈 목록)
     */
    public List<NewsArticleCandidate> parse(String xmlContent, String defaultSourceName) {
        if (xmlContent == null || xmlContent.isBlank()) {
            return Collections.emptyList();
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            // OWASP 권장 XXE 방어 설정
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            String channelTitle = extractChannelTitle(doc);
            String fallbackSource = (channelTitle != null && !channelTitle.isBlank()) ? channelTitle : defaultSourceName;

            // 1. RSS 2.0 (<item>) 파싱
            NodeList itemNodes = doc.getElementsByTagName("item");
            if (itemNodes.getLength() > 0) {
                return parseRssItems(itemNodes, fallbackSource);
            }

            // 2. Atom (<entry>) 파싱
            NodeList entryNodes = doc.getElementsByTagName("entry");
            if (entryNodes.getLength() > 0) {
                return parseAtomEntries(entryNodes, fallbackSource);
            }

            log.debug("피드 내에 <item> 또는 <entry> 항목이 존재하지 않습니다.");
            return Collections.emptyList();

        } catch (Exception e) {
            log.warn("XML 피드 파싱 실패: cause={}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<NewsArticleCandidate> parseRssItems(NodeList itemNodes, String fallbackSource) {
        List<NewsArticleCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Node node = itemNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                try {
                    NewsArticleCandidate candidate = parseRssItem(element, fallbackSource);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                } catch (Exception e) {
                    log.warn("개별 RSS 아이템 파싱 오류로 건너뜀: cause={}", e.getMessage());
                }
            }
        }
        return candidates;
    }

    private NewsArticleCandidate parseRssItem(Element element, String fallbackSource) {
        String title = cleanText(getDirectChildElementText(element, "title"));
        String link = getDirectChildElementText(element, "link");
        if (link == null || link.isBlank()) {
            link = getDirectChildElementText(element, "guid");
        }
        if (link == null || link.isBlank()) {
            link = getDirectChildElementText(element, "origLink");
        }

        // 필수 필드(title, link) 누락 또는 빈 값인 경우 건너뜀
        if (title == null || title.isBlank() || link == null || link.isBlank()) {
            return null;
        }

        String normalizedUrl = link.trim();
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            return null;
        }

        String source = cleanText(getDirectChildElementText(element, "source"));
        if (source == null || source.isBlank()) {
            source = cleanText(getDirectChildElementText(element, "creator"));
        }
        if (source == null || source.isBlank()) {
            source = fallbackSource;
        }

        String pubDateStr = getDirectChildElementText(element, "pubDate");
        if (pubDateStr == null || pubDateStr.isBlank()) {
            pubDateStr = getDirectChildElementText(element, "date");
        }
        LocalDateTime publishedAt = RssDateParser.parse(pubDateStr);

        String description = cleanText(getDirectChildElementText(element, "description"));
        if (description == null || description.isBlank()) {
            description = cleanText(getDirectChildElementText(element, "encoded"));
        }

        return NewsArticleCandidate.builder()
                .title(title)
                .url(normalizedUrl)
                .source(source)
                .publishedAt(publishedAt)
                .description(description)
                .build();
    }

    private List<NewsArticleCandidate> parseAtomEntries(NodeList entryNodes, String fallbackSource) {
        List<NewsArticleCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < entryNodes.getLength(); i++) {
            Node node = entryNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                try {
                    NewsArticleCandidate candidate = parseAtomEntry(element, fallbackSource);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                } catch (Exception e) {
                    log.warn("개별 Atom 엔트리 파싱 오류로 건너뜀: cause={}", e.getMessage());
                }
            }
        }
        return candidates;
    }

    private NewsArticleCandidate parseAtomEntry(Element element, String fallbackSource) {
        String title = cleanText(getDirectChildElementText(element, "title"));

        String link = null;
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                if ("link".equalsIgnoreCase(el.getLocalName()) || "link".equalsIgnoreCase(el.getNodeName())) {
                    if (el.hasAttribute("href")) {
                        link = el.getAttribute("href");
                        break;
                    } else if (el.getTextContent() != null && !el.getTextContent().isBlank()) {
                        link = el.getTextContent().trim();
                        break;
                    }
                }
            }
        }

        if (title == null || title.isBlank() || link == null || link.isBlank()) {
            return null;
        }

        String normalizedUrl = link.trim();
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            return null;
        }

        String source = fallbackSource;
        String authorName = getAuthorName(element);
        if (authorName != null && !authorName.isBlank()) {
            source = cleanText(authorName);
        }

        String publishedStr = getDirectChildElementText(element, "published");
        if (publishedStr == null || publishedStr.isBlank()) {
            publishedStr = getDirectChildElementText(element, "updated");
        }
        LocalDateTime publishedAt = RssDateParser.parse(publishedStr);

        String summary = cleanText(getDirectChildElementText(element, "summary"));
        if (summary == null || summary.isBlank()) {
            summary = cleanText(getDirectChildElementText(element, "content"));
        }

        return NewsArticleCandidate.builder()
                .title(title)
                .url(normalizedUrl)
                .source(source)
                .publishedAt(publishedAt)
                .description(summary)
                .build();
    }

    private String getAuthorName(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                if ("author".equalsIgnoreCase(el.getLocalName()) || "author".equalsIgnoreCase(el.getNodeName())) {
                    return getDirectChildElementText(el, "name");
                }
            }
        }
        return null;
    }

    private String extractChannelTitle(Document doc) {
        NodeList channelNodes = doc.getElementsByTagName("channel");
        if (channelNodes.getLength() > 0) {
            Element channel = (Element) channelNodes.item(0);
            return cleanText(getDirectChildElementText(channel, "title"));
        }
        NodeList feedNodes = doc.getElementsByTagName("feed");
        if (feedNodes.getLength() > 0) {
            Element feed = (Element) feedNodes.item(0);
            return cleanText(getDirectChildElementText(feed, "title"));
        }
        return null;
    }

    private String getDirectChildElementText(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                String nodeName = el.getNodeName();
                String localName = el.getLocalName();
                if (tagName.equalsIgnoreCase(nodeName) || (localName != null && tagName.equalsIgnoreCase(localName))) {
                    return el.getTextContent();
                }
            }
        }
        return null;
    }

    private String cleanText(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("<[^>]*>", "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .trim();
    }
}
