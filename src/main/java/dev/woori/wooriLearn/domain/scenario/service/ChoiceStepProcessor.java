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

/**
 * CHOICE 타입 스텝을 처리하는 Processor
 *
 * - 사용자가 아직 선택을 하지 않은 경우(answer == null)
 *      -> CHOICE_REQUIRED 상태를 반환하고, 현재 스텝을 그대로 유지
 * - 사용자가 선택한 선택지가 오루트(good == false)인 경우
 *      -> nextStep이 있으면 해당 스텝으로 이동
 *      -> nextStep이 없으면 현재 CHOICE 스텝을 배드 엔딩으로 간주
 * - 사용자가 선택한 선택지가 정루트(good == true)인 경우
 *      -> nextStep이 있으면 해당 스텝으로 이동
 *      -> nextStep이 없으면 마지막으로 보고 완료 처리 및 시작 스텝 복귀
 */
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
            // 1) 아직 선택을 하지 않은 경우 -> 선택 필요 상태 반환
            service.updateProgressAndSave(progress, current, scenario, true);
            return new AdvanceResDto(AdvanceStatus.CHOICE_REQUIRED, service.mapStep(current), null);
        }

        // 2) 사용자가 선택한 인덱스를 기반으로 ChoiceInfo 생성
        ChoiceInfo choice = service.parseChoice(current, answer);

        // 3) 오루트 처리
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

        // 4) 정루트 처리
        //  - 명시적 next가 있으면 우선
        //  - 없으면 일반 nextStep 사용
        Long nextId = (choice.nextStepId() != null)
                ? choice.nextStepId()
                : (current.getNextStep() != null ? current.getNextStep().getId() : null);

        // 4-1) 다음 스텝이 없다면 -> 정상 루트 마지막 스텝으로 간주하고 완료 처리
        if (nextId == null) {
            // 정루트 마지막 -> 완료 처리 + 재개 지점은 시작 스텝
            return service.handleScenarioCompletion(ctx);
        }

        // 4-2) 다음 스텝으로 이동
        ScenarioStep next = byId.get(nextId);
        if (next == null) {
            throw new CommonException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "다음 스텝이 존재하지 않습니다. id=" + nextId
            );
        }

        // 정루트에서는 진행률 정상적으로 계산
        service.updateProgressAndSave(progress, next, scenario, false);
        return new AdvanceResDto(AdvanceStatus.ADVANCED, service.mapStep(next), null);
    }
}
