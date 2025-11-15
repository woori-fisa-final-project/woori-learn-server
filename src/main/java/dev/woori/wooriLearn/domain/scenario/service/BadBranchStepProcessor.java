package dev.woori.wooriLearn.domain.scenario.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgress;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.AdvanceStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BadBranchStepProcessor implements StepProcessor {

    @Override
    public AdvanceResDto process(StepContext ctx, ScenarioProgressService service) {
        Scenario scenario = ctx.scenario();
        Map<Long, ScenarioStep> byId = ctx.byId();
        ScenarioStep current = ctx.current();
        ScenarioProgress progress = ctx.progress();

        // 1) 배드 엔딩 스텝
        if (ctx.badEnding()) {
            // 복귀 지점: 배드 엔딩 스텝의 nextStep (예: 선택지 스텝 ID 지정)
            ScenarioStep anchor = null;
            if (current.getNextStep() != null) {
                anchor = byId.get(current.getNextStep().getId());
            }
            // nextStep 이 비어있으면 시작 스텝으로 복귀
            if (anchor == null) {
                anchor = ctx.startStep();
            }
            if (anchor == null) {
                throw new CommonException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "배드 엔딩 복귀 지점을 찾을 수 없습니다. scenarioId=" + scenario.getId()
                );
            }

            // 진행률 동결 + 복귀 지점으로 저장
            service.updateProgressAndSave(progress, anchor, scenario, true);

            // 클라이언트에는 배드 엔딩 화면을 그리기 위해 current 를 내려줌
            return new AdvanceResDto(AdvanceStatus.BAD_ENDING, service.mapStep(current), null);
        }

        // 2) 배드 브랜치 중간 스텝 → 다음 배드 스텝으로 이동
        ScenarioStep next = (current.getNextStep() != null)
                ? byId.get(current.getNextStep().getId())
                : null;

        if (next == null) {
            // 다음 스텝도 없는데 배드 브랜치 → 방어적으로 BAD_ENDING + 시작 스텝 복귀
            ScenarioStep anchor = ctx.startStep();
            if (anchor == null) {
                throw new CommonException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "배드 브랜치 복귀 지점을 찾을 수 없습니다. scenarioId=" + scenario.getId()
                );
            }
            service.updateProgressAndSave(progress, anchor, scenario, true);
            return new AdvanceResDto(AdvanceStatus.BAD_ENDING, service.mapStep(current), null);
        }

        service.updateProgressAndSave(progress, next, scenario, true);
        return new AdvanceResDto(AdvanceStatus.ADVANCED_FROZEN, service.mapStep(next), null);
    }
}
