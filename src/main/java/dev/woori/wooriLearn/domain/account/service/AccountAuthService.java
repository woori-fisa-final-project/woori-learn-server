package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.domain.account.dto.AccountAuthDto;
import dev.woori.wooriLearn.domain.account.entity.AccountAuth;
import dev.woori.wooriLearn.domain.account.repository.AccountAuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public AccountAuthDto.Response request(String userId, AccountAuthDto.Request req) {
        // 1) 외부 인증 서버 호출
        String code = externalAuthClient.requestOtp(
                req.getName(), req.getBirthdate(), req.getPhoneNum()
        );

        // 2) upsert: userId가 있으면 authCode 갱신, 없으면 새 레코드 생성
        AccountAuth row = accountAuthRepository.findByUserId(userId)
                .map(existing -> { existing.setAuthCode(code); return existing; })
                .orElseGet(() -> AccountAuth.builder()
                        .userId(userId)
                        .authCode(code)
                        .build());

        // 3) 저장
        accountAuthRepository.save(row);

        return new AccountAuthDto.Response("SENT");
    }

    /**
     * 인증번호(OTP) 검증.
     *
     * 동작
     * - 저장된 OTP와 사용자가 입력한 code를 비교하여 일치 여부 판단.
     * - 일치 시 해당 userId 레코드를 삭제하여 재사용을 방지.
     */
    @Transactional
    public AccountAuthDto.VerifyResponse verify(String userId, AccountAuthDto.VerifyRequest req) {
        // 1) 최근 발급 레코드 조회
        AccountAuth row = accountAuthRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("인증요청 이력이 없습니다."));

        // 2) 코드 비교
        boolean ok = row.getAuthCode().equals(req.getCode());
        // 3) 성공 시 레코드 삭제
        if (ok) {
            accountAuthRepository.deleteByUserId(userId);
        }
        // 4) 검증 결과 반환
        return new AccountAuthDto.VerifyResponse(ok);
    }
}