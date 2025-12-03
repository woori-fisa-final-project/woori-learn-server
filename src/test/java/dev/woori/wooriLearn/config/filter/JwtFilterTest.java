package dev.woori.wooriLearn.config.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.config.jwt.JwtInfo;
import dev.woori.wooriLearn.config.jwt.JwtValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtFilterTest {

    private JwtValidator jwtValidator;
    private JwtFilter filter;

    @BeforeEach
    void setUp() {
        jwtValidator = mock(JwtValidator.class);
        filter = new JwtFilter(jwtValidator, new ObjectMapper());
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_setsAuthenticationAndContinuesChain() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token123");

        when(jwtValidator.parseToken("token123")).thenReturn(new JwtInfo("user1", dev.woori.wooriLearn.domain.auth.entity.Role.ROLE_USER));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user1", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertEquals(List.of("ROLE_USER"), SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().map(a -> a.getAuthority()).toList());
        verify(chain).doFilter(req, res);
    }

    @Test
    void invalidToken_writesErrorResponse() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad");
        when(jwtValidator.parseToken("bad")).thenThrow(new CommonException(ErrorCode.TOKEN_UNAUTHORIZED));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(ErrorCode.TOKEN_UNAUTHORIZED.getStatus().value(), res.getStatus());
        String body = res.getContentAsString();
        assertNotNull(body);
        // JSON contains code/message
        assert body.contains("\"code\":40102");
    }
}
