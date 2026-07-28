package com.mastery.phase3_springboot;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/phase3")
public class Phase3Controller {

    private final CounterService counterService;
    private final OrderService orderService;

    public Phase3Controller(CounterService counterService, OrderService orderService) {
        this.counterService = counterService;
        this.orderService = orderService;
    }

    @GetMapping("/race-condition")
    public String triggerRaceCondition() throws InterruptedException {
        counterService.reset();
        int numberOfRequests = 100_000;
        
        System.out.println("🚦 Bắt đầu giả lập " + numberOfRequests + " request cùng lúc...");
        
        try (ExecutorService executor = Executors.newFixedThreadPool(100)) {
            for (int i = 0; i < numberOfRequests; i++) {
                executor.submit(() -> {
                    counterService.increment();
                });
            }
        } // Executor tự động chờ tất cả luồng hoàn thành

        int finalCount = counterService.getCount();
        System.out.println("Kết quả đếm được: " + finalCount);
        
        /*
         * 🎯 BÀI TẬP THỰC HÀNH (Race Condition):
         * 1. Hãy truy cập trình duyệt: http://localhost:8081/api/v1/phase3/race-condition
         * 2. Mục tiêu là gọi 100k lần, nhưng kết quả in ra chắc chắn bị thiếu.
         * 3. Hãy vào CounterService, thay int bằng AtomicInteger và làm lại để thấy sự kỳ diệu.
         */
        
        return "Gửi 100,000 requests. Bạn đếm được: " + finalCount + " (Mở console log để xem chi tiết)";
    }

    @GetMapping("/order")
    public String triggerSelfInvocation() {
        System.out.println("\n=== THỬ NGHIỆM ĐẶT HÀNG (SELF-INVOCATION) ===");
        orderService.placeOrder();
        return "Đã chạy thử đặt hàng. Hãy xem Console Log của Spring Boot để kiểm tra xem Transaction có được Rollback không!";
    }
}
