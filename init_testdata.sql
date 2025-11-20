USE wooriLearn;

-- 기존 데이터 삭제 (외래키 순서 반대로)
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
-- 1. testuser를 ID 1로 생성 (비밀번호: test1234)
-- ============================================
INSERT INTO auth_users (user_id, password, role, created_at, updated_at)
VALUES ('testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_USER', NOW(6), NOW(6));

INSERT INTO users (auth_user_id, user_id, nickname, points, created_at, updated_at)
VALUES (1, 'testuser', '테스트유저', 10000, NOW(6), NOW(6));

-- ============================================
-- 2. testuser의 교육용 계좌 (ID 1, 2) - 비밀번호: 1234
-- ============================================
-- 계좌 비밀번호: 1234 (BCrypt)
INSERT INTO educational_account (user_id, account_number, balance, account_password, account_name)
VALUES
(1, '1002-555-123456', 3000000, '$2a$10$iQtRSBwa7wqLD6w8ODusOO7O4GXSnkHY/opzHcBJAHoSJBCxWUC0O', '테스트입출금통장'),
(1, '1002-666-789012', 1500000, '$2a$10$iQtRSBwa7wqLD6w8ODusOO7O4GXSnkHY/opzHcBJAHoSJBCxWUC0O', '테스트저축통장');

-- ============================================
-- 3. testuser의 자동이체 설정
-- ============================================
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
-- 계좌 1번의 자동이체들
(1, '1002-999-888777', '020', 600000, '김집주인', '월세납부', 1, 1, '2024-01-01', '2025-12-31', 'ACTIVE'),
(1, '1002-111-222333', '020', 30000, 'KT통신', '인터넷요금', 1, 10, '2024-01-01', '2025-12-31', 'ACTIVE'),
(1, '1002-444-555666', '020', 50000, 'SKT', '휴대폰요금', 1, 5, '2024-01-01', '2025-12-31', 'ACTIVE'),

-- 계좌 2번의 자동이체들
(2, '1002-987-654321', '020', 1000000, '우리은행', '정기적금', 1, 25, '2024-01-01', '2025-12-31', 'ACTIVE'),
(2, '1002-123-456789', '020', 500000, '우리카드', '카드대금', 1, 15, '2024-01-01', '2025-12-31', 'ACTIVE'),

-- 해지된 자동이체
(1, '1002-123-456789', '020', 17000, '넷플릭스코리아', '스트리밍구독', 1, 20, '2024-01-01', '2024-10-31', 'CANCELLED');

-- ============================================
-- 4. testuser 거래내역
-- ============================================
INSERT INTO transaction_history (
    account_id,
    transaction_date,
    counterparty_name,
    display_name,
    amount,
    description
)
VALUES
-- 계좌 1번 거래내역
(1, '2024-11-01 09:00:00.000000', '김집주인', '월세 자동이체', -600000, '11월 월세'),
(1, '2024-11-05 10:00:00.000000', 'SKT', '휴대폰 자동이체', -50000, '11월 휴대폰요금'),
(1, '2024-11-10 10:00:00.000000', 'KT통신', '인터넷 자동이체', -30000, '11월 인터넷요금'),
(1, '2024-11-20 14:00:00.000000', '회사', '급여', 4000000, '11월 급여'),
(1, '2024-11-25 16:30:00.000000', '편의점', '출금', -15000, 'GS25'),

-- 계좌 2번 거래내역
(2, '2024-11-15 09:30:00.000000', '우리카드', '카드대금 자동이체', -500000, '11월 카드대금'),
(2, '2024-11-25 10:00:00.000000', '우리은행', '적금 자동이체', -1000000, '11월 적금'),
(2, '2024-11-28 15:00:00.000000', '친구', '입금', 100000, '밥값');

-- ============================================
-- 5. testuser 포인트 내역
-- ============================================
-- 주의: PointsHistoryType은 DEPOSIT, WITHDRAW만 존재 (EARN은 없음)
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
-- 6. 추가 테스트 사용자들 (선택사항)
-- ============================================
INSERT INTO auth_users (user_id, password, role, created_at, updated_at)
VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_ADMIN', NOW(6), NOW(6)),
('testuser2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_USER', NOW(6), NOW(6));

INSERT INTO users (auth_user_id, user_id, nickname, points, created_at, updated_at)
VALUES
(2, 'admin', '관리자', 100000, NOW(6), NOW(6)),
(3, 'testuser2', '테스트유저2', 5000, NOW(6), NOW(6));

-- ============================================
-- 7. 시나리오 & 퀴즈 데이터
-- ============================================
-- total_normal_steps: 일반 스텝의 총 개수 (실제 스텝 데이터 추가 시 업데이트 필요)
INSERT INTO scenario (title, total_normal_steps) VALUES
('자동이체 설정하기', 0),
('계좌이체 연습', 0),
('예금 상품 가입', 0);

INSERT INTO quiz (question, options, answer) VALUES
('자동이체의 장점이 아닌 것은?', '["매번 직접 이체할 필요가 없다", "납부 기한을 놓칠 염려가 없다", "수수료가 항상 면제된다", "정해진 날짜에 자동으로 이체된다"]', 2),
('자동이체 설정 시 확인해야 할 사항으로 적절하지 않은 것은?', '["출금 계좌의 잔액", "이체 금액", "이체 주기", "받는 사람의 나이"]', 3);

-- ============================================
-- 확인
-- ============================================
SELECT '=== 사용자 목록 ===' AS '';
SELECT u.id, u.user_id, u.nickname, u.points
FROM users u;

SELECT '=== testuser 계좌 ===' AS '';
SELECT ea.id, ea.account_number, ea.account_name, ea.balance
FROM educational_account ea
WHERE ea.user_id = 1;

SELECT '=== testuser 자동이체 ===' AS '';
SELECT ap.id, ea.account_number AS from_account,
       ap.counterparty_name, ap.display_name, ap.amount,
       ap.designated_date AS day, ap.processing_status
FROM auto_payment ap
JOIN educational_account ea ON ap.educational_account_id = ea.id
WHERE ea.user_id = 1;

SELECT '=== testuser 포인트 내역 ===' AS '';
SELECT ph.id, ph.amount, ph.type, ph.status, ph.processed_at
FROM points_history ph
WHERE ph.user_id = 1;

SELECT 'DB 초기화 완료! testuser는 ID 1, 계좌는 ID 1,2 입니다.' AS message;
SELECT '계좌 비밀번호: 1234' AS password_info;
SELECT 'testuser 로그인 비밀번호: test1234' AS login_info;
