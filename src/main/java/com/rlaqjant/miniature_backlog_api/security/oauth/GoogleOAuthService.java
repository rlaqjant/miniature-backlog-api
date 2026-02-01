package com.rlaqjant.miniature_backlog_api.security.oauth;

import com.rlaqjant.miniature_backlog_api.common.exception.BusinessException;
import com.rlaqjant.miniature_backlog_api.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Google OAuth 서비스
 * Authorization Code 방식으로 서버 사이드에서 토큰 교환 처리
 */
@Slf4j
@Service
public class GoogleOAuthService {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final RestTemplate restTemplate;

    public GoogleOAuthService(
            @Value("${google.client-id}") String clientId,
            @Value("${google.client-secret}") String clientSecret,
            @Value("${google.redirect-uri}") String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Google OAuth 동의 화면 URL 생성
     */
    public String generateAuthUrl(String state) {
        return GOOGLE_AUTH_URL
                + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode("openid email profile", StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=select_account";
    }

    /**
     * Authorization Code로 Google 토큰 교환 후 사용자 정보 반환
     *
     * @return 사용자 정보 (email, sub)
     */
    public GoogleUserInfo exchangeCodeForUserInfo(String code) {
        try {
            // 1. code → access_token 교환
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);
            params.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restTemplate.postForObject(
                    GOOGLE_TOKEN_URL, request, Map.class);

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                log.error("Google 토큰 교환 실패: 응답이 null이거나 access_token이 없음");
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED);
            }

            String accessToken = (String) tokenResponse.get("access_token");

            // 2. access_token으로 사용자 정보 조회
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> userInfoRequest = new HttpEntity<>(userInfoHeaders);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                    GOOGLE_USERINFO_URL, HttpMethod.GET, userInfoRequest, Map.class);

            Map<String, Object> userInfo = userInfoResponse.getBody();
            if (userInfo == null || !userInfo.containsKey("email") || !userInfo.containsKey("sub")) {
                log.error("Google 사용자 정보 조회 실패");
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED);
            }

            return new GoogleUserInfo(
                    (String) userInfo.get("email"),
                    (String) userInfo.get("sub")
            );

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google OAuth 처리 실패", e);
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED);
        }
    }

    /**
     * Google 사용자 정보
     */
    public record GoogleUserInfo(String email, String googleId) {
    }
}
