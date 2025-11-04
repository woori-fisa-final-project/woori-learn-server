package dev.woori.wooriLearn.config.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try{
            log.info("REQUEST [{}][{}]", request.getMethod(), request.getRequestURI());
            chain.doFilter(request, response);
        }catch(Exception e){
            throw e;
        }finally{
            long duration = System.currentTimeMillis() - startTime;
            log.info("RESPONSE [{}] [{}] returned {} ({} ms)",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
        }
    }
}
