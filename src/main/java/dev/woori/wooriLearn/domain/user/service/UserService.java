package dev.woori.wooriLearn.domain.user.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.user.dto.SignupReqDto;
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
    private final PasswordEncoder passwordEncoder;

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
}
