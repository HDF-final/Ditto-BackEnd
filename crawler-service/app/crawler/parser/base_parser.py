"""Base parser interface and common extraction logic for news articles."""

from abc import ABC, abstractmethod
from datetime import datetime
import re
from urllib.parse import urlparse

from selenium.webdriver.common.by import By
from selenium.webdriver.remote.webdriver import WebDriver

from app.crawler.exceptions import ArticleParseError
from app.models.crawled_article import CrawledArticle


class BaseNewsArticleParser(ABC):
    """뉴스 기사 파서 공통 추상 클래스."""

    @abstractmethod
    def get_supported_domains(self) -> list[str]:
        """지원 대상 도메인 목록 (예: ['yna.co.kr'])."""
        pass

    @abstractmethod
    def get_default_source(self) -> str:
        """기본 언론사 출처명 (예: 'Yonhap News')."""
        pass

    @abstractmethod
    def get_title_selectors(self) -> list[str]:
        """제목 추출용 CSS Selector 목록 (우선순위 순)."""
        pass

    @abstractmethod
    def get_body_container_selectors(self) -> list[str]:
        """본문 컨테이너 추출용 CSS Selector 목록."""
        pass

    @abstractmethod
    def get_date_selectors(self) -> list[str]:
        """발행일시 추출용 CSS Selector 목록."""
        pass

    def supports(self, url: str) -> bool:
        """
        URL의 hostname을 기반으로 해당 언론사 지원 여부를 엄격하게 판별합니다.
        위장 URL(쿼리스트링 위장, 서브도메인 위장 등)을 방어합니다.
        """
        if not url or not isinstance(url, str) or not url.strip():
            return False

        try:
            parsed = urlparse(url.strip())
            if parsed.scheme.lower() not in ("http", "https") or not parsed.hostname:
                return False

            hostname = parsed.hostname.lower()
            for domain in self.get_supported_domains():
                lower_domain = domain.lower()
                if hostname == lower_domain or hostname.endswith("." + lower_domain):
                    return True
            return False
        except Exception:
            return False

    def parse(self, driver: WebDriver, url: str) -> CrawledArticle:
        """Selenium WebDriver를 통해 렌더링된 DOM에서 기사 데이터를 추출합니다."""
        title = self.extract_title(driver)
        body = self.extract_body(driver)
        published_at = self.extract_published_at(driver)
        image_url = self.extract_image_url(driver)
        source = self.get_default_source()

        return CrawledArticle(
            title=title,
            body=body,
            url=url,
            source=source,
            published_at=published_at,
            image_url=image_url,
        )

    def extract_title(self, driver: WebDriver) -> str:
        """기사 제목을 우선순위 selector, og:title, document.title 순으로 추출합니다."""
        # 1. 사이트 전용 selector
        for selector in self.get_title_selectors():
            try:
                elements = driver.find_elements(By.CSS_SELECTOR, selector)
                for elem in elements:
                    text = self._clean_text(elem.text)
                    if text:
                        return text
            except Exception:
                continue

        # 2. meta[property="og:title"]
        try:
            og_elements = driver.find_elements(By.CSS_SELECTOR, 'meta[property="og:title"]')
            for elem in og_elements:
                content = elem.get_attribute("content")
                if content and content.strip():
                    return self._clean_text(content)
        except Exception:
            pass

        # 3. document title fallback
        try:
            doc_title = driver.title
            if doc_title and doc_title.strip():
                return self._clean_text(doc_title)
        except Exception:
            pass

        raise ArticleParseError("기사 제목(title)을 추출할 수 없습니다.")

    def extract_body(self, driver: WebDriver) -> str:
        """기사 본문 컨테이너에서 p 태그 기반으로 문단을 추출하고 정제합니다."""
        container = None
        for selector in self.get_body_container_selectors():
            try:
                elements = driver.find_elements(By.CSS_SELECTOR, selector)
                if elements and elements[0].is_displayed():
                    container = elements[0]
                    break
                elif elements:
                    container = elements[0]
                    break
            except Exception:
                continue

        if container is None:
            try:
                container = driver.find_element(By.TAG_NAME, "body")
            except Exception:
                raise ArticleParseError("기사 본문 컨테이너를 찾을 수 없습니다.")

        paragraphs: list[str] = []

        # 1. 컨테이너 내부 p 태그 탐색
        try:
            p_elements = container.find_elements(By.TAG_NAME, "p")
            for p in p_elements:
                text = self._clean_paragraph(p.text)
                if text and not self._is_boilerplate(text):
                    paragraphs.append(text)
        except Exception:
            pass

        # 2. p 태그로 추출된 문단이 없을 경우 줄바꿈 분할 탐색
        if not paragraphs:
            try:
                raw_text = container.text
                for line in raw_text.splitlines():
                    text = self._clean_paragraph(line)
                    if text and not self._is_boilerplate(text):
                        paragraphs.append(text)
            except Exception:
                pass

        if not paragraphs:
            raise ArticleParseError("기사 본문(body) 문단을 추출할 수 없습니다.")

        combined_body = "\n\n".join(paragraphs)
        if len(combined_body) < 20:
            raise ArticleParseError("추출된 기사 본문이 너무 짧아 유효하지 않습니다.")

        return combined_body

    def extract_published_at(self, driver: WebDriver) -> datetime | None:
        """기사 발행 일시를 추출합니다."""
        # 1. 사이트 전용 date selector
        for selector in self.get_date_selectors():
            try:
                elements = driver.find_elements(By.CSS_SELECTOR, selector)
                for elem in elements:
                    datetime_attr = elem.get_attribute("datetime")
                    parsed = self._parse_date_string(datetime_attr) if datetime_attr else None
                    if parsed:
                        return parsed

                    parsed = self._parse_date_string(elem.text)
                    if parsed:
                        return parsed
            except Exception:
                continue

        # 2. meta tags (article:published_time, pubdate, date)
        meta_selectors = [
            'meta[property="article:published_time"]',
            'meta[name="pubdate"]',
            'meta[name="date"]',
        ]
        for meta_sel in meta_selectors:
            try:
                elements = driver.find_elements(By.CSS_SELECTOR, meta_sel)
                for elem in elements:
                    content = elem.get_attribute("content")
                    parsed = self._parse_date_string(content)
                    if parsed:
                        return parsed
            except Exception:
                continue

        return None

    def extract_image_url(self, driver: WebDriver) -> str | None:
        """대표 이미지 메타 태그(og:image, twitter:image) URL을 추출합니다."""
        meta_selectors = [
            'meta[property="og:image"]',
            'meta[name="twitter:image"]',
        ]
        for meta_sel in meta_selectors:
            try:
                elements = driver.find_elements(By.CSS_SELECTOR, meta_sel)
                for elem in elements:
                    content = elem.get_attribute("content")
                    if content and isinstance(content, str):
                        trimmed = content.strip()
                        if trimmed.startswith("http://") or trimmed.startswith("https://"):
                            return trimmed
            except Exception:
                continue

        return None

    def _clean_paragraph(self, text: str) -> str:
        """문단 앞뒤 공백 및 연속 공백을 정규화합니다."""
        if not text:
            return ""
        # 탭, 유니코드 공백 정규화
        normalized = re.sub(r"[\t\s]+", " ", text)
        return normalized.strip()

    def _clean_text(self, text: str) -> str:
        """일반 텍스트 정제."""
        if not text:
            return ""
        return re.sub(r"\s+", " ", text).strip()

    def _is_boilerplate(self, text: str) -> bool:
        """기사 본문과 무관한 저작권, 기자 이메일, 광고 등 보일러플레이트 문구 필터링."""
        if len(text) < 3:
            return True

        lower = text.lower()
        # 저작권 및 무단 전재 문구
        if (
            "all rights reserved" in lower
            or "copyright" in lower
            or "무단 전재" in lower
            or "무단전재" in lower
            or "저작권자" in lower
        ):
            return True

        # 단독 이메일 행
        if re.match(r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}\s*$", text.strip()):
            return True

        return False

    def _parse_date_string(self, raw: str | None) -> datetime | None:
        """다양한 형식의 날짜 문자열을 datetime 객체로 파싱합니다."""
        if not raw or not isinstance(raw, str) or not raw.strip():
            return None

        cleaned = raw.strip()

        # 1. ISO 8601 형식 (2026-08-16T10:00:00+09:00, 2026-08-16T10:00:00Z)
        try:
            iso_str = cleaned.replace("Z", "+00:00")
            return datetime.fromisoformat(iso_str)
        except Exception:
            pass

        # 2. 한국어 날짜 형식: 2026-08-16 10:00:00 또는 2026-08-16 10:00 또는 2026.08.16. 10:00
        match = re.search(r"(\d{4})[-./](\d{1,2})[-./](\d{1,2})[^\d]*(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?", cleaned)
        if match:
            year, month, day, hour, minute, second = match.groups()
            sec = int(second) if second else 0
            try:
                return datetime(int(year), int(month), int(day), int(hour), int(minute), sec)
            except Exception:
                pass

        # 3. 영문 월일 형식: Aug 16, 2026 - 15:30
        month_map = {
            "jan": 1, "feb": 2, "mar": 3, "apr": 4, "may": 5, "jun": 6,
            "jul": 7, "aug": 8, "sep": 9, "oct": 10, "nov": 11, "dec": 12
        }
        en_match = re.search(r"([a-zA-Z]{3})\s+(\d{1,2}),?\s+(\d{4})[^\d]*(\d{1,2}):(\d{1,2})", cleaned)
        if en_match:
            mon_str, day_str, yr_str, hr_str, min_str = en_match.groups()
            mon_num = month_map.get(mon_str.lower())
            if mon_num:
                try:
                    return datetime(int(yr_str), mon_num, int(day_str), int(hr_str), int(min_str), 0)
                except Exception:
                    pass

        return None
