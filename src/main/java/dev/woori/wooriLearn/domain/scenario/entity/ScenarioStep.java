package dev.woori.wooriLearn.domain.scenario.entity;

import dev.woori.wooriLearn.domain.scenario.model.StepType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scenario_step")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ScenarioStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StepType type;

    @Column(nullable = false, columnDefinition = "json")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_step")
    private ScenarioStep nextStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Column(name = "normal_index")
    private Integer normalIndex;
}
