"""The Korea Times article parser."""

from app.crawler.parser.base_parser import BaseNewsArticleParser


class KoreaTimesArticleParser(BaseNewsArticleParser):
    """The Korea Times 기사 파서."""

    def get_supported_domains(self) -> list[str]:
        return ["koreatimes.co.kr"]

    def get_default_source(self) -> str:
        return "The Korea Times"

    def get_title_selectors(self) -> list[str]:
        return [
            "div.view_head h1",
            "h1.headline",
            "div.view_tit",
            "h1.view_title",
        ]

    def get_body_container_selectors(self) -> list[str]:
        return [
            "#article-body",
            "div#articleBody",
            "div.view_article",
            "div.article_body",
        ]

    def get_date_selectors(self) -> list[str]:
        return [
            "div.view_head_info .date",
            "span.date",
            "div.date_area",
            "div.view_info span",
        ]
