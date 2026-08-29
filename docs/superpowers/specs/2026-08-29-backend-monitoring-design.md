# 백엔드 트래픽 모니터링 (Prometheus + Grafana) 설계

## 배경

백엔드는 ASG(`HDF-asg-backend`) + GitHub Actions Instance Refresh 방식으로 배포되어, 배포마다 EC2 인스턴스가 새로 뜨고 이전 인스턴스는 사라진다. 이 환경에서 백엔드 서버의 트래픽(요청량, 지연시간, 에러, JVM 상태)을 실시간으로 확인할 수 있는 모니터링 스택이 없다.

## 목표

- 프라이빗 서브넷에 Prometheus + Grafana를 두고, ASG가 교체되어도 자동으로 새 인스턴스를 스크레이프 대상에 포함시킨다.
- 앱 트래픽(HTTP 요청수/지연시간/상태코드)과 JVM(메모리/GC/스레드) 메트릭을 본다. 서버 자원(CPU/메모리) 지표나 ALB 레벨 지표는 이번 범위에 넣지 않는다.
- 기존 베스천 호스트를 통해서만 Grafana 대시보드에 접근한다.
- 인프라는 콘솔/CLI로 수동 관리한다 (Terraform/CDK 등 IaC 도입 안 함).

## 현재 AWS 환경 (2026-08-29 기준 조사)

| 항목 | 값 |
|---|---|
| 계정 | 601202752151 |
| 리전 | ap-northeast-2 |
| VPC | `vpc-0bbf264ac01aeffde` (HDF-vpc, 10.0.0.0/16) |
| 프라이빗 서브넷 | `subnet-04d927c95391bacba` (2a), `subnet-02a84a1e4ecb48137` (2b) |
| 백엔드 ASG | `HDF-asg-backend` (Launch Template `HDF-lt-hdf-backend`) |
| 백엔드 SG | `sg-08ef85081dd234e5f` (HDF-backend-sg) — 현재 ALB SG(`sg-09c75878774fe6a9a`)로부터 8080만 인바운드 허용 |
| 베스천 인스턴스 | `i-059f356b8fbcb152c` (HDF-bastion-host), SG `sg-0b29b7c4994502e36` |
| AMI (백엔드와 동일 계열) | `ami-0e4ab31f1847c850c` (Ubuntu 24.04 noble) |

**중요한 발견:** 이 계정의 모든 EC2(베스천 포함)는 SSH 키페어 없이 **SSM Session Manager로만** 접근한다 (`AmazonSSMManagedInstanceCore` IAM 정책 부착). 레포에 이미 있는 `scripts/start-rds-tunnel.sh`가 베스천(`i-059f356b8fbcb152c`)을 거쳐 `AWS-StartPortForwardingSessionToRemoteHost` 문서로 RDS(1521)에 터널링하는 패턴이고, RDS/Redis 보안그룹도 베스천 SG를 소스로 허용하는 동일한 컨벤션을 쓴다. Grafana 접근도 이 패턴을 그대로 따른다.

## 아키텍처

```
GitHub Actions(dev push) → ECR push + ASG Instance Refresh
                                  │
                                  ▼
                    [ASG: 백엔드 EC2] (프라이빗 서브넷)
                       :8080 → 앱 트래픽 (ALB 그대로)
                       :8081 → /actuator/prometheus (신규, ALB 미노출)
                                  │  (VPC 내부, 모니터링 SG → 백엔드 SG)
                                  ▼
                    [모니터링 EC2 1대] (프라이빗 서브넷, docker compose)
                       ├─ Prometheus: ec2_sd_configs로 HDF-asg-backend 태그 기준
                       │              인스턴스를 매 스크레이프마다 재탐색
                       └─ Grafana: 로컬 Prometheus를 데이터소스로 사용
                                  ▲
                                  │  SSM 포트포워딩 (AWS-StartPortForwardingSessionToRemoteHost)
                            [베스천] ← scripts/start-grafana-tunnel.sh
```

## 구성 요소별 설계

### 1. 백엔드 계측

- `micrometer-registry-prometheus` 의존성 추가.
- `management.server.port: 8081`로 액추에이터를 앱 포트(8080)와 분리. 별도 내장 서버라 기존 `SecurityConfig`의 세션 인증 필터체인이 적용되지 않을 가능성이 높다 (구현 중 실측 필요). 인증 대신 **보안그룹으로만 접근을 제한**한다.
- `management.endpoints.web.exposure.include: health, info, prometheus`
- ALB 타겟그룹은 그대로 8080만 봐서 변경 없음.

### 2. Prometheus 서비스 디스커버리

- `ec2_sd_configs`로 `tag:aws:autoscaling:groupName = HDF-asg-backend` 필터, 포트 8081, `metrics_path: /actuator/prometheus`.
- 모니터링 EC2의 IAM 역할에 `ec2:DescribeInstances` 최소 권한만 부여 (자격증명 파일 불필요, 인스턴스 프로파일로 처리).

### 3. 네트워크 / 보안그룹

- 신규 SG `HDF-monitoring-sg`: 인바운드 3000(Grafana)을 베스천 SG(`sg-0b29b7c4994502e36`)로부터만 허용. 아웃바운드는 계정 내 다른 SG들과 동일하게 전체 허용(기존 컨벤션).
- `HDF-backend-sg`에 인바운드 8081을 `HDF-monitoring-sg`로부터 허용하는 규칙 추가.

### 4. Grafana 배포/접근

- 모니터링 EC2 한 대에 Prometheus + Grafana를 docker compose로 같이 띄운다 (백엔드와 동일한 운영 패턴).
- 접근: `scripts/start-grafana-tunnel.sh`(신규, `start-rds-tunnel.sh` 패턴 그대로) 실행 → `localhost:3000` 접속.

### 5. 운영 노트

- 모니터링 EC2는 ASG가 아닌 고정 단일 인스턴스라 EBS에 데이터가 유지된다. 별도 데이터 볼륨은 이번 범위에 넣지 않는다.
- Prometheus 리텐션 15일(기본값에 가까운 값)로 시작.
- Alertmanager/알림은 이번 범위 밖.
- ALB 레벨 지표(CloudWatch), 서버 자원(node_exporter)은 이번 범위 밖 — 필요해지면 별도 스펙으로 확장.

## 범위 밖

- Terraform/CDK 등 IaC 도입
- ALB/CloudWatch 지표 연동
- node_exporter(서버 자원 지표)
- Alertmanager
- Grafana 공개 접근(ALB/인증)
