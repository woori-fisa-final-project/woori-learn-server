package dev.woori.wooriLearn.config.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);

    @Test
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
