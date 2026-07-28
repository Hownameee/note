package com.mastery.phase1_core;

import java.util.ArrayList;
import java.util.List;

public class Ex02_StreamParallel {

    public static void main(String[] args) {
        System.out.println("=== SO SÁNH STREAM THƯỜNG VÀ PARALLEL STREAM ===");

        // Chuẩn bị 1 triệu bản ghi ảo
        int size = 2_000_000;
        List<Integer> numbers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            numbers.add(i);
        }

        System.out.println("Bắt đầu xử lý (Đếm các số nguyên tố trong 2 triệu số)...");

        // 1. Dùng Stream Tuần tự (Sequential)
        long startTime = System.currentTimeMillis();
        long count1 = numbers.stream()
                .filter(Ex02_StreamParallel::isPrime) // Thuật toán tốn CPU
                .count();
        long endTime = System.currentTimeMillis();
        System.out.println(
                "Thời gian chạy Stream Thường (1 CPU Core): " + (endTime - startTime) + " ms. Kết quả: " + count1);

        // 2. Dùng Parallel Stream (Work-stealing trên nhiều CPU Cores)
        long startTimeParallel = System.currentTimeMillis();
        long count2 = numbers.parallelStream() // CHỈ KHÁC BIỆT ĐÚNG 1 CHỮ NÀY
                .filter(Ex02_StreamParallel::isPrime)
                .count();
        long endTimeParallel = System.currentTimeMillis();
        System.out.println("Thời gian chạy Parallel Stream (Nhiều CPU Cores): " + (endTimeParallel - startTimeParallel)
                + " ms. Kết quả: " + count2);

        /*
         * 🎯 BÀI TẬP THỰC HÀNH:
         * 1. Hãy Run file này và quan sát thời gian chạy (chênh lệch rất lớn).
         * 2. Thử đổi ruột hàm filter thành `n % 2 == 0` (thay vì gọi hàm isPrime tốn
         * sức).
         * Hãy chạy lại và xem điều kỳ lạ gì xảy ra? Lúc này Parallel Stream có còn
         * nhanh hơn không? Tại sao?
         */
    }

    // Hàm mô phỏng công việc tính toán nặng (CPU-Bound task)
    private static boolean isPrime(int number) {
        // if (number <= 1) return false;
        // for (int i = 2; i <= Math.sqrt(number); i++) {
        // if (number % i == 0) return false;
        // }
        // return true;
        return number % 2 == 0;
    }
}

/*
 * === SO SÁNH STREAM THƯỜNG VÀ PARALLEL STREAM ===
 * Bắt đầu xử lý (Đếm các số nguyên tố trong 2 triệu số)...
 * Thời gian chạy Stream Thường (1 CPU Core): 272 ms. Kết quả: 148933
 * Thời gian chạy Parallel Stream (Nhiều CPU Cores): 71 ms. Kết quả: 148933
 * 
 * === SO SÁNH STREAM THƯỜNG VÀ PARALLEL STREAM ===
 * Bắt đầu xử lý (Đếm các số nguyên tố trong 2 triệu số)...
 * Thời gian chạy Stream Thường (1 CPU Core): 13 ms. Kết quả: 1000000
 * Thời gian chạy Parallel Stream (Nhiều CPU Cores): 52 ms. Kết quả: 1000000
 * 
 * ==========================================
 * 💡 LỜI GIẢI THÍCH CHI TIẾT TỪ CHUYÊN GIA:
 * ==========================================
 * Kết quả bạn vừa chạy ra chính là minh chứng kinh điển cho nguyên lý: 
 * "KHÔNG PHẢI CỨ CHẠY ĐA LUỒNG (PARALLEL) LÀ NHANH HƠN".
 * 
 * 1. Khi chạy hàm `isPrime()` (Kịch bản 1):
 * - Đây là một tác vụ tính toán phức tạp (CPU-Bound heavy). 
 * - Thời gian để CPU kiểm tra từng số là khá lâu. Việc chia nhỏ mảng ra cho nhiều nhân CPU cùng làm mang lại lợi ích khổng lồ, bù đắp dư sức cho thời gian Java phải quản lý luồng.
 * -> Parallel Stream thắng tuyệt đối (71ms vs 272ms).
 * 
 * 2. Khi chạy phép tính `n % 2 == 0` (Kịch bản 2):
 * - Phép chia lấy dư là một phép toán phần cứng cực kỳ đơn giản. MỘT NHÂN CPU (Single Thread) dư sức lướt qua 2 triệu số chỉ trong chớp mắt (13ms).
 * - NHƯNG khi bạn ép nó dùng `parallelStream()`, hệ thống dưới nền (ForkJoinPool) phải làm một đống các "Thủ tục hành chính": 
 *   + Cắt mảng 2 triệu phần tử thành các khối nhỏ (Fork).
 *   + Điều phối và giao việc cho các luồng.
 *   + Bắt CPU thực hiện Context Switch (đảo luồng liên tục).
 *   + Khóa bộ nhớ, gom và cộng gộp kết quả từ nhiều luồng lại (Join).
 * - Chống chất đống "Chi phí quản lý" (Overhead cost) này tốn tới gần 40ms, trong khi thời gian thực tế để tính toán chỉ mất chưa tới 10ms! 
 * -> Kết quả là Parallel Stream trở nên cồng kềnh và chậm chạp hơn (52ms vs 13ms).
 * 
 * 🌟 BÀI HỌC KIẾN TRÚC RÚT RA:
 * Tuyệt đối KHÔNG LẠM DỤNG `parallelStream()`. Chỉ sử dụng khi thỏa mãn CẢ 2 điều kiện:
 * 1. Số lượng phần tử cực kỳ lớn.
 * 2. Nghiệp vụ bên trong hàm xử lý phải thực sự phức tạp và ngốn nhiều CPU.
 */