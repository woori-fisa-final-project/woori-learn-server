-- ============================================
-- WooriLearn 테스트 데이터 초기화 스크립트
-- ============================================
-- 작성일: 2024-11-26
--
-- [중요 특이사항]
-- 1. testuser의 ID는 1번으로 고정 (auth_user_id=1, user_id=1)
-- 2. testuser의 교육용 계좌는 2개 (ID: 1, 2)
-- 3. 자동이체는 user_id=1의 계좌에만 등록되어 있음
-- 4. 비밀번호:
--    - 로그인 비밀번호: test1234
--    - 계좌 비밀번호: 1234
-- ============================================

USE wooriLearn;

-- ============================================
-- 0. 기존 데이터 전체 삭제 (외래키 순서 고려)
-- ============================================
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE scenario_progress;
TRUNCATE TABLE scenario_completed;
TRUNCATE TABLE auto_payment;
TRUNCATE TABLE transaction_history;
TRUNCATE TABLE points_history;
TRUNCATE TABLE educational_account;
TRUNCATE TABLE account;
TRUNCATE TABLE account_auth;
TRUNCATE TABLE refresh_token;
TRUNCATE TABLE users;
TRUNCATE TABLE auth_users;
TRUNCATE TABLE scenario_step;
TRUNCATE TABLE scenario;
TRUNCATE TABLE quiz;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 1. 인증 사용자 (auth_users)
-- ============================================
-- 비밀번호: test1234
-- BCrypt Hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO auth_users (user_id, password, role, created_at, updated_at)
VALUES
('testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_USER', NOW(6), NOW(6)),
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_ADMIN', NOW(6), NOW(6)),
('testuser2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_USER', NOW(6), NOW(6));

-- ============================================
-- 2. 사용자 정보 (users)
-- ============================================
-- testuser는 ID 1로 고정
INSERT INTO users (auth_user_id, user_id, nickname, points, created_at, updated_at)
VALUES
(1, 'testuser', '테스트유저', 10000, NOW(6), NOW(6)),
(2, 'admin', '관리자', 100000, NOW(6), NOW(6)),
(3, 'testuser2', '테스트유저2', 5000, NOW(6), NOW(6));

-- ============================================
-- 3. 교육용 계좌 (educational_account)
-- ============================================
-- 비밀번호: 1234
-- BCrypt Hash: $2a$10$iQtRSBwa7wqLD6w8ODusOO7O4GXSnkHY/opzHcBJAHoSJBCxWUC0O
--
-- [중요] testuser(ID=1)만 계좌가 있음 (ID: 1, 2)
INSERT INTO educational_account (user_id, account_number, balance, account_password, account_name, account_type)
VALUES
(1, '1002-555-123456', 3000000, '$2a$10$iQtRSBwa7wqLD6w8ODusOO7O4GXSnkHY/opzHcBJAHoSJBCxWUC0O', '테스트입출금통장', 'CHECKING'),
(1, '1002-666-789012', 1500000, '$2a$10$iQtRSBwa7wqLD6w8ODusOO7O4GXSnkHY/opzHcBJAHoSJBCxWUC0O', '테스트저축통장', 'SAVINGS');

-- ============================================
-- 4. 자동이체 (auto_payment)
-- ============================================
-- [중요] testuser(user_id=1)의 계좌에만 자동이체가 등록되어 있음
--
-- 계좌 1번 (1002-555-123456):
--   - ACTIVE: 월세, 인터넷, 휴대폰 (3건)
--   - CANCELLED: 넷플릭스 (1건)
--
-- 계좌 2번 (1002-666-789012):
--   - ACTIVE: 적금, 카드대금 (2건)
INSERT INTO auto_payment (
    educational_account_id,
    deposit_number,
    deposit_bank_code,
    amount,
    counterparty_name,
    display_name,
    transfer_cycle,
    designated_date,
    start_date,
    expiration_date,
    processing_status
)
VALUES
-- 계좌 1번의 활성 자동이체
(1, '1002-999-888777', '020', 600000, '김집주인', '월세납부', 1, 1, '2024-01-01', '2025-12-31', 'ACTIVE'),
(1, '1002-111-222333', '020', 30000, 'KT통신', '인터넷요금', 1, 10, '2024-01-01', '2025-12-31', 'ACTIVE'),
(1, '1002-444-555666', '020', 50000, 'SKT', '휴대폰요금', 1, 5, '2024-01-01', '2025-12-31', 'ACTIVE'),

-- 계좌 2번의 활성 자동이체
(2, '1002-987-654321', '020', 1000000, '우리은행', '정기적금', 1, 25, '2024-01-01', '2025-12-31', 'ACTIVE'),
(2, '1002-123-456789', '020', 500000, '우리카드', '카드대금', 1, 15, '2024-01-01', '2025-12-31', 'ACTIVE'),

-- 계좌 1번의 해지된 자동이체
(1, '1002-321-654987', '020', 17000, '넷플릭스코리아', '스트리밍구독', 1, 20, '2024-01-01', '2024-10-31', 'CANCELLED');

-- ============================================
-- 5. 거래내역 (transaction_history)
-- ============================================
-- [중요] testuser(user_id=1)의 계좌에만 거래내역이 있음
INSERT INTO transaction_history (
    account_id,
    transaction_date,
    counterparty_name,
    display_name,
    amount,
    description
)
VALUES
-- 계좌 1번 (1002-555-123456) 거래내역
(1, '2024-11-01 09:00:00.000000', '김집주인', '월세 자동이체', -600000, '11월 월세'),
(1, '2024-11-05 10:00:00.000000', 'SKT', '휴대폰 자동이체', -50000, '11월 휴대폰요금'),
(1, '2024-11-10 10:00:00.000000', 'KT통신', '인터넷 자동이체', -30000, '11월 인터넷요금'),
(1, '2024-11-20 14:00:00.000000', '회사', '급여', 4000000, '11월 급여'),
(1, '2024-11-25 16:30:00.000000', '편의점', '출금', -15000, 'GS25'),

-- 계좌 2번 (1002-666-789012) 거래내역
(2, '2024-11-15 09:30:00.000000', '우리카드', '카드대금 자동이체', -500000, '11월 카드대금'),
(2, '2024-11-25 10:00:00.000000', '우리은행', '적금 자동이체', -1000000, '11월 적금'),
(2, '2024-11-28 15:00:00.000000', '친구', '입금', 100000, '밥값');

-- ============================================
-- 6. 포인트 내역 (points_history)
-- ============================================
-- [중요] testuser(user_id=1)만 포인트 내역이 있음
-- type: DEPOSIT (포인트 적립), WITHDRAW (포인트 출금)
-- status: APPLY (신청중), SUCCESS (성공), FAILED (실패)
INSERT INTO points_history (
    user_id,
    amount,
    type,
    status,
    processed_at,
    fail_reason,
    created_at,
    updated_at
)
VALUES
(1, 2000, 'DEPOSIT', 'SUCCESS', '2024-11-01 10:00:00', NULL, NOW(6), NOW(6)),
(1, 3000, 'DEPOSIT', 'SUCCESS', '2024-11-10 15:00:00', NULL, NOW(6), NOW(6)),
(1, 5000, 'DEPOSIT', 'SUCCESS', '2024-11-15 12:00:00', NULL, NOW(6), NOW(6));

-- ============================================
-- 7. 실제 은행 계좌 (account) - 은행 API 연동용
-- ============================================
-- [중요] 이 계좌는 교육용 계좌(educational_account)와 별개
-- 은행 서버와 연동되는 실제 계좌 정보
INSERT INTO account (user_id, account_number, bank_code, account_name, created_at, updated_at)
VALUES
(1, '1002-555-123456', '020', '우리은행 테스트계좌', NOW(6), NOW(6));

-- ============================================
-- 8. 시나리오 (scenario)
-- ============================================
INSERT INTO scenario (title, total_normal_steps)
VALUES
('자동이체 설정하기', 5),
('계좌이체 연습', 4),
('보이스피싱 예방', 5);

-- ============================================
-- 9. 퀴즈 (quiz)
-- ============================================
INSERT INTO quiz (question, options, answer)
VALUES
('자동이체의 장점이 아닌 것은?', '["매번 직접 이체할 필요가 없다", "납부 기한을 놓칠 염려가 없다", "수수료가 항상 면제된다", "정해진 날짜에 자동으로 이체된다"]', 2),
('자동이체 설정 시 확인해야 할 사항으로 적절하지 않은 것은?', '["출금 계좌의 잔액", "이체 금액", "이체 주기", "받는 사람의 나이"]', 3),
('모르는 번호로 전화가 와서 검찰이라고 하며 계좌 비밀번호를 요구합니다. 어떻게 해야 할까요?', '["비밀번호를 알려준다", "전화를 끊고 해당 기관에 확인한다", "친구에게 물어본다", "그냥 끊는다"]', 1);

-- ============================================
-- 10. 시나리오 스텝 (scenario_step)
-- ============================================
-- [주의] next_step은 자기 참조 외래키이므로 2단계로 INSERT
-- 1단계: next_step을 NULL로 먼저 INSERT
-- 2단계: UPDATE로 next_step 설정

-- 1단계: 시나리오 1 - 자동이체 설정하기 (next_step = NULL)
INSERT INTO scenario_step (id, scenario_id, type, content, normal_index, quiz_id)
VALUES
(1, 1, 'DIALOG', '{"speaker": "나", "text": "매달 월세 납부하는 게 너무 번거로운데..."}', 1, NULL),
(2, 1, 'DIALOG', '{"speaker": "친구", "text": "자동이체 설정하면 편해! 한 번 설정해두면 자동으로 이체돼."}', 2, NULL),
(3, 1, 'CHOICE', '{"text": "자동이체의 장점은 무엇일까요?"}', 3, 1),
(4, 1, 'DIALOG', '{"speaker": "시스템", "text": "정답입니다! 이제 자동이체를 설정해보세요."}', 4, NULL),
(5, 1, 'DIALOG', '{"speaker": "나", "text": "오! 이제 매달 자동으로 납부되네. 편하다!"}', 5, NULL);

-- 1단계: 시나리오 3 - 보이스피싱 예방 (next_step = NULL)
INSERT INTO scenario_step (id, scenario_id, type, content, normal_index, quiz_id)
VALUES
(11, 3, 'DIALOG', '{"speaker": "나", "text": "어? 모르는 번호네.. 누구지?"}', 1, NULL),
(12, 3, 'DIALOG', '{"speaker": "전화", "text": "여보세요, 서울중앙지검입니다. 본인 계좌가 범죄에 연루되었습니다."}', 2, NULL),
(13, 3, 'CHOICE', '{"text": "어떻게 대처하시겠습니까?"}', 3, 3),
(14, 3, 'DIALOG', '{"speaker": "나", "text": "잠시만요, 제가 직접 확인해보겠습니다. (뚝)"}', 4, NULL),
(15, 3, 'DIALOG', '{"speaker": "시스템", "text": "잘하셨습니다! 의심스러운 전화는 일단 끊고 확인하는 것이 중요합니다."}', 5, NULL);

-- 2단계: next_step 설정 (UPDATE)
UPDATE scenario_step SET next_step = 2 WHERE id = 1;
UPDATE scenario_step SET next_step = 3 WHERE id = 2;
UPDATE scenario_step SET next_step = 4 WHERE id = 3;
UPDATE scenario_step SET next_step = 5 WHERE id = 4;
-- id = 5는 마지막 스텝이므로 next_step = NULL

UPDATE scenario_step SET next_step = 12 WHERE id = 11;
UPDATE scenario_step SET next_step = 13 WHERE id = 12;
UPDATE scenario_step SET next_step = 14 WHERE id = 13;
UPDATE scenario_step SET next_step = 15 WHERE id = 14;
-- id = 15는 마지막 스텝이므로 next_step = NULL

-- ============================================
-- 확인 쿼리
-- ============================================
SELECT '=== 사용자 목록 ===' AS '';
SELECT u.id, u.user_id, u.nickname, u.points
FROM users u;

SELECT '=== testuser 교육용 계좌 ===' AS '';
SELECT ea.id, ea.account_number, ea.account_name, FORMAT(ea.balance, 0) AS balance
FROM educational_account ea
WHERE ea.user_id = 1;

SELECT '=== testuser 자동이체 목록 ===' AS '';
SELECT
    ap.id,
    ea.account_number AS from_account,
    ea.account_name,
    ap.counterparty_name,
    ap.display_name,
    FORMAT(ap.amount, 0) AS amount,
    ap.designated_date AS day,
    ap.processing_status AS status
FROM auto_payment ap
JOIN educational_account ea ON ap.educational_account_id = ea.id
WHERE ea.user_id = 1
ORDER BY ap.processing_status DESC, ap.designated_date;

SELECT '=== testuser 거래내역 (최근 5건) ===' AS '';
SELECT
    th.transaction_date,
    ea.account_number,
    th.counterparty_name,
    FORMAT(th.amount, 0) AS amount,
    th.description
FROM transaction_history th
JOIN educational_account ea ON th.account_id = ea.id
WHERE ea.user_id = 1
ORDER BY th.transaction_date DESC
LIMIT 5;

SELECT '=== 시나리오 목록 ===' AS '';
SELECT id, title, total_normal_steps
FROM scenario;

SELECT '' AS '';
SELECT '✅ DB 초기화 완료!' AS message;
SELECT '📌 testuser (ID: 1) 로그인 정보' AS '';
SELECT '   - 아이디: testuser' AS '';
SELECT '   - 비밀번호: test1234' AS '';
SELECT '   - 계좌 비밀번호: 1234' AS '';
SELECT '   - 교육용 계좌: 2개 (ID: 1, 2)' AS '';
SELECT '   - 자동이체: 5건 (ACTIVE), 1건 (CANCELLED)' AS '';
