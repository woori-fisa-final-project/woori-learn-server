package dev.woori.wooriLearn.domain.scenario.entity;

import dev.woori.wooriLearn.config.BaseEntity;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scenario_progress")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ScenarioProgress extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "progress_rate", nullable = false)
    private Double progressRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private ScenarioStep step;

    public void moveToStep(ScenarioStep nextStep) {
        if (nextStep == null) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "nextStep은 null일 수 없습니다.");
        }
        this.step = nextStep;
    }

    public void moveToStep(ScenarioStep nextStep, double newRate) {
        moveToStep(nextStep);
        if (newRate < 0.0 || newRate > 100.0) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "progressRate 범위(0~100) 위반");
        }
        this.progressRate = newRate;
    }
}