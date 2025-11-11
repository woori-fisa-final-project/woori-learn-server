package dev.woori.wooriLearn.domain.scenario.dto;

import com.fasterxml.jackson.databind.JsonNode;
import dev.woori.wooriLearn.domain.scenario.model.StepType;

/**
 * 시나리오 문서 내 개별 스텝 DTO
 *
 * @param id        스텝 ID
 * @param type      스텝 유형 (DIALOG/CHOICE/OVERLAY/MODAL/ETC)
 * @param next      다음 스텝 ID (마지막 스텝이면 null)
 * @param quizId    연결된 퀴즈 ID (퀴즈가 없으면 null)
 * @param content   스텝 콘텐츠 (JSON)
 */
public record ScenarioDocStepDto(
        Long id,
        StepType type,
        Long next,
        Long quizId,
        JsonNode content
) {}
