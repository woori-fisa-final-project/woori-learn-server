package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.*;
import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

    private final AccountClient accountClient;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Value("${external.bank.account-url}")
    private String bankAccountUrl;

    private static final String WOORI_BANK_CODE = "020";

    /**
     * userId를 토대로 은행의 url 및 url에 접근하기 위한 access token을 반환합니다.
     * @param userId 사용자 id
     * @return String url - 계좌개설 url / String accessToken
     */
    public AccountUrlResDto getAccountUrl(String userId) {
        try{
            BankTokenResDto response = accountClient.getAccountUrl(new BankTokenReqDto(userId));
            return AccountUrlResDto.builder()
                    .accessToken(response.data().accessToken())
                    .url(bankAccountUrl)
                    .build();
        }catch(HttpClientErrorException e){
            log.warn("API 요청 실패 - [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) {
                throw new CommonException(ErrorCode.UNAUTHORIZED, "은행 인증 토큰을 가져오는 데 실패했습니다.");
            }
            throw new CommonException(ErrorCode.EXTERNAL_API_FAIL, "은행 API 요청 중 클라이언트 오류가 발생했습니다.");
        }
        catch (RestClientException e) {
            log.error("API 요청 오류: {}", e.getMessage(), e);
            throw new CommonException(ErrorCode.EXTERNAL_API_FAIL);
        }
    }

    /**
     * 요청에 담긴 정보를 토대로 계좌번호를 저장합니다.
     * @param userId id값
     * @param request 은행과의 통신에 사용할 임시 코드
     */
    public void registerAccount(String userId, AccountCreateReqDto request){
        // 사용자 찾기
        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "해당 사용자를 찾을 수 없습니다."));

        try {
            // 은행 서버에서 계좌번호 받아오기
            AccountCreateResDto response = accountClient.getAccountNum(
                    new AccountCheckReqDto(userId, request.code())
            );

            // 응답값의 null 체크
            if (response.data() == null) {
                log.warn("은행 API 응답의 data 필드가 null입니다.");
                throw new CommonException(ErrorCode.EXTERNAL_API_FAIL, "은행으로부터 유효하지 않은 계좌 정보를 받았습니다.");
            }

            // 계좌번호 저장하기
            Account account = Account.builder()
                    .accountName(response.data().name())
                    .accountNumber(response.data().accountNum())
                    .bankCode(WOORI_BANK_CODE)
                    .user(user)
                    .build();
            accountRepository.save(account);

        } catch (DataIntegrityViolationException e) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 등록된 계좌번호입니다.");
        } catch (HttpClientErrorException e) {
            // 4xx 클라이언트 에러 처리 (가장 자주 발생하는 케이스)
            HttpStatusCode statusCode = e.getStatusCode();
            log.warn("은행 서버 오류 [{}]", statusCode);

            throw switch (statusCode.value()) {
                case 401 ->
                    // 401 Unauthorized (인증 실패/토큰 만료 등)
                        new CommonException(ErrorCode.UNAUTHORIZED, "은행 인증 정보가 유효하지 않습니다.");
                case 404 ->
                    // 404 Not Found (요청한 리소스를 찾을 수 없음, 예를 들어 유효하지 않은 코드)
                        new CommonException(ErrorCode.ENTITY_NOT_FOUND, "은행 계좌 등록 요청 정보가 유효하지 않습니다.");
                case 400 ->
                    // 400 Bad Request (잘못된 요청 형식)
                        new CommonException(ErrorCode.INVALID_REQUEST, "은행 API 요청 형식이 잘못되었습니다.");
                default ->
                    // 그 외 4xx 에러는 통합 처리
                        new CommonException(ErrorCode.EXTERNAL_API_FAIL, "은행 API 처리 중 클라이언트 오류가 발생했습니다.");
            };

        } catch (RestClientException e) {
            // 4xx가 아닌 모든 예외 (5xx 서버 에러, 연결 실패 등)
            log.error("은행 서버에서 에러 발생: {}", e.getMessage(), e);

            // 5xx 서버 에러는 그대로 외부 API 실패로 처리
            throw new CommonException(ErrorCode.EXTERNAL_API_FAIL, "은행 서버 내부 오류 또는 연결 오류가 발생했습니다.");
        }
    }
}
