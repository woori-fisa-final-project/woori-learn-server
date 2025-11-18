package dev.woori.wooriLearn.domain.auth.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.config.jwt.JwtInfo;
import dev.woori.wooriLearn.domain.auth.jwt.JwtIssuer;
import dev.woori.wooriLearn.config.jwt.JwtValidator;
import dev.woori.wooriLearn.config.security.Encoder;
import dev.woori.wooriLearn.domain.auth.dto.*;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.RefreshToken;
import dev.woori.wooriLearn.domain.auth.port.AuthUserPort;
import dev.woori.wooriLearn.domain.auth.port.RefreshTokenPort;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.auth.service.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AuthUserPort authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final Encoder encoder;
    private final JwtIssuer jwtIssuer;
    private final JwtValidator jwtValidator;
    private final RefreshTokenPort refreshTokenRepository;

    /**
     * id와 pw를 확인 후 사용자임이 확인되면 jwt 토큰을 발급합니다.
     * @param loginReqDto 로그인 입력값 - id / pw
     * @return loginResDto - access token / refresh token
     */
    public TokenWithCookie login(LoginReqDto loginReqDto) {
        AuthUsers user = authUserRepository.findByUserId(loginReqDto.userId())
                .orElseThrow(() -> new CommonException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        if(!passwordEncoder.matches(loginReqDto.password(), user.getPassword())){
            throw new CommonException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return generateAndSaveToken(loginReqDto.userId(), user.getRole());
    }

    /**
     * 회원가입 진행 전 동일한 userId가 존재하는지 확인합니다.
     * @param userId 사용자가 입력한 userId
     */
    public void verify(String userId){
        if(authUserRepository.existsByUserId(userId))
            throw new CommonException(ErrorCode.CONFLICT, "이미 존재하는 id입니다.");
    }

    /**
     * 입력된 refresh token을 이용해 새 access token을 발급합니다.
     * @param refreshToken 사용자의 refresh token
     * @return access token
     */
    public TokenWithCookie refresh(String refreshToken) {
        // 토큰 만료 및 유효성 검증
        JwtInfo jwtInfo = jwtValidator.parseToken(refreshToken);
        String username = jwtInfo.username();
        Role role = jwtInfo.role();

        // 토큰 존재 여부 검증
        RefreshToken token = refreshTokenRepository.findByUsername(username)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "토큰이 존재하지 않습니다."));

        // 토큰 일치 여부 검증
        if(!encoder.matches(refreshToken, token.getToken())){
            throw new CommonException(ErrorCode.UNAUTHORIZED, "토큰이 일치하지 않습니다.");
        }

        // 검증 끝나면 access token/refresh token 생성해서 return
        return generateAndSaveToken(username, role);
    }

    /**
     * 회원이 입력한 비밀번호를 검증하고 새로운 값으로 수정합니다.
     * @param userId jwt 토큰을 파싱해서 얻은 userId
     * @param request 현재 비밀번호 / 변경할 비밀번호가 담긴 요청 dto 객체
     */
    public void changePassword(String userId, ChangePasswdReqDto request) {
        AuthUsers user = authUserRepository.findByUserId(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 입력한 기존 비밀번호와 db에 저장된 기존 비밀번호가 일치하는지 검증
        if(!passwordEncoder.matches(request.currentPassword(), user.getPassword())){
            throw new CommonException(ErrorCode.UNAUTHORIZED, "비밀번호 인증에 실패했습니다.");
        }

        // 검증이 성공하면 업데이트
        user.updatePassword(passwordEncoder.encode(request.newPassword()));
    }

    /**
     * 사용자의 요청을 받아서 로그아웃 처리 - db에서 refresh token 삭제
     * @param username 사용자 id
     */
    public void logout(String username) {
        refreshTokenRepository.deleteByUsername(username);
    }

    public TokenWithCookie generateAndSaveToken(String username, Role role){
        // jwt 토큰 저장 로직
        String accessToken = jwtIssuer.generateAccessToken(username, role);
        var refreshTokenInfo = jwtIssuer.generateRefreshToken(username, role);
        String refreshToken = refreshTokenInfo.token();
        Instant refreshTokenExpiration = refreshTokenInfo.expiration();

        // 이전 토큰이 있다면 유효기간 갱신
        // 없다면 만들어서 저장
        RefreshToken token = refreshTokenRepository.findByUsername(username)
                .map(entity -> {
                    entity.updateToken(encoder.encode(refreshToken), refreshTokenExpiration);
                    return entity;
                })
                .orElseGet(() -> RefreshToken.builder()
                        .username(username)
                        .token(encoder.encode(refreshToken))
                        .expiration(refreshTokenExpiration)
                        .build());
        refreshTokenRepository.save(token);

        long maxAgeSeconds = refreshTokenExpiration.getEpochSecond() - Instant.now().getEpochSecond();
        return new TokenWithCookie(accessToken, CookieUtil.createRefreshTokenCookie(refreshToken, maxAgeSeconds));
    }
}
