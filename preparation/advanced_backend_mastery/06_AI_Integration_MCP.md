# 🟣 Bài 6: Tích hợp AI, Tool Calling & Model Context Protocol (MCP)

Thế giới Backend đang chuyển mình mạnh mẽ. Kỹ sư tương lai không chỉ viết API cho Frontend (React/Vue/Mobile) gọi, mà còn phải viết "API" cho Trí tuệ nhân tạo (AI Agent) sử dụng. Phần này chúng ta sẽ bóc tách cơ chế đó.

---

## 1. Sự tiến hóa: Từ Chatbot đến AI Agent (Tool Calling)

LLMs (như GPT-4, Gemini, Claude) về bản chất là một cỗ máy sinh văn bản (Text-in, Text-out). Nó bị nhốt trong một "chiếc hộp", không biết mấy giờ, không biết truy cập Internet, không biết chọc vào Database của bạn.

Làm sao để AI tự động gửi Email cho khách hàng? Hay tự truy vấn Database tìm lỗi?
👉 Lời giải là **Tool Calling (Function Calling - Gọi hàm)**.

### Cơ chế hoạt động của Tool Calling (Under the hood)

1. **Khai báo Tool:** Code Spring Boot của bạn gửi cho AI một danh sách các "Công cụ" dưới định dạng JSON Schema. Ví dụ:
   * *Hàm `get_weather(location)`: Lấy thời tiết.*
   * *Hàm `delete_user(userId)`: Xóa người dùng.*
2. **User ra lệnh:** User chat *"Thời tiết ở Hà Nội hôm nay sao?"*.
3. **AI suy luận (Reasoning):** AI nhận ra nó không có data thời tiết hiện tại. Nó KHÔNG trả lời user bằng chữ. Thay vào đó, nó báo lại cho Backend của bạn một yêu cầu: `Hãy chạy dùm tao hàm get_weather với tham số location="Hà Nội"`.
4. **Backend thực thi:** Code Java của bạn nhận lệnh, MÓC VÀO DATABASE hoặc GỌI API THỜI TIẾT thực sự, lấy ra kết quả `30 độ C`.
5. **Nạp lại (Feedback):** Backend gửi câu trả lời `{"location": "Hà Nội", "temp": 30}` ngược lại cho AI.
6. **Final Output:** AI dựa vào data đó, sinh ra câu tiếng Việt mượt mà: *"Dạ, thời tiết Hà Nội hôm nay là 30 độ C ạ"*.

👉 *Sự thật:* AI KHÔNG HỀ thực thi bất cứ hành động nào. Nó chỉ đóng vai trò là "Bộ Não" chỉ đạo. Backend của bạn mới là "Chân Tay" đi làm việc.

---

## 2. Nỗi đau của System Design khi tích hợp AI

Khi hệ thống nhỏ, bạn tự định nghĩa JSON Schema cho vài ba cái Tools thì rất dễ.
Nhưng khi hệ thống phình to: Bạn muốn AI đọc được tin nhắn Slack, móc được kho code Github, truy vấn được Database Postgres, gọi được hệ thống Jira...
Mỗi nền tảng lại có một cách giao tiếp khác nhau. Bạn sẽ rơi vào cảnh phải đi code "Cầu nối" (Integration) cho hàng chục hệ thống, maintain JSON schema gãy tay.

---

## 3. Lời giải cách mạng: Model Context Protocol (MCP)

Được Anthropic (Công ty tạo ra Claude) Open-source vào cuối năm 2024, **MCP** được ví như *"Cổng USB Type-C cho kỷ nguyên AI"*.

Thay vì AI phải đi học cách giao tiếp với từng nền tảng riêng biệt, mọi nền tảng sẽ phải bọc mình lại bằng một cái chuẩn chung: **Chuẩn MCP**.

### Kiến trúc 3 phần của hệ thống MCP

1. **MCP Host (Máy chủ chứa AI):** Đây là phần mềm có chứa trí tuệ nhân tạo. (Ví dụ: Ứng dụng Claude Desktop, Cursor IDE, hay chính con AI Agent đang chat với bạn đây).
2. **MCP Client (Người gọi):** Là một module nằm bên trong Host, có nhiệm vụ "bắt tay" (handshake) và kết nối với các Server khác.
3. **MCP Server (Máy chủ cung cấp Tool):** Đây chính là thứ Backend Dev phải xây dựng! Nó là một chương trình chạy độc lập bọc lấy Database/API của công ty bạn.

### Sức mạnh của MCP Server

Khi MCP Client kết nối vào MCP Server của bạn, Server sẽ tự động "Nôn" ra danh sách:

* **Resources:** (Các file, data có sẵn mà AI có thể đọc).
* **Tools:** (Các hàm thực thi như chạy Query, Xóa file).
* **Prompts:** (Các mẫu hướng dẫn AI).

Client (AI) chỉ việc "Cắm và Chạy" (Plug & Play) cực kỳ trơn tru mà không cần tốn nửa dòng code cấu hình JSON lằng nhằng nào ở phía AI nữa. Github, Slack, Postgres, Jira đều đã và đang viết các MCP Server của riêng họ.

---

## 4. Tương lai của AI-Native Backend

Kỹ sư Backend tương lai sẽ chuyển dịch từ việc **"Viết REST API cho Frontend gọi"** sang **"Viết MCP Server cho AI Agent gọi"**.
Bạn sẽ xây dựng các nghiệp vụ cốt lõi (Core Business) thật chuẩn, biến chúng thành các MCP Tools. Sau đó, người dùng chỉ cần ra lệnh bằng giọng nói, AI Agent sẽ tự động cắm vào MCP Server của bạn, bốc các Tools ra ghép nối lại để hoàn thành công việc một cách tự chủ.

---

## 🎯 Câu hỏi kiểm tra tư duy AI Engineering

**1. Liệu AI có khả năng "Lén lút" chạy lệnh `DROP TABLE` phá hoại Database không?**

👉 **Trả lời chi tiết:**

* Suy luận của bạn hoàn toàn chính xác! Câu trả lời là **KHÔNG THỂ**.
* AI hoàn toàn bị cô lập trong hộp đen và không có bất kỳ kết nối mạng trực tiếp nào xuống Database của bạn. Nó chỉ được quyền "đề xuất" một hàm dựa trên danh sách Tools do chính bạn (Kỹ sư Backend) định nghĩa và cung cấp thông qua MCP Server.
* Nếu bạn KHÔNG lập trình ra một hàm nào có chức năng xóa DB, hoặc code hàm đó nhưng check quyền Auth ngặt nghèo, thì dù AI có bị "Promp Injection" (hack bằng câu lệnh), nó cũng chỉ có thể "nói mồm" yêu cầu Backend chạy một hàm không tồn tại/không có quyền. Backend của bạn mới là vị Thẩm Phán cuối cùng quyết định có chạy hay không.

**2. Bài toán bảo mật lệnh chuyển 1 tỷ: Làm sao chặn AI làm bậy?**

👉 **Trả lời chi tiết (Kiến trúc HITL - Human-in-the-loop):**

* Chúc mừng! Phương án "Thêm nút bấm xác nhận" mà bạn đưa ra chính là một Design Pattern kinh điển mang tên **Human-in-the-loop (Có con người trong vòng lặp)** - Tiêu chuẩn vàng của mọi hệ thống Agentic AI hiện nay!
* Đối với các hành động mang rủi ro cao hoặc tàn phá dữ liệu (Chuyển tiền, Xóa bài, Gửi email hàng loạt), Backend KHÔNG BAO GIỜ được phép nhắm mắt thực thi ngay lệnh do AI trả về.
* **Quy trình chuẩn phải là:**
  1. AI trả về yêu cầu gọi hàm: `transfer_money(amount=1_tỷ, to="ABC")`.
  2. Backend nhận lệnh, nhưng chỉ lưu giao dịch này dưới trạng thái `PENDING` (Chờ duyệt).
  3. Backend bắn một tín hiệu lên UI (Frontend) cho người dùng thật nhìn thấy: *"AI đang đề xuất thao tác: Chuyển 1 tỷ cho ABC. Bạn có đồng ý thực thi không?"* (Kèm theo nút XÁC NHẬN hoặc nhập OTP).
  4. Người dùng (Human) bấm xác nhận.
  5. Lúc này Backend nhận được tín hiệu Auth từ con người, mới chính thức chọt vào Database trừ tiền.
