package com.mastery.phase3_springboot;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class CounterService {

    // 🟢 ĐÃ SỬA LỖI: Sử dụng AtomicInteger để đảm bảo an toàn luồng (Thread-safe) trong Singleton
    private AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        /*
         * 💡 TẠI SAO ATOMIC INTEGER LẠI CHỐNG ĐƯỢC RACE CONDITION?
         * - Phép toán `count++` của biến int nguyên thủy nhìn thì có vẻ đơn giản, nhưng bên dưới máy ảo 
         *   nó bị băm thành 3 bước rời rạc: [Đọc giá trị] -> [Cộng thêm 1] -> [Ghi lại]. 
         *   Nếu 2 luồng cùng chạy vào ngay khoảnh khắc nó đang "Đọc", chúng sẽ ghi đè kết quả của nhau (gây hụt số đếm).
         * - Khi dùng `count.incrementAndGet()`, Java sử dụng cơ chế CAS (Compare-And-Swap) khóa chốt ở 
         *   tận cấp độ phần cứng của CPU. Nó gom 3 bước Đọc-Cộng-Ghi đó thành MỘT KHỐI HÀNH ĐỘNG NGUYÊN TỬ (Atomic) 
         *   duy nhất và không thể bị luồng khác xen ngang. Nhờ vậy, 100,000 requests đều được đếm đủ mà 
         *   không cần phải khóa toàn bộ hàm bằng từ khóa `synchronized` (giữ cho hiệu năng cực kỳ nhanh).
         */
        count.incrementAndGet(); 
    }

    public int getCount() {
        return count.get();
    }

    public void reset() {
        count.set(0);
    }
}
