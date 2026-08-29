#!/bin/bash
set -eux
export DEBIAN_FRONTEND=noninteractive
apt update -y
apt install -y docker.io docker-compose-v2 curl

systemctl enable --now docker

APP_DIR="/opt/monitoring"
mkdir -p "$APP_DIR/grafana-provisioning/datasources"
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
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
DS_EOF

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
    ports:
      - "3000:3000"

volumes:
  prometheus-data:
  grafana-data:
COMPOSE_EOF

docker compose -f "$APP_DIR/docker-compose.yml" up -d
