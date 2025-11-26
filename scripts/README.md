# 데이터베이스 초기화 스크립트

## 파일 설명

### 1. `init-test-data.sql` ⭐
**우리 서버(wooriLearn) 테스트 데이터 초기화 스크립트**

이 파일을 사용하여 개발/테스트 환경의 DB를 초기화합니다.

### 2. `bank-server-init.sql`
**은행 서버(wooribank) DB 스크립트 (참고용)**

⚠️ **이 파일은 수정하지 마세요!** 은행 서버 개발팀에서 관리합니다.

---

## 사용 방법

### 로컬 MySQL에서 실행
```bash
# wooriLearn DB 초기화
mysql -u root -p < init-test-data.sql

# 또는 MySQL Workbench에서 파일 열어서 실행
```

### Docker MySQL에서 실행
```bash
docker exec -i mysql-container mysql -uroot -proot < init-test-data.sql
```

---

## 테스트 계정 정보

### 사용자 계정

| 구분 | 아이디 | 비밀번호 | 역할 | user_id (PK) |
|------|--------|---------|------|--------------|
| **메인 테스트 계정** | `testuser` | `test1234` | USER | 1 |
| 관리자 계정 | `admin` | `test1234` | ADMIN | 2 |
| 서브 테스트 계정 | `testuser2` | `test1234` | USER | 3 |

### testuser의 계좌 정보

| 계좌 ID | 계좌번호 | 계좌명 | 잔액 | 계좌 비밀번호 |
|---------|----------|--------|------|---------------|
| 1 | 1002-555-123456 | 테스트입출금통장 | 3,000,000원 | `1234` |
| 2 | 1002-666-789012 | 테스트저축통장 | 1,500,000원 | `1234` |

---

## 중요 특이사항 ⚠️

### 1. 자동이체 데이터
**testuser(user_id=1)의 계좌에만 자동이체가 등록되어 있습니다.**

#### 계좌 1번 (입출금통장)의 자동이체
| ID | 상태 | 받는 분 | 용도 | 금액 | 이체일 |
|----|------|---------|------|------|--------|
| 1 | ACTIVE | 김집주인 | 월세납부 | 600,000원 | 매월 1일 |
| 2 | ACTIVE | KT통신 | 인터넷요금 | 30,000원 | 매월 10일 |
| 3 | ACTIVE | SKT | 휴대폰요금 | 50,000원 | 매월 5일 |
| 6 | CANCELLED | 넷플릭스코리아 | 스트리밍구독 | 17,000원 | (해지됨) |

#### 계좌 2번 (저축통장)의 자동이체
| ID | 상태 | 받는 분 | 용도 | 금액 | 이체일 |
|----|------|---------|------|------|--------|
| 4 | ACTIVE | 우리은행 | 정기적금 | 1,000,000원 | 매월 25일 |
| 5 | ACTIVE | 우리카드 | 카드대금 | 500,000원 | 매월 15일 |

**✅ 총 5건의 활성 자동이체, 1건의 해지된 자동이체**

### 2. 거래내역 데이터
**testuser(user_id=1)의 계좌에만 거래내역이 있습니다.**

- 계좌 1번: 5건
- 계좌 2번: 3건

### 3. 포인트 내역
**testuser(user_id=1)만 포인트 내역이 있습니다.**

- 총 포인트: 10,000점
- 적립 내역: 3건 (2,000 + 3,000 + 5,000)
- 타입: `DEPOSIT` (포인트 적립), `WITHDRAW` (포인트 출금)
- 상태: `APPLY` (신청중), `SUCCESS` (성공), `FAILED` (실패)

### 4. 시나리오 & 퀴즈
- 시나리오 3개 생성됨
- 퀴즈 3개 생성됨
- 시나리오 스텝은 시나리오 1, 3번만 생성되어 있음
- **특이사항**: `scenario_step`의 `next_step`은 자기 참조 외래키이므로, INSERT 후 UPDATE로 설정됨

---

## 테스트 시나리오

### ✅ 자동이체 목록 조회 테스트

```bash
# 1. testuser로 로그인
POST /auth/login
{
  "userId": "testuser",
  "password": "test1234"
}

# 2. 계좌 1번의 활성 자동이체 조회 (3건 나와야 함)
GET /education/auto-payment/list?educationalAccountId=1&status=ACTIVE

# 3. 계좌 1번의 전체 자동이체 조회 (4건 나와야 함 - CANCELLED 포함)
GET /education/auto-payment/list?educationalAccountId=1&status=ALL

# 4. 계좌 2번의 활성 자동이체 조회 (2건 나와야 함)
GET /education/auto-payment/list?educationalAccountId=2&status=ACTIVE
```

### ✅ 자동이체 등록 테스트

```bash
POST /education/auto-payment
{
  "educationalAccountId": 1,
  "depositNumber": "1002-000-111222",
  "depositBankCode": "020",
  "amount": 100000,
  "counterpartyName": "테스트상점",
  "displayName": "테스트자동이체",
  "transferCycle": 1,
  "designatedDate": 20,
  "startDate": "2024-12-01",
  "expirationDate": "2025-12-31",
  "accountPassword": "1234"
}
```

### ✅ 캐시 테스트

```bash
# 1. 첫 조회 (캐시 미스 - DB 조회)
GET /education/auto-payment/list?educationalAccountId=1&status=ACTIVE

# 2. 동일 조회 (캐시 히트 - Redis에서 조회)
GET /education/auto-payment/list?educationalAccountId=1&status=ACTIVE

# 3. 대소문자 다르게 조회 (캐시 히트 - 정규화됨)
GET /education/auto-payment/list?educationalAccountId=1&status=active
```

---

## 데이터 확인 쿼리

### 사용자 확인
```sql
SELECT u.id, u.user_id, u.nickname, u.points
FROM users u;
```

### testuser 계좌 확인
```sql
SELECT ea.id, ea.account_number, ea.account_name, FORMAT(ea.balance, 0) AS balance
FROM educational_account ea
WHERE ea.user_id = 1;
```

### testuser 자동이체 확인
```sql
SELECT
    ap.id,
    ea.account_number,
    ap.counterparty_name,
    ap.display_name,
    FORMAT(ap.amount, 0) AS amount,
    ap.designated_date,
    ap.processing_status
FROM auto_payment ap
JOIN educational_account ea ON ap.educational_account_id = ea.id
WHERE ea.user_id = 1
ORDER BY ap.processing_status DESC, ap.designated_date;
```

---

## 주의사항

1. **스크립트 실행 순서가 중요합니다.** 외래키 제약조건 때문에 순서를 바꾸면 에러가 발생할 수 있습니다.

2. **기존 데이터는 모두 삭제됩니다.** `TRUNCATE` 명령으로 모든 테이블 데이터가 삭제되므로 운영 DB에서는 절대 실행하지 마세요!

3. **testuser의 ID는 항상 1입니다.** 이 ID를 기준으로 테스트 코드가 작성되어 있으므로 변경하지 마세요.

4. **비밀번호 해시값을 직접 수정하지 마세요.** BCrypt 해시는 온라인 도구나 테스트 유틸리티를 사용해서 생성하세요.

---

## 문제 해결

### Q: "외래키 제약조건 에러"가 발생합니다.
A: `SET FOREIGN_KEY_CHECKS = 0;`을 먼저 실행하고 다시 시도하세요.

### Q: 자동이체가 조회되지 않습니다.
A: testuser(user_id=1)의 계좌(ID: 1 또는 2)로 조회하고 있는지 확인하세요. 다른 사용자는 자동이체 데이터가 없습니다.

### Q: 로그인이 안 됩니다.
A: 비밀번호가 `test1234`인지 확인하세요. 계좌 비밀번호(`1234`)와 다릅니다!

---

## 작성자
- 작성일: 2024-11-26
- 마지막 수정: 2024-11-26
