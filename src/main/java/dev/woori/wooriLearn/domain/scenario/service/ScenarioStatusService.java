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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioStatusService {

    private final ScenarioCompletedRepository scenarioCompletedRepository;
    private final ScenarioProgressRepository scenarioProgressRepository;
    private final UserService userService;

    public List<ScenarioCompletedResDto> getCompletedScenarios(String username) {
        Users user = userService.getByUserIdOrThrow(username);
        return scenarioCompletedRepository.findByUser(user).stream()
                .map(ScenarioCompletedResDto::from)
                .toList();
    }

    public List<ScenarioProgressResDto> getScenarioProgress(String username) {
        Users user = userService.getByUserIdOrThrow(username);
        return scenarioProgressRepository.findByUser(user).stream()
                .map(ScenarioProgressResDto::from)
                .toList();
    }
}