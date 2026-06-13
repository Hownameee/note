# GitHub API Reference — User, Profile & Repositories

Tài liệu này là danh sách các API cần thiết để lấy thông tin user, profile, email và repositories.

## Setup

```bash
export ACCESS_TOKEN="your_github_access_token_here"
```

---

## 1. User Profile

### GET /user — Lấy thông tin profile của chính mình

Trả về thông tin đầy đủ của user đang đăng nhập (owner của access token).

**Scopes cần thiết:** `read:user` hoặc `user`

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/user
```

**Response fields quan trọng:**

| Field                 | Type      | Mô tả                                        |
| :-------------------- | :-------- | :------------------------------------------- |
| `login`               | `string`  | Username trên GitHub                         |
| `id`                  | `integer` | ID duy nhất của user                         |
| `avatar_url`          | `string`  | URL ảnh đại diện                             |
| `name`                | `string`  | Tên hiển thị                                 |
| `email`               | `string`  | Email công khai (có thể null nếu để private) |
| `bio`                 | `string`  | Tiểu sử                                      |
| `location`            | `string`  | Vị trí địa lý                                |
| `company`             | `string`  | Tên công ty                                  |
| `blog`                | `string`  | URL website/blog                             |
| `public_repos`        | `integer` | Số repo công khai                            |
| `total_private_repos` | `integer` | Số repo private (chỉ có khi dùng token)      |
| `followers`           | `integer` | Số người theo dõi                            |
| `following`           | `integer` | Số người đang theo dõi                       |
| `created_at`          | `string`  | Ngày tạo tài khoản (ISO 8601)                |
| `updated_at`          | `string`  | Ngày cập nhật lần cuối                       |

**Ví dụ response:**

```json
{
  "login": "Hownameee",
  "id": 123456,
  "avatar_url": "https://avatars.githubusercontent.com/u/123456",
  "name": "Nam Nguyen",
  "email": null,
  "bio": "Fullstack Engineer | Never give up!",
  "location": "Ho Chi Minh City",
  "company": null,
  "blog": "",
  "public_repos": 15,
  "total_private_repos": 5,
  "followers": 12,
  "following": 20,
  "created_at": "2020-03-15T10:00:00Z",
  "updated_at": "2026-06-13T08:00:00Z"
}
```

---

### PATCH /user — Cập nhật profile

Cập nhật một hoặc nhiều field trong profile. Chỉ thay đổi các field được gửi lên, các field khác giữ nguyên.

**Scopes cần thiết:** `user` (không phải `read:user`)

```bash
curl -L \
  -X PATCH \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/user \
  -d '{
    "name": "Nam Nguyen",
    "bio": "Fullstack Engineer | Never give up! Everyday improvement.",
    "location": "Ho Chi Minh City",
    "blog": "https://github.com/Hownameee"
  }'
```

**Request body fields:**

| Field      | Type      | Mô tả                       |
| :--------- | :-------- | :-------------------------- |
| `name`     | `string`  | Tên hiển thị                |
| `email`    | `string`  | Email công khai             |
| `blog`     | `string`  | URL website                 |
| `company`  | `string`  | Tên công ty                 |
| `location` | `string`  | Vị trí                      |
| `hireable` | `boolean` | Cho phép tuyển dụng         |
| `bio`      | `string`  | Tiểu sử (tối đa 160 ký tự)  |

**Response:** Trả về toàn bộ object user đã cập nhật (giống response của `GET /user`).

---

## 2. Email

### GET /user/emails — Lấy danh sách email

Trả về tất cả email (cả public lẫn private) của user đang đăng nhập.

**Scopes cần thiết:** `user:email` hoặc `user`

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/user/emails
```

**Response:**

```json
[
  {
    "email": "nam@example.com",
    "primary": true,
    "verified": true,
    "visibility": "private"
  },
  {
    "email": "nam.public@example.com",
    "primary": false,
    "verified": true,
    "visibility": "public"
  }
]
```

**Response fields:**

| Field        | Type      | Mô tả                         |
| :----------- | :-------- | :---------------------------- |
| `email`      | `string`  | Địa chỉ email                 |
| `primary`    | `boolean` | Email chính dùng để thông báo |
| `verified`   | `boolean` | Đã xác minh chưa              |
| `visibility` | `string`  | `public` hoặc `private`       |

---

### GET /user/public_emails — Lấy chỉ email public

Chỉ trả về các email có `visibility: public`. Không cần scope `user:email`.

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/user/public_emails
```

---

## 3. Repositories

### GET /user/repos — Lấy danh sách repo của mình

Trả về tất cả repositories mà token đang có quyền truy cập (bao gồm repo private, repo tổ chức nếu được authorize).

**Scopes cần thiết:** `repo` (để xem private), hoặc không cần scope (chỉ public)

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/user/repos?sort=updated&direction=desc&per_page=30&page=1"
```

**Query parameters:**

| Parameter     | Giá trị                                                                        | Mặc định                                 |
| :------------ | :----------------------------------------------------------------------------- | :--------------------------------------- |
| `visibility`  | `all` / `public` / `private`                                                   | `all`                                    |
| `affiliation` | `owner` / `collaborator` / `organization_member` (có thể kết hợp bằng dấu phẩy)| `owner,collaborator,organization_member` |
| `type`        | `all` / `owner` / `member`                                                     | `all`                                    |
| `sort`        | `created` / `updated` / `pushed` / `full_name`                                 | `full_name`                              |
| `direction`   | `asc` / `desc`                                                                 | Phụ thuộc `sort`                         |
| `per_page`    | số nguyên (max: 100)                                                           | `30`                                     |
| `page`        | số nguyên                                                                      | `1`                                      |

**Response fields quan trọng (mỗi item trong array):**

| Field               | Type      | Mô tả                                            |
| :------------------ | :-------- | :----------------------------------------------- |
| `id`                | `integer` | ID repo                                          |
| `name`              | `string`  | Tên repo (không có owner prefix)                 |
| `full_name`         | `string`  | Tên đầy đủ: `owner/repo`                         |
| `private`           | `boolean` | Có phải repo private không                       |
| `description`       | `string`  | Mô tả repo                                       |
| `fork`              | `boolean` | Có phải repo được fork không                     |
| `html_url`          | `string`  | URL trang GitHub của repo                        |
| `clone_url`         | `string`  | URL để clone qua HTTPS                           |
| `ssh_url`           | `string`  | URL để clone qua SSH                             |
| `default_branch`    | `string`  | Branch mặc định (thường là `main` hoặc `master`) |
| `language`          | `string`  | Ngôn ngữ lập trình chính                         |
| `stargazers_count`  | `integer` | Số stars                                         |
| `forks_count`       | `integer` | Số forks                                         |
| `open_issues_count` | `integer` | Số issues đang mở                                |
| `size`              | `integer` | Kích thước repo (KB)                             |
| `visibility`        | `string`  | `public` hoặc `private`                          |
| `created_at`        | `string`  | Ngày tạo (ISO 8601)                              |
| `updated_at`        | `string`  | Ngày cập nhật metadata lần cuối                  |
| `pushed_at`         | `string`  | Ngày commit mới nhất được push                   |

---

### GET /repos/{owner}/{repo} — Lấy thông tin chi tiết một repo

Trả về metadata đầy đủ của một repository cụ thể.

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note
```

**Response fields bổ sung (so với list):**

| Field               | Type      | Mô tả                                                     |
| :------------------ | :-------- | :-------------------------------------------------------- |
| `owner`             | `object`  | Object chứa thông tin owner (`login`, `id`, `avatar_url`) |
| `topics`            | `array`   | Danh sách topics/tags của repo                            |
| `has_issues`        | `boolean` | Có bật Issues không                                       |
| `has_wiki`          | `boolean` | Có bật Wiki không                                         |
| `has_pages`         | `boolean` | Có GitHub Pages không                                     |
| `license`           | `object`  | Thông tin license (`spdx_id`, `name`)                     |
| `subscribers_count` | `integer` | Số người đang watch repo                                  |
| `network_count`     | `integer` | Số repo trong network (forks)                             |
| `permissions`       | `object`  | Quyền của token hiện tại: `admin`, `push`, `pull`         |

**Ví dụ response (rút gọn):**

```json
{
  "id": 789012,
  "name": "note",
  "full_name": "Hownameee/note",
  "private": false,
  "owner": {
    "login": "Hownameee",
    "id": 123456,
    "avatar_url": "https://avatars.githubusercontent.com/u/123456"
  },
  "description": "My personal notes",
  "fork": false,
  "html_url": "https://github.com/Hownameee/note",
  "clone_url": "https://github.com/Hownameee/note.git",
  "ssh_url": "git@github.com:Hownameee/note.git",
  "default_branch": "main",
  "language": "Markdown",
  "stargazers_count": 0,
  "forks_count": 0,
  "open_issues_count": 0,
  "topics": ["notes", "documentation"],
  "visibility": "public",
  "license": null,
  "permissions": {
    "admin": true,
    "maintain": true,
    "push": true,
    "triage": true,
    "pull": true
  },
  "created_at": "2024-01-01T00:00:00Z",
  "updated_at": "2026-06-13T00:00:00Z",
  "pushed_at": "2026-06-13T07:00:00Z"
}
```

---

### GET /repos/{owner}/{repo}/languages — Ngôn ngữ trong repo

Trả về danh sách ngôn ngữ lập trình và số bytes code tương ứng.

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/languages
```

**Response:**

```json
{
  "Java": 48234,
  "Markdown": 12048,
  "Shell": 320
}
```

---

### GET /repos/{owner}/{repo}/topics — Tags/Topics của repo

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/topics
```

**Response:**

```json
{
  "names": ["notes", "backend", "spring-boot"]
}
```

---

### GET /repos/{owner}/{repo}/contributors — Danh sách contributor

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/contributors
```

**Response:** Array of user objects, mỗi object có thêm field `contributions` (số lần commit).

---

## 4. Branches

### GET /repos/{owner}/{repo}/branches — Danh sách branches

Trả về danh sách tất cả branches của repository.

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/branches
```

**Response fields (mỗi item):**

| Field                    | Type      | Mô tả                                           |
| :----------------------- | :-------- | :-------------------------------------------    |
| `name`                   | `string`  | Tên branch                                      |
| `commit.sha`             | `string`  | SHA của commit mới nhất trên branch             |
| `commit.url`             | `string`  | URL API của commit đó                           |
| `protected`              | `boolean` | Branch có đang được bảo vệ (branch rules) không |

---

### GET /repos/{owner}/{repo}/branches/{branch} — Chi tiết một branch

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/branches/main
```

**Response fields bổ sung:**

| Field                         | Type     | Mô tả                                                  |
| :---------------------------- | :------- | :----------------------------------------------------- |
| `commit.commit.message`       | `string` | Commit message của commit mới nhất                     |
| `commit.commit.author.name`   | `string` | Tên tác giả commit                                     |
| `commit.commit.author.date`   | `string` | Thời gian commit (ISO 8601)                            |
| `_links.self`                 | `string` | URL API của branch này                                 |
| `protection.enabled`          | `boolean`| Branch protection có đang bật không                    |

---

### POST /repos/{owner}/{repo}/git/refs — Tạo branch mới

Tạo branch mới từ một SHA commit. Cần lấy SHA của branch gốc trước (thường là `main`).

**Bước 1:** Lấy SHA của branch gốc:

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/git/ref/heads/main
```

> Lấy giá trị `object.sha` từ response.

**Bước 2:** Tạo branch mới:

```bash
curl -L \
  -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/git/refs \
  -d '{"ref":"refs/heads/feature/my-feature","sha":"abc123def456..."}'
```

**Request body:**

| Field  | Type     | Bắt buộc   | Mô tả                                                                                       |
| :----- | :------- | :--------: | :------------------------------------------------------------------------------------------ |
| `ref`  | `string` | ✅         | Tên đầy đủ của ref, phải bắt đầu bằng `refs/heads/` (VD: `refs/heads/feature/my-feature`)   |
| `sha`  | `string` | ✅         | SHA của commit muốn tạo branch từ đó                                                        |

---

### DELETE /repos/{owner}/{repo}/git/refs/heads/{branch} — Xóa branch

```bash
curl -L \
  -X DELETE \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/git/refs/heads/feature/my-feature
```

**Response:** `204 No Content` nếu thành công (không có body).

---

## 5. Pull Requests

### GET /repos/{owner}/{repo}/pulls — Danh sách Pull Requests

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/Hownameee/note/pulls?state=open"
```

**Query parameters:**

| Parameter   | Giá trị                                          | Mặc định  |
| :---------- | :----------------------------------------------- | :-------- |
| `state`     | `open` / `closed` / `all`                        | `open`    |
| `head`      | `{user}:{branch}` để filter theo branch nguồn    | —         |
| `base`      | Tên branch đích (VD: `main`)                     | —         |
| `sort`      | `created` / `updated` / `popularity`             | `created` |
| `direction` | `asc` / `desc`                                   | `desc`    |
| `per_page`  | số nguyên (max: 100)                             | `30`      |
| `page`      | số nguyên                                        | `1`       |

**Response fields quan trọng (mỗi item):**

| Field              | Type      | Mô tả                                              |
| :----------------- | :-------- | :------------------------------------------------- |
| `number`           | `integer` | Số thứ tự PR trong repo                            |
| `title`            | `string`  | Tiêu đề PR                                         |
| `state`            | `string`  | `open` hoặc `closed`                               |
| `draft`            | `boolean` | Có phải Draft PR không                             |
| `merged`           | `boolean` | Đã được merge chưa                                 |
| `body`             | `string`  | Nội dung mô tả PR                                  |
| `head.ref`         | `string`  | Tên branch nguồn (nơi có thay đổi)                 |
| `base.ref`         | `string`  | Tên branch đích (nơi muốn merge vào)               |
| `user.login`       | `string`  | Username người tạo PR                              |
| `created_at`       | `string`  | Thời điểm tạo PR                                   |
| `merged_at`        | `string`  | Thời điểm merge (null nếu chưa merge)              |
| `html_url`         | `string`  | URL trang PR trên GitHub                           |
| `commits`          | `integer` | Số commits trong PR                                |
| `changed_files`    | `integer` | Số files thay đổi                                  |
| `additions`        | `integer` | Số dòng được thêm                                  |
| `deletions`        | `integer` | Số dòng bị xóa                                     |

---

### GET /repos/{owner}/{repo}/pulls/{pull_number} — Chi tiết một Pull Request

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/pulls/1
```

---

### POST /repos/{owner}/{repo}/pulls — Tạo Pull Request mới

```bash
curl -L \
  -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/pulls \
  -d '{
    "title": "feat: add new feature",
    "body": "## Summary\nAdds the new awesome feature.",
    "head": "feature/my-feature",
    "base": "main",
    "draft": false
  }'
```

**Request body:**

| Field    | Type      | Bắt buộc   | Mô tả                                          |
| :------- | :-------- | :-------:  | :--------------------------------------------- |
| `title`  | `string`  | ✅         | Tiêu đề Pull Request                           |
| `head`   | `string`  | ✅         | Tên branch nguồn chứa các thay đổi             |
| `base`   | `string`  | ✅         | Tên branch đích muốn merge vào (VD: `main`)    |
| `body`   | `string`  | —          | Nội dung mô tả PR (hỗ trợ Markdown)            |
| `draft`  | `boolean` | —          | `true` để tạo Draft PR, mặc định `false`       |

**Response:** Object PR đầy đủ, `201 Created`.

---

### PATCH /repos/{owner}/{repo}/pulls/{pull_number} — Cập nhật Pull Request

Dùng để đổi tiêu đề, mô tả, base branch, hoặc trạng thái (đóng PR).

```bash
curl -L \
  -X PATCH \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/pulls/1 \
  -d '{"title":"fix: updated title","state":"closed"}'
```

**Request body:**

| Field    | Type     | Mô tả                                           |
| :------- | :------- | :---------------------------------------------- |
| `title`  | `string` | Tiêu đề mới                                     |
| `body`   | `string` | Mô tả mới                                       |
| `state`  | `string` | `open` để mở lại, `closed` để đóng PR           |
| `base`   | `string` | Đổi branch đích (chỉ khi PR chưa merge)         |

---

### PUT /repos/{owner}/{repo}/pulls/{pull_number}/merge — Merge Pull Request

```bash
curl -L \
  -X PUT \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/pulls/1/merge \
  -d '{"commit_title":"feat: merge my-feature into main","merge_method":"squash"}'
```

**Request body:**

| Field            | Type     | Mô tả                                                              |
| :--------------- | :------- | :----------------------------------------------------------------- |
| `commit_title`   | `string` | Tiêu đề merge commit                                               |
| `commit_message` | `string` | Nội dung merge commit message                                      |
| `merge_method`   | `string` | `merge` (merge commit) / `squash` (squash & merge) / `rebase`      |

**Response:**

```json
{
  "sha": "abc123...",
  "merged": true,
  "message": "Pull Request successfully merged"
}
```

---

## 6. CI Pipeline — Commit Checks & Statuses

GitHub có **2 hệ thống song song** để báo cáo trạng thái CI/CD trên một commit:

| Hệ thống                | API               | Dùng khi nào                                             |
| :---------------------- | :---------------- | :------------------------------------------------------- |
| **Commit Statuses**     | `/statuses/{sha}` | Legacy CI (Jenkins, CircleCI cũ, custom webhook)         |
| **Check Runs / Suites** | `/check-runs`     | GitHub Actions và CI hiện đại (tích hợp deep hơn)        |

---

### 6.1 Commit Statuses (Legacy)

#### GET /repos/{owner}/{repo}/commits/{ref}/statuses — Danh sách statuses của một commit

`{ref}` có thể là SHA commit, tên branch, hoặc tag.

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/commits/main/statuses
```

**Response fields (mỗi item):**

| Field         | Type      | Mô tả                                               |
| :------------ | :-------- | :-------------------------------------------------- |
| `id`          | `integer` | ID của status                                       |
| `state`       | `string`  | `pending` / `success` / `failure` / `error`         |
| `description` | `string`  | Mô tả ngắn gọn (VD: "Build passed", "Tests failed") |
| `target_url`  | `string`  | URL dẫn đến trang CI job chi tiết                   |
| `context`     | `string`  | Tên CI service (VD: `ci/jenkins`, `build/test`)     |
| `created_at`  | `string`  | Thời điểm tạo                                       |

---

#### GET /repos/{owner}/{repo}/commits/{ref}/status — Trạng thái tổng hợp (combined status)

Trả về **một trạng thái tổng hợp** duy nhất cho commit, tính từ tất cả statuses. Tiện để biết nhanh commit đó pass hay fail.

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/commits/main/status
```

**Response:**

```json
{
  "state": "success",
  "statuses": [...],
  "sha": "abc123...",
  "total_count": 3,
  "repository": { "full_name": "Hownameee/note" },
  "commit_url": "https://api.github.com/repos/Hownameee/note/commits/abc123..."
}
```

> Logic của `state`: `failure`/`error` nếu có bất kỳ status nào fail → `pending` nếu có pending → `success` nếu tất cả đều success.

---

#### POST /repos/{owner}/{repo}/statuses/{sha} — Tạo commit status (báo kết quả CI)

Dùng khi bạn muốn tự báo cáo kết quả CI về một commit (VD: từ webhook hoặc script tùy chỉnh).

**Scopes cần thiết:** `repo:status`

```bash
curl -L \
  -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/statuses/abc123def456 \
  -d '{
    "state": "success",
    "target_url": "https://ci.example.com/builds/42",
    "description": "All tests passed!",
    "context": "ci/my-pipeline"
  }'
```

**Request body:**

| Field         | Type     | Bắt buộc | Mô tả                                           |
| :------------ | :------- | :------: | :---------------------------------------------- |
| `state`       | `string` |    ✅    | `pending` / `success` / `failure` / `error`     |
| `target_url`  | `string` |    —     | URL dẫn đến trang log CI chi tiết               |
| `description` | `string` |    —     | Mô tả ngắn (tối đa 140 ký tự)                   |
| `context`     | `string` |    —     | Nhãn phân biệt CI source (mặc định: `default`)  |

---

### 6.2 Check Runs (GitHub Actions & CI hiện đại)

#### GET /repos/{owner}/{repo}/commits/{ref}/check-runs — Check Runs của một commit

Trả về danh sách các check runs (GitHub Actions jobs, hoặc CI bên thứ 3 dùng Checks API) cho một commit.

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/Hownameee/note/commits/main/check-runs"
```

**Query parameters:**

| Parameter    | Giá trị                                   | Mặc định |
| :----------- | :---------------------------------------- | :------- |
| `check_name` | Filter theo tên check (VD: `"build"`)     | —        |
| `status`     | `queued` / `in_progress` / `completed`    | —        |
| `filter`     | `latest` (chỉ lần chạy mới nhất) / `all`  | `latest` |
| `per_page`   | số nguyên (max: 100)                      | `30`     |

**Response fields quan trọng (mỗi item):**

| Field          | Type      | Mô tả                                                                                            |
| :------------- | :-------- | :----------------------------------------------------------------------------------------------- |
| `id`           | `integer` | ID của check run                                                                                 |
| `name`         | `string`  | Tên check (VD: `"build"`, `"test"`, `"lint"`)                                                    |
| `status`       | `string`  | `queued` / `in_progress` / `completed`                                                           |
| `conclusion`   | `string`  | Kết quả khi `status=completed`: `success` / `failure` / `cancelled` / `skipped` / `timed_out`    |
| `started_at`   | `string`  | Thời điểm bắt đầu chạy                                                                           |
| `completed_at` | `string`  | Thời điểm kết thúc (null nếu chưa xong)                                                          |
| `html_url`     | `string`  | URL trang GitHub Actions job                                                                     |
| `details_url`  | `string`  | URL log chi tiết của CI bên ngoài (nếu có)                                                       |
| `app.slug`     | `string`  | App tạo check run (VD: `github-actions`)                                                         |

---

#### GET /repos/{owner}/{repo}/commits/{ref}/check-suites — Check Suites của một commit

Mỗi **Check Suite** là một nhóm các Check Runs (tương đương một workflow run trong GitHub Actions).

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/commits/main/check-suites
```

---

### 6.3 GitHub Actions Workflow Runs

#### GET /repos/{owner}/{repo}/actions/runs — Danh sách workflow runs

Trả về lịch sử các lần chạy GitHub Actions trong repo.

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/Hownameee/note/actions/runs?per_page=10"
```

**Query parameters:**

| Parameter  | Giá trị                                                                      | Mặc định |
| :--------- | :--------------------------------------------------------------------------- | :------- |
| `actor`    | Filter theo username người trigger                                           | —        |
| `branch`   | Filter theo branch name                                                      | —        |
| `event`    | `push` / `pull_request` / `schedule` / `workflow_dispatch` / ...             | —        |
| `status`   | `queued` / `in_progress` / `completed` / `success` / `failure` / `cancelled` | —        |
| `head_sha` | Filter theo SHA commit cụ thể                                                | —        |
| `per_page` | số nguyên (max: 100)                                                         | `30`     |

**Response fields quan trọng (mỗi item):**

| Field         | Type      | Mô tả                                                                        |
| :------------ | :-------- | :--------------------------------------------------------------------------- |
| `id`          | `integer` | ID của workflow run                                                          |
| `name`        | `string`  | Tên workflow                                                                 |
| `status`      | `string`  | `queued` / `in_progress` / `completed`                                       |
| `conclusion`  | `string`  | `success` / `failure` / `cancelled` / `skipped` / `timed_out` (khi completed)|
| `event`       | `string`  | Loại event trigger (`push`, `pull_request`, ...)                             |
| `head_branch` | `string`  | Branch đã trigger workflow                                                   |
| `head_sha`    | `string`  | SHA của commit đã trigger                                                    |
| `run_number`  | `integer` | Số thứ tự lần chạy trong workflow                                            |
| `run_attempt` | `integer` | Lần thử (1 nếu không retry)                                                  |
| `created_at`  | `string`  | Thời điểm tạo                                                                |
| `updated_at`  | `string`  | Thời điểm cập nhật lần cuối                                                  |
| `html_url`    | `string`  | URL trang GitHub Actions run                                                 |

---

#### GET /repos/{owner}/{repo}/actions/runs/{run_id} — Chi tiết một workflow run

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/actions/runs/123456789
```

---

#### GET /repos/{owner}/{repo}/actions/runs/{run_id}/jobs — Các jobs trong một run

Trả về từng job (step-level) bên trong một workflow run.

```bash
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/actions/runs/123456789/jobs
```

**Response fields quan trọng (mỗi job):**

| Field          | Type      | Mô tả                                                                               |
| :------------- | :-------- | :---------------------------------------------------------------------------------- |
| `id`           | `integer` | ID của job                                                                          |
| `name`         | `string`  | Tên job (VD: `"build"`, `"test (ubuntu-latest)"`)                                   |
| `status`       | `string`  | `queued` / `in_progress` / `completed`                                              |
| `conclusion`   | `string`  | `success` / `failure` / `cancelled` / `skipped`                                     |
| `started_at`   | `string`  | Thời điểm job bắt đầu                                                               |
| `completed_at` | `string`  | Thời điểm job kết thúc                                                              |
| `html_url`     | `string`  | URL log của job trên GitHub Actions                                                 |
| `steps`        | `array`   | Danh sách các steps trong job, mỗi step có `name`, `status`, `conclusion`, `number` |

---

#### POST /repos/{owner}/{repo}/actions/runs/{run_id}/rerun — Chạy lại workflow

```bash
curl -L \
  -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/actions/runs/123456789/rerun
```

**Response:** `201 Created`, không có body.

---

#### POST /repos/{owner}/{repo}/actions/runs/{run_id}/rerun-failed-jobs — Chạy lại chỉ các jobs bị fail

```bash
curl -L \
  -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/actions/runs/123456789/rerun-failed-jobs
```

---

#### DELETE /repos/{owner}/{repo}/actions/runs/{run_id} — Xóa workflow run

```bash
curl -L \
  -X DELETE \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/Hownameee/note/actions/runs/123456789
```

**Response:** `204 No Content`.
