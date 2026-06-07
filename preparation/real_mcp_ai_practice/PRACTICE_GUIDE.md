# Hướng dẫn kiểm thử MCP Banking System (Node.js & Spring Boot)

Hệ thống MCP này chạy trên giao thức **STDIO**. Khi Spring Boot Client (MCP Client) khởi chạy, nó tự động khởi động và giao tiếp với Node.js MCP Server dưới nền.

---

## 1. Cách Khởi Chạy Hệ Thống

### Bước 1: Chuẩn bị biến môi trường

Mở Terminal và export GitHub Token để Client gọi mô hình LLM gpt-4o-mini:

```bash
export GITHUB_TOKEN="<token_github_cua_ban>"
```

### Bước 2: Chạy Spring Boot Client

Tại thư mục `real_mcp_ai_practice/mcp-chatbot-client`, chạy lệnh sau để build và khởi động:

```bash
mvn clean spring-boot:run
```

*(MCP Server sẽ tự động khởi chạy cùng ứng dụng Spring Boot, không cần chạy thủ công)*

---

## 2. Cách Xem Log Tool Được Gọi Bên MCP Server

Vì kết nối sử dụng giao thức **STDIO**:

- **Không dùng `console.log`** bên MCP server vì `stdout` được dành riêng cho các gói tin JSON-RPC của giao thức MCP.
- **Dùng `console.error`** để in log debug ra `stderr`.
- Spring Boot Client sẽ tự động lắng nghe luồng `stderr` này và in ra màn hình console của Java Client với tiền tố `STDERR Message received:`.

### Log Thực Tế Khi Gọi Tool

Khi một yêu cầu chat được gửi, trên màn hình terminal của Spring Boot Client sẽ hiện log:

```text
INFO 52934 --- [pool-4-thread-1] i.m.c.transport.StdioClientTransport : STDERR Message received: [Server Log] Calling tool getAccountBalance with args: {"accountId":"123"}
```

Dòng `[Server Log] Calling tool...` chính là log được in ra trực tiếp từ hàm xử lý bên file `index.js` của MCP Server.

---

## 3. Các Lệnh cURL Kiểm Thử (Chạy ở Terminal thứ 2)

### Kịch bản 1: Kiểm tra số dư tài khoản

```bash
curl -X POST -H "Content-Type: text/plain; charset=UTF-8" -d "Số dư tài khoản 123 là bao nhiêu" http://localhost:8081/api/chat
```

- **Kỳ vọng**: Trả về số dư `5.000.000 VND`.
- **Log MCP Server**: `Calling tool getAccountBalance with args: {"accountId":"123"}`

### Kịch bản 2: Thực hiện chuyển tiền chưa có OTP

```bash
curl -X POST -H "Content-Type: text/plain; charset=UTF-8" -d "Chuyển 500k từ 123 sang 456" http://localhost:8081/api/chat
```

- **Kỳ vọng**: Trả về yêu cầu nhập mã OTP.
- **Log MCP Server**: `Calling tool transferMoney with args: {"amount":500000,"fromAccountId":"123","toAccountId":"456"}`

### Kịch bản 3: Thực hiện chuyển tiền kèm OTP

```bash
curl -X POST -H "Content-Type: text/plain; charset=UTF-8" -d "Chuyển 500k từ 123 sang 456 với mã OTP là 123456" http://localhost:8081/api/chat
```

- **Kỳ vọng**: Trả về thông báo giao dịch thành công.
- **Log MCP Server**: `Calling tool transferMoney with args: {"amount":500000,"fromAccountId":"123","toAccountId":"456","otp":"123456"}`
