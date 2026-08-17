"""Unit tests for SeleniumNewsCrawler and site-specific parsers."""

from datetime import datetime
from unittest.mock import MagicMock, patch
import pytest

from app.crawler.exceptions import (
    ArticleParseError,
    InvalidUrlError,
    UnsupportedNewsDomainError,
)
from app.crawler.parser.base_parser import BaseNewsArticleParser
from app.crawler.parser.korea_herald_parser import KoreaHeraldArticleParser
from app.crawler.parser.korea_times_parser import KoreaTimesArticleParser
from app.crawler.parser.yonhap_parser import YonhapNewsArticleParser
from app.crawler.selenium_news_crawler import SeleniumNewsCrawler
from app.models.crawled_article import CrawledArticle


# ==============================================================================
# 1. URL Host Validation and Spoofed URL Defense Tests
# ==============================================================================
class TestParserSupports:

    @pytest.fixture
    def yonhap_parser(self):
        return YonhapNewsArticleParser()

    @pytest.fixture
    def korea_herald_parser(self):
        return KoreaHeraldArticleParser()

    @pytest.fixture
    def korea_times_parser(self):
        return KoreaTimesArticleParser()

    def test_supports_valid_domains_and_subdomains(
        self, yonhap_parser, korea_herald_parser, korea_times_parser
    ):
        assert yonhap_parser.supports("https://www.yna.co.kr/view/AKR123") is True
        assert yonhap_parser.supports("https://en.yna.co.kr/view/AEN123") is True
        assert yonhap_parser.supports("http://yna.co.kr/view/1") is True

        assert korea_herald_parser.supports("http://www.koreaherald.com/view.php?ud=1") is True
        assert korea_herald_parser.supports("https://koreaherald.com/article/123") is True

        assert korea_times_parser.supports("https://www.koreatimes.co.kr/www/art/123.html") is True
        assert korea_times_parser.supports("http://koreatimes.co.kr/1") is True

    def test_rejects_other_domains(
        self, yonhap_parser, korea_herald_parser, korea_times_parser
    ):
        assert yonhap_parser.supports("https://www.koreaherald.com/1") is False
        assert korea_herald_parser.supports("https://www.koreatimes.co.kr/1") is False
        assert korea_times_parser.supports("https://www.bbc.com/news") is False

    def test_rejects_spoofed_urls(
        self, yonhap_parser, korea_herald_parser, korea_times_parser
    ):
        # 1. 쿼리 파라미터 위장
        assert yonhap_parser.supports("https://evil.com/?url=https://yna.co.kr") is False
        assert korea_herald_parser.supports("https://attacker.org/news?site=koreaherald.com") is False

        # 2. 공격자 도메인의 서브도메인 위장
        assert yonhap_parser.supports("https://yna.co.kr.evil.com/phishing") is False
        assert korea_times_parser.supports("https://koreatimes.co.kr.attacker.net/1") is False

        # 3. 점(.) 경계 없는 유사 도메인
        assert yonhap_parser.supports("https://fakeyna.co.kr/view") is False
        assert korea_herald_parser.supports("https://notkoreaherald.com/1") is False
        assert korea_times_parser.supports("https://fakekoreatimes.co.kr/1") is False

    def test_rejects_none_blank_and_invalid_schemes(self, yonhap_parser):
        assert yonhap_parser.supports(None) is False
        assert yonhap_parser.supports("") is False
        assert yonhap_parser.supports("   ") is False
        assert yonhap_parser.supports("ftp://www.yna.co.kr/1") is False
        assert yonhap_parser.supports("javascript:alert(1)") is False


# ==============================================================================
# 2. SeleniumNewsCrawler Orchestrator Tests
# ==============================================================================
class TestSeleniumNewsCrawler:

    def test_validate_url_success(self):
        crawler = SeleniumNewsCrawler()
        assert crawler.validate_url("https://www.yna.co.kr/view/1") == "https://www.yna.co.kr/view/1"
        assert crawler.validate_url("  http://koreaherald.com/1  ") == "http://koreaherald.com/1"

    def test_validate_url_raises_for_invalid_input(self):
        crawler = SeleniumNewsCrawler()
        with pytest.raises(InvalidUrlError):
            crawler.validate_url(None)
        with pytest.raises(InvalidUrlError):
            crawler.validate_url("")
        with pytest.raises(InvalidUrlError):
            crawler.validate_url("   ")
        with pytest.raises(InvalidUrlError):
            crawler.validate_url("ftp://koreaherald.com/1")

    def test_find_parser_returns_matching_strategy(self):
        crawler = SeleniumNewsCrawler()
        assert isinstance(
            crawler.find_parser("https://www.yna.co.kr/view/1"), YonhapNewsArticleParser
        )
        assert isinstance(
            crawler.find_parser("https://koreaherald.com/view.php"), KoreaHeraldArticleParser
        )
        assert isinstance(
            crawler.find_parser("https://www.koreatimes.co.kr/1.html"), KoreaTimesArticleParser
        )

    def test_find_parser_raises_for_unsupported_domain(self):
        crawler = SeleniumNewsCrawler()
        with pytest.raises(UnsupportedNewsDomainError):
            crawler.find_parser("https://www.unsupported-news.com/article/1")

    @patch.object(SeleniumNewsCrawler, "create_driver")
    def test_crawl_guarantees_driver_quit_on_success(self, mock_create_driver):
        mock_driver = MagicMock()
        mock_create_driver.return_value = mock_driver

        mock_parser = MagicMock(spec=BaseNewsArticleParser)
        mock_parser.supports.return_value = True
        mock_parser.parse.return_value = CrawledArticle(
            title="Sample Title",
            body="Sample Body Content",
            url="https://www.yna.co.kr/view/1",
            source="Yonhap News",
            published_at=datetime(2026, 8, 16, 10, 0),
            image_url="https://img.yna.co.kr/1.jpg",
        )

        crawler = SeleniumNewsCrawler(parsers=[mock_parser])
        article = crawler.crawl("https://www.yna.co.kr/view/1")

        assert article.title == "Sample Title"
        mock_driver.get.assert_called_once_with("https://www.yna.co.kr/view/1")
        mock_driver.quit.assert_called_once()

    @patch.object(SeleniumNewsCrawler, "create_driver")
    def test_crawl_guarantees_driver_quit_on_failure(self, mock_create_driver):
        mock_driver = MagicMock()
        mock_create_driver.return_value = mock_driver

        mock_parser = MagicMock(spec=BaseNewsArticleParser)
        mock_parser.supports.return_value = True
        mock_parser.parse.side_effect = ArticleParseError("DOM parsing failed")

        crawler = SeleniumNewsCrawler(parsers=[mock_parser])

        with pytest.raises(ArticleParseError):
            crawler.crawl("https://www.yna.co.kr/view/1")

        mock_driver.quit.assert_called_once()


# ==============================================================================
# 3. BaseParser Extraction and Fallback Tests
# ==============================================================================
class TestBaseParserExtraction:

    def test_extract_title_fallbacks(self):
        parser = YonhapNewsArticleParser()
        mock_driver = MagicMock()

        # 1. 사이트 전용 selector 성공
        mock_title_elem = MagicMock()
        mock_title_elem.text = "  K-POP Global Chart Record  "
        mock_driver.find_elements.side_effect = lambda by, sel: (
            [mock_title_elem] if sel == "h1.tit" else []
        )
        assert parser.extract_title(mock_driver) == "K-POP Global Chart Record"

        # 2. og:title fallback
        mock_og_elem = MagicMock()
        mock_og_elem.get_attribute.return_value = "OG Fallback Title"
        mock_driver.find_elements.side_effect = lambda by, sel: (
            [mock_og_elem] if sel == 'meta[property="og:title"]' else []
        )
        assert parser.extract_title(mock_driver) == "OG Fallback Title"

        # 3. document title fallback
        mock_driver.find_elements.side_effect = None
        mock_driver.find_elements.return_value = []
        mock_driver.title = "Document Title"
        assert parser.extract_title(mock_driver) == "Document Title"

        # 4. 모두 실패 시 예외
        mock_driver.title = ""
        with pytest.raises(ArticleParseError):
            parser.extract_title(mock_driver)

    def test_extract_body_paragraphs_and_boilerplate_removal(self):
        parser = YonhapNewsArticleParser()
        mock_driver = MagicMock()

        mock_container = MagicMock()
        mock_p1 = MagicMock()
        mock_p1.text = "첫 번째 문단입니다. 그룹의 신곡이 발매되었습니다."
        mock_p2 = MagicMock()
        mock_p2.text = "   " # 빈 문단
        mock_p3 = MagicMock()
        mock_p3.text = "두 번째 문단입니다. 전 세계 차트 1위에 올랐습니다."
        mock_p4 = MagicMock()
        mock_p4.text = "reporter@yna.co.kr" # 이메일 보일러플레이트
        mock_p5 = MagicMock()
        mock_p5.text = "(c) All rights reserved. 무단 전재 및 재배포 금지" # 저작권

        mock_container.find_elements.return_value = [mock_p1, mock_p2, mock_p3, mock_p4, mock_p5]
        mock_driver.find_elements.return_value = [mock_container]

        body = parser.extract_body(mock_driver)

        paragraphs = body.split("\n\n")
        assert len(paragraphs) == 2
        assert paragraphs[0] == "첫 번째 문단입니다. 그룹의 신곡이 발매되었습니다."
        assert paragraphs[1] == "두 번째 문단입니다. 전 세계 차트 1위에 올랐습니다."

    def test_extract_body_raises_when_empty_or_too_short(self):
        parser = YonhapNewsArticleParser()
        mock_driver = MagicMock()

        mock_container = MagicMock()
        mock_container.find_elements.return_value = []
        mock_container.text = "short"
        mock_driver.find_elements.return_value = [mock_container]

        with pytest.raises(ArticleParseError):
            parser.extract_body(mock_driver)

    def test_date_parsing_various_formats(self):
        parser = YonhapNewsArticleParser()

        # ISO 8601
        iso_date = parser._parse_date_string("2026-08-16T10:30:00+09:00")
        assert iso_date == datetime.fromisoformat("2026-08-16T10:30:00+09:00")

        # 한국어 송고시간 형식
        kr_date = parser._parse_date_string("송고시간 2026-08-16 09:15")
        assert kr_date == datetime(2026, 8, 16, 9, 15, 0)

        # 영문 날짜 형식
        en_date = parser._parse_date_string("Published : Aug 16, 2026 - 15:30")
        assert en_date == datetime(2026, 8, 16, 15, 30, 0)

        # 잘못된 형식은 None 반환
        assert parser._parse_date_string("invalid-date") is None
        assert parser._parse_date_string(None) is None

    def test_extract_image_url(self):
        parser = YonhapNewsArticleParser()
        mock_driver = MagicMock()

        mock_img_elem = MagicMock()
        mock_img_elem.get_attribute.return_value = "https://img.yna.co.kr/sample.jpg"
        mock_driver.find_elements.side_effect = lambda by, sel: (
            [mock_img_elem] if sel == 'meta[property="og:image"]' else []
        )

        assert parser.extract_image_url(mock_driver) == "https://img.yna.co.kr/sample.jpg"

        # invalid scheme
        mock_img_elem.get_attribute.return_value = "data:image/png;base64,..."
        assert parser.extract_image_url(mock_driver) is None
