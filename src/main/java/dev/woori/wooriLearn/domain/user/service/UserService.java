package dev.woori.wooriLearn.domain.user.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.port.AuthUserPort;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.repository.EdubankapiAccountRepository;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import dev.woori.wooriLearn.domain.user.dto.ChangeNicknameReqDto;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final AuthUserPort authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final PointsHistoryRepository pointsHistoryRepository;
    private final EdubankapiAccountRepository eduAccountRepository;

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

        // 신규 회원 포인트 지급
        pointsHistoryRepository.save(
                PointsHistory.builder()
                        .user(user)
                        .amount(NEW_MEMBER_REGISTRATION_POINTS)
                        .type(PointsHistoryType.DEPOSIT)
                        .status(PointsStatus.SUCCESS)
                        .build()
        );

        // 입출금계좌 생성
        eduAccountRepository.save(
                EducationalAccount.builder()
                        .accountNumber(generateAccountNumber())   // 랜덤 계좌번호 생성 함수
                        .accountType(EducationalAccount.AccountType.CHECKING)
                        .balance(5000000)
                        .accountPassword(passwordEncoder.encode("1234"))
                        .accountName(user.getNickname())
                        .user(user)   // 연관관계 설정
                        .build()
        );

        // 예금 계좌 생성
        eduAccountRepository.save(
                EducationalAccount.builder()
                        .accountNumber(generateAccountNumber())   // 랜덤 계좌번호 생성 함수
                        .accountType(EducationalAccount.AccountType.SAVINGS)
                        .balance(5000000)
                        .accountPassword(passwordEncoder.encode("1234"))
                        .accountName(user.getNickname())
                        .user(user)   // 연관관계 설정
                        .build()
        );
    }

    public UserInfoResDto getUserInfo(String userId){
        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. userId=" + userId));
        return UserInfoResDto.builder()
                .nickname(user.getNickname())
                .point(user.getPoints())
                .build();
    }

    public void changeNickname(String userId, ChangeNicknameReqDto request){
        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. userId=" + userId));
        user.updateNickname(request.nickname());
        
        // 계좌 이름 변경
        List<EducationalAccount> accounts = eduAccountRepository.findAllByUserId(user.getId());
        for (EducationalAccount account : accounts) {
            account.updateAccountName(request.nickname());
        }
    }

    public Users getByUserIdOrThrow(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다: " + userId
                ));
    }

    private String generateAccountNumber() {
        return UUID.randomUUID().toString().substring(0, 16);
    }
}
