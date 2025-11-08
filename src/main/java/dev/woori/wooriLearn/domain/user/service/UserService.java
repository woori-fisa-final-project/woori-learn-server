package dev.woori.wooriLearn.domain.user.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.repository.AuthUserPort;
import dev.woori.wooriLearn.domain.auth.repository.AuthUserRepository;
import dev.woori.wooriLearn.domain.user.dto.SignupReqDto;
import dev.woori.wooriLearn.domain.user.entity.Role;
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
public class UserService {

    private final UserRepository userRepository;
    private final AuthUserPort authUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * id와 비밀번호, 사용자 이름을 입력받아 회원가입을 진행합니다.
     * @param signupReqDto id / pw / 이름
     */
    public void signup(SignupReqDto signupReqDto) {
        if(authUserRepository.existsByUserId(signupReqDto.userId())){
            throw new CommonException(ErrorCode.CONFLICT);
        }

        AuthUsers authUser = AuthUsers.builder()
                .userId(signupReqDto.userId())
                .password(passwordEncoder.encode(signupReqDto.password()))
                .role(Role.ROLE_USER)
                .build();

        Users user = Users.builder()
                .authUser(authUser)
                .nickname(signupReqDto.nickname())
                .points(0)
                .build();

        authUserRepository.save(authUser);
        userRepository.save(user);
        // TODO: 회원가입 이후 기본 포인트를 지급한다 하면 포인트 내역 추가 로직 필요
    }
}
