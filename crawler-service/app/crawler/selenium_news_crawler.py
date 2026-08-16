"""Selenium-based news crawler orchestrator."""

from urllib.parse import urlparse
from selenium import webdriver
from selenium.webdriver.chrome.options import Options

from app.crawler.exceptions import InvalidUrlError, UnsupportedNewsDomainError
from app.crawler.parser.base_parser import BaseNewsArticleParser
from app.crawler.parser.korea_herald_parser import KoreaHeraldArticleParser
from app.crawler.parser.korea_times_parser import KoreaTimesArticleParser
from app.crawler.parser.yonhap_parser import YonhapNewsArticleParser
from app.models.crawled_article import CrawledArticle


class SeleniumNewsCrawler:
    """Selenium WebDriver를 관리하고 사이트별 Parser에 위임하는 뉴스 크롤러."""

    def __init__(
        self,
        parsers: list[BaseNewsArticleParser] | None = None,
        timeout_seconds: int = 10,
    ):
        self.timeout_seconds = timeout_seconds
        self.parsers = parsers or [
            YonhapNewsArticleParser(),
            KoreaHeraldArticleParser(),
            KoreaTimesArticleParser(),
        ]

    def create_driver(self) -> webdriver.Chrome:
        """Headless Chrome WebDriver를 생성합니다 (Selenium Manager 자동 연동)."""
        options = Options()
        options.add_argument("--headless=new")
        options.add_argument("--disable-gpu")
        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        options.add_argument("--window-size=1920,1080")
        options.add_argument("--disable-extensions")
        # 텍스트 및 메타데이터 파싱 속도를 위한 eager 로딩 전략
        options.page_load_strategy = "eager"
        return webdriver.Chrome(options=options)

    def validate_url(self, url: str) -> str:
        """기사 URL의 형식 및 프로토콜을 검증합니다."""
        if not url or not isinstance(url, str) or not url.strip():
            raise InvalidUrlError("기사 URL이 비어있거나 유효하지 않습니다.")

        trimmed = url.strip()
        try:
            parsed = urlparse(trimmed)
            if parsed.scheme.lower() not in ("http", "https") or not parsed.hostname:
                raise InvalidUrlError(f"지원하지 않는 프로토콜 또는 잘못된 URL 형식입니다: {url}")
        except Exception as e:
            if isinstance(e, InvalidUrlError):
                raise
            raise InvalidUrlError(f"URL 파싱 오류: {url}") from e

        return trimmed

    def find_parser(self, url: str) -> BaseNewsArticleParser:
        """주어진 URL을 지원하는 사이트 Parser를 탐색합니다."""
        for parser in self.parsers:
            if parser.supports(url):
                return parser
        raise UnsupportedNewsDomainError(f"지원하지 않는 뉴스 언론사 도메인입니다: {url}")

    def crawl(self, url: str) -> CrawledArticle:
        """
        주어진 뉴스 기사 URL을 Selenium으로 렌더링하고 기사 정보를 추출합니다.
        성공 및 실패 모든 상황에서 WebDriver의 종료(driver.quit)가 보장됩니다.
        """
        valid_url = self.validate_url(url)
        parser = self.find_parser(valid_url)

        driver = self.create_driver()
        try:
            driver.set_page_load_timeout(self.timeout_seconds)
            driver.get(valid_url)
            return parser.parse(driver, valid_url)
        finally:
            try:
                driver.quit()
            except Exception:
                pass
