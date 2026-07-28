# 🟢 Bài 1: Quản lý Bộ Nhớ, Pass-by-Value & Immutability trong Java

Để tối ưu và viết code Thread-safe (an toàn đa luồng), chúng ta cần hiểu rõ cách Java lưu trữ dữ liệu.

---

## 1. Stack và Heap (Bộ Nhớ Trong Java)

Khi một chương trình Java chạy, bộ nhớ được chia làm 2 vùng chính:

* **Stack Memory (Ngăn xếp):**
  * Lưu trữ các biến nguyên thủy (primitives: `int`, `boolean`, `double`...).
  * Lưu trữ **Tham chiếu** (References) trỏ tới các Object.
  * Mỗi Thread (luồng) sẽ có một vùng Stack riêng biệt (Thread-safe tự nhiên).
* **Heap Space (Đống):**
  * Nơi lưu trữ **thực thể của tất cả các Object** (được tạo bằng từ khóa `new`).
  * Tất cả các Thread đều chia sẻ chung vùng Heap này (Đây là lý do gây ra lỗi xung đột đa luồng).
  * Garbage Collector (GC) hoạt động ở đây để dọn dẹp các Object không còn reference nào trỏ tới.

---

## 2. Bản chất: Java CHỈ CÓ Pass-by-Value

Rất nhiều lập trình viên nhầm lẫn rằng Java truyền tham số nguyên thủy bằng giá trị (Pass-by-value) và truyền Object bằng tham chiếu (Pass-by-reference). **Sự thật là Java chỉ có Pass-by-value.**

* **Đối với kiểu nguyên thủy:** Java copy chính xác giá trị đó (ví dụ copy số `5`) truyền vào hàm.
* **Đối với Object:** Java **không copy Object**, mà nó **copy cái Tham chiếu (địa chỉ bộ nhớ)** đang trỏ tới Object đó.

### 📝 Code Example chứng minh

```java
public class PassByValueExample {

    public static void main(String[] args) {
        User user1 = new User("Nam");
        
        // 1. Thử đổi tên User -> Thành công
        mutateUser(user1);
        System.out.println(user1.name); // In ra: "Alex"
        
        // 2. Thử trỏ User sang object khác -> Thất bại
        reassignUser(user1);
        System.out.println(user1.name); // Vẫn in ra: "Alex" (Không phải "Bob")
    }

    // Hàm này THÀNH CÔNG vì ta dùng bản sao của tham chiếu 
    // để đi tới đúng object trên Heap và thay đổi thuộc tính của nó.
    public static void mutateUser(User u) {
        u.name = "Alex"; 
    }

    // Hàm này THẤT BẠI vì 'u' ở đây chỉ là một BẢN SAO của địa chỉ bộ nhớ.
    // Việc trỏ bản sao này sang một Object mới hoàn toàn không ảnh hưởng tới 'user1' ở hàm main.
    public static void reassignUser(User u) {
        u = new User("Bob"); 
    }
}
```

**💡 Tóm lại:** Bạn có thể thay đổi trạng thái của một Object khi truyền vào hàm, nhưng bạn không thể thay đổi cái mà biến gốc đang trỏ tới.

---

## 3. Mutability vs. Immutability (Thay đổi vs Bất biến)

### Mutability (Có thể thay đổi)

Là những Object sau khi khởi tạo xong, bạn có thể gọi các hàm `setter` để thay đổi thuộc tính của nó (Giống ví dụ `User` ở trên).

* **Nhược điểm lớn nhất:** Nếu 2 Threads cùng truy cập một Object ở vùng Heap và cùng sửa dữ liệu, chương trình sẽ gặp lỗi "Race Condition" (Kết quả không thể đoán trước) => Không an toàn!

### Immutability (Không thể thay đổi - Bất biến)

Một Object Immutable là một Object **không bao giờ thay đổi trạng thái** kể từ giây phút nó được khởi tạo.

* **Ví dụ chuẩn mực:** `String` trong Java là immutable.
* **Lợi ích tuyệt đối:** **Thread-safe**. Khi một thứ không thể thay đổi, bạn có thể cho 1000 Threads đọc nó cùng lúc mà không cần phải dùng `synchronized` hay `Lock` (giúp tăng performance rất mạnh).

### 🛠 Cách tạo một Immutable Class chuyên nghiệp

Để tạo ra một class hoàn toàn bất biến, bạn tuân thủ các quy tắc sau:

1. Class phải là `final` (Không cho phép kế thừa để ghi đè behavior).
2. Tất cả properties (field) phải là `private` và `final`.
3. Chỉ khởi tạo giá trị qua Constructor (Không có setter).
4. Nếu class chứa tham chiếu đến một đối tượng Mutabe (VD: `List`, `Date`), bạn phải return ra "bản sao" (Defensive Copy) của đối tượng đó trong hàm getter.

```java
import java.util.ArrayList;
import java.util.List;

// 1. Đánh dấu class là final
public final class ImmutableUser {
    
    // 2. Các field là private và final
    private final String name;
    private final List<String> roles;

    // 3. Set dữ liệu 1 lần duy nhất qua Constructor
    public ImmutableUser(String name, List<String> roles) {
        this.name = name;
        // Thực hiện Defensive Copy (Tạo ra mảng mới) để tránh bị sửa mảng từ bên ngoài
        this.roles = new ArrayList<>(roles);
    }

    public String getName() {
        return name;
    }

    // 4. Getter của object bên trong cũng phải return một bản sao (hoặc dùng thư viện ImmutableList)
    public List<String> getRoles() {
        return new ArrayList<>(roles);
    }
}
```

### 🚀 Tính năng mới: Java Records (Java 14+)

Từ Java 14 trở đi, bạn không cần phải viết đống code dài ngoằng như trên. Java cung cấp `record` để tạo ra một Immutable class trong 1 dòng:

```java
public record UserRecord(String name, int age) {
    // Nó tự động cung cấp constructor, getter, equals, hashCode và toString.
    // Tất cả các field đều mặc định là private final.
}
```

---

## 🎯 Bài tập nhỏ để xác nhận bạn đã nắm rõ

### 1. Tại sao nói `String` trong Java là Immutable? Nếu mình gọi `String s = "Hello"; s = s + " World";` thì chuyện gì đã xảy ra dưới vùng nhớ Heap?

👉 **Trả lời chi tiết:**

* Biến `s` nằm ở vùng **Stack** và đóng vai trò là một con trỏ (Tham chiếu) trỏ tới vùng **Heap**, nơi chứa một mảng byte/char thực sự mang chữ `"Hello"`.
* Khi gọi `s = s + " World"`, Java đi xuống vùng Heap, tạo ra một Object mới toanh chứa chữ `"Hello World"`. Sau đó, con trỏ `s` ở vùng Stack tự động dời đi (reassign) và trỏ vào Object mới này.
* Object `"Hello"` cũ vẫn nằm nguyên đó ở vùng Heap (không hề bị sửa đổi).
* 🔥 **Deep Dive (String Pool):** Sở dĩ String buộc phải thiết kế là Immutable để hỗ trợ **String Pool** (Hồ chứa bộ nhớ). Nếu bạn khai báo `String a = "Nam";` và `String b = "Nam";`, Java sẽ không tạo 2 object, mà cho cả `a` và `b` cùng trỏ vào 1 object trong String Pool để tiết kiệm RAM. Nếu String mà đổi được, sửa `a` sẽ vô tình làm `b` bị lỗi theo!

### 2. Trong thiết kế kiến trúc Backend, bạn sẽ dùng Immutable object cho những thành phần nào? (Ví dụ: Request/Response DTOs, hay Entity Database?)

👉 **Trả lời chi tiết:**

* **Nơi NÊN dùng Immutable (Sử dụng `record` hoặc final class):**
  * **Request / Response DTOs:** Rất quan trọng! Dữ liệu nhận từ Client không bao giờ được phép thay đổi giữa chừng khi truyền từ Controller xuống Service để đảm bảo tính toàn vẹn (Data Integrity).
  * **Value Objects (DDD):** Các object như Toạ độ GPS, Tiền tệ (Money), Address.
  * **Config Properties:** Các biến cấu hình từ hệ thống.
* **Nơi KHÔNG dùng Immutable:**
  * **Database Entities:** Cần có setter (Mutable) để hỗ trợ thao tác update data xuống database hoặc hỗ trợ tính năng Dirty Checking của Hibernate.
* *(Lưu ý: Service, Controller, Repository không được gọi là Immutable, mà gọi là **Stateless Components** - Thành phần không lưu trạng thái).*
