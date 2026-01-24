package com.rlaqjant.miniature_backlog_api.security.jwt;

import com.rlaqjant.miniature_backlog_api.security.userdetails.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * 모든 요청에서 JWT 토큰을 검증하고 인증 정보를 설정
 * 쿠키 또는 Authorization 헤더에서 토큰 추출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final JwtCookieUtil jwtCookieUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 요청에서 JWT 토큰 추출
        String token = resolveToken(request);

        // 토큰이 유효하면 인증 정보 설정
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);

            UserDetails userDetails = userDetailsService.loadUserByEmail(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("인증 정보 설정 완료: {}", email);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 토큰 추출 (우선순위: 쿠키 > Authorization 헤더)
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. 쿠키에서 토큰 추출 시도
        String token = jwtCookieUtil.getTokenFromCookies(request);
        if (StringUtils.hasText(token)) {
            return token;
        }

        // 2. Authorization 헤더에서 토큰 추출 (하위 호환성)
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
