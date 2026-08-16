from fastapi import FastAPI
from pydantic import BaseModel, Field


class HealthResponse(BaseModel):
    status: str = Field(..., description="Service health status", examples=["ok"])
    service: str = Field(..., description="Service identifier", examples=["ditto-news-crawler"])


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
