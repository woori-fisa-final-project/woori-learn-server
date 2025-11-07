package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.AccountAuthReqDto;
import dev.woori.wooriLearn.domain.account.dto.AccountAuthVerifyReqDto;
import dev.woori.wooriLearn.domain.account.entity.AccountAuth;
import dev.woori.wooriLearn.domain.account.repository.AccountAuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 본인 인증(OTP) 발급/검증 비즈니스 로직을 담당하는 서비스.
 *
 * 동작 개요
 * - 발급(request):
 *   1) 외부 인증 서버에 {name, birthdate, phone}을 전달하여 6자리 OTP를 발급받는다.
 *   2) account_auth 테이블에 userId 기준으로 OTP를 저장(이미 있으면 덮어쓰기 = upsert).
 *   3) 응답 바디에는 OTP를 노출하지 않고 "SENT" 고정 메시지를 반환.
 *
 * - 검증(verify):
 *   1) userId로 저장된 OTP를 조회한다(없으면 발급 이력 없음 예외).
 *   2) 사용자 입력 code와 저장된 authCode가 일치하면 성공 처리 후 레코드 삭제.
 *   3) 일치 여부를 { verified: true/false }로 반환.
 */
@Service
@RequiredArgsConstructor
public class AccountAuthService {

    /** OTP를 저장/조회/삭제하는 리포지토리 */
    private final AccountAuthRepository accountAuthRepository;
    /** 외부 인증 서버 호출용 클라이언트 */
    private final ExternalAuthClient externalAuthClient;

    /**
     * 인증번호(OTP) 발급/재발급.
     *
     * 트랜잭션
     * - OTP 발급 후 DB upsert까지 원자적으로 처리.
     */
    @Transactional
    public void request(String userId, AccountAuthReqDto req) {
        // 1) 외부 인증 서버 호출
        String code = externalAuthClient.requestOtp(
                req.getName(), req.getBirthdate(), req.getPhoneNum()
        );

        // 2) upsert: userId가 있으면 authCode 갱신, 없으면 새 레코드 생성
        accountAuthRepository.findByUserId(userId)
                .ifPresentOrElse(
                        existing -> existing.updateAuthCode(code),
                        () -> accountAuthRepository.save(
                                AccountAuth.builder()
                                        .userId(userId)
                                        .authCode(code)
                                        .build()
                        )
                );
    }

    /**
     * 인증번호(OTP) 검증.
     *
     * 동작
     * - 저장된 OTP와 사용자가 입력한 code를 비교하여 일치 여부 판단.
     * - 일치 시 해당 userId 레코드를 삭제하여 재사용을 방지.
     */
    @Transactional
    public void verify(String userId, AccountAuthVerifyReqDto req) {
        // 1) 최근 발급 레코드 조회
        AccountAuth row = accountAuthRepository.findByUserId(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.INVALID_REQUEST, "인증요청 이력이 없습니다."));

        // 2) 코드 비교
        if (!constantTimeEquals(row.getAuthCode(), req.getCode())) {
            // 틀리면 예외 -> 전역 예외 핸들러에서 400 반환
            throw new CommonException(ErrorCode.INVALID_REQUEST, "인증번호가 일치하지 않습니다.");
        }

        // 3) 성공 시 레코드 삭제
            accountAuthRepository.deleteByUserId(userId);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(x, y);
    }
}