package dev.woori.wooriLearn.domain.scenario.entity;

import dev.woori.wooriLearn.domain.scenario.model.StepType;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScenarioEntitiesTest {

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

    @Test
    void scenarioAndStep_buildersPopulateFields() {
        Scenario scenario = Scenario.builder()
                .id(10L)
                .title("My Scenario")
                .totalNormalSteps(3)
                .build();

        Quiz quiz = Quiz.builder()
                .id(30L)
                .question("Q?")
                .options("[1,2]")
                .answer(1)
                .build();

        ScenarioStep step = ScenarioStep.builder()
                .id(20L)
                .scenario(scenario)
                .type(StepType.DIALOG)
                .content("{\"q\":\"a\"}")
                .quiz(quiz)
                .normalIndex(1)
                .build();

        assertEquals("My Scenario", scenario.getTitle());
        assertEquals(3, scenario.getTotalNormalSteps());
        assertEquals(quiz, step.getQuiz());
        assertEquals(StepType.DIALOG, step.getType());
        assertEquals("{\"q\":\"a\"}", step.getContent());
        assertNull(step.getNextStep());
    }

    @Test
    void scenarioCompleted_setsUserAndScenario() {
        Scenario scenario = Scenario.builder()
                .id(11L)
                .title("Scenario")
                .totalNormalSteps(2)
                .build();
        LocalDateTime completedAt = LocalDateTime.now();

        ScenarioCompleted completed = ScenarioCompleted.builder()
                .id(99L)
                .completedAt(completedAt)
                .user(user())
                .scenario(scenario)
                .build();

        assertEquals(99L, completed.getId());
        assertEquals(completedAt, completed.getCompletedAt());
        assertEquals(scenario, completed.getScenario());
        assertEquals("user", completed.getUser().getUserId());
    }
}
