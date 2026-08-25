#!/bin/bash
# AWS SSM RDS Port Forwarding Tunnel Script

PROFILE="HDF-ko"
REGION="ap-northeast-2"
TARGET_INSTANCE="i-059f356b8fbcb152c"
RDS_HOST="ditto-dev-oracle.caz9bnevtpvk.ap-northeast-2.rds.amazonaws.com"
REMOTE_PORT="1521"
LOCAL_PORT="11521"

echo "=========================================================="
echo " Starting AWS SSM Port Forwarding Tunnel to Oracle RDS..."
echo " Local Port  : ${LOCAL_PORT}"
echo " Remote RDS  : ${RDS_HOST}:${REMOTE_PORT}"
echo " EC2 Bastion : ${TARGET_INSTANCE}"
echo "=========================================================="
echo " (Press Ctrl+C to stop the tunnel)"
echo ""

aws ssm start-session \
  --profile "${PROFILE}" \
  --region "${REGION}" \
  --target "${TARGET_INSTANCE}" \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{\"host\":[\"${RDS_HOST}\"],\"portNumber\":[\"${REMOTE_PORT}\"],\"localPortNumber\":[\"${LOCAL_PORT}\"]}"
