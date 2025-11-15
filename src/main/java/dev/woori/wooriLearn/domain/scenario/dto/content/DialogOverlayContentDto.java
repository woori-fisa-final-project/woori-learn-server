package dev.woori.wooriLearn.domain.scenario.dto.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DIALOG / OVERLAY 타입 스텝의 content 전체를 표현하는 DTO
 * @param meta      해당 스텝의 메타 정보
 * @param text      화면에 표시할 실제 대사/설명 텍스트
 * @param character 말을 하는 화자
 * @param image     캐릭터 이미지 URL
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DialogOverlayContentDto(
        StepMetaDto meta,
        String text,
        String character,
        String image
) {}
