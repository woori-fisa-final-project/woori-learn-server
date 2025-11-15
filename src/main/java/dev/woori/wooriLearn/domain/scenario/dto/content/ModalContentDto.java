package dev.woori.wooriLearn.domain.scenario.dto.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * MODAL 타입 스텝의 content 전체를 표현하는 DTO
 * @param meta  해당 스텝의 메타 정보
 * @param title 모달 창의 제목 텍스트
 * @param text  모달 본문 내용
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModalContentDto(
        StepMetaDto meta,
        String title,
        String text
) {}
