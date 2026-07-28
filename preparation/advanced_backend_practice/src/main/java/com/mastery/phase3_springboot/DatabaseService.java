package com.mastery.phase3_springboot;

import org.springframework.stereotype.Service;

@Service
public class DatabaseService {
    @FakeTransactional
    public void saveDataToDB() {
        System.out.println("💾 OrderService: Bắt đầu insert xuống Database...");
        throw new RuntimeException("Lỗi bất ngờ sập Database!");
    }
}
