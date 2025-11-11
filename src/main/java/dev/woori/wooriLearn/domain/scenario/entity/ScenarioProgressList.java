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
public class ScenarioProgressList extends BaseEntity {
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
            throw new CommonException(ErrorCode.INVALID_REQUEST, "nextStep가 null입니다.");
        }
        this.step = nextStep;
    }
}