package com.mastery.phase2_security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class JwtController {

    @GetMapping("/public")
    public String getPublicData() {
        return "Xin chào! Đây là dữ liệu Public. Ai cũng có thể vào được (Không cần JWT).";
    }

    @GetMapping("/secure-data")
    public Map<String, Object> getSecureData(@AuthenticationPrincipal Jwt jwt) {
        // Nhờ kiến trúc Stateless, ta trích xuất dữ liệu trực tiếp từ Token mà không cần gọi Database
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Chào mừng bạn đã lọt qua lớp SecurityFilterChain!");
        response.put("token_id", jwt.getId()); // Tham số này có thể dùng lưu vào Redis để làm Blacklist
        response.put("user_name", jwt.getClaimAsString("preferred_username"));
        response.put("email", jwt.getClaimAsString("email"));
        
        /*
         * 🎯 BÀI TẬP THỰC HÀNH (Security):
         * 1. Hãy thử gọi API GET http://localhost:8081/api/v1/secure-data từ Postman mà KHÔNG gài Token 
         *    xem có bị chặn ngay từ cổng bởi FilterChain (lỗi 401) không?
         * 2. Boot Docker Compose của Keycloak, đăng nhập lấy Token, ném vào Header: `Authorization: Bearer <token>`.
         * 3. Hãy lên trang web jwt.io, copy cái token ném vào xem có bị lộ toàn bộ thông tin Payload không?
         */
        
        return response;
    }
}
