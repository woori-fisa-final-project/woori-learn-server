package dev.woori.wooriLearn.domain.user.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.config.response.PageResponse;
import dev.woori.wooriLearn.domain.account.dto.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.dto.PointsHistoryDto;
import dev.woori.wooriLearn.domain.account.entity.Account;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.AccountRepository;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.scenario.dto.ScenarioProgressResDto;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioCompleted;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioCompletedCount;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioCompletedRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioProgressRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioRepository;
import dev.woori.wooriLearn.domain.user.dto.AdminUserInfoResDto;
import dev.woori.wooriLearn.domain.user.dto.AdminUserListResDto;
import dev.woori.wooriLearn.domain.user.dto.UserAccountDto;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserService {

    private final UserRepository userRepository;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioProgressRepository scenarioProgressRepository;
    private final AccountRepository accountRepository;
    private final PointsHistoryRepository pointsHistoryRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserListResDto> getAdminUserList(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<Users> usersPage = userRepository.findAll(pageable);

        // 1. 전체 시나리오 목록 조회
        List<Long> scenarioIds = scenarioRepository.findAll().stream()
                .map(Scenario::getId)
                .toList();
        int scenarioCount = scenarioIds.size();

        // 2. 유저 ID 목록 추출
        List<Long> userIds = usersPage.map(Users::getId).toList();
        // 3. 유저들의 시나리오 진행률을 조회
        List<ScenarioProgress> progresses = scenarioProgressRepository.findByUserIdIn(userIds);

        // 4. 유저 - 시나리오 진행률 매핑
        // { id : { 시나리오 id : 진행률 } } 형태 ({key:value})
        Map<Long, Map<Long, Double>> progressMap = new HashMap<>();

        for (ScenarioProgress sp : progresses) {
            Long userId = sp.getUser().getId();
            Long scenarioId = sp.getScenario().getId();
            Double progress = sp.getProgressRate();

            progressMap
                    .computeIfAbsent(userId, k -> new HashMap<>())
                    .put(scenarioId, progress);
        }

        // 5. 전체 진행률 계산해서 넣어주기
        List<AdminUserListResDto> content = usersPage.stream()
                .map(user -> {
                    // 진행률 가져오기
                    Map<Long, Double> userProgress = progressMap.getOrDefault(user.getId(), Map.of());

                    // 전체 진행률 계산
                    double totalProgress = 0.0;
                    for (Long scenarioId : scenarioIds) {
                        totalProgress += userProgress.getOrDefault(scenarioId, 0.0);
                    }
                    double progressRate = scenarioCount == 0
                            ? 0
                            : totalProgress / scenarioCount;

                    return AdminUserListResDto.builder()
                            .id(user.getId())
                            .userId(user.getUserId())
                            .nickname(user.getNickname())
                            .points(user.getPoints())
                            .role(user.getAuthUser().getRole())
                            .createdAt(user.getCreatedAt())
                            .progressRate(progressRate)
                            .build();
                })
                .toList();

        return PageResponse.of(usersPage, content);
    }

    public AdminUserInfoResDto getAdminUserInfo(Long id){
        // 1. 사용자 정보 가져오기
        Users user = userRepository.findById(id).orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));

        // 2. 전체 진행률 계산하기
        // 2-1. 전체 시나리오 목록 가져오기
        List<Scenario> allScenarios = scenarioRepository.findAll();
        List<Long> scenarioIds = allScenarios.stream()
                .map(Scenario::getId)
                .toList();
        int scenarioCount = scenarioIds.size();

        // 2-2. 사용자의 시나리오 진행률 리스트 가져오기
        List<ScenarioProgress> progresses = scenarioProgressRepository.findByUser(user);

        // 2-3. 시나리오 id - 진행률 매핑하기
        Map<Long, Double> progressMap = progresses.stream()
                .collect(Collectors.toMap(
                        sp -> sp.getScenario().getId(),
                        ScenarioProgress::getProgressRate
                ));

        // 2-4. 전체 진행률 계산하기
        double totalProgress = scenarioIds.stream()
                .mapToDouble(scenarioId -> progressMap.getOrDefault(scenarioId, 0.0))
                .sum();

        double progressRate = scenarioCount == 0 ? 0 : totalProgress / scenarioCount;

        // 3. 연결된 실제 계좌정보 가져오기 (없을 경우 null)
        UserAccountDto accountDto = accountRepository.findByUserId(id)
                .map(a -> new UserAccountDto(a.getAccountNumber(), a.getCreatedAt()))
                .orElse(null);

        // 4. 개별 시나리오 진행률 가져오기
        List<ScenarioProgressResDto> scenarioProgressDtos = allScenarios.stream()
                .map(scenario -> new ScenarioProgressResDto(
                        scenario.getId(),
                        scenario.getTitle(),
                        progressMap.getOrDefault(scenario.getId(), 0.0)  // 없으면 0%
                ))
                .toList();

        // 5. 포인트 목록 가져오기
        List<PointsHistory> pointsHistoryList = pointsHistoryRepository.findByUserId(id);

        List<PointsHistoryDto> pointHistories = pointsHistoryList.stream()
                .map(PointsHistoryDto::from)
                .toList();

        int exchangedPoints = pointsHistoryList.stream()
                .filter(ph -> ph.getType() == PointsHistoryType.WITHDRAW
                        && ph.getStatus() == PointsStatus.SUCCESS)
                .mapToInt(PointsHistory::getAmount)
                .sum();

        return AdminUserInfoResDto.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .points(user.getPoints())
                .exchangedPoints(exchangedPoints)
                .role(user.getAuthUser().getRole())
                .createdAt(user.getCreatedAt())
                .progressRate(progressRate)
                .account(accountDto)
                .scenarios(scenarioProgressDtos)
                .pointHistories(pointHistories)
                .build();
    }
}
