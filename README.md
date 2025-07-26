
# 📘 설계 및 보안 아키텍처 기술서

---

## A. 시스템 아키텍처

### ✅ 기술 스택

| 구성요소         | 선택 기술                  | 선택 이유                                                        |
|------------------|----------------------------|------------------------------------------------------------------|
| 언어             | Java 17+                  | 안정성과 유지보수성이 뛰어나고 대규모 서비스에 적합             |
| 프레임워크       | Spring Boot               | REST API 개발, IoC/DI, JPA 등 구조화된 설계 가능                 |
| 데이터베이스     | PostgreSQL                | 트랜잭션 처리에 강하고 정형 데이터 관리에 적합                   |
| 빌드 도구        | Gradle or Maven           | 의존성 관리 용이                                                 |
| API 테스트 도구  | Postman, Swagger          | REST API 개발 및 테스트 편의                                     |
| 배포 환경        | Docker + AWS/GCP/Azure    | 확장성과 클라우드 호환성 고려                                   |

---

### ✅ DB 스키마 설계

#### 📄 ERD 구조

- **Company**
  - `company_id` (PK)
  - `company_name`

- **Category**
  - `category_id` (PK)
  - `company_id` (FK)
  - `category_name`

- **Transaction**
  - `transaction_id` (PK)
  - `company_id` (FK)
  - `transaction_date`
  - `transaction_description`
  - `deposit_amount`
  - `withdraw_amount`
  - `balance_amount`
  - `transaction_place`
  - `category_id` (FK)
  - `category_name`
  - `classified` (boolean)

#### 📜 CREATE TABLE 예시 (PostgreSQL)

```sql
CREATE TABLE companies (
                         company_id VARCHAR PRIMARY KEY,
                         company_name VARCHAR NOT NULL
);

CREATE TABLE categories (
                          category_id VARCHAR PRIMARY KEY,
                          company_id VARCHAR REFERENCES companies(company_id),
                          category_name VARCHAR NOT NULL
);

CREATE TABLE categorized_transactions (
                                        id SERIAL PRIMARY KEY,
                                        company_id VARCHAR REFERENCES companies(company_id),
                                        transaction_date TIMESTAMP,
                                        transaction_description TEXT,
                                        deposit_amount BIGINT,
                                        withdraw_amount BIGINT,
                                        balance_amount BIGINT,
                                        transaction_place TEXT,
                                        category_id VARCHAR,
                                        category_name VARCHAR,
                                        classified BOOLEAN
);
```

---

## B. 핵심 자동 분류 로직

### 🔍 기본 동작 방식

1. 클라이언트가 `rules.json`과 `bank_transactions.csv` 파일 업로드
2. 서버에서 `rules.json`을 파싱하여 기업별 카테고리 규칙(CompanyRule) 목록 생성
3. CSV 내 각 거래건의 `적요` 필드를 기준으로 키워드 검색
4. 첫 번째로 매칭되는 카테고리에 해당 거래를 자동 분류
5. 일치하는 규칙이 없으면 `uncategorized`, 미분류로 지정

### 📌 규칙 예시 (`rules.json`)

```json
{
  "companies": [
    {
      "company_id": "com_1",
      "company_name": "A 커머스",
      "categories": [
        {
          "category_id": "cat_101",
          "category_name": "매출",
          "keywords": ["네이버페이", "쿠팡"]
        }
      ]
    }
  ]
}
```

---

### 🚀 확장 아이디어

| 기능 요구         | 개선 아이디어                                                                 |
|------------------|------------------------------------------------------------------------------|
| 금액 조건        | `minAmount`, `maxAmount` 필드를 CategoryRule에 추가하여 필터링 로직 확장         |
| 제외 키워드      | `excludeKeywords` 리스트 추가 후 contains() 체크 시 배제 로직 추가              |
| 복수 조건 AND    | 키워드 + 금액 조건을 모두 만족해야 매칭되도록 RuleEvaluator 도입               |
| 규칙 우선순위    | `priority` 필드 도입 후 정렬 및 적용                                           |
| 학습 기반 분류   | 규칙 기반 외에도 과거 패턴 학습 기반 분류 추가 (ML 모델 등)                   |

---

## C. 보안 강화 방안

### 🔐 공인인증서 및 민감정보 보안 전략

| 보안 항목         | 구현 방안                                                                 |
|------------------|--------------------------------------------------------------------------|
| 파일 저장         | 인증서 파일 자체는 저장하지 않음. 필요 시 메모리 내 일시 처리 후 삭제             |
| 암호화            | AES-256 기반 대칭키 암호화 (JCE, BouncyCastle 등 활용)                     |
| 비밀번호 저장     | 절대 저장 금지. 세션 기반 전달 후 즉시 파기                                  |
| 키 관리           | AWS KMS 또는 Vault 기반의 Key Management 시스템 활용                        |
| 전송 보안         | 모든 요청은 HTTPS 기반으로 통신                                             |
| 접근 제어         | 인증 기반 API 접근 제한 (OAuth2 / Spring Security)                         |
| 로그 마스킹       | 거래 내역, 계좌번호 등은 로그에 기록 금지 또는 마스킹 처리                    |

---

## D. 문제상황 해결책 제시

### ❗ 시나리오: 타 고객사의 거래 데이터 노출

#### 1. ✅ 즉시 대응 조치

- 관련 API 즉시 중지 (트래픽 차단 또는 Feature Flag 활용)
- 로그 및 DB 백업 확보 (사고 시점 기준)
- 고객사에 공식 사과 및 공지

#### 2. 🔍 원인 분석

- 거래 조회 API에서 인증 또는 권한 체크 누락 여부 확인
- `companyId` 값이 클라이언트에서 조작 가능한 구조였는지 검토
- DB join 또는 filtering 누락 여부 조사 (e.g., `WHERE company_id = ?` 누락)

#### 3. 🔒 재발 방지

- API 레벨에서 JWT 인증 도입 및 `companyId`를 토큰 기반 추출로 전환
- 사용자 권한 기반 데이터 필터링 (Spring Security + AOP 기반 필터링)
- 정기적 보안 점검 및 Code Review 강화
- 자동화 테스트에 "타 회사 데이터 접근 테스트" 추가

---

## 📦 3. 실행 및 테스트 가이드

### 🛠️ 프로젝트 실행 방법

```bash
# 1. 프로젝트 클론
git clone https://github.com/your-repo/your-project.git
cd your-project

# 2. 빌드 및 실행 (예: Gradle + Spring Boot)
./gradlew bootRun
```
### 🔍 API 테스트 방법 (curl 사용 예시)

#### 1. 거래 CSV + 규칙 JSON 업로드

```bash
curl -X POST http://localhost:8080/api/v1/accounting/process \
  -F "transactions=@path/to/transactions.csv" \
  -F "rules=@path/to/rules.json" 
```

#### 2. 특정 회사 거래 조회

```bash
curl "http://localhost:8080/api/v1/accounting/records?companyId=com_1"
```
#### 2-1. 가독성을 고려한 조회

```bash
curl.exe "http://localhost:8080/api/v1/accounting/records?companyId=com_1" -o output.json

Get-Content -Path output.json -Encoding UTF8 | ConvertFrom-Json | Format-List
```
---

## ⚙️ 개발 중 AI 제안과의 차이 및 수정 결정 사항

AI가 초기에 제안한 컨트롤러는 프로젝트의 실제 목적을 충분히 반영하지 못한 채, `companyId`를 클라이언트로부터 매개변수로 받아 처리하는 구조로 작성되었습니다. 하지만 해당 프로젝트는 인증된 사용자의 소속 기업 정보를 기반으로 요청을 처리해야 하며, 외부에서 임의의 `companyId`를 주입하는 것은 보안상 부적절한 방식이었습니다. 따라서 프롬프트를 재작성해 코드를 다시 생성받기보다는, 직접 컨트롤러와 서비스 로직을 수정하는 것이 더 효율적이라 판단하였습니다.

또한 거래 내역을 담은 파일 처리 로직에서, AI는 `InputStream`을 직접 활용하는 구조를 제안했으나, 이는 개발자 입장에서 가독성과 유지보수성이 떨어진다고 판단하였습니다. 이에 따라 거래 데이터를 명확하고 직관적인 도메인 객체로 파싱하여 처리하도록 구조를 변경하였으며, 이를 통해 로직의 이해도와 코드의 안정성을 높일 수 있었습니다.

이처럼 자동화된 제안이 항상 프로젝트의 맥락을 충분히 이해하지 못할 수 있으므로, 실제 비즈니스 요구와 보안 요구 사항에 맞게 개발자의 판단 아래 수정 및 보완이 이루어졌습니다.

---
