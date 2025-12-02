package dev.woori.wooriLearn.domain.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.domain.auth.dto.LoginReqDto;
import dev.woori.wooriLearn.domain.auth.entity.AuthUsers;
import dev.woori.wooriLearn.domain.auth.entity.Role;
import dev.woori.wooriLearn.domain.auth.port.AuthUserPort;
import dev.woori.wooriLearn.domain.auth.port.RefreshTokenPort;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class LoginTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthUserPort authUserRepository;

    @Autowired
    private RefreshTokenPort refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String testUserId = "test1234";
    private String testPassword = "!qwe1234";

    @BeforeEach
    void setup() {
        AuthUsers user = AuthUsers.builder()
                .userId(testUserId)
                .password(passwordEncoder.encode(testPassword))
                .role(Role.ROLE_USER)
                .build();
        authUserRepository.save(user);
    }

    @Test
    void login_success() throws Exception {
        LoginReqDto loginReqDto = new LoginReqDto(testUserId, testPassword);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReqDto)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("accessToken"));

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken"));
    }

    @Test
    void login_fail_wrongPassword() throws Exception {
        LoginReqDto loginReqDto = new LoginReqDto(testUserId, "wrongpw123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReqDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    void refresh_success() throws Exception {
        // 1. 로그인 후 쿠키 획득
        LoginReqDto loginReqDto = new LoginReqDto(testUserId, testPassword);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReqDto)))
                .andExpect(status().isOk())
                .andReturn();

        // 2. Set-Cookie에서 refreshToken 값만 추출
        String setCookie = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        String refreshTokenValue = Arrays.stream(setCookie.split(";"))
                .findFirst() // "refreshToken=eyJ0eXAiOiJKV..."
                .orElseThrow()
                .split("=")[1]; // 값만 추출

        // 3. Refresh 요청 시 MockMvc cookie() 사용
        Cookie refreshCookie = new Cookie("refreshToken", refreshTokenValue);

        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                        .cookie(refreshCookie)) // 이름=값 형태로 안전하게 전달
                .andExpect(status().isOk())
                .andReturn();

        // 4. 응답 확인
        String refreshResponse = refreshResult.getResponse().getContentAsString();
        assertTrue(refreshResponse.contains("accessToken"));
    }

    @Test
    void refresh_fail_noCookie() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isBadRequest()); // 혹은 컨트롤러에서 처리한 401/400
    }

    @Test
    void refresh_fail_tokenNotInDB() throws Exception {
        // 쿠키만 만들어서 DB에는 없음
        Cookie fakeCookie = new Cookie("refreshToken", "fake.token.value");

        mockMvc.perform(post("/auth/refresh")
                        .cookie(fakeCookie)) // cookie() 사용
                .andExpect(status().is4xxClientError());
    }

    @Test
    void refresh_fail_tokenMismatch() throws Exception {
        // 1. 로그인 후 쿠키 획득
        LoginReqDto loginReqDto = new LoginReqDto(testUserId, testPassword);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReqDto)))
                .andExpect(status().isOk())
                .andReturn();

        // 2. Set-Cookie에서 실제 토큰 값만 추출
        String setCookie = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        String originalTokenValue = Arrays.stream(setCookie.split(";"))
                .findFirst()
                .orElseThrow()
                .split("=")[1];

        // 3. 쿠키 값 변조 → DB 토큰과 불일치
        Cookie fakeCookie = new Cookie("refreshToken", "fake.invalid.token");

        mockMvc.perform(post("/auth/refresh")
                        .cookie(fakeCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_success() throws Exception {
        LoginReqDto loginReqDto = new LoginReqDto(testUserId, testPassword);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReqDto)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        MvcResult logoutResult = mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String setCookie = logoutResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken=;"));
        assertTrue(refreshTokenRepository.findByUsername(testUserId).isEmpty());
    }

    @Test
    void logout_fail_invalidToken() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }
}
