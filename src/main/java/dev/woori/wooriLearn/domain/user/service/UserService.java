package dev.woori.wooriLearn.domain.user.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.port.AuthUserPort;
import dev.woori.wooriLearn.domain.user.dto.ChangeNicknameReqDto;
import dev.woori.wooriLearn.domain.user.dto.ChangePasswdReqDto;
import dev.woori.wooriLearn.domain.user.dto.SignupReqDto;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.user.dto.UserInfoResDto;
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
    private final PointsHistoryRepository pointsHistoryRepository;

    private static final int NEW_MEMBER_REGISTRATION_POINTS = 1000;

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
                .userId(signupReqDto.userId())
                .nickname(signupReqDto.nickname())
                .points(NEW_MEMBER_REGISTRATION_POINTS)
                .build();

        authUserRepository.save(authUser);
        userRepository.save(user);

        // TODO: 회원가입 이후 기본 포인트를 지급한다 하면 포인트 내역 추가 로직 필요
        pointsHistoryRepository.save(
                PointsHistory.builder()
                        .user(user)
                        .amount(NEW_MEMBER_REGISTRATION_POINTS)
                        .type(PointsHistoryType.DEPOSIT)
                        .status(PointsStatus.SUCCESS)
                        .build()
        );
    }

    public UserInfoResDto getUserInfo(String userId){
        Users user = userRepository.findByUserId(userId).orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        return UserInfoResDto.builder()
                .nickname(user.getNickname())
                .point(user.getPoints())
                .build();
    }

    public void changeNickname(String userId, ChangeNicknameReqDto request){
        Users user = userRepository.findByUserId(userId).orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        user.updateNickname(request.nickname());
    }

    public void changePassword(String userId, ChangePasswdReqDto request) {
        AuthUsers user = authUserRepository.findByUserId(userId).orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        user.updatePassword(passwordEncoder.encode(request.password()));
    }
}
