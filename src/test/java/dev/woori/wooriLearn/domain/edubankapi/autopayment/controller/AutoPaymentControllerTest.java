package dev.woori.wooriLearn.domain.edubankapi.autopayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.dto.AutoPaymentCreateRequest;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.dto.AutoPaymentResponse;
import dev.woori.wooriLearn.domain.edubankapi.autopayment.service.AutoPaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AutoPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutoPaymentService autoPaymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("자동이체 목록 조회 성공")
    void getAutoPaymentList_Success() throws Exception {
        // given
        Long educationalAccountId = 1L;
        String status = "ACTIVE";
        List<AutoPaymentResponse> responses = Arrays.asList(
                new AutoPaymentResponse(
                        1L,
                        educationalAccountId,
                        "1234567890",
                        "001",
                        50000,
                        "김철수",
                        "용돈",
                        1,
                        15,
                        LocalDate.now(),
                        LocalDate.now().plusYears(1),
                        "ACTIVE"
                )
        );

        given(autoPaymentService.getAutoPaymentList(educationalAccountId, status))
                .willReturn(responses);

        // when & then
        mockMvc.perform(get("/education/auto-payment/list")
                        .param("educationalAccountId", educationalAccountId.toString())
                        .param("status", status))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].depositNumber").value("1234567890"))
                .andExpect(jsonPath("$.data[0].amount").value(50000));
    }

    @Test
    @DisplayName("자동이체 목록 조회 - 교육용 계좌 ID 양수 검증 실패")
    void getAutoPaymentList_InvalidAccountId() throws Exception {
        // when & then
        mockMvc.perform(get("/education/auto-payment/list")
                        .param("educationalAccountId", "-1")
                        .param("status", "ACTIVE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("자동이체 상세 조회 성공")
    void getAutoPaymentDetail_Success() throws Exception {
        // given
        Long autoPaymentId = 1L;
        AutoPaymentResponse response = new AutoPaymentResponse(
                autoPaymentId,
                1L,
                "1234567890",
                "001",
                50000,
                "김철수",
                "용돈",
                1,
                15,
                LocalDate.now(),
                LocalDate.now().plusYears(1),
                "ACTIVE"
        );

        given(autoPaymentService.getAutoPaymentDetail(autoPaymentId))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/education/auto-payment/detail/{autoPaymentId}", autoPaymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(autoPaymentId))
                .andExpect(jsonPath("$.data.depositNumber").value("1234567890"))
                .andExpect(jsonPath("$.data.amount").value(50000));
    }

    @Test
    @DisplayName("자동이체 상세 조회 - 자동이체 ID 양수 검증 실패")
    void getAutoPaymentDetail_InvalidId() throws Exception {
        // when & then
        mockMvc.perform(get("/education/auto-payment/detail/{autoPaymentId}", -1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("자동이체 등록 성공")
    void createAutoPayment_Success() throws Exception {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                1L,
                "001",
                "1234567890",
                50000,
                "김철수",
                "용돈",
                1,
                15,
                LocalDate.now(),
                LocalDate.now().plusYears(1),
                "1234"
        );

        AutoPaymentResponse response = new AutoPaymentResponse(
                1L,
                1L,
                "1234567890",
                "001",
                50000,
                "김철수",
                "용돈",
                1,
                15,
                LocalDate.now(),
                LocalDate.now().plusYears(1),
                "ACTIVE"
        );

        given(autoPaymentService.createAutoPayment(any(AutoPaymentCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/education/auto-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.amount").value(50000));
    }

    @Test
    @DisplayName("자동이체 등록 - 필수값 누락")
    void createAutoPayment_MissingRequiredFields() throws Exception {
        // given
        AutoPaymentCreateRequest request = new AutoPaymentCreateRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ""
        );

        // when & then
        mockMvc.perform(post("/education/auto-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}