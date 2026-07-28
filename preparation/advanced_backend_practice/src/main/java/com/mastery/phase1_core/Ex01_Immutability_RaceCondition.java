package com.mastery.phase1_core;

import java.util.ArrayList;
import java.util.List;

public class Ex01_Immutability_RaceCondition {

    // 🔴 BẦY LỖI: Đây là một lớp Mutable (có thể thay đổi trạng thái).
    // Trong môi trường đa luồng, List roles có thể bị sửa đổi ngoài ý muốn.
    public static class MutableUser {
        private String name;
        private List<String> roles;

        public MutableUser(String name, List<String> roles) {
            this.name = name;
            // Bẫy kinh điển: Copy tham chiếu trực tiếp thay vì "Defensive Copy"
            this.roles = roles;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getRoles() {
            // Bẫy: Trả về trực tiếp tham chiếu của mảng.
            // Người ngoài lấy được mảng này có thể gọi .add() hoặc .clear() làm hỏng dữ
            // liệu gốc.
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== THỬ NGHIỆM LỖI MUTABLE OBJECT ===");

        List<String> initialRoles = new ArrayList<>();
        initialRoles.add("USER");

        ImmutableUser user = new ImmutableUser("Nam", initialRoles);

        // Luồng 1 (Thread A) - Cố gắng cấp quyền ADMIN
        Thread threadA = new Thread(() -> {
            List<String> roles = user.getRoles();
            roles.add("ADMIN"); // Sửa trực tiếp list lấy từ Object
            System.out.println("Thread A đã cấp quyền ADMIN.");
        });

        // Luồng 2 (Thread B) - Cố gắng xóa mọi quyền
        Thread threadB = new Thread(() -> {
            List<String> roles = user.getRoles();
            roles.clear(); // Vô tình hoặc cố ý xóa toàn bộ quyền
            System.out.println("Thread B đã xóa sạch quyền.");
        });

        threadA.start();
        threadB.start();
        threadA.join();
        threadB.join();

        System.out.println("Quyền hiện tại của User (Bị lỗi Race Condition): " + user.getRoles());

        /*
         * 🎯 BÀI TẬP THỰC HÀNH:
         * Dữ liệu đã bị phá hỏng vì các luồng cùng đâm vào sửa mảng `roles` của đối
         * tượng.
         * Yêu cầu: Bạn hãy tạo ra một class `ImmutableUser` (hoặc record
         * ImmutableUserRecord) bên dưới.
         * Khởi tạo class đó và làm sao để khi Thread A gọi getRoles().add("ADMIN"),
         * Java sẽ ném ra lỗi hoặc không làm ảnh hưởng tới cái mảng gốc của User.
         */
    }

    public static final class ImmutableUser {
        private final String name;
        private final List<String> roles;

        public ImmutableUser(String name, List<String> roles) {
            this.name = name;
            this.roles = new ArrayList<>(roles);
        }

        public String getName() {
            return name;
        }

        public List<String> getRoles() {
            return new ArrayList<String>(roles);
        }
    }
}
