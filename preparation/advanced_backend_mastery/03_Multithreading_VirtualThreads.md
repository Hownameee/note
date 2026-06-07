# 🟢 Bài 3: Multithreading, Thread Pools & Sự trỗi dậy của Virtual Threads (Java 21)

Xử lý đa luồng (Multithreading) là thứ phân biệt một "Thợ gõ code" và một "Kỹ sư Backend". Nếu server của bạn có 16 CPU Cores, nhưng code của bạn chỉ chạy đơn luồng, bạn đang lãng phí 93% sức mạnh của máy chủ.

---

## 1. Nỗi đau của Platform Thread (Luồng truyền thống)

Từ Java 1 đến Java 20, mỗi khi bạn tạo ra một Thread mới (`new Thread()`), Java sẽ yêu cầu Hệ điều hành (OS) cấp phát một **OS Thread** thực sự.
Cơ chế này (gọi là Platform Thread) có 3 tử huyệt cực lớn:

1. **Quá tốn RAM:** Mỗi OS Thread tốn khoảng 1MB bộ nhớ ảo. Nếu bạn tạo 10.000 luồng, bạn bay mất 10GB RAM chỉ để... duy trì sự sống cho các luồng đó.
2. **Context Switching cực chậm:** Khi CPU phải đổi qua đổi lại giữa hàng nghìn luồng (lưu trạng thái luồng cũ, tải trạng thái luồng mới), Hệ điều hành tốn rất nhiều thời gian vô ích thay vì thực thi logic.
3. **Vấn đề "Blocking I/O":** Khi một luồng gửi truy vấn xuống Database và phải đợi mất 1 giây để lấy kết quả. Trong 1 giây đó, luồng này bị "Blocked" (bị khóa). Nó không làm gì cả, nhưng nó VẪN NGHIỄM NHIÊN CHIẾM DỤNG 1 OS THREAD và 1MB RAM.

---

## 2. Giải pháp truyền thống: Thread Pool (`ExecutorService`)

Vì việc tạo mới/tiêu hủy một OS Thread quá tốn kém, các Senior Dev không bao giờ gọi `new Thread()` trong môi trường Production. Giải pháp là dùng **Thread Pool (Hồ bơi chứa luồng)**.

```java
// Tạo một Thread Pool chứa sẵn 10 luồng
ExecutorService threadPool = Executors.newFixedThreadPool(10);

for (int i = 0; i < 100; i++) {
    // Đẩy 100 công việc (Task) vào hàng đợi (Queue)
    threadPool.submit(() -> {
        System.out.println("Đang xử lý bởi luồng: " + Thread.currentThread().getName());
    });
}
// 10 luồng sẽ liên tục chạy vào Queue lấy việc ra làm, 
// xong việc lại quay lại Queue lấy việc tiếp. Rất tối ưu!
```

* **Ưu điểm:** Tái sử dụng luồng, tránh sập server vì quá tải bộ nhớ.
* **Nhược điểm:** Server vẫn chỉ xử lý đồng thời được 10 request. Nếu 10 luồng này đều đang rủ nhau đứng đợi Database (Blocking I/O), thì luồng số 11 phải đứng chờ ngoài Queue, server coi như "bị treo".

---

## 3. Lập trình Bất đồng bộ với `CompletableFuture`

Để giải quyết tình trạng "luồng đứng chờ vô ích", Java cung cấp `CompletableFuture`. Nó cho phép bạn ném công việc ra chạy ngầm, và cài đặt sẵn một cái "Hành động tiếp theo" (Callback) để khi nào xong việc thì luồng nào đó rảnh sẽ tự động xử lý tiếp.

```java
CompletableFuture.supplyAsync(() -> {
    // Luồng A: Chạy đi lấy dữ liệu user từ DB mất 2 giây
    return database.getUser("Nam");
}).thenApplyAsync(user -> {
    // Sau khi có user, Luồng B (hoặc vẫn luồng A) nhận lấy data và đi gửi Email
    return emailService.send(user.getEmail());
}).thenAccept(result -> {
    // Luồng C nhận kết quả in ra màn hình.
    System.out.println("Đã gửi email thành công!");
});

System.out.println("Dòng code này sẽ chạy NGAY LẬP TỨC mà không bị khựng lại!");
```

* **Ưu điểm:** Khai thác tối đa sức mạnh CPU. Không ai phải đứng đợi ai.
* **Nhược điểm:** Code bị phân mảnh, khó debug (mỗi dòng log in ra một tên luồng khác nhau), mất dấu vết Stack Trace nếu xảy ra Exception.

---

## 4. Cú chuyển mình vĩ đại: Virtual Threads (Java 21 - Project Loom)

Năm 2023, Java tung ra **Virtual Threads** (Luồng ảo), thay đổi hoàn toàn cách chúng ta viết Backend. Đánh gục các ưu điểm của `Node.js` hay `Go`.

### Virtual Thread là gì?

Nó là một Thread siêu siêu nhẹ do chính máy ảo Java (JVM) quản lý chứ không phải OS quản lý.

* Thay vì ánh xạ 1:1 với OS Thread, JVM dùng mô hình **M:N**. Tức là: **Hàng triệu Virtual Threads** sẽ được "cưỡi" trên **vài chục Platform Threads** (còn gọi là Carrier Threads).

### Sức mạnh ma thuật: Khả năng "Unmount" (Tháo dỡ)

Bạn nhớ ví dụ luồng bị khóa khi chờ Database chứ? Đây là cách Virtual Thread giải quyết:

1. Virtual Thread số 1 (V1) đang "cưỡi" trên Carrier Thread số 1 (C1).
2. V1 gửi query xuống DB và phải chờ 1 giây (Blocking I/O).
3. Ngay lập tức, JVM sẽ **"Tháo dỡ" (Unmount)** V1 ra khỏi C1. Nó cất V1 vào một vùng nhớ nhỏ trong Heap.
4. Carrier Thread C1 bây giờ **RẢNH RỖI**. Nó ngay lập tức cho Virtual Thread số 2 (V2) "cưỡi" lên để làm việc khác.
5. Khi DB trả kết quả về, JVM lại bốc V1 từ trong Heap ra, gắn nó lên một Carrier Thread đang rảnh bất kỳ (Ví dụ C3) để chạy tiếp lệnh tiếp theo.

👉 **Kết quả:** OS Thread KHÔNG BAO GIỜ bị block. Bạn có thể tự tin tạo ra **1.000.000 (1 Triệu) Virtual Threads** chỉ với 2GB RAM!

```java
// Tạo 10,000 Virtual Threads CỰC KỲ NHẸ NHÀNG
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10000; i++) {
        executor.submit(() -> {
            // Chờ DB thoải mái, lúc chờ nó sẽ Unmount, không sợ hao tốn tài nguyên
            Thread.sleep(Duration.ofSeconds(1)); 
            System.out.println("Xong Task!");
        });
    }
} // Không cần quan tâm tới Thread Pool size nữa!
```

---

## 🎯 Bài tập tư duy kiến trúc hệ thống

**1. Với sự xuất hiện của Virtual Threads (Java 21), liệu chúng ta có còn cần phải duy trì các Thread Pool giới hạn (`FixedThreadPool`) nữa hay không?**

👉 **Trả lời chi tiết:**

* Về cơ bản là **KHÔNG**. Chúng ta tuyệt đối KHÔNG "Pool" (tái sử dụng) Virtual Threads vì việc tạo và hủy chúng rẻ như tạo một String (không tốn kém như OS Thread). Bạn cứ dùng `Executors.newVirtualThreadPerTaskExecutor()`.
* Tuy nhiên, Platform Thread Pool vẫn cần thiết trong một số trường hợp để **Giới hạn số lượng kết nối (Throttling)**. Ví dụ: Nếu 10.000 Virtual Threads cùng truy vấn Database một lúc, DB của bạn sẽ sập vì quá tải Connection. Để giải quyết, người ta thường dùng `Semaphore` để giới hạn Virtual Thread, hoặc gom các tác vụ đụng tới DB cũ/Legacy code ném vào một Thread Pool riêng có giới hạn.

**2. Giả sử hệ thống chạy hàm băm mật khẩu Bcrypt tốn 2 giây CPU. Nên dùng Virtual Thread hay Platform Thread?**

👉 **Trả lời chi tiết (Đây là cạm bẫy lớn nhất của Virtual Thread):**

* BẮT BUỘC phải dùng **Platform Thread (Thread Pool)**. Tuyệt đối KHÔNG dùng Virtual Thread cho tác vụ nặng về CPU (CPU-Bound).
* **Bản chất:** Máy ảo Java (JVM) chỉ tự động "Tháo dỡ" (Unmount) Virtual Thread khi nó phát hiện ra hành động **I/O Blocking** (chờ mạng, chờ DB, đọc file, `Thread.sleep`).
* Tính toán Bcrypt là thao tác vắt kiệt CPU (chạy vòng lặp mã hóa liên tục). Do đó nó KHÔNG HỀ UNMOUNT. Nó sẽ **Ghim cứng (Pinning)** cái luồng vật lý (Carrier Thread) bên dưới. Giả sử máy chủ có 16 luồng vật lý, chỉ cần 16 người dùng cùng đăng ký tài khoản (chạy Bcrypt bằng Virtual Thread), 16 luồng vật lý sẽ bị ghim cứng. Toàn bộ hàng triệu Virtual Threads khác trong server sẽ bị "chết đói" (Starvation) vì không còn Carrier Thread nào rảnh để chúng nhảy lên chạy!
* 👉 **Quy tắc vàng của Backend:**
  * Tác vụ **I/O-Bound** (Đọc ghi File, gọi Database, gọi API bên ngoài) -> Dùng **Virtual Thread**.
  * Tác vụ **CPU-Bound** (Mã hóa, nén file, xử lý hình ảnh, tính toán AI) -> Dùng **Platform Thread Pool**.
