# 🟢 Bài 2: Functional Interfaces, Lambda & Streams API

Trong Java, kể từ phiên bản 8 trở đi, lập trình viên Backend phải quen thuộc với phong cách lập trình **Declarative (Khai báo)** thay vì **Imperative (Mệnh lệnh)** kiểu cũ. Nó giúp code cực kỳ ngắn gọn, dễ đọc, và dễ dàng chuyển đổi sang chạy đa luồng.

---

## 1. Lập trình Khai báo (Declarative) vs Mệnh lệnh (Imperative)

Giả sử bạn có một danh sách `List<User>` và muốn lấy ra "Tên của những User có tuổi > 18".

* **Kiểu cũ (Imperative):** Bạn ra lệnh cho máy tính TỪNG BƯỚC MỘT (tạo list mới, lặp for, if check điều kiện, lấy tên, add vào list mới). Dài dòng, khó tái sử dụng và dễ có bug.
* **Kiểu mới (Declarative):** Bạn chỉ cần "khai báo" KẾT QUẢ BẠN MUỐN (Tôi cần filter theo tuổi, sau đó map lấy tên).

```java
// Code Declarative với Streams API
List<String> validNames = users.stream()
    .filter(u -> u.getAge() > 18)
    .map(User::getName)
    .toList(); // Từ Java 16, dùng toList() ngắn hơn Collectors.toList()
```

Sức mạnh đằng sau cú pháp ma thuật này chính là sự kết hợp giữa **Functional Interfaces** và **Lambda Expressions**.

---

## 2. Functional Interfaces là gì?

Functional Interface đơn giản là một Interface **chỉ có duy nhất MỘT abstract method**.
Nó thường được gắn annotation `@FunctionalInterface` để báo cho compiler kiểm tra nghiêm ngặt.

Tại sao phải sinh ra khái niệm này? Bởi vì để truyền được một đoạn code (như hàm nặc danh Lambda) vào làm tham số của một hàm khác (ví dụ truyền vào hàm `filter` ở trên), Java cần một "cái khuôn" chứa định nghĩa của hàm đó. Cụ thể hơn, Lambda expression thực chất là một cách viết tắt để implement cái method duy nhất của Functional Interface.

### 4 "Tứ trụ" Functional Interface trong thư viện `java.util.function`

Java đã định nghĩa sẵn 4 loại "khuôn" cơ bản nhất để bạn không cần phải tự viết interface:

1. **`Predicate<T>` (Bộ kiểm tra/Bộ lọc):**
    * Nhận vào 1 tham số kiểu `T`, trả về `boolean`.
    * *Dùng để:* Check điều kiện (ví dụ dùng trong hàm `filter()`).
    * *Ví dụ:* `Predicate<String> isLong = s -> s.length() > 5;`
2. **`Function<T, R>` (Bộ biến đổi):**
    * Nhận vào kiểu `T`, biến đổi và trả về kiểu `R`.
    * *Dùng để:* Chuyển đổi dữ liệu (ví dụ dùng trong hàm `map()`).
    * *Ví dụ:* `Function<User, String> toName = user -> user.getName();`
3. **`Consumer<T>` (Người tiêu thụ):**
    * Nhận vào 1 tham số kiểu `T`, KHÔNG trả về gì cả (`void`).
    * *Dùng để:* Thực hiện một action (như in ra màn hình, gửi log, save DB).
    * *Ví dụ:* `Consumer<String> print = s -> System.out.println(s);` (hay dùng trong `forEach()`).
4. **`Supplier<T>` (Nhà cung cấp):**
    * KHÔNG nhận tham số nào cả, luôn luôn tạo/trả về một object kiểu `T`.
    * *Dùng để:* Sinh dữ liệu lười (Lazy generation) hoặc ném Exception (`orElseThrow`).
    * *Ví dụ:* `Supplier<Double> randomGen = () -> Math.random();`

---

## 3. Deep Dive vào Streams API

Stream không phải là Cấu trúc dữ liệu (như List hay Set). Nó không lưu trữ dữ liệu. Nó là một **Đường ống (Pipeline)** dẫn dữ liệu chạy qua để xử lý.

### Quy tắc "Lazy Evaluation" (Đánh giá lười biếng) cực kỳ bá đạo

Mọi đường ống Stream đều bao gồm 2 loại operation (thao tác):

1. **Intermediate Operations (Thao tác Trung gian):** `filter()`, `map()`, `sorted()`, `distinct()`, `limit()`.
    * Chúng KHÔNG BAO GIỜ CHẠY ngay lập tức. Chúng chỉ "đứng đó và ghi nhận" yêu cầu cấu hình đường ống. Mỗi hàm sẽ trả về một cái Stream mới nối tiếp vào.
2. **Terminal Operations (Thao tác Kết thúc):** `collect()`, `toList()`, `forEach()`, `count()`, `findFirst()`.
    * Chỉ khi nào bạn gọi một hàm Terminal, "van nước" mới được mở ra. Lúc này các phần tử mới bắt đầu chạy dọc qua tất cả các phễu (intermediate) phía trên.

### 💡 Bài học "Under the hood": Tối ưu luồng chạy

Bạn hãy thử phân tích đoạn code sau:

```java
List<String> names = List.of("Anna", "Bob", "Charlie", "David");

String result = names.stream()
    .filter(name -> {
        System.out.println("Đang lọc: " + name);
        return name.length() > 3;
    })
    .map(name -> {
        System.out.println("Đang chuyển đổi: " + name);
        return name.toUpperCase();
    })
    .findFirst() // Terminal operation: Cần lấy 1 phần tử đầu tiên thoả mãn
    .orElse("Unknown");
```

**Luồng chạy thực tế:**
Nó **không duyệt hết toàn bộ cả list**!

1. Nước chảy tới "Anna" -> chạy qua `filter` (Đúng) -> qua `map` chuyển thành "ANNA" -> chảy tới `findFirst()`.
2. Hàm `findFirst()` thông báo: "Tôi tìm thấy phần tử đầu tiên rồi, DỪNG LẠI TẤT CẢ!".
3. Toàn bộ đường ống đóng lại. Bob, Charlie, David thậm chí không bao giờ bị chạm tới hay xử lý.
👉 Đây chính là sức mạnh tối ưu cực lớn của Streams (Short-circuiting).

---

## 4. Method Reference (Tham chiếu phương thức)

Thay vì viết hàm Lambda rườm rà như `user -> user.getName()`, bạn có thể viết gọn lại thành `User::getName`.
Đây là cách pass thẳng một method có sẵn vào chỗ đòi hỏi Functional Interface.

* `System.out::println` thay cho `x -> System.out.println(x)`
* `String::toUpperCase` thay cho `s -> s.toUpperCase()`
* `ArrayList::new` thay cho `() -> new ArrayList<>()`

---

## 🎯 Bài tập tư duy hệ thống

**1. Giả sử bạn có một danh sách 1 triệu bản ghi `Transaction`. Nếu dùng `stream()` bình thường mất 10 giây để xử lý tuần tự. Theo bạn, làm sao chỉ cần đổi đúng 1 từ khóa trong Stream API để phân chia công việc này ra cho nhiều CPU Core cùng chạy song song, giúp tốc độ tăng lên nhiều lần?**

👉 **Trả lời chi tiết:**

* Từ khóa đó chính là: Thay vì gọi `list.stream()`, bạn đổi thành **`list.parallelStream()`** (hoặc gọi `stream().parallel()`).
* **Cơ chế (Under the hood):** Java sẽ tự động sử dụng **ForkJoinPool** (một dạng Thread Pool có sẵn trong JVM). Nó chia (Fork) 1 triệu bản ghi thành nhiều nhóm nhỏ, đưa vào các hàng đợi (Queue) của từng CPU Core để xử lý song song. Xử lý xong, nó gộp (Join) kết quả lại.
* ⚠️ *Lưu ý của Senior:* Đừng lạm dụng `parallelStream()`. Chỉ dùng khi lượng dữ liệu cực lớn và thao tác xử lý không phụ thuộc vào thứ tự trước/sau. Nếu dữ liệu nhỏ, việc JVM phải tốn công tạo luồng và phân chia dữ liệu sẽ làm code chạy... **chậm hơn** cả stream bình thường!

**2. Trong Stream, hàm `map()` và `flatMap()` dùng để biến đổi dữ liệu. Nhưng khi nào thì phải dùng `flatMap()` thay vì `map()`?**

👉 **Trả lời chi tiết:**

* Suy luận của bạn **chính xác 100%**. Chữ `flat` có nghĩa là "Làm phẳng".
* Giả sử bạn có một `List<User>`, mỗi User lại chứa một `List<Email>`.
* Nếu bạn dùng `map(User::getEmails)`, dòng chảy Stream của bạn sẽ biến thành `Stream<List<Email>>` (Một dòng chảy chứa toàn các cái hộp, trong hộp chứa email). Rất khó lặp hay xử lý tiếp.
* Nếu bạn dùng `flatMap(user -> user.getEmails().stream())`, nó sẽ mở tất cả các hộp ra, gom toàn bộ email bên trong thả vào một đường ống duy nhất. Bạn sẽ nhận được `Stream<Email>`. Dữ liệu đã được "làm phẳng".
