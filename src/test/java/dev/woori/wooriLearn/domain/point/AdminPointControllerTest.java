package dev.woori.wooriLearn.domain.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.domain.account.dto.request.PointsDepositRequestDto;
import dev.woori.wooriLearn.domain.account.dto.response.PointsDepositResponseDto;
import dev.woori.wooriLearn.domain.account.dto.response.PointsHistoryResponseDto;
import dev.woori.wooriLearn.domain.account.dto.response.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.service.PointsExchangeFacade;
import dev.woori.wooriLearn.domain.account.service.PointsDepositService;
import dev.woori.wooriLearn.domain.account.service.PointsExchangeService;
import dev.woori.wooriLearn.domain.account.service.PointsExchangeFacade;
import dev.woori.wooriLearn.domain.account.service.PointsHistoryService;
import dev.woori.wooriLearn.domain.user.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * 관리자 포인트 API 컨트롤러 테스트
 * - Security 필터는 비활성화(addFilters = false)하고, SecurityContextHolder에 ROLE_ADMIN 인증을 직접 주입한다.
 * - 서비스 레이어는 @MockBean 으로 대체하여 컨트롤러의 라우팅/보안/응답 래핑만 검증한다.
 * - 대상 엔드포인트
 *   1) POST /api/points/deposit        : 관리자 포인트 적립
 *   2) PUT  /admin/points/exchange/... : 환전 승인
 *   3) GET  /admin/points/exchange/... : 환전 대기목록 조회
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AdminPointControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    PointsDepositService pointsDepositService;

    @MockitoBean
    PointsExchangeService pointsExchangeService;

    @MockitoBean
    PointsExchangeFacade pointsExchangeFacade;

    @MockitoBean
    PointsHistoryService pointsHistoryService;

    @MockBean
    PointsExchangeFacade pointsExchangeFacade;

    @BeforeEach
    void setUpSecurityContext() {
        // 테스트 시작마다 관리자 권한 인증을 컨텍스트에 주입
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        "N/A",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );
    }

    @Test
    @DisplayName("POST /api/points/deposit 성공 - 관리자")
    void deposit_success_admin() throws Exception {
        // 1) 서비스 Mock 응답 준비
        PointsDepositResponseDto resp = PointsDepositResponseDto.builder()
                .userId(10L)
                .addedPoint(1000)
                .currentBalance(2500)
                .status(PointsStatus.SUCCESS)
                .message("포인트 적립 완료")
                .createdAt(LocalDateTime.now())
                .build();

        // 2) Mock 동작 설정: 관리자 적립 서비스 호출 시 준비한 응답 반환
        given(pointsDepositService.depositPoints(anyString(), any()))
                .willReturn(resp);

        // 3) 요청 DTO 준비
        PointsDepositRequestDto req = new PointsDepositRequestDto(1000, "테스트");

        // 4) API 호출 및 응답 검증
        mockMvc.perform(post("/api/points/deposit")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                "admin",
                                "N/A",
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        )))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.addedPoint").value(1000))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("PUT /admin/points/exchange/approve/{id} 성공 - 관리자")
    void approve_success_admin() throws Exception {
        // 1) 서비스 Mock 응답 준비
        PointsExchangeResponseDto resp = PointsExchangeResponseDto.builder()
                .requestId(1L)
                .userId(10L)
                .exchangeAmount(15000)
                .status(PointsStatus.SUCCESS)
                .processedDate(LocalDateTime.now())
                .message("정상적으로 처리되었습니다.")
                .build();

        // 2) Mock 동작 설정: 환전 승인 서비스 호출 시 준비한 응답 반환
        given(pointsExchangeFacade.executeTransfer(eq(1L))).willReturn(resp);

        // 3) API 호출 및 응답 검증
        mockMvc.perform(put("/admin/points/exchange/approve/{id}", 1L)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                "admin",
                                "N/A",
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        )))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestId").value(1L))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /admin/points/exchange/apply 성공 - 관리자")
    void listPending_success_admin() throws Exception {
        // 1) 도메인/DTO 샘플 데이터 준비
        Users user = Users.builder()
                .id(10L)
                .userId("user1")
                .nickname("u1")
                .points(2000)
                .build();

        PointsHistory h = PointsHistory.builder()
                .id(200L)
                .user(user)
                .amount(15000)
                .type(PointsHistoryType.WITHDRAW)
                .status(PointsStatus.APPLY)
                .build();

        PointsHistoryResponseDto dto = new PointsHistoryResponseDto(h);
        Page<PointsHistoryResponseDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
        // 2) Mock 동작 설정: APPLY 상태 목록 페이지 반환
        given(pointsExchangeService.getPendingWithdrawals(any(), any())).willReturn(page);

        // 3) API 호출 및 응답 검증
        mockMvc.perform(get("/admin/points/exchange/apply")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                "admin",
                                "N/A",
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        )))
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("APPLY"))
                .andExpect(jsonPath("$.data.content[0].type").value("WITHDRAW"))
                .andExpect(jsonPath("$.data.content[0].amount").value(15000));
    }

    @Test
    @DisplayName("GET /admin/points/history 성공 - 관리자")
    void adminHistory_success() throws Exception {
        // 1) 샘플 데이터
        Users user = Users.builder()
                .id(10L)
                .userId("user1")
                .nickname("u1")
                .points(2000)
                .build();

        PointsHistory h = PointsHistory.builder()
                .id(300L)
                .user(user)
                .amount(1000)
                .type(PointsHistoryType.DEPOSIT)
                .status(PointsStatus.SUCCESS)
                .build();

        Page<PointsHistory> page = new PageImpl<>(List.of(h), PageRequest.of(0, 20), 1);
        // 2) Mock: 관리자 true로 호출 시 페이지 반환
        given(pointsHistoryService.getUnifiedHistory(anyString(), any(), eq(true))).willReturn(page);

        // 3) 호출/검증
        mockMvc.perform(get("/admin/points/history")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                "admin",
                                "N/A",
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        )))
                        .param("status", "ALL")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].amount").value(1000));
    }
}
