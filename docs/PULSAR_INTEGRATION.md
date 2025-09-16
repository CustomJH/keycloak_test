# Keycloak-Pulsar JWT 인증 통합 가이드

Apache Pulsar와 Keycloak을 연동하여 JWT 토큰 기반 인증을 구현하는 완전한 가이드입니다.

## 🏗️ 아키텍처 개요

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Pulsar Client │───▶│    Keycloak      │───▶│  Pulsar Broker  │
│                 │    │ (JWT Token 발급) │    │ (JWT 토큰 검증) │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

**통합 구성요소:**
- **Keycloak**: JWT 토큰 발급 서버 (OIDC Provider)
- **Apache Pulsar**: JWT 토큰 기반 인증 지원
- **Client Applications**: 토큰 획득 후 Pulsar 접근

## ⚡ 빠른 시작

### 1. Keycloak 설정 초기화

```bash
# Pulsar 전용 realm, client, roles 생성
./scripts/setup-pulsar-auth.sh
```

### 2. JWT 토큰 생성 및 테스트

```bash
# 토큰 발급 및 검증 테스트
./scripts/test-pulsar-tokens.sh
```

## 🔑 인증 설정 상세

### Keycloak 구성

**생성된 리소스:**
- **Realm**: `pulsar-realm`
- **Client**: `pulsar-client` (Client Secret: `pulsar-client-secret`)
- **Test User**: `pulsar-user` / `pulsar123!`
- **Roles**: 
  - `pulsar-producer`: 메시지 발행 권한
  - `pulsar-consumer`: 메시지 구독 권한  
  - `pulsar-admin`: 관리자 권한

**주요 엔드포인트:**
- Admin Console: http://localhost:18200/admin
- OIDC Discovery: http://localhost:18200/realms/pulsar-realm/.well-known/openid_configuration
- Token Endpoint: http://localhost:18200/realms/pulsar-realm/protocol/openid-connect/token
- JWKS URI: http://localhost:18200/realms/pulsar-realm/protocol/openid-connect/certs

## 🚀 토큰 발급 방법

### 1. Service-to-Service 인증 (Client Credentials Flow)

자동화된 시스템이나 서비스 계정용:

```bash
curl -X POST "http://localhost:18200/realms/pulsar-realm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=pulsar-client" \
  -d "client_secret=pulsar-client-secret"
```

### 2. 사용자 인증 (Password Flow)

사용자별 클라이언트 애플리케이션용:

```bash
curl -X POST "http://localhost:18200/realms/pulsar-realm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=pulsar-client" \
  -d "client_secret=pulsar-client-secret" \
  -d "username=pulsar-user" \
  -d "password=pulsar123!"
```

## 🔧 Pulsar Broker 설정

### broker.conf 설정

```properties
# 인증 활성화
authenticationEnabled=true
authorizationEnabled=true

# JWT 인증 프로바이더 설정
authenticationProviders=org.apache.pulsar.broker.authentication.AuthenticationProviderToken

# 토큰 검증을 위한 공개키 설정 (둘 중 하나 선택)
# 옵션 1: 로컬 공개키 파일 사용
# tokenPublicKey=file:///opt/pulsar/keys/keycloak-public.key

# 옵션 2: JWKS URI 사용 (권장)
tokenPublicKeyUri=http://localhost:18200/realms/pulsar-realm/protocol/openid-connect/certs

# JWT 토큰 설정
tokenAuthClaim=sub
tokenAudience=account

# 슈퍼 유저 역할 설정
superUserRoles=pulsar-admin

# 네임스페이스 권한 설정 (선택사항)
authorizationProvider=org.apache.pulsar.broker.authorization.PulsarAuthorizationProvider
```

### 공개키 추출 (로컬 파일 방식 사용시)

```bash
# Keycloak에서 공개키 추출
curl -s "http://localhost:18200/realms/pulsar-realm/protocol/openid-connect/certs" | \
  jq -r '.keys[0] | "-----BEGIN PUBLIC KEY-----\n" + .x5c[0] + "\n-----END PUBLIC KEY-----"' > \
  /opt/pulsar/keys/keycloak-public.key
```

## 📱 Pulsar 클라이언트 사용법

### Java 클라이언트

```java
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.impl.auth.AuthenticationToken;

public class PulsarKeycloakClient {
    public static void main(String[] args) throws PulsarClientException {
        // JWT 토큰 (실제로는 Keycloak에서 획득)
        String jwtToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...";
        
        // Pulsar 클라이언트 생성
        PulsarClient client = PulsarClient.builder()
            .serviceUrl("pulsar://localhost:6650")
            .authentication(AuthenticationFactory.token(jwtToken))
            .build();
        
        // Producer 생성
        Producer<String> producer = client.newProducer(Schema.STRING)
            .topic("persistent://public/default/my-topic")
            .create();
        
        // 메시지 전송
        producer.send("Hello Pulsar with Keycloak!");
        
        // Consumer 생성
        Consumer<String> consumer = client.newConsumer(Schema.STRING)
            .topic("persistent://public/default/my-topic")
            .subscriptionName("my-subscription")
            .subscribe();
        
        // 메시지 수신
        Message<String> message = consumer.receive();
        System.out.println("Received: " + message.getValue());
        
        // 리소스 정리
        producer.close();
        consumer.close();
        client.close();
    }
}
```

### Python 클라이언트

```python
import pulsar
import requests
import json

def get_jwt_token():
    """Keycloak에서 JWT 토큰 획득"""
    token_url = "http://localhost:18200/realms/pulsar-realm/protocol/openid-connect/token"
    data = {
        "grant_type": "client_credentials",
        "client_id": "pulsar-client",
        "client_secret": "pulsar-client-secret"
    }
    
    response = requests.post(token_url, data=data)
    return response.json()["access_token"]

def main():
    # JWT 토큰 획득
    jwt_token = get_jwt_token()
    
    # Pulsar 클라이언트 생성
    client = pulsar.Client(
        'pulsar://localhost:6650',
        authentication=pulsar.AuthenticationToken(jwt_token)
    )
    
    # Producer 생성
    producer = client.create_producer('persistent://public/default/my-topic')
    
    # 메시지 전송
    producer.send(b'Hello Pulsar with Keycloak!')
    
    # Consumer 생성
    consumer = client.subscribe(
        'persistent://public/default/my-topic',
        subscription_name='my-subscription'
    )
    
    # 메시지 수신
    msg = consumer.receive()
    print(f"Received: {msg.data()}")
    consumer.acknowledge(msg)
    
    # 리소스 정리
    producer.close()
    consumer.close()
    client.close()

if __name__ == "__main__":
    main()
```

### CLI 도구 사용

```bash
# 토큰을 환경변수로 설정
export PULSAR_TOKEN=$(cat /tmp/pulsar-service-token.jwt)

# pulsar-admin 명령어
pulsar-admin --auth-plugin org.apache.pulsar.client.impl.auth.AuthenticationToken \
  --auth-params token:$PULSAR_TOKEN \
  topics list public/default

# pulsar-client 명령어
pulsar-client --auth-plugin org.apache.pulsar.client.impl.auth.AuthenticationToken \
  --auth-params token:$PULSAR_TOKEN \
  produce persistent://public/default/my-topic \
  --messages "Hello Keycloak!"
```

## 🔄 토큰 갱신 전략

### 1. 서비스 계정 토큰

```bash
# Client Credentials Flow로 새 토큰 획득
refresh_service_token() {
    curl -s -X POST "http://localhost:18200/realms/pulsar-realm/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "grant_type=client_credentials" \
      -d "client_id=pulsar-client" \
      -d "client_secret=pulsar-client-secret" | \
      jq -r '.access_token' > /tmp/pulsar-service-token.jwt
}
```

### 2. 사용자 토큰 (Refresh Token 사용)

```bash
# Refresh Token으로 새 Access Token 획득
refresh_user_token() {
    local refresh_token=$1
    curl -s -X POST "http://localhost:18200/realms/pulsar-realm/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "grant_type=refresh_token" \
      -d "client_id=pulsar-client" \
      -d "client_secret=pulsar-client-secret" \
      -d "refresh_token=$refresh_token"
}
```

## 🛡️ 보안 고려사항

### 1. 토큰 저장
- **운영환경**: 환경변수나 보안 볼트에 저장
- **개발환경**: 임시 파일 사용 시 권한 제한 (600)

### 2. 네트워크 보안
- **HTTPS 사용**: 운영환경에서는 반드시 HTTPS 활성화
- **방화벽 설정**: Keycloak과 Pulsar 간 통신 포트만 개방

### 3. 토큰 만료 관리
- **자동 갱신**: 토큰 만료 전 자동 갱신 로직 구현
- **에러 핸들링**: 인증 실패 시 재시도 로직

## 🔍 문제 해결

### 일반적인 오류

#### 1. "Authentication failed" 오류
```bash
# 토큰 유효성 확인
curl -X POST "http://localhost:18200/realms/pulsar-realm/protocol/openid-connect/token/introspect" \
  -u "pulsar-client:pulsar-client-secret" \
  -d "token=YOUR_TOKEN"
```

#### 2. "Authorization failed" 오류
```bash
# 사용자 역할 확인
# Keycloak Admin Console에서 사용자의 Role Mappings 확인
```

#### 3. 공개키 검증 실패
```bash
# JWKS URI 접근 확인
curl "http://localhost:18200/realms/pulsar-realm/protocol/openid-connect/certs"
```

### 로그 확인

```bash
# Pulsar broker 로그
tail -f /opt/pulsar/logs/pulsar-broker.log | grep -i auth

# Keycloak 로그  
docker logs keycloak -f | grep -i token
```

## 📊 성능 최적화

### 1. 토큰 캐싱
- 클라이언트에서 토큰을 메모리에 캐시
- 만료 시간 기반 자동 갱신

### 2. Connection Pooling
- Pulsar 클라이언트 인스턴스 재사용
- 연결 풀 설정으로 성능 향상

### 3. 배치 처리
- 여러 메시지를 배치로 처리
- 토큰 획득 횟수 최소화

## 🎯 실전 예제

### Spring Boot 통합

```java
@Configuration
public class PulsarConfig {
    
    @Value("${keycloak.token-uri}")
    private String tokenUri;
    
    @Value("${pulsar.service-url}")
    private String pulsarServiceUrl;
    
    @Bean
    public PulsarClient pulsarClient() throws PulsarClientException {
        String token = getJwtToken();
        
        return PulsarClient.builder()
            .serviceUrl(pulsarServiceUrl)
            .authentication(AuthenticationFactory.token(token))
            .build();
    }
    
    private String getJwtToken() {
        // Keycloak에서 토큰 획득 로직
        // RestTemplate 또는 WebClient 사용
    }
}
```

## 📚 추가 리소스

- [Apache Pulsar 보안 문서](https://pulsar.apache.org/docs/security-overview/)
- [Keycloak OIDC 문서](https://www.keycloak.org/docs/latest/server_admin/#_oidc)
- [JWT 토큰 디버깅 도구](https://jwt.io/)

---

## ✅ 검증 체크리스트

- [ ] Keycloak pulsar-realm 생성됨
- [ ] pulsar-client 생성 및 설정됨
- [ ] JWT 토큰 발급 성공
- [ ] 토큰 검증 성공
- [ ] Pulsar broker JWT 설정 완료
- [ ] 클라이언트 연결 테스트 완료
- [ ] 권한 기반 접근 제어 테스트
- [ ] 토큰 갱신 프로세스 구현

**설정 완료시 다음 명령어로 전체 테스트:**
```bash
./scripts/test-pulsar-tokens.sh
```