package com.mastery.phase3_springboot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    @Autowired
    DatabaseService dbService;

    // Controller sẽ gọi vào hàm này. Hàm này KHÔNG có Proxy giao dịch
    public void placeOrder() {
        System.out.println("📦 OrderService: Đang xử lý đặt hàng...");

        // BẪY SELF-INVOCATION (Gọi hàm nội bộ qua this)
        // Spring hiểu ngầm là this.saveDataToDB().
        // Lệnh này không đi xuyên qua lớp Proxy ở ngoài, nên Aspect sẽ KHÔNG bắt được.
        try {
            dbService.saveDataToDB();
        } catch (Exception e) {
            System.out.println("📦 OrderService: Bắt được lỗi: " + e.getMessage());
        }
    }

    @FakeTransactional
    public void saveDataToDB() {
        System.out.println("💾 OrderService: Bắt đầu insert xuống Database...");
        throw new RuntimeException("Lỗi bất ngờ sập Database!");
        /*
         * 🎯 BÀI TẬP THỰC HÀNH (Self-Invocation Trap):
         * 1. Khi bạn gọi API /api/v1/phase3/order, hãy nhìn vào Console Log.
         * 2. Bạn sẽ thấy chữ "Bắt đầu insert xuống DB" và Exception bị văng ra.
         * 3. NHƯNG bạn TUYỆT ĐỐI KHÔNG thấy dòng log "TỰ ĐỘNG ROLLBACK DB" của Aspect!
         * => Giao dịch đã bị phá hỏng vì bạn gọi nội bộ.
         * 4. Yêu cầu:
         * - Tạo một file `DatabaseService.java` kế bên file này.
         * - Di dời hàm saveDataToDB sang file đó.
         * - Inject DatabaseService vào OrderService và gọi nó thay vì
         * this.saveDataToDB().
         * - Bạn sẽ thấy log báo ROLLBACK xuất hiện vì luồng code đã đi ra ngoài xuyên
         * qua Proxy!
         */
    }
}

/*
 * === THỬ NGHIỆM ĐẶT HÀNG (SELF-INVOCATION) ===
 * 📦 OrderService: Đang xử lý đặt hàng...
 * 🛡️ [PROXY] --- BẮT ĐẦU MỞ GIAO DỊCH DB (BEGIN TRANSACTION) ---
 * 💾 OrderService: Bắt đầu insert xuống Database...
 * 🛡️ [PROXY] 💥 PHÁT HIỆN LỖI! --- TỰ ĐỘNG ROLLBACK DB ---
 * 📦 OrderService: Bắt được lỗi: Lỗi bất ngờ sập Database!
 */