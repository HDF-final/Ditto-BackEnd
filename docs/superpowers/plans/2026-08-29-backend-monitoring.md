# 백엔드 모니터링 (Prometheus + Grafana) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** ASG로 계속 교체되는 백엔드 EC2의 트래픽(HTTP)과 JVM 메트릭을, 프라이빗 서브넷의 Prometheus+Grafana로 보고 기존 베스천을 통해 조회할 수 있게 한다.

**Architecture:** 백엔드에 `/actuator/prometheus`(포트 8081, ALB 미노출)를 추가하고, 별도 모니터링 EC2 1대에서 Prometheus가 `ec2_sd_configs`로 ASG(`HDF-asg-backend`) 소속 인스턴스를 매 스크레이프마다 재탐색해 8081을 스크레이프한다. 같은 인스턴스의 Grafana가 로컬 Prometheus를 데이터소스로 쓴다. 접근은 베스천을 통한 SSM 포트포워딩(`AWS-StartPortForwardingSessionToRemoteHost`)만 사용 — SSH 키 없음.

**Tech Stack:** Spring Boot Actuator + Micrometer(prometheus registry), Docker Compose, Prometheus, Grafana OSS, AWS EC2/IAM/SSM (CLI, IaC 없음).

**Spec:** `docs/superpowers/specs/2026-08-29-backend-monitoring-design.md`

## Global Constraints

- IaC(Terraform/CDK) 도입 금지 — 모든 AWS 리소스는 CLI로 수동 생성.
- SSH 키페어 사용 금지 — 모든 접근은 SSM Session Manager 경유.
- Grafana 접근은 베스천(`i-059f356b8fbcb152c`)을 통한 포트포워딩만 허용 — 공개 접근 없음.
- ALB 타겟그룹은 8080만 계속 봐야 함 — 8081(메트릭 포트)을 ALB에 절대 물리지 않는다.
- 리전은 `ap-northeast-2`, VPC는 `vpc-0bbf264ac01aeffde`로 고정.
- 레포 루트: `/Users/an/Downloads/Ditto-back/Ditto-backend/Ditto-BackEnd` — 파일을 다루는 모든 태스크는 이 경로에서 `cd` 후 시작한다.

---

### Task 1: 백엔드에 Prometheus 메트릭 엔드포인트 노출

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/application.yml:80-87`
- Modify: `compose.yml`
- Modify: `.github/workflows/backend-cicd.yml` (user-data 내 embedded `docker-compose.yml` 부분)

**Interfaces:**
- Produces: 백엔드 인스턴스의 `:8081/actuator/prometheus` (Prometheus 텍스트 포맷). Task 4(Prometheus 설정)가 이 경로/포트를 스크레이프 대상으로 소비한다.

- [ ] **Step 1: build.gradle에 micrometer-registry-prometheus 의존성 추가**

```bash
cd /Users/an/Downloads/Ditto-back/Ditto-backend/Ditto-BackEnd
```

`build.gradle`에서 `implementation 'org.springframework.boot:spring-boot-starter-actuator'` 줄 바로 아래에 추가:

```groovy
    implementation 'io.micrometer:micrometer-registry-prometheus'
```

- [ ] **Step 2: application.yml에 관리 포트 분리 및 prometheus 엔드포인트 노출**

`src/main/resources/application.yml`의 기존 `management:` 블록(80~87번째 줄 부근)을 아래로 교체:

```yaml
# Actuator Health Check & Prometheus Metrics
management:
  server:
    port: 8081
  endpoints:
    web:
      exposure:
        include: health, info, prometheus
  endpoint:
    health:
      show-details: when-authorized
```

- [ ] **Step 3: 로컬 컴파일 확인**

Run: `./gradlew compileJava -x test --no-daemon`
Expected: `BUILD SUCCESSFUL`. 실패하면 의존성 좌표나 YAML 들여쓰기를 확인한다.

- [ ] **Step 4: compose.yml에 8081 포트 매핑 추가**

`compose.yml`의 `ports:` 아래 `"8080:8080"` 다음 줄에 추가:

```yaml
      - "8081:8081"
```

- [ ] **Step 5: CI/CD user-data에 embedded compose.yml도 동일하게 수정**

`.github/workflows/backend-cicd.yml`의 `cat << 'COMPOSE_EOF' > "\$APP_DIR/docker-compose.yml"` 블록 안에서도 `"8080:8080"` 다음 줄에 동일하게 추가:

```yaml
              - "8081:8081"
```

(이 블록은 `compose.yml`과 내용이 중복 관리되는 기존 구조이므로 두 파일을 항상 같이 수정한다.)

- [ ] **Step 6: 커밋**

```bash
git add build.gradle src/main/resources/application.yml compose.yml .github/workflows/backend-cicd.yml
git commit -m "feat: 백엔드에 Prometheus 메트릭 엔드포인트(8081) 노출"
```

---

### Task 2: 모니터링 EC2용 IAM 역할/인스턴스 프로파일 생성

**Files:** 없음 (AWS 리소스, CLI로만 생성)

**Interfaces:**
- Consumes: 없음
- Produces: IAM 인스턴스 프로파일 이름 `HDF-Monitoring-EC2-Role` — Task 5(EC2 생성)가 `--iam-instance-profile Name=HDF-Monitoring-EC2-Role`로 소비한다.

- [ ] **Step 1: EC2 신뢰 정책 파일 작성**

```bash
cat > /tmp/monitoring-ec2-trust-policy.json <<'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {"Service": "ec2.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
```

- [ ] **Step 2: IAM 역할 생성 및 SSM 정책 부착**

```bash
aws iam create-role \
  --role-name HDF-Monitoring-EC2-Role \
  --assume-role-policy-document file:///tmp/monitoring-ec2-trust-policy.json

aws iam attach-role-policy \
  --role-name HDF-Monitoring-EC2-Role \
  --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
```

- [ ] **Step 3: Prometheus EC2 Discovery용 최소권한 인라인 정책 부착**

```bash
aws iam put-role-policy \
  --role-name HDF-Monitoring-EC2-Role \
  --policy-name PrometheusEC2Discovery \
  --policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Action":"ec2:DescribeInstances","Resource":"*"}]}'
```

- [ ] **Step 4: 인스턴스 프로파일 생성 및 역할 연결**

```bash
aws iam create-instance-profile --instance-profile-name HDF-Monitoring-EC2-Role

aws iam add-role-to-instance-profile \
  --instance-profile-name HDF-Monitoring-EC2-Role \
  --role-name HDF-Monitoring-EC2-Role
```

- [ ] **Step 5: 확인**

Run: `aws iam get-instance-profile --instance-profile-name HDF-Monitoring-EC2-Role --query 'InstanceProfile.Roles[0].RoleName' --output text`
Expected: `HDF-Monitoring-EC2-Role`

IAM 인스턴스 프로파일은 생성 직후 EC2에서 바로 안 보일 수 있어(전파 지연) Task 5 실행 시 몇십 초 재시도가 필요할 수 있다.

---

### Task 3: 보안그룹 설정

**Files:** 없음 (AWS 리소스, CLI로만 생성)

**Interfaces:**
- Consumes: 베스천 SG `sg-0b29b7c4994502e36`, 백엔드 SG `sg-08ef85081dd234e5f`, VPC `vpc-0bbf264ac01aeffde`
- Produces: 모니터링 SG ID(`$MON_SG`) — Task 5(EC2 생성)와 Task 4의 문서화가 이 값을 사용한다.

- [ ] **Step 1: 모니터링 SG 생성**

```bash
MON_SG=$(aws ec2 create-security-group \
  --group-name HDF-monitoring-sg \
  --description "Prometheus+Grafana monitoring host" \
  --vpc-id vpc-0bbf264ac01aeffde \
  --query 'GroupId' --output text)
echo "MON_SG=$MON_SG"
```

- [ ] **Step 2: 베스천에서 Grafana(3000)로만 인바운드 허용**

```bash
aws ec2 authorize-security-group-ingress \
  --group-id "$MON_SG" \
  --protocol tcp --port 3000 \
  --source-group sg-0b29b7c4994502e36
```

- [ ] **Step 3: 백엔드 SG에 모니터링 SG로부터 8081 인바운드 허용 추가**

```bash
aws ec2 authorize-security-group-ingress \
  --group-id sg-08ef85081dd234e5f \
  --protocol tcp --port 8081 \
  --source-group "$MON_SG"
```

- [ ] **Step 4: 확인**

```bash
aws ec2 describe-security-groups --group-ids "$MON_SG" sg-08ef85081dd234e5f \
  --query 'SecurityGroups[].{Name:GroupName,Ingress:IpPermissions}'
```

Expected: `HDF-monitoring-sg`에 3000/tcp from bastion SG, `HDF-backend-sg`에 8081/tcp from `$MON_SG` 규칙이 보여야 한다.

`$MON_SG` 값을 다음 태스크들에서 계속 쓰므로 셸에 남겨두거나 메모해둔다.

---

### Task 4: 모니터링 EC2 user-data 스크립트 작성 (Prometheus + Grafana)

**Files:**
- Create: `monitoring/user-data.sh`

**Interfaces:**
- Consumes: Task 1에서 만든 `/actuator/prometheus`(8081), ASG 이름 `HDF-asg-backend`
- Produces: 모니터링 EC2 부팅 시 `/opt/monitoring/`에 Prometheus(9090)+Grafana(3000)를 띄우는 부트스트랩 스크립트 — Task 5가 `--user-data file://monitoring/user-data.sh`로 소비한다.

- [ ] **Step 1: monitoring/ 디렉터리와 user-data.sh 작성**

```bash
cd /Users/an/Downloads/Ditto-back/Ditto-backend/Ditto-BackEnd
mkdir -p monitoring
cat > monitoring/user-data.sh <<'USERDATA_EOF'
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
USERDATA_EOF

chmod +x monitoring/user-data.sh
```

- [ ] **Step 2: 문법 확인 (bash -n)**

Run: `bash -n monitoring/user-data.sh`
Expected: 아무 출력 없이 종료(문법 오류 없음).

- [ ] **Step 3: 커밋**

```bash
git add monitoring/user-data.sh
git commit -m "feat: 모니터링 EC2 부트스트랩 스크립트(Prometheus+Grafana) 추가"
```

---

### Task 5: 모니터링 EC2 인스턴스 생성

**Files:** 없음 (AWS 리소스, CLI로만 생성)

**Interfaces:**
- Consumes: Task 2의 `HDF-Monitoring-EC2-Role`, Task 3의 `$MON_SG`, Task 4의 `monitoring/user-data.sh`
- Produces: 모니터링 EC2 인스턴스 ID와 프라이빗 IP — Task 6(터널 스크립트)이 프라이빗 IP를 소비한다.

- [ ] **Step 1: 인스턴스 생성**

```bash
cd /Users/an/Downloads/Ditto-back/Ditto-backend/Ditto-BackEnd

MON_INSTANCE_ID=$(aws ec2 run-instances \
  --image-id ami-0e4ab31f1847c850c \
  --instance-type t3.micro \
  --subnet-id subnet-04d927c95391bacba \
  --security-group-ids "$MON_SG" \
  --iam-instance-profile Name=HDF-Monitoring-EC2-Role \
  --user-data file://monitoring/user-data.sh \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=HDF-monitoring-host}]' \
  --count 1 \
  --query 'Instances[0].InstanceId' --output text)
echo "MON_INSTANCE_ID=$MON_INSTANCE_ID"
```

IAM 인스턴스 프로파일 전파 지연으로 `InvalidParameterValue` 오류가 나면 10~20초 후 재시도한다.

- [ ] **Step 2: 실행 상태 대기**

```bash
aws ec2 wait instance-running --instance-ids "$MON_INSTANCE_ID"
```

- [ ] **Step 3: 프라이빗 IP 확인**

```bash
MON_PRIVATE_IP=$(aws ec2 describe-instances \
  --instance-ids "$MON_INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].PrivateIpAddress' --output text)
echo "MON_PRIVATE_IP=$MON_PRIVATE_IP"
```

- [ ] **Step 4: SSM 관리 대상 등록 확인**

```bash
aws ssm describe-instance-information \
  --filters "Key=InstanceIds,Values=$MON_INSTANCE_ID" \
  --query 'InstanceInformationList[0].PingStatus' --output text
```

Expected: `Online` (부팅 직후라면 1~2분 걸릴 수 있음, 안 뜨면 재조회).

---

### Task 6: 베스천 경유 Grafana 터널 스크립트 작성

**Files:**
- Create: `scripts/start-grafana-tunnel.sh`

**Interfaces:**
- Consumes: Task 5의 `$MON_PRIVATE_IP`, 베스천 인스턴스 ID `i-059f356b8fbcb152c`
- Produces: 로컬 `localhost:3000` → 모니터링 EC2 Grafana로의 SSM 터널

- [ ] **Step 1: 기존 `scripts/start-rds-tunnel.sh` 패턴을 그대로 따라 작성**

`$MON_PRIVATE_IP`를 Task 5에서 확인한 실제 프라이빗 IP로 치환해서 작성한다:

```bash
cd /Users/an/Downloads/Ditto-back/Ditto-backend/Ditto-BackEnd
cat > scripts/start-grafana-tunnel.sh <<EOF
#!/bin/bash
# AWS SSM Grafana Port Forwarding Tunnel Script

PROFILE="HDF-ko"
REGION="ap-northeast-2"
TARGET_INSTANCE="i-059f356b8fbcb152c"
GRAFANA_HOST="${MON_PRIVATE_IP}"
REMOTE_PORT="3000"
LOCAL_PORT="3000"

echo "=========================================================="
echo " Starting AWS SSM Port Forwarding Tunnel to Grafana..."
echo " Local Port     : \${LOCAL_PORT}"
echo " Remote Grafana : \${GRAFANA_HOST}:\${REMOTE_PORT}"
echo " EC2 Bastion    : \${TARGET_INSTANCE}"
echo "=========================================================="
echo " (Press Ctrl+C to stop the tunnel)"
echo ""

aws ssm start-session \\
  --profile "\${PROFILE}" \\
  --region "\${REGION}" \\
  --target "\${TARGET_INSTANCE}" \\
  --document-name AWS-StartPortForwardingSessionToRemoteHost \\
  --parameters "{\\"host\\":[\\"\${GRAFANA_HOST}\\"],\\"portNumber\\":[\\"\${REMOTE_PORT}\\"],\\"localPortNumber\\":[\\"\${LOCAL_PORT}\\"]}"
EOF

chmod +x scripts/start-grafana-tunnel.sh
```

- [ ] **Step 2: 생성된 스크립트에 실제 IP가 박혔는지 확인**

Run: `grep GRAFANA_HOST= scripts/start-grafana-tunnel.sh`
Expected: `GRAFANA_HOST="<Task 5에서 확인한 실제 프라이빗 IP>"` (변수가 아니라 실제 IP 문자열이어야 함)

- [ ] **Step 3: 커밋**

```bash
git add scripts/start-grafana-tunnel.sh
git commit -m "feat: 베스천 경유 Grafana SSM 터널 스크립트 추가"
```

---

### Task 7: 엔드투엔드 검증

**Files:** 없음 (검증만)

**Interfaces:**
- Consumes: Task 1(배포된 8081 엔드포인트), Task 5/6(모니터링 EC2 + 터널)

이 태스크는 **`dev` 브랜치로 머지해서 실제 운영 ASG를 재배포**하는 단계를 포함한다 — 사용자 확인 없이 진행하지 않는다.

- [ ] **Step 1: Task 1 변경분을 dev로 반영 (사용자 확인 후 진행)**

기존 컨벤션(레포의 PR 워크플로)대로 PR을 만들어 `dev`에 머지한다. 머지되면 `.github/workflows/backend-cicd.yml`이 자동으로 새 이미지를 빌드하고 ASG Instance Refresh를 수행한다.

- [ ] **Step 2: Instance Refresh 완료 대기**

```bash
aws autoscaling describe-instance-refreshes \
  --auto-scaling-group-name HDF-asg-backend \
  --query 'InstanceRefreshes[0].Status' --output text
```

Expected: 최종적으로 `Successful`.

- [ ] **Step 3: Prometheus가 새 인스턴스를 스크레이프 대상으로 잡았는지 확인**

새 터미널에서 Prometheus(9090)도 같은 방식으로 임시 터널링:

```bash
aws ssm start-session \
  --profile HDF-ko --region ap-northeast-2 \
  --target i-059f356b8fbcb152c \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{\"host\":[\"$MON_PRIVATE_IP\"],\"portNumber\":[\"9090\"],\"localPortNumber\":[\"9090\"]}"
```

다른 터미널에서:

```bash
curl -s http://localhost:9090/api/v1/targets | grep -o '"health":"[a-z]*"'
```

Expected: 적어도 하나의 `"health":"up"` — 즉 백엔드 인스턴스가 스크레이프되고 있음.

- [ ] **Step 4: Grafana에서 데이터소스 확인**

```bash
./scripts/start-grafana-tunnel.sh
```

브라우저에서 `http://localhost:3000` 접속 → 기본 계정(admin/admin, 최초 로그인 시 변경 요구됨)으로 로그인 → Connections → Data sources에서 `Prometheus`가 이미 연결돼 있는지 확인 → Explore에서 `http_server_requests_seconds_count` 쿼리로 백엔드 트래픽 메트릭이 찍히는지 확인.

Expected: 쿼리 결과에 값이 나오고, 배포 후 요청을 몇 번 보내면(예: 헬스체크 엔드포인트 호출) 그래프가 움직인다.
