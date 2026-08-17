from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_get_health_returns_200_and_expected_payload():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["service"] == "ditto-news-crawler"
