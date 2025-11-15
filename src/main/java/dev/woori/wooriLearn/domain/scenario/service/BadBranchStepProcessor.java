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

/**
 * 배드 브랜치 / 배드 엔딩 스텝을 처리하는 Processor
 *
 * - 배드 엔딩 스텝(meta.badEnding == true)
 * 1) 현재 스텝을 BAD_ENDING 상태로 내려준다.
 * 2) 진행률은 동결한 채, 복귀 지점으로 사용자의 진행 위치를 저장한다.
 *
 * - 배드 브랜치 중간 스텝(meta.branch == "bad", badEnding == false)
 * 1) nextStep이 있으면 해당 스텝으로 이동 (진행률은 동결)
 * 2) nextStep이 없으면 BAD_ENDING으로 처리하고, 복귀 지점 설정
 */
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
            // 복귀 지점: 배드 엔딩 스텝의 nextStep
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

        // 2) 배드 브랜치 중간 스텝 -> 다음 배드 스텝으로 이동
        ScenarioStep next = (current.getNextStep() != null)
                ? byId.get(current.getNextStep().getId())
                : null;

        if (next == null) {
            // 다음 스텝 없는 배드 브랜치 -> BAD_ENDING + 시작 스텝 복귀
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

        // 정상적인 배드 브랜치 흐름: 다음 배드 스텝으로 이동 (진행률은 계속 동결)
        service.updateProgressAndSave(progress, next, scenario, true);
        return new AdvanceResDto(AdvanceStatus.ADVANCED_FROZEN, service.mapStep(next), null);
    }
}
