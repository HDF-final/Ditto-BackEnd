"""The Korea Herald article parser."""

from app.crawler.parser.base_parser import BaseNewsArticleParser


class KoreaHeraldArticleParser(BaseNewsArticleParser):
    """The Korea Herald 기사 파서."""

    def get_supported_domains(self) -> list[str]:
        return ["koreaherald.com"]

    def get_default_source(self) -> str:
        return "The Korea Herald"

    def get_title_selectors(self) -> list[str]:
        return [
            "h1.view_tit",
            "h1.article_title",
            "div.view_tit",
            "h1.headline",
        ]

    def get_body_container_selectors(self) -> list[str]:
        return [
            "#articleText",
            "div#articleText",
            "div.view_con_t",
            "div.article_content",
            "div#article-body",
        ]

    def get_date_selectors(self) -> list[str]:
        return [
            ".view_tit_by span",
            "div.view_tit_by",
            ".date_time",
            "p.posted_time",
        ]
