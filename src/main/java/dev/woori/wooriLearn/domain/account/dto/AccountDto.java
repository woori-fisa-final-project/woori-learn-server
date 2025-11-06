package dev.woori.wooriLearn.domain.account.dto;

import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
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
@AllArgsConstructor
public class AccountDto {

    private String accountName;
    private String accountNumber;
    private Integer balance;

    /*
        정적 팩토리 메서드
        컨트롤러와 클라이언트에 필요한 필드만 노출하기 위해 DTO 매핑
        엔티티 -> DTO로 변환
     */

    public static AccountDto from(EducationalAccount account){
        return new AccountDto(
                account.getAccountName(),
                account.getAccountNumber(),
                account.getBalance()
        );
    }

}
