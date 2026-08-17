"""Custom exception classes for news crawler service."""


class CrawlerError(Exception):
    """뉴스 크롤러 최상위 기본 예외."""
    pass


class InvalidUrlError(CrawlerError):
    """유효하지 않은 URL 입력 시 발생하는 예외."""
    pass


class UnsupportedNewsDomainError(CrawlerError):
    """지원하지 않는 뉴스 언론사 도메인일 때 발생하는 예외."""
    pass


class ArticleParseError(CrawlerError):
    """기사 본문 또는 필수 정보 추출 실패 시 발생하는 예외."""
    pass
