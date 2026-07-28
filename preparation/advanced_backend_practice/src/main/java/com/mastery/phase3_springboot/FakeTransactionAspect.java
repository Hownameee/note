package com.mastery.phase3_springboot;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FakeTransactionAspect {

    // Lắng nghe (Intercept) tất cả các hàm có gắn @FakeTransactional
    // Hoạt động y hệt cơ chế Proxy của Spring Security và Spring Data JPA
    @Around("@annotation(FakeTransactional)")
    public Object manageTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("🛡️ [PROXY] --- BẮT ĐẦU MỞ GIAO DỊCH DB (BEGIN TRANSACTION) ---");
        try {
            Object result = joinPoint.proceed(); // Nhường quyền cho hàm thật chạy
            System.out.println("🛡️ [PROXY] --- COMMIT GIAO DỊCH THÀNH CÔNG ---");
            return result;
        } catch (Exception e) {
            System.out.println("🛡️ [PROXY] 💥 PHÁT HIỆN LỖI! --- TỰ ĐỘNG ROLLBACK DB ---");
            throw e; // Ném ngược lỗi ra ngoài
        }
    }
}
