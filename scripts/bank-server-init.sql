-- ============================================
-- 은행 서버 DB 초기화 스크립트 (참고용)
-- ============================================
-- [주의] 이 파일은 은행 서버용이므로 수정하지 마세요!
-- 은행 서버 개발팀에서 관리합니다.
-- ============================================

CREATE DATABASE IF NOT EXISTS wooribank;

USE wooribank;

-- ============================================
-- 1. 클라이언트 앱 인증 테이블
-- ============================================
CREATE TABLE IF NOT EXISTS bank_client_app (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_key VARCHAR(255) NOT NULL UNIQUE COMMENT '클라이언트 앱 키',
    secret_key VARCHAR(255) NOT NULL COMMENT '비밀 키 (BCrypt 암호화)',
    name VARCHAR(100) NOT NULL COMMENT '클라이언트 앱 이름',
    status VARCHAR(20) NOT NULL COMMENT '상태 (ACTIVE/INACTIVE)',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='클라이언트 앱 인증';

-- 테스트용 클라이언트 앱
-- secretKey 평문: "test-secret-123"
INSERT INTO bank_client_app (app_key, secret_key, name, status, created_at, updated_at)
VALUES (
    'test-app-key-001',
    '$2a$12$JEYf6CrrCzaW2N7OAKXd4uw8IbSu8x1l/vOM9NOkM9Lc1NSt1ZT2O',
    'Postman Test Client',
    'ACTIVE',
    NOW(6),
    NOW(6)
);

-- ============================================
-- 2. 은행 사용자 테이블
-- ============================================
CREATE TABLE IF NOT EXISTS bank_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name_kr VARCHAR(30) NOT NULL COMMENT '한글 이름',
    name_en VARCHAR(50) COMMENT '영문 이름',
    email VARCHAR(50) NOT NULL UNIQUE COMMENT '이메일',
    phone_number VARCHAR(20) NOT NULL COMMENT '전화번호',
    birth DATE NOT NULL COMMENT '생년월일',
    auth_token VARCHAR(255) NOT NULL UNIQUE COMMENT '메인 서버 userId',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    INDEX idx_auth_token (auth_token),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='은행 사용자';

-- ============================================
-- 3. 은행 계좌 테이블
-- ============================================
CREATE TABLE IF NOT EXISTS bank_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    account_number VARCHAR(20) NOT NULL UNIQUE COMMENT '계좌번호',
    password VARCHAR(60) NOT NULL COMMENT '계좌 PIN (BCrypt 암호화)',
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '잔액',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    CONSTRAINT fk_bank_account_user FOREIGN KEY (user_id) REFERENCES bank_users(id) ON DELETE CASCADE,
    INDEX idx_account_number (account_number),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='은행 계좌';
