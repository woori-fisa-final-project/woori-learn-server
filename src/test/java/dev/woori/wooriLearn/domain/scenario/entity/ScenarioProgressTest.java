package dev.woori.wooriLearn.domain.scenario.entity;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.user.entity.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScenarioProgressTest {

    private Users user() {
        return Users.builder()
                .id(1L)
                .authUser(AuthUsers.builder()
                        .id(2L)
                        .userId("user")
                        .password("pw")
                        .role(Role.ROLE_USER)
                        .build())
                .userId("user")
                .nickname("nick")
                .points(0)
                .build();
    }

    private Scenario scenario() {
        return Scenario.builder()
                .id(10L)
                .title("title")
                .totalNormalSteps(1)
                .build();
    }

    private ScenarioStep step(Long id) {
        return ScenarioStep.builder()
                .id(id)
                .scenario(scenario())
                .type(dev.woori.wooriLearn.domain.scenario.model.StepType.DIALOG)
                .content("{\"text\":\"hi\"}")
                .build();
    }

    @Test
    @DisplayName("다음 스텝으로 이동하면 step이 교체된다")
    void moveToStep_updatesStep() {
        ScenarioStep current = step(1L);
        ScenarioStep next = step(2L);
        ScenarioProgress progress = ScenarioProgress.builder()
                .id(5L)
                .progressRate(10.0)
                .user(user())
                .scenario(scenario())
                .step(current)
                .build();

        progress.moveToStep(next);

        assertEquals(next, progress.getStep());
    }

    @Test
    @DisplayName("이동 시 진행률을 함께 전달하면 진행률도 갱신된다")
    void moveToStep_withRate_updatesRateAndStep() {
        ScenarioStep current = step(1L);
        ScenarioStep next = step(2L);
        ScenarioProgress progress = ScenarioProgress.builder()
                .id(6L)
                .progressRate(5.0)
                .user(user())
                .scenario(scenario())
                .step(current)
                .build();

        progress.moveToStep(next, 50.0);

        assertEquals(next, progress.getStep());
        assertEquals(50.0, progress.getProgressRate());
    }

    @Test
    @DisplayName("다음 스텝이 null이면 INTERNAL_SERVER_ERROR 예외를 던진다")
    void moveToStep_null_throwsInternalServerError() {
        ScenarioProgress progress = ScenarioProgress.builder()
                .id(7L)
                .progressRate(5.0)
                .user(user())
                .scenario(scenario())
                .step(step(1L))
                .build();

        CommonException ex = assertThrows(CommonException.class, () -> progress.moveToStep(null));
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("진행률이 0 미만이면 INVALID_REQUEST 예외를 던진다")
    void moveToStep_invalidRateLow_throwsInvalidRequest() {
        ScenarioProgress progress = ScenarioProgress.builder()
                .id(8L)
                .progressRate(5.0)
                .user(user())
                .scenario(scenario())
                .step(step(1L))
                .build();

        CommonException ex = assertThrows(CommonException.class, () -> progress.moveToStep(step(2L), -1.0));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    @DisplayName("진행률이 100 초과이면 INVALID_REQUEST 예외를 던진다")
    void moveToStep_invalidRateHigh_throwsInvalidRequest() {
        ScenarioProgress progress = ScenarioProgress.builder()
                .id(9L)
                .progressRate(5.0)
                .user(user())
                .scenario(scenario())
                .step(step(1L))
                .build();

        CommonException ex = assertThrows(CommonException.class, () -> progress.moveToStep(step(2L), 101.0));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }
}
