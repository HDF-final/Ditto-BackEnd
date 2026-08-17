from unittest.mock import MagicMock, patch
from datetime import datetime
from fastapi.testclient import TestClient

from app.main import app
from app.models.crawled_article import CrawledArticle

client = TestClient(app)


def test_post_crawl_success():
    mock_article = CrawledArticle(
        title="Mock News Title",
        body="Mock article body content.",
        url="https://www.yna.co.kr/view/AKR20260816000100005",
        source="Yonhap News",
        published_at=datetime(2026, 8, 16, 12, 0, 0),
        image_url="https://img.yna.co.kr/photo.jpg",
    )

    with patch("app.main.SeleniumNewsCrawler") as mock_crawler_cls:
        mock_instance = MagicMock()
        mock_instance.crawl.return_value = mock_article
        mock_crawler_cls.return_value = mock_instance

        response = client.post("/crawl", json={"url": "https://www.yna.co.kr/view/AKR20260816000100005"})

        assert response.status_code == 200
        data = response.json()
        assert data["title"] == "Mock News Title"
        assert data["body"] == "Mock article body content."
        assert data["url"] == "https://www.yna.co.kr/view/AKR20260816000100005"
        assert data["source"] == "Yonhap News"
        assert data["image_url"] == "https://img.yna.co.kr/photo.jpg"


def test_post_crawl_invalid_url_returns_400():
    response = client.post("/crawl", json={"url": "ftp://invalid-url.com"})
    assert response.status_code == 400


def test_post_crawl_unsupported_domain_returns_400():
    response = client.post("/crawl", json={"url": "https://www.unsupported-domain.com/news/1"})
    assert response.status_code == 400
