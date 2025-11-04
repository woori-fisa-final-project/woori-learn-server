package dev.woori.wooriLearn.example.dto;

import lombok.Builder;

// 엔티티 변환용
@Builder
public record TestDto (
        String title,
        String content
){
}
