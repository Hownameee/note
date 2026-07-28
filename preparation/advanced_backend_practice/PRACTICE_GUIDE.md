# Hướng Dẫn Thực Hành: Advanced Backend Mastery

Chào mừng bạn đến với môi trường thực hành. Thư mục này chứa một dự án Spring Boot tiêu chuẩn (Java 21, Maven).
Mục đích của dự án này KHÔNG PHẢI là để chạy lên một Web App hoàn chỉnh, mà là để **bạn trực tiếp quan sát và khắc phục các cạm bẫy hệ thống**.

## Cấu trúc thư mục

- `docker-compose.yml`: Dùng để khởi chạy Keycloak (cho Phase 2).
- `src/main/java/com/mastery/phase1_core`: Chứa các bài tập Java chạy qua hàm `public static void main`.
- `src/main/java/com/mastery/phase2_security`: Chứa cấu hình bảo mật JWT và API Test.
- `src/main/java/com/mastery/phase3_springboot`: Chứa bài tập Spring Boot (Lỗi Singleton & Lỗi AOP Self-invocation).

## Cách chạy bài tập

1. **Với Phase 1:** Bạn chỉ cần mở file `.java` tương ứng (Ví dụ: `Ex01_Immutability_RaceCondition.java`) trên IDE của bạn (IntelliJ, VSCode) và ấn nút `Run` ở hàm `main()`. Xem log in ra và đọc yêu cầu sửa code trong comment.
2. **Với Phase 2 & 3:** Bạn mở terminal, chạy lệnh `mvn spring-boot:run` để boot server ở cổng `8081`.

Sau khi Server Spring Boot đã chạy ổn định, mở một Terminal khác và thực hiện các lệnh cURL sau để kiểm chứng lý thuyết:

### Lệnh chạy Test Phase 2 (Kiến trúc Bảo mật JWT)

**1. Thử gọi API khi KHÔNG CÓ Token (Sẽ bị chặn lỗi 401)**
```bash
curl -i http://localhost:8081/api/v1/secure-data
```

**2. Gọi lên Keycloak xin cấp Token và tự động lưu vào biến `$TOKEN`**
```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/realms/unihub/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=frontend-app" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" | jq -r .access_token)
```

**3. Kẹp Token vào Header để đi xuyên qua Spring Security**
```bash
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/v1/secure-data
```

---

### Lệnh chạy Test Phase 3 (Cạm bẫy Spring Boot)

**1. Kích hoạt bẫy Race Condition (Mô phỏng 100,000 requests cùng lúc)**
```bash
curl -i http://localhost:8081/api/v1/phase3/race-condition
```
*💡 Giải thích: Khi bạn gọi lệnh này, kết quả in ra chắc chắn sẽ bị hao hụt (không bao giờ chạm mốc 100,000). Hãy vào `CounterService.java` đổi `int` thành `AtomicInteger` để tự tay sửa lỗi.*

**2. Kích hoạt bẫy Proxy Self-Invocation (Giao dịch DB không thể Rollback)**
```bash
curl -i http://localhost:8081/api/v1/phase3/order
```
*💡 Giải thích: Khi gọi lệnh này, hãy quay lại màn hình Terminal đang chạy Spring Boot để xem Log. Bạn sẽ thấy rớt Exception vì sập Database, nhưng **KHÔNG HỀ CÓ DÒNG LOG NÀO GHI CHỮ "TỰ ĐỘNG ROLLBACK DB"**. Đó là do bạn gọi `this.saveDataToDB()` nên giao dịch đã bị phá hỏng. Hãy dời hàm save sang một `DatabaseService` để fix lỗi.*
