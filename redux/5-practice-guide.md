# Thực Hành Redux: Kiến Trúc, Best Practices & Cheatsheet

Chúc mừng bạn đã đi qua toàn bộ lộ trình từ Core Redux, Redux Toolkit, React Redux cho đến RTK Query.
Tài liệu cuối cùng này tổng hợp các kinh nghiệm thực tế, cách tổ chức thư mục dự án chuẩn chỉnh và một bảng tra nhanh (Cheatsheet) để bạn áp dụng vào dự án thực tế.

---

## 1. Tổ chức cấu trúc thư mục (Folder Structure)

Hiện nay, cộng đồng Redux khuyến nghị cấu trúc thư mục theo **Feature (Tính năng)** thay vì cấu trúc theo **Type (Loại file)**.

### Cách 1: Feature Folder Structure (Khuyên dùng cho dự án vừa & lớn)

Mỗi tính năng tự chứa toàn bộ logic Redux, component và styles của nó. Điều này giúp dễ tìm kiếm, cô lập lỗi và tái sử dụng component.

```text
src/
├── app/
│   ├── store.js             # Cấu hình store chung
│   └── rootReducer.js       # (Tùy chọn) Gộp các reducer nếu dự án cực lớn
├── features/
│   ├── auth/
│   │   ├── authSlice.js     # Redux logic cho authentication
│   │   ├── LoginView.jsx    # React component
│   │   └── authApi.js       # RTK Query endpoints cho auth
│   ├── todos/
│   │   ├── todosSlice.js
│   │   └── TodoList.jsx
│   └── posts/
│       ├── postsSlice.js
│       └── PostsList.jsx
├── components/              # Các UI component dùng chung (Button, Input, Layout)
├── App.jsx
└── main.jsx
```

### Cách 2: Type Folder Structure (Chỉ phù hợp dự án siêu nhỏ)

Các file được phân chia theo kiểu file: tất cả actions nằm chung một chỗ, tất cả reducers nằm chung một chỗ.

```text
src/
├── store/
├── actions/
│   ├── authActions.js
│   └── todoActions.js
├── reducers/
│   ├── authReducer.js
│   └── todoReducer.js
```

*⚠️ Lưu ý: Tránh sử dụng cấu trúc Type Folder vì khi thêm một tính năng, bạn sẽ phải mở 4-5 thư mục khác nhau để sửa code, rất dễ gây nhầm lẫn.*

---

## 2. Các Best Practices cốt lõi khi làm việc với Redux

### A. Phân định rõ ràng: UI State vs Server State vs Global State

Không phải cái gì cũng nhét vào Redux!

- **Local/UI State (Dùng `useState` / `useReducer` của React)**: Trạng thái mở/đóng modal, giá trị nhập trong form tạm thời, tab đang được chọn, v.v. Những thông tin này chỉ liên quan đến hiển thị của chính component đó và biến mất khi component unmount.
- **Server State (Dùng RTK Query)**: Dữ liệu tải về từ API cần cache và đồng bộ lại với máy chủ.
- **Global State (Dùng Redux Slice)**: Dữ liệu chia sẻ giữa rất nhiều component không có quan hệ họ hàng (ví dụ: thông tin User đăng nhập, tùy chọn Dark Mode/Theme, giỏ hàng Cart).

### B. Giữ cho State được phẳng (Normalized State Shape)

Tránh lồng ghép các object quá sâu trong store state.

- *Ví dụ xấu*:

  ```javascript
  const state = {
    posts: [
      { id: 1, comments: [{ id: 101, user: { id: 200, name: 'Nam' } }] }
    ]
  }
  ```

  Nếu bạn cần cập nhật tên user, code update state sẽ cực kỳ phức tạp và dễ gây lỗi re-render diện rộng.
- *Giải pháp*: Tổ chức state phẳng bằng cách tách biệt thực thể (Post, Comment, User) và liên kết chúng qua ID (có thể dùng thư viện `normalizr`).

### C. Luôn sử dụng Redux DevTools

Hãy tải tiện ích mở rộng **Redux DevTools** trên Chrome/Firefox. Nó cung cấp các tính năng vô giá:

1. **Time-travel debugging**: Nhấp chuột để quay ngược thời gian xem trạng thái UI thay đổi thế nào ứng với từng action được dispatch.
2. **State Diff**: Xem chính xác thuộc tính nào đã thay đổi sau một action.
3. **Action Dispatcher**: Thử phát trực tiếp một action ngay trên trình duyệt để kiểm tra phản ứng của app.

---

## 3. Redux Toolkit Cheatsheet (Bảng tra nhanh)

### Thiết lập Store nhanh

```javascript
import { configureStore } from '@reduxjs/toolkit';
import countReducer from './countSlice';
import { myApi } from './myApi';

export const store = configureStore({
  reducer: {
    counter: countReducer,
    [myApi.reducerPath]: myApi.reducer,
  },
  middleware: (getDefault) => getDefault().concat(myApi.middleware),
});
```

### Tạo Slice đồng bộ nhanh

```javascript
import { createSlice } from '@reduxjs/toolkit';

const todoSlice = createSlice({
  name: 'todo',
  initialState: [],
  reducers: {
    addTodo: (state, action) => {
      // Immer tự động clone state giúp ta dùng push thoải mái
      state.push({ id: Date.now(), text: action.payload, completed: false });
    },
    toggleTodo: (state, action) => {
      const todo = state.find(t => t.id === action.payload);
      if (todo) todo.completed = !todo.completed;
    }
  }
});
export const { addTodo, toggleTodo } = todoSlice.actions;
export default todoSlice.reducer;
```

### Tạo Async Thunk nhanh

```javascript
import { createAsyncThunk } from '@reduxjs/toolkit';

export const fetchUsers = createAsyncThunk('users/fetch', async (_, thunkAPI) => {
  try {
    const res = await fetch('/api/users');
    return await res.json();
  } catch (err) {
    return thunkAPI.rejectWithValue(err.message);
  }
});
// Nhớ bắt các case: fetchUsers.pending, fetchUsers.fulfilled, fetchUsers.rejected trong extraReducers!
```

### Tích hợp vào Component nhanh

```jsx
import { useSelector, useDispatch } from 'react-redux';
import { addTodo } from './todoSlice';

export function TodoApp() {
  const todos = useSelector(state => state.todos); // Lấy state
  const dispatch = useDispatch();                  // Lấy hàm dispatch

  return (
    <div>
      <button onClick={() => dispatch(addTodo('Học Redux'))}>Thêm</button>
      {todos.map(t => <p key={t.id}>{t.text}</p>)}
    </div>
  );
}
```

---

Hy vọng bộ tài liệu 5 bài học này sẽ giúp bạn làm chủ được Redux một cách trực quan và sâu sắc nhất. Hãy bắt tay vào xây dựng một ứng dụng React thực tế để kiểm nghiệm các kiến thức đã học!
