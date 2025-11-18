package dev.woori.wooriLearn.domain.scenario.service;

import dev.woori.wooriLearn.domain.scenario.dto.ScenarioCompletedResDto;
import dev.woori.wooriLearn.domain.scenario.dto.ScenarioProgressResDto;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioCompletedRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioProgressRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자의 시나리오 진행 현황(완료/진행률)을 조회하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioStatusService {

    private final ScenarioCompletedRepository scenarioCompletedRepository;
    private final ScenarioProgressRepository scenarioProgressRepository;
    private final UserService userService;

    /**
     * 사용자가 완료한 시나리오 목록 조회
     *
     * 흐름
     * 1) username으로 Users 엔티티 조회
     * 2) 해당 사용자 기준으로 ScenarioCompleted 목록 조회
     * 3) 엔티티 리스트를 ScenarioCompletedResDto 리스트로 변환 후 반환
     *
     * @param username 인증 정보에서 가져온 사용자 식별자
     * @return 완료한 시나리오 목록 응답 DTO 리스트
     */
    public List<ScenarioCompletedResDto> getCompletedScenarios(String username) {
        Users user = userService.getByUserIdOrThrow(username);
        return scenarioCompletedRepository.findByUser(user).stream()
                .map(ScenarioCompletedResDto::from)
                .toList();
    }

    /**
     * 사용자가 진행 중인 시나리오 진행률 조회
     *
     * 흐름
     * 1) username으로 Users 엔티티 조회
     * 2) 해당 사용자 기준으로 ScenarioProgress 목록 조회
     * 3) 엔티티 리스트를 ScenarioProgressResDto 리스트로 변환 후 반환
     *
     * @param username 인증 정보에서 가져온 사용자 식별자
     * @return 각 시나리오의 진행률 목록 응답 DTO 리스트
     */
    public List<ScenarioProgressResDto> getScenarioProgress(String username) {
        Users user = userService.getByUserIdOrThrow(username);
        return scenarioProgressRepository.findByUser(user).stream()
                .map(ScenarioProgressResDto::from)
                .toList();
    }
}