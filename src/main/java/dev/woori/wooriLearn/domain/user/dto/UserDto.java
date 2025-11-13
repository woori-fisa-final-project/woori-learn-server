package dev.woori.wooriLearn.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDto {
    private Long id;          // DB PK
    private String userId;    // 로그인용 사용자 아이디
    private String nickname;
    private Integer points;   // 보유 포인트
    private String createdAt;
    private String updatedAt;
}
