package dev.woori.wooriLearn.domain.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.domain.auth.dto.LoginReqDto;
import dev.woori.wooriLearn.domain.auth.repository.RefreshTokenRepository;
import dev.woori.wooriLearn.domain.user.dto.SignupReqDto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private static String accessToken;
    private static String refreshToken;

    @Test
    @Order(1)
    @DisplayName("회원가입 성공 테스트")
    void signup_success() throws Exception {
        SignupReqDto signupReqDto = new SignupReqDto("testUser", "1234", "테스트닉네임");

        mockMvc.perform(post("/user/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupReqDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    @Order(2)
    @DisplayName("로그인 성공 테스트")
    void login_success() throws Exception {
        LoginReqDto loginReqDto = new LoginReqDto("testUser", "1234");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReqDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(responseJson).path("data").path("accessToken").asText();
        refreshToken = objectMapper.readTree(responseJson).path("data").path("refreshToken").asText();
    }

    @Test
    @Order(3)
    @DisplayName("토큰 재발급 성공 테스트")
    void token_refresh_success() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn();

        accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test
    @Order(4)
    @DisplayName("로그아웃 성공 테스트")
    void logout_success() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 로그아웃 후 RefreshToken 삭제 확인
        boolean exists = refreshTokenRepository.findByUsername("testUser").isPresent();
        Assertions.assertFalse(exists, "로그아웃 후 refresh token이 삭제되어야 함");
    }
}