package dev.woori.wooriLearn.domain.scenario.dto;

import com.fasterxml.jackson.databind.JsonNode;
import dev.woori.wooriLearn.domain.scenario.model.StepType;

/**
 * 시나리오 재개 응답 DTO
 * 진행 이력이 있으면 해당 스텝을, 없으면 시작 스텝을 반환
 *
 * @param scenarioId    재개 대상 시나리오 ID
 * @param nowStepId     현재 바로 보여줄(재개할) 스텝 ID
 * @param type          현재 스텝 유형(DIALOG/CHOICE/OVERLAY/MODAL/ETC)
 * @param quizId        현재 스텝에 연결된 퀴즈 ID
 * @param content       현재 스텝의 콘텐츠(JSON)
 */
public record ProgressResumeResDto(
        Long scenarioId,
        Long nowStepId,
        StepType type,
        Long quizId,
        JsonNode content
) {}
