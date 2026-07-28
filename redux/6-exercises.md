# Danh sách Bài tập Thực hành Redux Toolkit (React + TS)

Dưới đây là 3 bài tập thực hành được thiết kế theo cấp độ tăng dần để bạn làm chủ Redux Toolkit ngay trên dự án Vite + TypeScript hiện tại.

---

## 1. Bài tập 1: Ứng dụng Todo List (Độ khó: Dễ)

**Mục tiêu**: Làm quen với State dạng Mảng (Array), truyền dữ liệu qua `action.payload`.

### 1.1 Yêu cầu chức năng

1. Tạo một slice mới tên là `todoSlice.ts` trong thư mục `src/app/slices/`.
2. Định nghĩa cấu trúc State là một mảng các Todo:

   ```typescript
   interface Todo {
     id: string;
     text: string;
     completed: boolean;
   }
   type TodoState = Todo[];
   ```

3. Tạo các Reducers/Actions sau:
   - `addTodo`: Nhận vào `text` (string), sinh ra một `id` ngẫu nhiên (hoặc dùng `Date.now().toString()`), mặc định `completed: false`, sau đó push vào mảng todo.
   - `toggleTodo`: Nhận vào `id` (string), tìm todo tương ứng và đảo ngược giá trị `completed` (`true <-> false`).
   - `deleteTodo`: Nhận vào `id` (string), xóa todo đó ra khỏi mảng.
4. Gắn `todoReducer` vào `store.ts`.
5. Tạo giao diện UI trong component:
   - Có 1 ô Input và nút "Thêm".
   - Danh sách các Todo hiển thị bên dưới.
   - Khi bấm vào chữ Todo: Toggle gạch ngang chữ nếu đã hoàn thành (`text-decoration: line-through`).
   - Có nút "Xóa" bên cạnh mỗi Todo.

---

## 2. Bài tập 2: Giỏ hàng mua sắm (Độ khó: Trung bình)

**Mục tiêu**: Thực hành chỉnh sửa các đối tượng lồng nhau (nested objects) một cách an toàn bằng Immer, tính toán tổng số lượng và giá trị giỏ hàng.

### 2.1 Yêu cầu chức năng

1. Tạo `cartSlice.ts` quản lý state có cấu trúc sau:

   ```typescript
   interface CartItem {
     id: string;
     name: string;
     price: number;
     quantity: number;
     totalItemPrice: number;
   }

   interface CartState {
     items: CartItem[];
     totalQuantity: number;
     totalPrice: number;
   }
   ```

2. Tạo các Reducers/Actions:
   - `addToCart`: Nhận vào một sản phẩm `{ id, name, price }`.
     - Nếu sản phẩm chưa có trong giỏ hàng: Thêm mới với `quantity = 1` và `totalItemPrice = price`.
     - Nếu sản phẩm đã có: Tăng `quantity` thêm 1, cập nhật lại `totalItemPrice = quantity * price`.
     - Cập nhật lại tổng số lượng `totalQuantity` và tổng tiền `totalPrice` của cả giỏ hàng.
   - `removeFromCart`: Nhận vào sản phẩm `id`.
     - Tìm sản phẩm trong giỏ. Giảm `quantity` đi 1 và cập nhật lại `totalItemPrice`.
     - Nếu `quantity` giảm về 0, xóa hoàn toàn sản phẩm đó khỏi danh sách `items`.
     - Cập nhật lại tổng số lượng `totalQuantity` và tổng tiền `totalPrice`.
   - `clearCart`: Xóa sạch giỏ hàng (reset về state ban đầu).
3. Gắn `cartReducer` vào `store.ts`.
4. Tạo UI hiển thị:
   - Một danh sách sản phẩm mẫu cố định trên giao diện (Ví dụ: iPhone 15 - $999, Macbook M3 - $1499).
   - Nút "Thêm vào giỏ" bên cạnh mỗi sản phẩm mẫu.
   - Phần hiển thị chi tiết Giỏ hàng hiện tại (Tên sản phẩm, đơn giá, số lượng, tổng giá trị sản phẩm đó, nút "+" và "-" để tăng giảm trực tiếp số lượng).
   - Dòng tổng kết: "Tổng số lượng: X | Tổng tiền thanh toán: $Y" cùng nút "Xóa sạch giỏ hàng".

---

## 3. Bài tập 3: Trích xuất danh sách Người dùng từ API (Độ khó: Khó)

**Mục tiêu**: Thực hành xử lý bất đồng bộ (Async Thunk), quản lý trạng thái loading, lỗi (error) và hiển thị dữ liệu server.

### 3.1 Yêu cầu chức năng

1. Tạo một Async Thunk tên là `fetchUsers` gọi tới API công khai:
   `https://jsonplaceholder.typicode.com/users`
2. Tạo `userSlice.ts` quản lý state:

   ```typescript
   interface User {
     id: number;
     name: string;
     email: string;
     phone: string;
   }

   interface UserState {
     users: User[];
     loading: boolean;
     error: string | null;
   }
   ```

3. Xử lý các trạng thái trong `extraReducers`:
   - `fetchUsers.pending`: Đặt `loading = true`, `error = null`.
   - `fetchUsers.fulfilled`: Đặt `loading = false`, gán mảng kết quả nhận được từ API vào `users`.
   - `fetchUsers.rejected`: Đặt `loading = false`, gán thông điệp lỗi nhận được từ API hoặc action vào `error`.
4. Gắn `userReducer` vào `store.ts`.
5. Tạo UI hiển thị:
   - Nút "Tải danh sách người dùng".
   - Nếu đang loading: Hiển thị chữ `"Đang tải dữ liệu..."` hoặc vòng xoay loading.
   - Nếu có lỗi: Hiển thị thông báo lỗi màu đỏ.
   - Nếu tải thành công: Hiển thị bảng/danh sách thông tin người dùng gồm Tên, Email, Điện thoại.
