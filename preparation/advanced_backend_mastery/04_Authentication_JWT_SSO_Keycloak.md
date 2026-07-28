# 🔵 Bài 4: Kiến trúc Xác thực, JWT, OAuth2 & Keycloak

Bảo mật là thành phần khó nhất và cũng là quan trọng nhất của Backend. Một sai lầm nhỏ trong xác thực có thể làm sập toàn bộ doanh nghiệp.

---

## 1. Nền tảng: Authentication vs Authorization

Đừng bao giờ nhầm lẫn hai khái niệm này:

* **Authentication (Xác thực):** Trả lời câu hỏi *"Bạn là ai?"*. (Ví dụ: Đăng nhập bằng Username/Password, Quẹt vân tay, Nhận diện khuôn mặt).
* **Authorization (Phân quyền):** Trả lời câu hỏi *"Bạn được phép làm gì?"*. (Ví dụ: Bạn đã đăng nhập rồi, nhưng bạn chỉ là User nên không được quyền bấm nút Xóa của Admin).

---

## 2. Sự thống trị của JWT (JSON Web Token)

Ngày xưa, khi làm Web nguyên khối (Monolith), chúng ta dùng **Session/Cookie**. Khi User đăng nhập thành công, Server lưu một cái `session_id` vào RAM.

* *Vấn đề:* Khi bạn có 10 server (Microservices), request bay vào Server 1 (lưu session), lát sau request bay sang Server 2, Server 2 không tìm thấy session trong RAM -> Bắt đăng nhập lại! Rất tệ để thiết kế hệ thống lớn (Scale).

**Giải pháp Stateless: JWT**
Server không thèm nhớ ai đã đăng nhập nữa. Sau khi check pass thành công, Server ký một cái **chứng minh thư (JWT)** giao cho Client. Client cứ cầm chứng minh thư đó kẹp vào Header (`Authorization: Bearer <token>`) gửi lên. Server chỉ cần nhìn "Chữ ký" trên chứng minh thư là biết hàng thật hay hàng giả.

### Cấu trúc 3 phần của JWT (Ngăn cách bởi dấu `.`)

1. **Header:** Chứa loại token (JWT) và Thuật toán ký mã hóa (VD: HS256, RS256).
2. **Payload (Claims):** Chứa Data của User (User ID, Name, Roles).
3. **Signature (Chữ ký điện tử):** Được tạo ra bằng cách băm (Header + Payload + **Secret Key của Server**).

⚠️ **Cạm bẫy chí mạng:** Payload của JWT **KHÔNG ĐƯỢC MÃ HÓA** (Nó chỉ encode Base64, copy quăng lên jwt.io là đọc được hết). Do đó tuyệt đối không bỏ Password hay mã thẻ tín dụng vào JWT.
*Hỏi:* Vậy lỡ hacker sửa Payload thì sao?
*Đáp:* Đổi Payload thì làm thay đổi chuỗi băm. Hacker không có **Secret Key** nên không thể tạo ra được Chữ ký (Signature) mới khớp với Payload đã sửa. Server check chữ ký thấy sai sẽ vứt bỏ request!

---

## 3. Kiến trúc OAuth2 & OpenID Connect (OIDC)

### OAuth2 (Chỉ là Authorization)

Ban đầu, OAuth2 được sinh ra để **Ủy quyền**. Giống như bạn có nhà, bạn nhờ ông bảo vệ giữ chìa khóa, rồi bạn cấp một cái "Thẻ bài" cho bà giúp việc. Bà giúp việc đưa thẻ bài cho bảo vệ để vào dọn nhà. Bạn không hề đưa chìa khóa gốc cho bà giúp việc!
👉 Đó là cách bạn click *"Đăng nhập Tinder bằng Facebook"*. Tinder cầm cái token để lấy ảnh đại diện của bạn từ Facebook mà không hề biết pass Facebook của bạn.

### OpenID Connect (OIDC) - Cứu cánh cho Authentication

Vì OAuth2 không sinh ra để Đăng nhập, người ta đã đắp thêm một lớp OIDC lên trên OAuth2. OIDC bổ sung thêm một cái Token gọi là **`id_token`** (Chính là JWT) chứa thông tin của người dùng.
Từ đó, chúng ta có một chuẩn xác thực hoàn hảo được dùng bởi Google, Microsoft, Facebook.

---

## 4. SSO (Single Sign-On) & Hệ Sinh Thái Keycloak

Tự code hệ thống Login, Quên mật khẩu, Xác thực 2 bước (MFA/2FA), Quản lý role... là một cực hình và đầy rủi ro bảo mật.
Các công ty lớn không tự code, họ dựng một hệ thống chuyên biệt gọi là **Identity Provider (IdP)**. **Keycloak** là một IdP mã nguồn mở cực kỳ mạnh mẽ của Red Hat.

### Luồng Single Sign-On (SSO) hoạt động thế nào?

Giả sử bạn có 1 app Frontend React và 1 backend Spring Boot, cùng kết nối vào Keycloak:

1. Người dùng vào trang chủ React App -> Bấm Đăng nhập.
2. React App **Redirect (Chuyển hướng)** thẳng người dùng sang trang Đăng nhập của Keycloak.
3. Người dùng nhập User/Pass trên giao diện của Keycloak. (React App hoàn toàn không biết Pass).
4. Keycloak check pass đúng -> Chuyển hướng người dùng trả về React App kèm theo một đoạn mã bí mật (Authorization Code).
5. React App giấu đoạn mã đó, ngầm gọi về Keycloak đổi lấy **Access Token (JWT)**.
6. Từ nay React App lấy JWT đó kẹp vào request gửi xuống Spring Boot.
7. Spring Boot cấu hình `oauth2ResourceServer`, nó tự động check "Chữ ký" của JWT xem có đúng là do Keycloak phát hành hay không. Nếu đúng thì cho đi tiếp.

👉 **Lợi ích:** Bạn viết thêm 1 app Angular, 1 app Mobile nữa. Người dùng chỉ cần đăng nhập ở React App là qua mấy app kia nó tự động vào luôn (Single Sign-On - Đăng nhập một lần).

---

## 🎯 Bài tập tư duy hệ thống bảo mật

**1. Mình thấy bạn đang thao tác với file `realm-export.json`. Bạn có biết "Realm" là gì và tại sao trong Keycloak lại phải chia ra làm các Realm khác nhau?**

👉 **Trả lời chi tiết:**

* Suy luận của bạn cực kỳ chuẩn xác. Realm là khái niệm đại diện cho kiến trúc **Multi-tenancy (Đa khách hàng)** của Keycloak.
* Một Realm giống như một không gian bị cô lập hoàn toàn (Highest Isolation). User, Role, Group, và Client App của Realm `Unihub` sẽ hoàn toàn không biết gì về sự tồn tại của Realm `Facebook`.
* Việc này giúp bạn chỉ cần tốn tiền chạy ĐÚNG 1 SERVER Keycloak, nhưng lại có thể cung cấp dịch vụ đăng nhập (Identity Provider) cho hàng chục công ty/dự án khác nhau mà không sợ dữ liệu bị rò rỉ chéo.
* *(Lưu ý: Có một Realm đặc biệt tên là `master`, đây là Realm gốc dùng để tạo ra các Realm khác, tuyệt đối không dùng `master` để quản lý user của app).*

**2. Làm sao vô hiệu hóa (Revoke) JWT NGAY LẬP TỨC khi mà nó là Stateless?**

👉 **Trả lời chi tiết (Chiến lược kiến trúc hệ thống):**

* Bạn đã nắm trọn vẹn 2 chiến lược kinh điển nhất mà các System Architect sử dụng:
* **Chiến lược 1 (Mặc định - Chuẩn OAuth2): Cấp Token siêu ngắn hạn (Short-lived Access Token).**
  * Đặt hạn sử dụng của JWT cực kỳ ngắn (5-15 phút).
  * Cấp kèm một cái `Refresh Token` dài hạn (có thể lưu State trong DB/Keycloak).
  * Khi JWT hết hạn, Frontend tự động dùng Refresh Token gọi ngầm lên Keycloak để xin JWT mới.
  * Nếu Admin vừa khóa tài khoản, lệnh "xin cấp mới" sẽ bị Keycloak từ chối ngay lập tức. (Nếu hacker trộm được JWT, thì cũng chỉ quậy được tối đa trong thời gian 5-15 phút đó).
* **Chiến lược 2 (Bảo mật tuyệt đối): Dùng Redis làm Blacklist (hoặc Whitelist).**
  * Bắt buộc phải **hi sinh tính "Stateless"** nguyên bản của JWT.
  * Khi user bấm Đăng xuất hoặc bị Admin khóa, ta quăng ID của JWT đó (cái `jti` claim) vào danh sách đen (Blacklist) trên Redis, cài thời gian sống (TTL) của record đó trong Redis bằng đúng thời gian hết hạn còn lại của JWT.
  * Mọi request tới Spring Boot, ngoài việc check Chữ ký, Spring Boot phải chọt vào Redis xem Token này có nằm trong danh sách cấm chưa. Nhờ Redis chạy trên RAM cực nhanh nên độ trễ gần như bằng 0.
