package dev.woori.wooriLearn.config.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationEntryPointTest {

    // ASCII-only comment to keep source encoding safe.

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);

    @Test
    @DisplayName("BadCredentialsException 발생 시 TOKEN_UNAUTHORIZED 응답을 JSON으로 작성한다")
    void commence_writesUnauthorizedBody() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse res = new MockHttpServletResponse();

        entryPoint.commence(req, res, new BadCredentialsException("bad"));

        assertEquals(401, res.getStatus());
        String body = res.getContentAsString();
        assertTrue(body.contains("\"code\":" + ErrorCode.TOKEN_UNAUTHORIZED.getCode()));
        assertTrue(body.contains(ErrorCode.TOKEN_UNAUTHORIZED.getMessage()));
    }
}
