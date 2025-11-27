package dev.woori.wooriLearn.domain.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.domain.account.dto.request.PointsExchangeRequestDto;
import dev.woori.wooriLearn.domain.account.dto.response.PointsExchangeResponseDto;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.service.PointsExchangeService;
import dev.woori.wooriLearn.domain.account.service.PointsHistoryService;
import dev.woori.wooriLearn.domain.user.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * 사용자 포인트 API 컨트롤러 테스트
 * - Security 필터는 비활성화(addFilters = false)하고, SecurityContextHolder에 ROLE_USER 인증을 직접 주입한다.
 * - 서비스 레이어는 @MockBean 으로 대체하여 컨트롤러의 라우팅/응답 래핑만 검증한다.
 * - 대상 엔드포인트
 *   1) POST /api/points/exchange : 환전 신청
 *   2) GET  /api/points/history : 이력 조회
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PointControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PointsExchangeService pointsExchangeService;

    @MockBean
    PointsHistoryService pointsHistoryService;

    @BeforeEach
    void setUpSecurityContext() {
        // 테스트 시작마다 사용자 권한 인증을 컨텍스트에 주입
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user1",
                        "N/A",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }

    @Test
    @DisplayName("POST /api/points/exchange 성공")
    void requestExchange_success() throws Exception {
        // 1) 서비스 Mock 응답 준비
        PointsExchangeResponseDto resp = PointsExchangeResponseDto.builder()
                .requestId(1L)
                .userId(10L)
                .exchangeAmount(15000)
                .status(PointsStatus.APPLY)
                .requestDate(LocalDateTime.now())
                .message("출금 요청 처리 중입니다.")
                .build();

        // 2) Mock 동작 설정: 환전 신청 서비스 호출 시 준비한 응답 반환
        given(pointsExchangeService.requestExchange(anyString(), any()))
                .willReturn(resp);

        // 3) 요청 DTO 준비
        PointsExchangeRequestDto req = new PointsExchangeRequestDto(15000, "110123456789", "088");

        // 4) API 호출 및 응답 검증
        mockMvc.perform(post("/api/points/exchange")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                "user1",
                                "N/A",
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.requestId").value(1L))
                .andExpect(jsonPath("$.data.status").value("APPLY"))
                .andExpect(jsonPath("$.data.exchangeAmount").value(15000));
    }

    @Test
    @DisplayName("GET /api/points/history 성공 - 사용자")
    void getHistory_success_user() throws Exception {
        // 1) 도메인/DTO 샘플 데이터 준비
        Users user = Users.builder()
                .id(10L)
                .userId("user1")
                .nickname("u1")
                .points(2000)
                .build();

        PointsHistory h = PointsHistory.builder()
                .id(100L)
                .user(user)
                .amount(1000)
                .type(PointsHistoryType.DEPOSIT)
                .status(PointsStatus.SUCCESS)
                .build();

        // 2) Mock 동작 설정: 이력 페이지 반환
        Page<PointsHistory> page = new PageImpl<>(List.of(h), PageRequest.of(0, 20), 1);
        given(pointsHistoryService.getUnifiedHistory(anyString(), any(), anyBoolean()))
                .willReturn(page);

        // 3) API 호출 및 응답 검증
        mockMvc.perform(get("/api/points/history")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                "user1",
                                "N/A",
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
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
