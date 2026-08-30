#!/bin/bash
# AWS SSM Grafana Port Forwarding Tunnel Script

REGION="ap-northeast-2"
TARGET_INSTANCE="i-059f356b8fbcb152c"
GRAFANA_HOST="10.0.142.75"
REMOTE_PORT="3000"
LOCAL_PORT="3000"

echo "=========================================================="
echo " Starting AWS SSM Port Forwarding Tunnel to Grafana..."
echo " Local Port     : ${LOCAL_PORT}"
echo " Remote Grafana : ${GRAFANA_HOST}:${REMOTE_PORT}"
echo " EC2 Bastion    : ${TARGET_INSTANCE}"
echo "=========================================================="
echo " (Press Ctrl+C to stop the tunnel)"
echo ""

aws ssm start-session \
  --region "${REGION}" \
  --target "${TARGET_INSTANCE}" \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{\"host\":[\"${GRAFANA_HOST}\"],\"portNumber\":[\"${REMOTE_PORT}\"],\"localPortNumber\":[\"${LOCAL_PORT}\"]}"
