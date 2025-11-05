package dev.woori.wooriLearn.domain.auth.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.config.jwt.JwtUtil;
import dev.woori.wooriLearn.domain.auth.dto.LoginReqDto;
import dev.woori.wooriLearn.domain.auth.dto.LoginResDto;
import dev.woori.wooriLearn.domain.auth.dto.SignupReqDto;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * id와 비밀번호, 사용자 이름을 입력받아 회원가입을 진행합니다.
     * @param signupReqDto id / pw / 이름
     * @return 회원가입 완료 안내문구
     */
    public String signup(SignupReqDto signupReqDto) {
        if(userRepository.existsByUserId(signupReqDto.userId())){
            throw new CommonException(ErrorCode.CONFLICT);
        }

        Users user = Users.builder()
                .userId(signupReqDto.userId())
                .password(passwordEncoder.encode(signupReqDto.password()))
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
    public LoginResDto login(LoginReqDto loginReqDto) {
        Users user = userRepository.findByUserId(loginReqDto.userId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "존재하지 않는 회원입니다."));

        if(!passwordEncoder.matches(loginReqDto.password(), user.getPassword())){
            throw new CommonException(ErrorCode.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtUtil.generateToken(loginReqDto.userId());
        // TODO: refresh token 발급 로직 추가

        return new LoginResDto(accessToken);
    }
}
