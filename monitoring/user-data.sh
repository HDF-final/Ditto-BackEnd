#!/bin/bash
set -eux
export DEBIAN_FRONTEND=noninteractive
apt update -y
apt install -y docker.io docker-compose-v2 curl

systemctl enable --now docker

APP_DIR="/opt/monitoring"
mkdir -p "$APP_DIR/grafana-provisioning/datasources" "$APP_DIR/grafana-provisioning/dashboards"
chown -R ubuntu:ubuntu "$APP_DIR"

cat << 'PROM_EOF' > "$APP_DIR/prometheus.yml"
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: "ditto-backend"
    metrics_path: /actuator/prometheus
    ec2_sd_configs:
      - region: ap-northeast-2
        port: 8081
        filters:
          - name: "tag:aws:autoscaling:groupName"
            values: ["HDF-asg-backend"]
    relabel_configs:
      - source_labels: [__meta_ec2_private_ip]
        target_label: instance
      - source_labels: [__meta_ec2_tag_Name]
        target_label: ec2_name
PROM_EOF

cat << 'DS_EOF' > "$APP_DIR/grafana-provisioning/datasources/prometheus.yml"
apiVersion: 1
datasources:
  - name: Prometheus
    uid: prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
DS_EOF

cat << 'DASH_PROV_EOF' > "$APP_DIR/grafana-provisioning/dashboards/dashboards.yml"
apiVersion: 1
providers:
  - name: ditto
    orgId: 1
    folder: ""
    type: file
    disableDeletion: false
    updateIntervalSeconds: 30
    options:
      path: /etc/grafana/provisioning/dashboards
DASH_PROV_EOF

cat << 'DASH_JSON_EOF' > "$APP_DIR/grafana-provisioning/dashboards/ditto-backend.json"
{
  "uid": "ditto-backend-overview",
  "title": "Ditto Backend Overview",
  "timezone": "browser",
  "schemaVersion": 39,
  "version": 2,
  "refresh": "30s",
  "time": { "from": "now-1h", "to": "now" },
  "panels": [
    {
      "id": 1,
      "title": "Backend Up",
      "type": "stat",
      "gridPos": { "x": 0, "y": 0, "w": 6, "h": 4 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        { "expr": "up{job=\"ditto-backend\"}", "legendFormat": "{{instance}}" }
      ],
      "fieldConfig": {
        "defaults": {
          "mappings": [
            { "type": "value", "options": { "0": { "text": "DOWN", "color": "red" }, "1": { "text": "UP", "color": "green" } } }
          ]
        }
      }
    },
    {
      "id": 2,
      "title": "Live Threads",
      "type": "stat",
      "gridPos": { "x": 6, "y": 0, "w": 6, "h": 4 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        { "expr": "jvm_threads_live_threads{job=\"ditto-backend\"}", "legendFormat": "{{instance}}" }
      ]
    },
    {
      "id": 3,
      "title": "CPU Usage %",
      "type": "stat",
      "gridPos": { "x": 12, "y": 0, "w": 6, "h": 4 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        { "expr": "process_cpu_usage{job=\"ditto-backend\"} * 100", "legendFormat": "{{instance}}" }
      ],
      "fieldConfig": { "defaults": { "unit": "percent" } }
    },
    {
      "id": 4,
      "title": "Heap Used (MB)",
      "type": "stat",
      "gridPos": { "x": 18, "y": 0, "w": 6, "h": 4 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        { "expr": "sum(jvm_memory_used_bytes{job=\"ditto-backend\",area=\"heap\"}) / 1024 / 1024", "legendFormat": "{{instance}}" }
      ]
    },
    {
      "id": 5,
      "title": "Request Rate by Status (req/s)",
      "type": "timeseries",
      "gridPos": { "x": 0, "y": 4, "w": 12, "h": 8 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "sum by (status) (rate(http_server_requests_seconds_count{job=\"ditto-backend\"}[5m]))",
          "legendFormat": "{{status}}"
        }
      ]
    },
    {
      "id": 6,
      "title": "Avg Response Time (ms)",
      "type": "timeseries",
      "gridPos": { "x": 12, "y": 4, "w": 12, "h": 8 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "1000 * sum(rate(http_server_requests_seconds_sum{job=\"ditto-backend\"}[5m])) / sum(rate(http_server_requests_seconds_count{job=\"ditto-backend\"}[5m]))",
          "legendFormat": "avg latency"
        }
      ],
      "fieldConfig": { "defaults": { "unit": "ms" } }
    },
    {
      "id": 7,
      "title": "DB Connection Pool (Hikari)",
      "type": "timeseries",
      "gridPos": { "x": 0, "y": 12, "w": 12, "h": 8 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        { "expr": "sum(hikaricp_connections_active{job=\"ditto-backend\"})", "legendFormat": "active" },
        { "expr": "sum(hikaricp_connections_idle{job=\"ditto-backend\"})", "legendFormat": "idle" },
        { "expr": "sum(hikaricp_connections_pending{job=\"ditto-backend\"})", "legendFormat": "pending (대기)" },
        { "expr": "sum(hikaricp_connections_max{job=\"ditto-backend\"})", "legendFormat": "max (풀 크기)" }
      ]
    },
    {
      "id": 8,
      "title": "Heap Memory Trend (누수 추적)",
      "type": "timeseries",
      "gridPos": { "x": 12, "y": 12, "w": 12, "h": 8 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        { "expr": "sum(jvm_memory_used_bytes{job=\"ditto-backend\",area=\"heap\"}) / 1024 / 1024", "legendFormat": "used (사용중)" },
        { "expr": "sum(jvm_memory_committed_bytes{job=\"ditto-backend\",area=\"heap\"}) / 1024 / 1024", "legendFormat": "committed (할당)" },
        { "expr": "sum(jvm_memory_max_bytes{job=\"ditto-backend\",area=\"heap\"}) / 1024 / 1024", "legendFormat": "max (한계)" }
      ],
      "fieldConfig": { "defaults": { "unit": "decmbytes" } },
      "description": "GC 이후에도 used가 매번 더 높은 바닥에서 시작하며 우상향하면 누수 의심. committed/max에 붙으면 OOM 위험."
    },
    {
      "id": 9,
      "title": "API별 요청률 Top 10 (req/s)",
      "type": "table",
      "gridPos": { "x": 0, "y": 20, "w": 12, "h": 8 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "topk(10, sum by (method, uri) (rate(http_server_requests_seconds_count{job=\"ditto-backend\"}[5m])))",
          "format": "table",
          "instant": true
        }
      ],
      "transformations": [
        { "id": "organize", "options": { "excludeByName": { "Time": true, "job": true, "instance": true, "ec2_name": true } } }
      ]
    },
    {
      "id": 10,
      "title": "API별 평균 지연 Top 10 (ms)",
      "type": "table",
      "gridPos": { "x": 12, "y": 20, "w": 12, "h": 8 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "topk(10, 1000 * sum by (uri) (rate(http_server_requests_seconds_sum{job=\"ditto-backend\"}[5m])) / sum by (uri) (rate(http_server_requests_seconds_count{job=\"ditto-backend\"}[5m])))",
          "format": "table",
          "instant": true
        }
      ],
      "transformations": [
        { "id": "organize", "options": { "excludeByName": { "Time": true, "job": true, "instance": true, "ec2_name": true } } }
      ]
    }
  ]
}
DASH_JSON_EOF

cat << 'COMPOSE_EOF' > "$APP_DIR/docker-compose.yml"
services:
  prometheus:
    image: prom/prometheus:v2.54.1
    container_name: ditto-prometheus
    restart: unless-stopped
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    command:
      - --config.file=/etc/prometheus/prometheus.yml
      - --storage.tsdb.path=/prometheus
      - --storage.tsdb.retention.time=15d
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana-oss:11.2.0
    container_name: ditto-grafana
    restart: unless-stopped
    depends_on:
      - prometheus
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana-provisioning/datasources:/etc/grafana/provisioning/datasources:ro
      - ./grafana-provisioning/dashboards:/etc/grafana/provisioning/dashboards:ro
    ports:
      - "3000:3000"

volumes:
  prometheus-data:
  grafana-data:
COMPOSE_EOF

docker compose -f "$APP_DIR/docker-compose.yml" up -d
