from fastapi import FastAPI, HTTPException, status
from pydantic import BaseModel, Field

from app.crawler.exceptions import (
    CrawlerError,
    InvalidUrlError,
    UnsupportedNewsDomainError,
    ArticleParseError,
)
from app.crawler.selenium_news_crawler import SeleniumNewsCrawler
from app.models.crawled_article import CrawledArticle


class HealthResponse(BaseModel):
    status: str = Field(..., description="Service health status", examples=["ok"])
    service: str = Field(..., description="Service identifier", examples=["ditto-news-crawler"])


class CrawlRequest(BaseModel):
    url: str = Field(..., description="크롤링 대상 기사 URL", examples=["https://www.yna.co.kr/view/AKR20240101000100005"])


app = FastAPI(
    title="DITTO News Crawler Service",
    description="Python-based news article crawling service for DITTO backend pipeline",
    version="1.0.0",
)


@app.get(
    "/health",
    response_model=HealthResponse,
    summary="Service Health Check",
    description="Checks whether the news crawler service process is running normally.",
    tags=["Monitoring"],
)
def get_health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        service="ditto-news-crawler",
    )


@app.post(
    "/crawl",
    response_model=CrawledArticle,
    summary="뉴스 기사 본문 크롤링",
    description="URL을 전달받아 Selenium으로 렌더링 후 기사 상세 정보를 추출합니다.",
    tags=["Crawler"],
)
def crawl_article(request: CrawlRequest) -> CrawledArticle:
    crawler = SeleniumNewsCrawler()
    try:
        return crawler.crawl(request.url)
    except (InvalidUrlError, UnsupportedNewsDomainError) as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        ) from e
    except ArticleParseError as e:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=str(e),
        ) from e
    except CrawlerError as e:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"크롤링 처리 실패: {e}",
        ) from e
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"서버 내부 오류 발생: {e}",
        ) from e
