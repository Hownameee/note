# GitHub OAuth2 — Token Scope & Expiration Guide

---

## 1. Token Expiration

### OAuth App Token không expired theo mặc định

Loại token bạn đang dùng (OAuth App) **không có thời hạn hết hạn theo mặc định**.

Kiểm tra setting tại:
`GitHub → Settings → Developer settings → OAuth Apps → [App của bạn]`

Tìm checkbox **"Expire user authorization tokens"**:

- **Unchecked (mặc định)** → token không bao giờ hết hạn, tồn tại cho đến khi user revoke
- **Checked** → token hết hạn sau **8 giờ**, kèm refresh token valid **6 tháng**

### So sánh các loại token

| Loại Token            | Tự hết hạn?              | Ghi chú                                      |
| :-------------------- | :----------------------- | :------------------------------------------- |
| OAuth App token       | Không (mặc định)         | Loại đang dùng. Tắt setting là xong          |
| GitHub App token      | Có — sau 1 giờ           | Cần implement refresh token flow             |
| PAT Classic           | Tuỳ chọn (có thể "None") | Set manually khi tạo token                   |
| PAT Fine-grained      | Có — tối đa 1 năm        | Bắt buộc có expiry, không thể vô hạn         |

**Kết luận:** Với mục tiêu "non-expired" → dùng OAuth App và tắt "Expire user authorization tokens" trong settings. Không cần thay đổi code.

---

## 2. Scopes — Toàn bộ quyền của user

Không có scope `all`. GitHub chia quyền thành các nhóm riêng.

### Nhóm User & Email

| Scope        | Quyền                                                              |
| :----------- | :----------------------------------------------------------------- |
| `user`       | Read/write toàn bộ profile (name, bio, location, email public...)  |
| `read:user`  | Chỉ đọc profile — không thể update                                 |
| `user:email` | Đọc tất cả email (kể cả private)                                   |
| `user:follow`| Follow/unfollow users                                              |

### Nhóm Repositories

| Scope            | Quyền                                                                    |
| :--------------  | :----------------------------------------------------------------------- |
| `repo`           | Full access: đọc/ghi code, commit, branch, PR, issue, wiki (cả private)  |
| `repo:status`    | Đọc/ghi commit statuses (CI pipeline)                                    |
| `repo_deployment`| Đọc/ghi deployment statuses                                              |
| `public_repo`    | Chỉ access repo public (subset của `repo`)                               |
| `repo:invite`    | Chấp nhận/từ chối lời mời vào repo                                       |
| `security_events`| Đọc/ghi code scanning alerts                                             |
| `delete_repo`    | Xóa repo — cần thêm riêng, `repo` không bao gồm                          |

### Nhóm GitHub Actions & Packages

| Scope              | Quyền                                                         |
| :----------------- | :------------------------------------------------             |
| `workflow`         | Đọc/ghi GitHub Actions workflow files (`.github/workflows`)   |
| `write:packages`   | Upload packages lên GitHub Packages                           |
| `read:packages`    | Download packages từ GitHub Packages                          |
| `delete:packages`  | Xóa packages                                                  |

### Nhóm Organization

| Scope               | Quyền                                                   |
| :-----------------  | :------------------------------------------------------ |
| `read:org`          | Đọc org membership, teams, projects                     |
| `write:org`         | Quản lý org membership và team                          |
| `admin:org`         | Full admin: tạo/xóa team, manage members                |
| `manage_runners:org`| Quản lý GitHub Actions runners của org                  |

### Nhóm SSH Keys & GPG

| Scope               | Quyền                                  |
| :------------------ | :----------------------------------    |
| `read:public_key`   | Xem SSH public keys                    |
| `write:public_key`  | Tạo/xóa SSH public keys                |
| `admin:public_key`  | Full CRUD SSH keys (bao gồm cả 2 trên) |
| `read:gpg_key`      | Xem GPG keys                           |
| `write:gpg_key`     | Tạo/xóa GPG keys                       |
| `admin:gpg_key`     | Full CRUD GPG keys                     |

### Nhóm Webhooks & Hooks

| Scope               | Quyền                                  |
| :------------------ | :------------------------------------- |
| `admin:repo_hook`   | Full CRUD webhooks trên repo           |
| `write:repo_hook`   | Tạo/xóa/ping webhooks trên repo        |
| `read:repo_hook`    | Xem webhooks trên repo                 |
| `admin:org_hook`    | Full CRUD webhooks trên org            |

### Các Scope khác

| Scope             | Quyền                                            |
| :---------------- | :----------------------------------------------- |
| `gist`            | Tạo/edit gists                                   |
| `notifications`   | Đọc notifications, đánh dấu đã đọc               |
| `project`         | Read/write user và org projects (Projects v1)    |
| `read:project`    | Chỉ đọc projects                                 |
| `codespace`       | Quản lý GitHub Codespaces                        |
| `copilot`         | Quản lý GitHub Copilot settings                  |

---

## 3. Scope cho "100% quyền user"

Không có cách nào request tất cả scopes trong 1 lần vì phụ thuộc vào nhu cầu thực tế. Nhưng nếu muốn **tối đa quyền thực tế** cho cá nhân:

```text
user repo delete_repo workflow gist notifications read:org admin:public_key admin:gpg_key admin:repo_hook read:packages write:packages
```

### Update App.jsx — scope tối đa cho personal use

```javascript
const SCOPES = [
  'user',           // full profile read/write
  'user:email',     // all emails
  'user:follow',    // follow/unfollow
  'repo',           // full repo access (code, PR, issues, wiki)
  'repo:status',    // commit statuses (CI)
  'delete_repo',    // delete repos
  'workflow',       // GitHub Actions workflows
  'gist',           // gists
  'notifications',  // notifications
  'read:org',       // read org membership
  'read:packages',  // read packages
  'write:packages', // publish packages
  'admin:public_key', // SSH keys
  'admin:gpg_key',  // GPG keys
  'admin:repo_hook',// repo webhooks
].join(' ');
```

---

## 4. Lưu ý quan trọng

### Scope không thể vượt qua quyền thực tế của user

Dù request scope `admin:org`, nếu user đó không phải admin của org → GitHub cấp token nhưng các API call liên quan sẽ trả về `403 Forbidden`.

### User phải đồng ý (consent screen)

Khi user đăng nhập OAuth lần đầu, GitHub hiển thị màn hình yêu cầu xác nhận từng scope. Scope càng nhiều → user càng thấy nhiều quyền được yêu cầu → có thể từ chối.

### Token không tự renew scope

Nếu bạn update scope trong code nhưng user đã đăng nhập trước đó → token cũ vẫn giữ scope cũ. User phải **logout rồi login lại** để GitHub cấp token mới với scope mới.

### Kiểm tra scope của token hiện tại

```bash
curl -I \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  https://api.github.com/user
```

Nhìn vào response header `X-OAuth-Scopes` — đây là danh sách scope token hiện tại đang có.
