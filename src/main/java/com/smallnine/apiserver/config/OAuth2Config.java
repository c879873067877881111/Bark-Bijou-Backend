package com.smallnine.apiserver.config;

import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.entity.RefreshToken;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.RefreshTokenService;
import com.smallnine.apiserver.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Configuration
@Slf4j
public class OAuth2Config {

    @Bean
    public AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler(
            UserDao userDao,
            JwtUtil jwtUtil,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            @Value("${app.oauth2.redirect-uri}") String redirectUri) {

        return new OAuth2SuccessHandler(userDao, jwtUtil, refreshTokenService, passwordEncoder, redirectUri);
    }

    @Bean
    public AuthenticationFailureHandler oAuth2AuthenticationFailureHandler(
            @Value("${app.oauth2.redirect-uri}") String redirectUri) {

        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) -> {
            log.error("action=oauth2_login result=failed reason={}", exception.getMessage());
            String errorRedirect = redirectUri.replace("/oauth/callback", "/login")
                    + "?error=" + URLEncoder.encode("Google 登入失敗", StandardCharsets.UTF_8);
            response.sendRedirect(errorRedirect);
        };
    }

    @Slf4j
    @RequiredArgsConstructor
    static class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

        private final UserDao userDao;
        private final JwtUtil jwtUtil;
        private final RefreshTokenService refreshTokenService;
        private final PasswordEncoder passwordEncoder;
        private final String redirectUri;

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                            Authentication authentication) throws IOException {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String picture = oAuth2User.getAttribute("picture");
            String googleSub = oAuth2User.getAttribute("sub");

            User user = userDao.findByGoogleUid(googleSub)
                    .orElseGet(() -> userDao.findByEmail(email).orElse(null));

            if (user == null) {
                user = new User();
                user.setUsername("google_" + googleSub);
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                user.setRealname(name != null ? name : "Google User");
                user.setGoogleUid(googleSub);
                user.setGoogleName(name);
                user.setImageUrl(picture != null ? picture : "/member/member_images/user-img.svg");
                user.setEmailValidated(true);
                user.setGender(User.Gender.male);
                userDao.insert(user);
                log.info("action=oauth2_login new_user={} email={}", user.getUsername(), email);
            } else if (user.getGoogleUid() == null) {
                user.setGoogleUid(googleSub);
                user.setGoogleName(name);
                if (picture != null) {
                    user.setImageUrl(picture);
                }
                user.setEmailValidated(true);
                userDao.update(user);
                log.info("action=oauth2_login linked_user={} email={}", user.getUsername(), email);
            }

            String accessToken = jwtUtil.generateAccessToken(user.getUsername());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            String targetUrl = redirectUri
                    + "?token=" + accessToken
                    + "&refreshToken=" + refreshToken.getToken();

            log.info("action=oauth2_login user={} result=success", user.getUsername());
            response.sendRedirect(targetUrl);
        }
    }
}
