package com.mastery.phase1_core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Ex03_VirtualThreadDemo {

    public static void main(String[] args) {
        System.out.println("=== SO SÁNH NỀN TẢNG THREADS (PLATFORM vs VIRTUAL) ===");

        int numberOfTasks = 10_000; // Giả lập 10 ngàn yêu cầu tải file

        // Cảnh báo: Chạy hàm testPlatformThreads() trên máy yếu có thể gây treo máy
        // hoặc văng lỗi OutOfMemory
        // Bạn có thể comment/uncomment để test từng loại

        testPlatformThreads(numberOfTasks);
        testVirtualThreads(numberOfTasks);

        /*
         * 🎯 BÀI TẬP THỰC HÀNH:
         * 1. Hãy quan sát thời gian chạy của cả 2 phương pháp. (Platform Threads do
         * dùng pool 1000
         * nên sẽ mất khoảng 10 giây để xử lý 10,000 task 1 giây).
         * 2. Virtual Threads sẽ xử lý xong gần như trong 1 giây (nhanh gấp 10 lần).
         * 3. [Tư duy] Nếu hàm simulateIoBoundTask() không dùng Thread.sleep, mà dùng
         * một vòng lặp
         * vô tận `while(true)` hoặc mã hóa mật khẩu cực nặng (CPU-Bound). Chuyện tồi tệ
         * gì sẽ xảy ra
         * với Virtual Threads? (Gợi ý: Hiện tượng Pinning).
         */
    }

    private static void testPlatformThreads(int numberOfTasks) {
        System.out.println("\nĐang khởi chạy " + numberOfTasks + " Platform Threads (OS Threads)...");
        long start = System.currentTimeMillis();

        try (ExecutorService executor = Executors.newFixedThreadPool(1000)) {
            for (int i = 0; i < numberOfTasks; i++) {
                executor.submit(Ex03_VirtualThreadDemo::simulateIoBoundTask);
            }
        } // Executor tự động chờ tất cả luồng hoàn thành (AutoCloseable)

        long end = System.currentTimeMillis();
        System.out.println("Platform Threads hoàn thành trong: " + (end - start) + " ms");
    }

    private static void testVirtualThreads(int numberOfTasks) {
        System.out.println("\nĐang khởi chạy " + numberOfTasks + " Virtual Threads...");
        long start = System.currentTimeMillis();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < numberOfTasks; i++) {
                executor.submit(Ex03_VirtualThreadDemo::simulateIoBoundTask);
            }
        }

        long end = System.currentTimeMillis();
        System.out.println("Virtual Threads hoàn thành trong: " + (end - start) + " ms");
    }

    // Giả lập một tác vụ gọi API/Database bị Blocking mất đúng 1 giây
    private static void simulateIoBoundTask() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/*
 * === SO SÁNH NỀN TẢNG THREADS (PLATFORM vs VIRTUAL) ===
 * 
 * Đang khởi chạy 10000 Platform Threads (OS Threads)...
 * Platform Threads hoàn thành trong: 10083 ms
 * 
 * Đang khởi chạy 10000 Virtual Threads...
 * Virtual Threads hoàn thành trong: 1036 ms
 * 
 * ==========================================
 * 💡 LỜI GIẢI THÍCH CHI TIẾT TỪ CHUYÊN GIA:
 * ==========================================
 * Kết quả này chứng minh sức mạnh cách mạng của Virtual Threads (Java 21) đối với các tác vụ I/O-Bound.
 * 
 * 1. Tại sao Platform Threads mất tận 10 giây?
 * - Hệ điều hành (OS) không cho phép bạn tạo 10,000 luồng thật vì mỗi luồng cắn 1MB RAM (sẽ ngốn 10GB RAM) và chi phí tạo luồng rất đắt đỏ.
 * - Do đó, ta phải dùng Thread Pool giới hạn 1000 luồng (`newFixedThreadPool(1000)`).
 * - Khi 1000 luồng đầu tiên chạy, nó gọi `Thread.sleep(1000)` (Giả lập việc chờ Database/Network trả kết quả). Cả 1000 luồng này bị "Khóa" (Block) trong 1 giây, hoàn toàn đứng im không làm gì cả nhưng cũng cương quyết KHÔNG nhả CPU cho người khác.
 * - Chờ xong 1 giây, nó mới giải phóng để 1000 luồng tiếp theo được chạy. Vậy 10,000 requests chia cho 1000 luồng = 10 đợt. Mỗi đợt 1 giây => Mất tổng cộng 10 giây!
 * 
 * 2. Tại sao Virtual Threads chỉ mất 1 giây?
 * - Virtual Thread cực kỳ nhẹ (chỉ tốn vài Byte). Bạn có thể khởi tạo hàng triệu cái tạo ngay lập tức mà không sợ hết RAM. Bạn tuyệt đối KHÔNG cần dùng Pool.
 * - Khi 10,000 Virtual Threads cùng gọi `Thread.sleep(1000)`, máy ảo JVM siêu thông minh phát hiện ra hành động "Chờ đợi" (I/O Blocking). 
 * - JVM lập tức "Tháo dỡ" (Unmount) Virtual Thread đó ra khỏi luồng vật lý của CPU, ném nó vào RAM, và nhường CPU cho Virtual Thread khác bay lên chạy tiếp.
 * - Kết quả: CPU lướt qua khởi động cả 10,000 Virtual Threads chỉ trong nháy mắt (vì đứa nào cũng vừa vào chạy là bị bế ra ngoài chờ). Tất cả 10,000 luồng cùng nằm chờ Database 1 giây song song với nhau. Hết 1 giây, tất cả cùng thức dậy => Tổng thời gian hoàn thành chỉ đúng ~1 giây!
 * 
 * ⚠️ LƯU Ý TỬ HUYỆT (HIỆN TƯỢNG PINNING):
 * - Nếu bạn đổi `Thread.sleep` thành một hàm chạy vòng lặp mã hóa mật khẩu Bcrypt (CPU-Bound).
 * - Lúc này hàm yêu cầu CPU phải tính toán liên tục, KHÔNG HỀ CÓ THỜI GIAN CHỜ (Không Blocking).
 * - JVM thấy nó đang tính toán nên không thể Unmount được. Hậu quả là Virtual Thread sẽ "Ghim chặt" (Pinning) vào Carrier Thread (luồng vật lý) bên dưới.
 * - Nếu server của bạn có 16 luồng vật lý, chỉ cần 16 user cùng đăng ký tài khoản (chạy Bcrypt), 16 luồng vật lý sẽ bị ghim cứng. 9984 Virtual Threads còn lại trong server sẽ bị "chết đói" (Starvation) vì không còn CPU vật lý nào rảnh để cõng chúng nó. Hệ thống sẽ tê liệt hoàn toàn!
 * 
 * 🌟 BÀI HỌC KIẾN TRÚC:
 * - Gọi Database, Gửi Email, Gọi API ngoài (I/O-Bound) -> DÙNG VIRTUAL THREADS để tiết kiệm cực độ phần cứng.
 * - Tính toán AI, mã hóa mật khẩu, xử lý hình ảnh (CPU-Bound) -> BẮT BUỘC DÙNG PLATFORM THREAD POOL GIỚI HẠN.
 */