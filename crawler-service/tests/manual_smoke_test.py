"""수동 외부 뉴스 기사 Selenium 크롤링 검증 스크립트.

실행 방법:
    cd crawler-service
    source .venv/bin/activate
    python -m tests.manual_smoke_test
"""

import sys
from app.crawler.selenium_news_crawler import SeleniumNewsCrawler


def main() -> None:
    # 기본 검증 URL (실시간 연합뉴스 K-POP 기사)
    target_url = (
        sys.argv[1]
        if len(sys.argv) > 1
        else "https://www.yna.co.kr/view/AKR20260814147200005"
    )

    print("\n" + "=" * 70)
    print(" >>> DITTO Python Selenium 뉴스 크롤러 수동 검증 시작 <<<")
    print(f" - 대상 URL: {target_url}")
    print("=" * 70)

    crawler = SeleniumNewsCrawler(timeout_seconds=15)
    article = crawler.crawl(target_url)

    print(f" [1] 제목: {article.title}")
    print(f" [2] 언론사: {article.source}")
    print(f" [3] 발행일시: {article.published_at}")
    print(f" [4] 대표 이미지: {article.image_url}")
    print(" [5] 본문 요약 (앞 250자):")
    preview = article.body[:250] if len(article.body) > 250 else article.body
    print(f'     "{preview}..."')
    print("=" * 70)
    print(" >>> 수동 검증 성공 완료 <<<\n")


if __name__ == "__main__":
    main()
