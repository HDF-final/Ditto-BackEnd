"""Yonhap News Agency article parser."""

from app.crawler.parser.base_parser import BaseNewsArticleParser


class YonhapNewsArticleParser(BaseNewsArticleParser):
    """연합뉴스(Yonhap News Agency) 기사 파서."""

    def get_supported_domains(self) -> list[str]:
        return ["yna.co.kr"]

    def get_default_source(self) -> str:
        return "Yonhap News"

    def get_title_selectors(self) -> list[str]:
        return [
            "h1.tit",
            "h1.tit-article",
            "h1.title-article",
            "div.title-article h1",
        ]

    def get_body_container_selectors(self) -> list[str]:
        return [
            "article.story-news",
            "div.story-news",
            "div.article",
            "div#articleBody",
        ]

    def get_date_selectors(self) -> list[str]:
        return [
            "p.update-time",
            "span.tt",
            "div.info-box span.txt-time",
            "p.txt-time",
        ]
