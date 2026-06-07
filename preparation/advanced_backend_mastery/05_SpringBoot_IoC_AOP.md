# 🟡 Bài 5: "Ma thuật" dưới nền của Spring Boot (IoC & AOP)

Hầu hết mọi người học Spring Boot bằng cách học thuộc các Annotation (`@Service`, `@Autowired`, `@Transactional`). Nhưng khi hệ thống phình to và gặp lỗi rò rỉ bộ nhớ hoặc dead-lock, họ không biết cách sửa. Để làm chủ (Master) Spring, bạn phải hiểu nó hoạt động như thế nào.

---

## 1. Lõi của Spring: IoC Container và Dependency Injection (DI)

### Inversion of Control (IoC) - Đảo ngược quyền điều khiển

* **Kiểu cũ:** Khi Class A cần dùng Class B. Bạn viết code: `B b = new B();` bên trong Class A. Tức là Class A tự **kiểm soát** việc tạo ra Class B.
* **IoC:** Khái niệm này bảo rằng: Đừng bao giờ dùng chữ `new` nữa! Spring sẽ tạo ra một cái Thùng chứa khổng lồ gọi là **ApplicationContext**. Khi khởi động, Spring quét toàn bộ code, lôi tất cả các class có đánh dấu `@Component`, `@Service`, `@Repository` tạo sẵn thành các Object (gọi là **Bean**) và quăng vào cái Thùng này để tự nó quản lý.

### Dependency Injection (DI) - Tiêm phụ thuộc

* Khi Class A cần dùng Class B, A chỉ cần khai báo một cái biến `private final B b;` vào constructor. Spring sẽ mò trong Thùng chứa (IoC Container) lấy thằng B "tiêm" vào cho thằng A.
* **Best Practice:** Đừng bao giờ dùng `@Autowired` trên field nữa (Field injection). Hãy dùng **Constructor Injection** (của Lombok `@RequiredArgsConstructor`). Nó giúp code của bạn dễ dàng viết Unit Test (mock data mà không cần phải bật Spring lên).

### Vòng đời của Bean (Bean Scopes)

Đây là kiến thức phỏng vấn cực kỳ quan trọng:

* **Singleton (Mặc định):** Spring chỉ tạo duy nhất 1 bản sao của Bean đó. 1000 request gọi API cùng lúc thì chúng đều chui vào ĐÚNG MỘT CÁI Controller và ĐÚNG MỘT CÁI Service đó. 👉 *Đó là lý do các class này phải hoàn toàn là Stateless (Không lưu trữ dữ liệu State).*
* **Prototype:** Mỗi lần có ai đó xin Bean này, Spring sẽ `new` ra một object hoàn toàn mới.
* **Request / Session:** (Dùng cho Web nguyên khối) Mỗi HTTP Request hoặc Session tạo 1 Bean. Ít dùng cho REST API.

---

## 2. AOP (Aspect-Oriented Programming) và Design Pattern "Proxy"

Đây là phần thú vị nhất giải thích toàn bộ "phép thuật" của Spring.

Bạn có bao giờ tự hỏi: Tại sao mình chỉ cần gắn chữ `@Transactional` lên đầu cái hàm, thì tự nhiên nếu code bên trong bị lỗi (Exception), Database lại tự động Rollback (hủy bỏ) giao dịch mà không cần mình phải viết hàm try-catch-rollback nào?

### Lời giải: Proxy Pattern (Người đóng thế)

Khi bạn đánh dấu một Class là `@Transactional` (hay `@PreAuthorize`), Spring **KHÔNG BAO GIỜ** giao cái Class thật của bạn cho người khác gọi. Nó tự động sinh ra một Class ảo (gọi là **Proxy**) bọc lấy Class thật của bạn.

*Luồng đi của Proxy:*

1. Controller gọi `userService.createUser()`.
2. Controller thực chất đang gọi vào lớp **Proxy** chứ không phải Service thật.
3. Lớp Proxy thực thi "Ma thuật": Bật kết nối xuống Database, bắt đầu một Giao dịch (Begin Transaction).
4. Proxy nhường lại quyền chạy hàm `createUser()` cho Service thật của bạn.
5. Service chạy xong. Proxy đứng ra hứng lấy. Nếu không có Exception, Proxy gõ lệnh `Commit` xuống DB. Nếu có Exception, Proxy gõ lệnh `Rollback`!

### Cạm bẫy kinh điển: Self-Invocation (Gọi hàm nội bộ)

Đây là bẫy phổ biến nhất gây lỗi data mà mọi Junior/Mid-level đều mắc phải.
Giả sử trong cùng 1 file `UserService`:

```java
public void createA() {
    this.createB(); // Code này gọi hàm bên dưới
}

@Transactional
public void createB() {
    // Code insert xuống DB...
}
```

* **Sự thật phũ phàng:** Hàm `createB` hoàn toàn bị MẤT TÍNH NĂNG ROLLBACK!
* **Tại sao?** Bởi vì hàm `createA` dùng từ khóa `this.` để gọi `createB()`. Việc gọi nội bộ bên trong class thì nó không đi xuyên qua lớp Proxy ở ngoài, nên lớp Proxy không có cơ hội được "Mở/Đóng Transaction".
* **Cách giải quyết:** Tách hàm `createB` sang một class Service khác, hoặc tự inject chính cái Service đó vào bản thân nó (hơi xấu nhưng chạy được).

---

## 3. Kiến trúc Spring Security (Filter Chain)

Nhiều người nghĩ code bảo mật sẽ được chạy ngay trước cửa Controller. **Sai!**
Spring Security là một bức tường dày cộp đứng chặn trước khi request kịp chạm vào kiến trúc lõi của Spring.

Nó được cấu thành từ một chuỗi gọi là **SecurityFilterChain**.

* Request HTTP bay vào -> Gặp Màng lọc số 1 (Check CORS) -> Gặp Màng lọc số 2 (Check JWT/Authentication) -> Màng lọc 3 (Check Quyền Authorization) -> ... -> DispatcherServlet -> Controller.
* Khi bạn dùng JWT (Stateless), trong file Config, bạn luôn phải gọi lệnh `http.csrf().disable()` và `sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)`. Lý do là CSRF chỉ chống tấn công đánh cắp Cookie, dùng JWT ta không xài Cookie nên không cần nó.

---

## 🎯 Bài tập kiểm tra độ sâu tư duy

**1. Khai báo biến `count` trong Singleton Service, 10.000 request gọi cùng lúc thì sao?**

👉 **Trả lời chi tiết:**

* Suy luận của bạn **chính xác 100%**. Kết quả cuối cùng gần như chắc chắn sẽ `< 10.000` do lỗi **Race Condition**.
* Vì Service là Singleton, 10.000 luồng (threads) cùng chia sẻ một biến `count` duy nhất trên vùng nhớ Heap. Phép toán `count++` thực chất gồm 3 bước: Đọc giá trị cũ -> Cộng thêm 1 -> Ghi lại giá trị mới. Khi các luồng chạy xen kẽ nhau, luồng A đang đọc số 5 chưa kịp ghi số 6 thì luồng B cũng bay vào đọc số 5. Kết quả là 2 luồng đều ghi số 6, bạn bị mất 1 lượt đếm.
* *Bài học:* Tuyệt đối không khai báo biến State (biến thay đổi được) bên trong Controller, Service hay Repository. Chúng phải hoàn toàn là Stateless. Nếu cần đếm đồng thời, hãy dùng kiểu dữ liệu an toàn luồng như `AtomicInteger` hoặc lưu xuống Redis/Database.

**2. Hàm không có Transactional gọi hàm có Transactional trong cùng 1 class. Có Rollback không?**

👉 **Trả lời chi tiết (Đây là bẫy lớn nhất của Spring AOP):**

* Câu trả lời là: **KHÔNG HỀ CÓ ROLLBACK NÀO XẢY RA CẢ!** đây là cạm bẫy kinh điển mang tên **Self-invocation (Gọi hàm nội bộ)**.
* Hãy nhớ lại kiến trúc Proxy mình vừa nói: Khi Controller gọi hàm `placeOrder()` (không có annotation), nó đi xuyên qua lớp Proxy. Lớp Proxy thấy hàm này không cần Transaction nên nó đẩy thẳng vào hàm thật bên trong.
* Từ bên trong hàm thật `placeOrder()`, code của bạn gọi hàm `saveDataToDB()`. Máy tính hiểu ngầm là `this.saveDataToDB()`. Từ khóa `this` gọi thẳng vào chính object hiện tại, **nó không đi vòng ra ngoài qua lớp Proxy nữa**.
* Hậu quả: Vì không đi qua Proxy, Spring không có cơ hội chặn lại để mở/đóng Transaction. Chữ `@Transactional` trên hàm `saveDataToDB()` lúc này chỉ là vật trang trí!
* *Cách sửa:* Tách hàm `saveDataToDB` sang một file `DatabaseService` riêng. Khi đó bạn gọi `databaseService.saveDataToDB()`, luồng code bắt buộc phải chui ra ngoài và đâm xuyên qua lớp Proxy của `databaseService`, lúc đó Rollback mới hoạt động bình thường.
