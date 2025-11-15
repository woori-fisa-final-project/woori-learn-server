package dev.woori.wooriLearn.domain.scenario.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.AdvanceStatus;
import dev.woori.wooriLearn.domain.scenario.model.ChoiceInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ChoiceStepProcessor implements StepProcessor {

    @Override
    public AdvanceResDto process(StepContext ctx, ScenarioProgressService service) {
        ScenarioStep current = ctx.current();
        Scenario scenario = ctx.scenario();
        Map<Long, ScenarioStep> byId = ctx.byId();
        ScenarioProgress progress = ctx.progress();
        Integer answer = ctx.answer();

        if (answer == null) {
            // 선택지 미제출 → 현재 스텝 유지 + 진행률 동결
            service.updateProgressAndSave(progress, current, scenario, true);
            return new AdvanceResDto(AdvanceStatus.CHOICE_REQUIRED, service.mapStep(current), null);
        }

        ChoiceInfo choice = service.parseChoice(current, answer);

        // 오루트
        if (!choice.good()) {
            Long nextId = choice.nextStepId();
            if (nextId == null) {
                // 오루트인데 다음 스텝도 없으면 이 CHOICE 자체를 배드 엔딩으로 취급
                service.updateProgressAndSave(progress, current, scenario, true);
                return new AdvanceResDto(AdvanceStatus.BAD_ENDING, service.mapStep(current), null);
            }

            ScenarioStep nextWrong = byId.get(nextId);
            if (nextWrong == null) {
                throw new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "잘못된 경로 next가 존재하지 않습니다. id=" + nextId
                );
            }

            // 오루트에서는 진행률 동결
            service.updateProgressAndSave(progress, nextWrong, scenario, true);
            return new AdvanceResDto(AdvanceStatus.ADVANCED_FROZEN, service.mapStep(nextWrong), null);
        }

        // 정루트: 명시적 next가 있으면 우선, 없으면 일반 nextStep 사용
        Long nextId = (choice.nextStepId() != null)
                ? choice.nextStepId()
                : (current.getNextStep() != null ? current.getNextStep().getId() : null);

        if (nextId == null) {
            // 정루트 마지막 → 완료 처리 + 재개 지점은 시작 스텝
            return service.handleScenarioCompletion(ctx);
        }

        ScenarioStep next = byId.get(nextId);
        if (next == null) {
            throw new CommonException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "다음 스텝이 존재하지 않습니다. id=" + nextId
            );
        }

        service.updateProgressAndSave(progress, next, scenario, false);
        return new AdvanceResDto(AdvanceStatus.ADVANCED, service.mapStep(next), null);
    }
}
