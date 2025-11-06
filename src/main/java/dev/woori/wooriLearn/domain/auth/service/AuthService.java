package dev.woori.wooriLearn.domain.auth.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.config.jwt.JwtUtil;
import dev.woori.wooriLearn.config.security.Encoder;
import dev.woori.wooriLearn.domain.auth.dto.*;
import dev.woori.wooriLearn.domain.auth.entity.RefreshToken;
import dev.woori.wooriLearn.domain.auth.repository.RefreshTokenRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final Encoder encoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * id와 비밀번호, 사용자 이름을 입력받아 회원가입을 진행합니다.
     * @param signupReqDto id / pw / 이름
     * @return 회원가입 완료 안내문구
     */
    @Transactional
    public String signup(SignupReqDto signupReqDto) {
        if(userRepository.existsByUserId(signupReqDto.userId())){
            throw new CommonException(ErrorCode.CONFLICT);
        }

        Users user = Users.builder()
                .userId(signupReqDto.userId())
                .password(encoder.encode(signupReqDto.password()))
                .nickname(signupReqDto.nickname())
                .points(0) // 초기 설정, 이후 수정 가능
                .build();

        userRepository.save(user);
        // TODO: 회원가입 이후 기본 포인트를 지급한다 하면 포인트 내역 추가 로직 필요

        return "회원가입이 완료되었습니다.";
    }

    /**
     * id와 pw를 확인 후 사용자임이 확인되면 jwt 토큰을 발급합니다.
     * @param loginReqDto 로그인 입력값 - id / pw
     * @return loginResDto - access token / refresh token
     */
    @Transactional
    public LoginResDto login(LoginReqDto loginReqDto) {
        Users user = userRepository.findByUserId(loginReqDto.userId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "존재하지 않는 회원입니다."));

        if(!encoder.matches(loginReqDto.password(), user.getPassword())){
            throw new CommonException(ErrorCode.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
        }

        return generateAndSaveToken(loginReqDto.userId());
    }

    /**
     * 입력된 refresh token을 이용해 새 access token을 발급합니다.
     * @param refreshReqDto 사용자의 refresh token
     * @return access token
     */
    @Transactional
    public LoginResDto refresh(RefreshReqDto refreshReqDto) {
        String refreshToken = refreshReqDto.refreshToken();
        String username;

        // 토큰 만료 및 유효성 검증
        try {
            username = jwtUtil.getUsername(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new CommonException(ErrorCode.TOKEN_EXPIRED, "토큰이 만료되었습니다.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new CommonException(ErrorCode.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }

        // 토큰 존재 여부 검증
        RefreshToken token = refreshTokenRepository.findByUsername(username)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "토큰이 존재하지 않습니다."));

        // 토큰 일치 여부 검증
        if(!encoder.matches(refreshReqDto.refreshToken(), token.getToken())){
            throw new CommonException(ErrorCode.UNAUTHORIZED, "토큰이 일치하지 않습니다.");
        }

        // 검증 끝나면 access token/refresh token 생성해서 return
        return generateAndSaveToken(username);
    }

    /**
     * 사용자의 요청을 받아서 로그아웃 처리 - db에서 refresh token 삭제
     * @param username 사용자 id
     * @return 결과 메시지
     */
    @Transactional
    public String logout(String username) {
        refreshTokenRepository.deleteByUsername(username);
        return "로그아웃되었습니다.";
    }

    public LoginResDto generateAndSaveToken(String username){
        // jwt 토큰 저장 로직
        String accessToken = jwtUtil.generateAccessToken(username);
        var refreshTokenInfo = jwtUtil.generateRefreshToken(username);
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

        return new LoginResDto(accessToken, refreshToken);
    }
}
