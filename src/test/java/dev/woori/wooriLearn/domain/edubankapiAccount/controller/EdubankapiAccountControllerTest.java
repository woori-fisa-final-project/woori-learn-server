package dev.woori.wooriLearn.domain.edubankapiAccount.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
public class EdubankapiAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("계좌 목록 조회 테스트")
    void testGetAccountList() throws Exception {
        mockMvc.perform(get("/education/accounts/list/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andDo(print());
    }

    @Test
    @DisplayName("거래내역 조회 테스트")
    void testGetTransactionList() throws Exception {
        mockMvc.perform(get("/education/accounts/transactions")
                        .param("accountId", "1")
                        .param("period", "1M")
                        .param("type", "ALL")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andDo(print());
    }

    @Test
    @DisplayName("계좌이체 성공 테스트")
    void testTransferSuccess() throws Exception {
        EdubankapiTransferRequestDto request = new EdubankapiTransferRequestDto(
                "1122334455",
                "5544332211",
                1000,
                "1111",
                "생활비",
                "홍길동"
        );

        mockMvc.perform(post("/education/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.message").value("이체가 완료되었습니다."))
                .andDo(print());
    }

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

        mockMvc.perform(post("/education/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("계좌 비밀번호가 일치하지 않습니다."))
                .andDo(print());
    }
}
