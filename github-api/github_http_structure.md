# GitHub REST API — HTTP Structure Guide

Tài liệu này giải thích cấu trúc của một HTTP request/response khi giao tiếp với GitHub REST API.

---

## 1. Base URL

Tất cả API đều có base URL là:

```text
https://api.github.com
```

---

## 2. Request Structure

### 2.1 HTTP Method

GitHub REST API tuân theo chuẩn REST, mỗi method mang ý nghĩa riêng:

| Method   | Ý nghĩa                                  | Ví dụ                          |
| :------- | :--------------------------------------- | :----------------------------- |
| `GET`    | Lấy dữ liệu (không thay đổi server)      | Lấy thông tin user             |
| `POST`   | Tạo mới một resource                     | Tạo issue, tạo branch          |
| `PATCH`  | Cập nhật một phần resource               | Cập nhật bio của user          |
| `PUT`    | Tạo mới hoặc thay thế hoàn toàn          | Tạo/update nội dung file       |
| `DELETE` | Xóa resource                             | Xóa branch, xóa comment        |

---

### 2.2 Request Headers

Đây là 3 headers **bắt buộc** trong hầu hết các request:

```http
Authorization: Bearer $ACCESS_TOKEN
Accept: application/vnd.github+json
X-GitHub-Api-Version: 2022-11-28
```

#### Chi tiết từng header

| Header                 | Bắt buộc    | Giá trị mặc định              | Mô tả                                                                                                                   |
| :--------------------- | :---------: | :---------------------------- | :---------------------------------------------------------------------------------------------------------------------  |
| `Authorization`        | ✅          | —                             | Token xác thực. Dùng `Bearer <token>` cho OAuth/PAT. Không có header này → chỉ truy cập được API public, rate limit thấp|
| `Accept`               | ✅          | `application/vnd.github+json` | Định dạng dữ liệu trả về. Xem chi tiết ở phần **Accept Media Types** bên dưới                                           |
| `X-GitHub-Api-Version` | Khuyến nghị | `2022-11-28` (default)        | Phiên bản API. Nếu bỏ qua, GitHub mặc định dùng phiên bản mới nhất được hỗ trợ                                          |
| `Content-Type`         | Khi có body | `application/json`            | Bắt buộc khi gửi `POST`, `PATCH`, `PUT` kèm JSON body                                                                   |

---

### 2.3 Accept Media Types

Header `Accept` điều khiển định dạng dữ liệu trả về:

| Media Type                       | Ý nghĩa                                                                 |
| :------------------------------- | :---------------------------------------------------------------------- |
| `application/vnd.github+json`    | ✅ **Mặc định**. Trả về JSON chuẩn của GitHub                           |
| `application/vnd.github.raw`     | Trả về nội dung thô (raw text) của file — dùng với endpoint `/contents` |
| `application/vnd.github.html`    | Trả về nội dung đã render sang HTML — dùng với Markdown files           |
| `application/vnd.github.diff`    | Trả về định dạng diff — dùng với commits/pull requests                  |
| `application/vnd.github.patch`   | Trả về định dạng patch — dùng với commits                               |

> **Lưu ý**: Không phải endpoint nào cũng hỗ trợ tất cả media types. Nếu endpoint không hỗ trợ, GitHub trả về JSON mặc định.

---

### 2.4 Request Body

Dùng khi gọi `POST`, `PATCH`, hoặc `PUT`. Body phải là **JSON** và cần header `Content-Type: application/json`.

**Ví dụ:**

```http
PATCH https://api.github.com/user
Content-Type: application/json

{
  "name": "Nam Nguyen",
  "bio": "Fullstack Engineer"
}
```

Với cURL:

```bash
curl -X PATCH \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/user \
  -d '{"bio": "Fullstack Engineer"}'
```

---

### 2.5 Query Parameters

Dùng để filter, phân trang, và sắp xếp kết quả trong các `GET` request:

```bash
# Lấy 10 repos mới nhất của user, sắp xếp theo thời gian update
GET /user/repos?sort=updated&direction=desc&per_page=10&page=1
```

| Parameter   | Mô tả                                                                |
| :---------- | :------------------------------------------------------------------- |
| `per_page`  | Số items trên mỗi trang (tối đa 100, mặc định 30)                    |
| `page`      | Số trang hiện tại (bắt đầu từ 1)                                     |
| `sort`      | Tiêu chí sắp xếp (tùy endpoint: `created`, `updated`, `pushed`, ...) |
| `direction` | Chiều sắp xếp: `asc` hoặc `desc`                                     |

---

## 3. Response Structure

### 3.1 Response Headers

GitHub trả về một số headers quan trọng trong response:

| Header                  | Ý nghĩa                                                                                 |
| :---------------------- | :-------------------------------------------------------------------------------------- |
| `X-RateLimit-Limit`     | Tổng số request được phép trong một window (thường là 5000/giờ với OAuth token)         |
| `X-RateLimit-Remaining` | Số request còn lại trong window hiện tại                                                |
| `X-RateLimit-Reset`     | Unix timestamp (epoch) khi rate limit được reset                                        |
| `X-RateLimit-Resource`  | Loại resource được áp rate limit (`core`, `search`, `graphql`, ...)                     |
| `Content-Type`          | Loại dữ liệu trả về (thường là `application/json; charset=utf-8`)                       |
| `Link`                  | URLs để phân trang (chứa `rel="next"`, `rel="prev"`, `rel="last"`)                      |
| `ETag`                  | Fingerprint của response — dùng để conditional requests (tránh nhận lại data không đổi) |

---

### 3.2 HTTP Status Codes

| Status Code                 | Ý nghĩa                                                                                                     |
| :-------------------------- | :---------------------------------------------------------------------------------------------------------- |
| `200 OK`                    | Request thành công, có dữ liệu trả về                                                                       |
| `201 Created`               | Tạo mới resource thành công (thường sau `POST`)                                                             |
| `204 No Content`            | Thành công nhưng không có body trả về (thường sau `DELETE`)                                                 |
| `301 Moved Permanently`     | Resource đã chuyển sang URL khác                                                                            |
| `304 Not Modified`          | Dữ liệu không đổi kể từ lần request trước (dùng với `ETag`/`If-None-Match`)                                 |
| `400 Bad Request`           | Request sai cú pháp hoặc thiếu field bắt buộc                                                               |
| `401 Unauthorized`          | Token không hợp lệ hoặc hết hạn, hoặc biến `$ACCESS_TOKEN` trống                                            |
| `403 Forbidden`             | Token hợp lệ nhưng không đủ scope/quyền để thực hiện hành động này                                          |
| `404 Not Found`             | Resource không tồn tại, hoặc token không đủ quyền để xem (GitHub dùng 404 thay 403 trong một số trường hợp) |
| `422 Unprocessable Entity`  | Dữ liệu đúng format JSON nhưng vi phạm business rules (VD: tên branch đã tồn tại)                           |
| `429 Too Many Requests`     | Vượt quá rate limit                                                                                         |
| `500 Internal Server Error` | Lỗi phía GitHub server                                                                                      |

---

### 3.3 Response Body (JSON)

#### Thành công — single resource

```json
{
  "id": 1,
  "login": "Hownameee",
  "name": "Nam Nguyen",
  "email": "nam@example.com",
  "bio": "Fullstack Engineer",
  "public_repos": 12,
  "followers": 5,
  "created_at": "2020-01-01T00:00:00Z",
  "updated_at": "2026-06-13T00:00:00Z"
}
```

#### Thành công — collection (array)

```json
[
  { "id": 1, "name": "repo-one", ... },
  { "id": 2, "name": "repo-two", ... }
]
```

#### Lỗi — error object

```json
{
  "message": "Bad credentials",
  "documentation_url": "https://docs.github.com/rest",
  "status": "401"
}
```

#### Lỗi validation — với field-level details

```json
{
  "message": "Validation Failed",
  "errors": [
    {
      "resource": "Issue",
      "field": "title",
      "code": "missing_field"
    }
  ],
  "documentation_url": "https://docs.github.com/rest"
}
```

---

## 4. Pagination

Các endpoint trả về danh sách lớn sẽ phân trang. Để duyệt qua các trang, đọc header `Link` trong response:

```http
Link: <https://api.github.com/user/repos?page=2>; rel="next",
      <https://api.github.com/user/repos?page=5>; rel="last"
```

**Cách gọi thủ công:**

```bash
# Trang 1
curl "https://api.github.com/user/repos?per_page=30&page=1" ...

# Trang 2
curl "https://api.github.com/user/repos?per_page=30&page=2" ...
```

---

## 5. Rate Limiting

| Loại token        | Giới hạn            |
| :---------------- | :------------------ |
| Không có token    | 60 requests/giờ     |
| OAuth token / PAT | 5,000 requests/giờ  |
| GitHub App        | 15,000 requests/giờ |

Kiểm tra rate limit hiện tại:

```bash
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
     https://api.github.com/rate_limit
```
