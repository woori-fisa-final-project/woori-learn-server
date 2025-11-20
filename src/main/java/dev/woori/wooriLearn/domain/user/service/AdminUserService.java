package dev.woori.wooriLearn.domain.user.service;

import dev.woori.wooriLearn.config.response.PageResponse;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioCompletedCount;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioCompletedRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioRepository;
import dev.woori.wooriLearn.domain.user.dto.AdminUserListResDto;
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
    private final ScenarioCompletedRepository scenarioCompletedRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserListResDto> getAdminUserList(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<Users> usersPage = userRepository.findAll(pageable);

        // 1) 유저 ID 목록 추출
        List<Long> userIds = usersPage.map(Users::getId).toList();

        // 2) 유저별 완료 수를 한 번에 조회
        List<ScenarioCompletedCount> completedCounts =
                scenarioCompletedRepository.countCompletedByUserIds(userIds);

        // 3) Map<userId, completedCount> 변환
        Map<Long, Long> completedCountMap = completedCounts.stream()
                .collect(Collectors.toMap(
                        ScenarioCompletedCount::getUserId,
                        ScenarioCompletedCount::getCompletedCount
                ));

        // 4) 전체 시나리오 개수 (한 번만 조회)
        long totalScenarioCount = scenarioRepository.count();

        List<AdminUserListResDto> content = usersPage.stream()
                .map(user -> {
                    long completed = completedCountMap.getOrDefault(user.getId(), 0L);
                    double progressRate = totalScenarioCount == 0
                            ? 0
                            : (completed * 100.0) / totalScenarioCount;

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
}
