package com.example.authdemo.securities;

import com.example.authdemo.dtos.UserResponse;
import com.example.authdemo.services.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

        private final OAuth2AuthorizedClientService authorizedClientService;
        private final JwtService jwtService;
        private final UserService userService;

        @Value("${app.frontend-auth-callback-url}")
        private String frontendAuthCallbackUrl;

        @Value("${app.jwt.expiration-minutes}")
        private long jwtExpirationMinutes;

        @Override
        public void onAuthenticationSuccess(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Authentication authentication) throws IOException, ServletException {
                OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                OAuth2User oauthUser = oauthToken.getPrincipal();
                OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                                oauthToken.getAuthorizedClientRegistrationId(),
                                oauthToken.getName());

                String username = oauthUser.getAttribute("login");
                String githubToken = client.getAccessToken().getTokenValue();
                printGitHubOAuthResult(oauthToken, oauthUser, client);

                UserResponse user = userService.saveGitHubLogin(username, githubToken);
                String jwtToken = jwtService.createToken(user);
                ResponseCookie authCookie = ResponseCookie.from(JwtService.AUTH_TOKEN_COOKIE_NAME, jwtToken)
                                .httpOnly(true)
                                .secure(false)
                                .sameSite("Lax")
                                .path("/")
                                .maxAge(Duration.ofMinutes(jwtExpirationMinutes))
                                .build();

                response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());
                response.sendRedirect(frontendAuthCallbackUrl);
        }

        private void printGitHubOAuthResult(
                        OAuth2AuthenticationToken oauthToken,
                        OAuth2User oauthUser,
                        OAuth2AuthorizedClient client) {
                log.info("GitHub OAuth registrationId: {}", oauthToken.getAuthorizedClientRegistrationId());
                log.info("GitHub OAuth principalName: {}", oauthToken.getName());
                log.info("GitHub OAuth authorities: {}", oauthToken.getAuthorities());
                log.info("GitHub OAuth userNameAttribute: {}", oauthUser.getName());

                for (Map.Entry<String, Object> attribute : oauthUser.getAttributes().entrySet()) {
                        log.info("GitHub OAuth user attribute {}: {}", attribute.getKey(), attribute.getValue());
                }

                log.info("GitHub OAuth access token type: {}", client.getAccessToken().getTokenType().getValue());
                log.info("GitHub OAuth access token scopes: {}", client.getAccessToken().getScopes());
                log.info("GitHub OAuth access token issuedAt: {}", client.getAccessToken().getIssuedAt());
                log.info("GitHub OAuth access token expiresAt: {}", client.getAccessToken().getExpiresAt());
                log.info("GitHub OAuth access token preview: {}", previewToken(client.getAccessToken().getTokenValue()));
        }

        private String previewToken(String token) {
                if (token == null || token.length() < 12) {
                        return "[redacted]";
                }

                return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
        }
}
