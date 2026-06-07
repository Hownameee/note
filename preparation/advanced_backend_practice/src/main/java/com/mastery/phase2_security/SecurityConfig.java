package com.mastery.phase2_security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Tắt CSRF vì REST API Stateless không xài Cookie
            // Cấu hình không lưu Session (Vì xài JWT là đã có đủ thông tin)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) 
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/public").permitAll() // API Mở không cần JWT
                .requestMatchers("/api/v1/secure-data").authenticated() // API bắt buộc có JWT hợp lệ
                .requestMatchers("/api/v1/phase3/**").permitAll() // Tạm mở cho Phase 3 thực hành AOP dễ dàng
                .anyRequest().permitAll()
            )
            // Biến hệ thống thành OAuth2 Resource Server (Hứng JWT và tự động mang đi so khớp chữ ký với Keycloak)
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {})); 

        return http.build();
    }
}
