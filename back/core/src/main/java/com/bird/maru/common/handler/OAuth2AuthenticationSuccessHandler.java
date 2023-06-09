package com.bird.maru.common.handler;

import com.bird.maru.common.redis.RedisCacheKey;
import com.bird.maru.common.util.JwtUtil;
import com.bird.maru.auth.service.dto.CustomUserDetails;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Authorization Code Grant 방식을 지원합니다. <br> OAuth 인증에 성공한 경우에 이 빈을 호출합니다.
 */
//@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;
    private final String redirectUri;

    public OAuth2AuthenticationSuccessHandler(
            RedisTemplate<String, String> redisTemplate,
            JwtUtil jwtUtil,
            @Value("${oauth2.success-client-url}") String redirectUri
    ) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
        this.redirectUri = redirectUri;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        CustomUserDetails member = (CustomUserDetails) authentication.getPrincipal();
        String accessToken = jwtUtil.generateAccessToken(member);
        String refreshToken = jwtUtil.generateRefreshToken(member);

        redisTemplate.opsForValue()
                     .set(
                             RedisCacheKey.REFRESH_TOKEN.getKey(member.getId()),
                             refreshToken,
                             jwtUtil.getRefreshTokenExpirationTime()
                     );

        getRedirectStrategy()
                .sendRedirect(request, response, getRedirectUrl(accessToken, refreshToken));
    }

    private String getRedirectUrl(String accessToken, String refreshToken) {
        return UriComponentsBuilder.fromUriString(redirectUri)
                                   .queryParam("accessToken", accessToken)
                                   .queryParam("refreshToken", refreshToken)
                                   .build()
                                   .toUriString();
    }

}
