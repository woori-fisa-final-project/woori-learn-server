package dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto;

import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;

/**
 * 교육용 계좌 응답 DTO
 *
 * 계좌 목록 조회 시 프론트엔드에 반환되는 데이터 구조
 * 엔티티를 전체 노출하지 않고 필요한 정보만 포함
 *
 * @param id 계좌 ID
 * @param accountName 계좌명
 * @param accountNumber 계좌번호
 * @param balance 잔액
 * @param userId 사용자 ID
 * @param accountType 계좌 타입 (필수 필드, nullable=false 보장)
 *                    - "CHECKING": 입출금 계좌
 *                    - "SAVINGS": 예금 계좌
 *                    - "DEPOSIT": 적금 계좌 (현재 미사용)
 */
public record EdubankapiAccountDto(
        Long id,
        String accountName,
        String accountNumber,
        Integer balance,
        String userId,
        String accountType  // ✅ 필수 필드 (DB: nullable=false)
) {
    /*
        정적 팩토리 메서드
        컨트롤러와 클라이언트에 필요한 필드만 노출하기 위해 DTO 매핑
        엔티티 -> DTO로 변환
     */
    public static EdubankapiAccountDto from(EducationalAccount account){
        return new EdubankapiAccountDto(
                account.getId(),
                account.getAccountName(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getUser().getUserId(),
                account.getAccountType().name()  // Enum을 String으로 변환
        );
    }

}
