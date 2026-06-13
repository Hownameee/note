# 🟡 Bài 10: Chiến lược Đồng bộ & Mapping DTO giữa Backend (Java) và Frontend (TypeScript)

Trong các dự án thực tế, sự bất đồng bộ giữa **DTO (Data Transfer Object)** ở Backend và **Type/Interface** ở Frontend (TypeScript) là nguyên nhân hàng đầu gây ra các lỗi runtime ngớ ngẩn (như FE gọi sai tên trường do BE mới đổi tên, sai kiểu dữ liệu, thiếu trường...).

Để giải quyết triệt để vấn đề này, dự án của bạn sử dụng giải pháp tự động dịch từ Java Class sang TypeScript Type ngay tại thời điểm build dự án (**Build-time Generation**).

---

## 1. Vấn đề cốt lõi của việc gõ DTO bằng tay ở Frontend

1. **Dễ sai sót (Human Error):** Chỉ cần một lỗi gõ phím nhỏ (typo) hoặc viết sai kiểu dữ liệu (ví dụ: `LocalDateTime` ở BE thành `string` ở FE nhưng xử lý nhầm thành `Date`), ứng dụng sẽ crash.
2. **Tốn thời gian bảo trì (Maintenance Hell):** Mỗi lần Backend thêm, sửa, xóa 1 trường trong Class Java DTO, họ phải thông báo cho Frontend để cập nhật thủ công. Nếu quên, lỗi sẽ chỉ xuất hiện khi chạy thực tế (Runtime).
3. **Mismatched naming convention:** Java thường dùng `camelCase`, nhưng một số API cũ dùng `snake_case`. Việc chuyển đổi thủ công rất mệt mỏi.

---

## 2. Giải pháp 1: Sử dụng Plugin Build-time (Java to TypeScript Generator) - Khuyên dùng

Giải pháp này dịch trực tiếp các Class Java DTO sang file `.d.ts` hoặc `.ts` ngay khi dự án Backend được biên dịch (build). Dữ liệu kiểu ở Frontend sẽ luôn luôn đi trước hoặc song hành cùng với code Backend.

Thư viện phổ biến nhất hiện nay cho việc này là **`typescript-generator`** (hỗ trợ cả Maven và Gradle).

```mermaid
flowchart LR
    BE[Spring Boot DTOs] -->|mvn clean compile| Plugin[typescript-generator-maven-plugin]
    Plugin -->|Tự động ghi đè| FE[frontend/src/types/backend-dto.ts]
```

### 2.1. Cấu hình Maven Plugin (`pom.xml`)

Dưới đây là cấu hình chuẩn chỉnh giúp quét toàn bộ package DTO và xuất ra định nghĩa TypeScript ở thư mục Frontend:

```xml
<plugin>
    <groupId>cz.habarta.typescript-generator</groupId>
    <artifactId>typescript-generator-maven-plugin</artifactId>
    <version>3.2.1263</version>
    <executions>
        <execution>
            <id>generate</id>
            <goals>
                <goal>generate</goal>
            </goals>
            <phase>process-classes</phase>
        </execution>
    </executions>
    <configuration>
        <jsonLibrary>jackson2</jsonLibrary>
        
        <!-- Quét tự động bằng Pattern để tránh việc phải khai báo từng class thủ công -->
        <classPatterns>
            <classPattern>com.example.dto.*Dto</classPattern>
        </classPatterns>

        <!-- Đường dẫn xuất file trực tiếp sang thư mục code của Frontend -->
        <outputFile>../frontend/src/types/backend-dto.ts</outputFile>
        
        <!-- 'implementationFile' giúp sinh ra code thực thi (như Enum thật), thay vì chỉ có interface ảo -->
        <outputFileType>implementationFile</outputFileType>
        
        <!-- Cấu hình cách map kiểu dữ liệu Date/Time -->
        <mapDate>string</mapDate>
        
        <!-- Tự động map các class lồng nhau (Nested Classes) -->
        <optionalProperties>all</optionalProperties>
    </configuration>
</plugin>
```

### 2.2. Ưu điểm nổi bật của giải pháp Build-time

* **Tự động hóa hoàn toàn:** Mỗi khi nhà phát triển Backend chạy `mvn compile` hoặc `mvn install`, file định nghĩa TypeScript của Frontend sẽ tự động cập nhật.
* **Type-safety tuyệt đối:** Không có độ trễ hay sai lệch. Nếu BE thay đổi tên trường, file TypeScript ở FE đổi theo ngay lập tức. Nếu FE đang gọi trường cũ, IDE của FE sẽ báo lỗi đỏ ngay lúc đó.
* **Không cần chạy ứng dụng:** Khác với OpenAPI cần phải bật Spring Boot lên để quét API Docs, giải pháp build-time phân tích các file `.class` tĩnh nên chạy cực kỳ nhanh và nhẹ.

---

## 3. Giải pháp phụ: OpenAPI (Swagger) + TypeScript Code Generator

Một hướng đi khác là để Backend tự động sinh ra tài liệu đặc tả API (OpenAPI JSON/YAML) bằng thư viện `springdoc-openapi` lúc chạy ứng dụng, sau đó Frontend sử dụng CLI `openapi-typescript` để sinh mã nguồn TypeScript.

* **Nhược điểm so với Build-time:**
  * Cần phải khởi động ứng dụng Spring Boot lên trước mới lấy được JSON spec.
  * Phải chạy qua 2 bước thủ công ở cả 2 phía BE và FE.
  * Khó cấu hình tùy biến sâu cho các Class DTO nội bộ không xuất hiện trực tiếp trên Controller Endpoints.

---

## 4. Xử lý khác biệt dữ liệu khi Mapping ở Frontend (Data Transformation)

Kiểu dữ liệu giữa Java và JavaScript/TypeScript có nhiều sự vênh nhau. Dưới đây là các cạm bẫy và cách xử lý chuẩn hóa khi dữ liệu được map từ Java sang TS:

### 4.1. Kiểu ngày tháng (`LocalDateTime`, `Instant`)

* **Backend:** Trả về dạng String ISO-8601 (ví dụ: `"2026-06-09T13:41:20Z"`).
* **Frontend:** TypeScript Generator sẽ tự động map kiểu này thành `string` (theo cấu hình `<mapDate>string</mapDate>`).
* **Cạm bẫy:** Lập trình viên Frontend thường quên chuyển thành đối tượng Date của JS dẫn đến lỗi khi hiển thị hoặc tính toán định dạng ngày.
* **Best Practice:** Sử dụng thư viện chuyên dụng như `dayjs` hoặc `date-fns` để parse và định dạng:

  ```typescript
  const formattedDate = dayjs(userDto.createdAt).format('DD/MM/YYYY HH:mm');
  ```

### 4.2. Kiểu Số Lớn (`BigDecimal`, `Long`)

* **Backend:** Kiểu `Long` (64-bit) trong Java có thể vượt quá giới hạn an toàn của kiểu `number` trong JavaScript (`Number.MAX_SAFE_INTEGER` = $2^{53} - 1$).
* **Cạm bẫy:** Nếu ID dạng Long quá lớn (ví dụ: ID sinh bởi Snowflake ID Generator), khi nhận ở Frontend qua JSON thông thường, JavaScript sẽ tự động làm tròn số, dẫn đến sai lệch ID và không thể gọi tiếp API.
* **Cách giải quyết:**
  1. Cấu hình Jackson ở Backend để serialize kiểu `Long` thành `String` khi trả về JSON:

     ```java
     @JsonSerialize(using = ToStringSerializer.class)
     private Long id;
     ```

  2. Ở Frontend, thuộc tính này sẽ được sinh ra dưới dạng `string` trong file `backend-dto.ts`.

### 4.3. Kiểu Enum

* **Backend:** Định nghĩa Enum trong Java.
* **Frontend:** Vì ta cấu hình `<outputFileType>implementationFile</outputFileType>`, TypeScript Generator sẽ tự động dịch Enum Java thành Enum TypeScript thực thụ (có thể chạy lúc runtime):

  ```typescript
  export enum UserStatus {
      ACTIVE = "ACTIVE",
      INACTIVE = "INACTIVE"
  }
  ```

  Bạn có thể import trực tiếp `UserStatus` ở Frontend để so sánh hoặc render dropdown list.

---

## 🎯 Bài tập kiểm tra tư duy thực chiến

**Tình huống:** Dự án của bạn tích hợp `typescript-generator-maven-plugin` ở Backend. Trong quá trình refactor hệ thống, lập trình viên Backend đổi tên thuộc tính `userId` trong `OrderResponseDto` thành `customerUniqueId`.

Khi Backend thực hiện build bằng lệnh `mvn clean compile`:

1. File `backend-dto.ts` ở Frontend có tự động cập nhật trường mới không?
2. Chuyện gì xảy ra với code Frontend (ví dụ React/Angular) đang gọi tới `order.userId` khi dự án Frontend được build? Lỗi được phát hiện ở giai đoạn nào?

*(Gợi ý trả lời:

1. Có tự động cập nhật. Khi chạy lệnh compile, plugin sẽ quét lại Class và viết đè lại file backend-dto.ts ở Frontend, trong đó thuộc tính userId biến mất và được thay bằng customerUniqueId.
2. Dự án Frontend sẽ bị lỗi biên dịch (Compile Error) ngay lập tức vì TypeScript phát hiện thuộc tính userId không còn tồn tại trên kiểu OrderResponseDto nữa. Lỗi được phát hiện ngay lập tức ở giai đoạn Compile-time của Frontend chứ không phải đợi ứng dụng chạy lên chạy lỗi mới biết).*
