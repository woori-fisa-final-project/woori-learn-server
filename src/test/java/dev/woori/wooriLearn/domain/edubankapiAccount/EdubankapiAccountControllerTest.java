package dev.woori.wooriLearn.domain.edubankapiAccount;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto.EdubankapiTransferRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EdubankapiAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     *  1. 계좌 목록 조회 테스트
     * - [GET] /accounts/list/{userId}
     * - userId=1 기준으로 계좌 목록 조회
     */
    @Test
    @DisplayName("계좌 목록 조회 테스트")
    void testGetAccountList() throws Exception {
        mockMvc.perform(get("/accounts/list/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andDo(print());
    }

    /**
     *  2. 거래내역 조회 테스트
     * - [GET] /accounts/transactions?accountId={id}
     * - accountId=1 기준으로 최근 거래내역 조회
     */
    @Test
    @DisplayName("거래내역 조회 테스트")
    void testGetTransactionList() throws Exception {
        mockMvc.perform(get("/accounts/transactions")
                        .param("accountId", "1")
                        .param("period", "1M")
                        .param("type", "ALL")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andDo(print());
    }


    /**
     *  3. 계좌이체 테스트
     * - [POST] /accounts/transfer
     * - fromAccount=1122334455, toAccount=5544332211, amount=1000, password=1111
     */
    @Test
    @DisplayName(" 계좌이체 성공 테스트 (비밀번호 1111, 금액 1000원)")
    void testTransferSuccess() throws Exception {

        // given
        EdubankapiTransferRequestDto request = new EdubankapiTransferRequestDto(
                "1122334455",  // 출금계좌
                "5544332211",  // 입금계좌
                1000,          // 금액
                "1111",        // 계좌 비밀번호
                "생활비",       // 내통장표시
                "홍길동"        // 상대방 이름
        );

        mockMvc.perform(post("/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.message").value("이체가 완료되었습니다."))
                .andExpect(jsonPath("$.data.amount").value(1000))
                .andDo(print());
    }

    /**
     *  4. 비밀번호 오류 케이스
     */
    @Test
    @DisplayName("계좌이체 실패 테스트 (비밀번호 불일치)")
    void testTransferWrongPassword() throws Exception {
        EdubankapiTransferRequestDto request = new EdubankapiTransferRequestDto(
                "1122334455",
                "5544332211",
                1000,
                "9999",
                "생활비",
                "홍길동"
        );

        mockMvc.perform(post("/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("계좌 비밀번호가 일치하지 않습니다."))
                .andDo(print()); // ✅
    }

}
