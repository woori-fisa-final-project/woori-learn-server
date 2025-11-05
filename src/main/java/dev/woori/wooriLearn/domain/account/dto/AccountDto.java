package dev.woori.wooriLearn.domain.account.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * => 응답 담당
 * 계좌 목록 조회 시 프론트에 반환되는 DTO
 *
 *  프론트엔드로 반환할 계좌 정보 데이터 구조 선언?
 *  엔티티를 전체 노출하지 않고 필요한 정보만 뽑아서 전달
 */

@Getter
@AllArgsConstructor     // 모든 필드를 파리미터로 받는 생성자 자동 생성 개발자 편의성 굳
public class AccountDto {

    // 계좌 이름
    private String accountName;

    // 계좌 번호
    private String accountNumber;

    // 현재 잔액
    // int보다 안정성과 확장성이 좋아서 Integer 사용
    private Integer balance;

}
