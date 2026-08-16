"""Data models for crawled news articles."""

from datetime import datetime
from pydantic import BaseModel, Field


class CrawledArticle(BaseModel):
    title: str = Field(..., description="기사 제목")
    body: str = Field(..., description="정제된 기사 본문 (\n\n으로 구분된 문단)")
    url: str = Field(..., description="기사 원문 URL")
    source: str | None = Field(default=None, description="언론사 / 출처")
    published_at: datetime | None = Field(default=None, description="기사 발행 일시")
    image_url: str | None = Field(default=None, description="대표 이미지 URL")
