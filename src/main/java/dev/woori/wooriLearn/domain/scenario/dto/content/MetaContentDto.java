package dev.woori.wooriLearn.domain.scenario.dto.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 스텝의 content 중, meta 정보만을 분리해서 다루기 위한 DTO
 * @param meta 스텝의 메타 정보
 *             - branch: bad인 경우 배드 브랜치로 간주
 *             - badEnding: true인 경우 배드 엔딩 스텝
 *             진행률 동결 여부나 분기 처리에 사용됨
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetaContentDto(
        StepMetaDto meta
) {}
