# Hướng dẫn chi tiết về Redux Toolkit (RTK)

Ở bài học trước, bạn đã biết cách hoạt động của Redux thuần. Dù mạnh mẽ, Redux thuần bị phàn nàn rất nhiều vì 3 lý do lớn:

1. **Quá nhiều boilerplate code**: Phải tạo action type, action creator, reducer riêng lẻ.
2. **Cấu hình Store phức tạp**: Việc thiết lập middleware (như Thunk) và DevTools đòi hỏi cấu hình thủ công rườm rà.
3. **Quản lý bất biến khó khăn**: Dễ sơ suất làm thay đổi trực tiếp (mutate) state cũ khi sử dụng toán tử spread (`...`).

**Redux Toolkit (RTK)** ra đời năm 2019 chính là giải pháp chuẩn hóa của đội ngũ Redux để giải quyết triệt để các vấn đề trên.

---

## 1. Tại sao Redux Toolkit lại là tiêu chuẩn hiện đại?

RTK mang đến các cải tiến đột phá:

- **Tích hợp sẵn thư viện Immer**: Giúp bạn viết mã cập nhật state trông giống như đang thay đổi trực tiếp (mutable), nhưng Immer sẽ tự động chuyển đổi nó thành cập nhật bất biến (immutable) một cách an toàn dưới nền.
- **`configureStore()`**: Thiết lập store cực nhanh với các cấu hình mặc định tuyệt vời (tự động bật Redux DevTools Extension, tích hợp sẵn Middleware chống biến đổi trực tiếp và Redux Thunk).
- **`createSlice()`**: Gom nhóm action creators và reducers vào một nơi duy nhất. Bạn không cần phải viết action type và action creator thủ công nữa.

---

## 2. Các API quan trọng nhất trong Redux Toolkit

### A. `configureStore()`

Thay thế cho `createStore` của Redux thuần. Nó tự động gộp các reducers của bạn, thêm middleware mặc định (bao gồm `redux-thunk`), và kích hoạt Redux DevTools.

```javascript
import { configureStore } from '@reduxjs/toolkit';
import counterReducer from './counterSlice';
import userReducer from './userSlice';

const store = configureStore({
  reducer: {
    counter: counterReducer,
    user: userReducer,
  },
  // DevTools và Thunk tự động được bật!
});
```

### B. `createSlice()`

Đây là API quan trọng nhất và được dùng nhiều nhất. Nó nhận vào tên của slice, state ban đầu, và một object chứa các hàm reducer. Sau đó nó tự động sinh ra các action tương ứng.

```javascript
import { createSlice } from '@reduxjs/toolkit';

const counterSlice = createSlice({
  name: 'counter',
  initialState: { value: 0 },
  reducers: {
    // Nhờ Immer, ta có thể viết "state.value++" thay vì "{ ...state, value: state.value + 1 }"
    increment: (state) => {
      state.value += 1;
    },
    decrement: (state) => {
      state.value -= 1;
    },
    incrementByAmount: (state, action) => {
      state.value += action.payload;
    }
  }
});

// RTK tự động tạo Action Creators từ các key trong reducers
export const { increment, decrement, incrementByAmount } = counterSlice.actions;

// Export Reducer để đưa vào configureStore
export default counterSlice.reducer;
```

#### Lưu ý về Immer trong `createSlice`

Immer hoạt động bằng cách theo dõi các thay đổi trên một đối tượng tạm thời (draft state) và sinh ra một object mới. Bạn chỉ được phép:

- Thay đổi trực tiếp thuộc tính: `state.value = 10`
- Hoặc trả về một đối tượng mới hoàn toàn: `return { value: 10 }`
- **KHÔNG ĐƯỢC LÀM CẢ HAI CÙNG LÚC** (Ví dụ: vừa gán `state.value = 10` vừa `return state` là sai).

### C. `createAsyncThunk()`

Dùng để xử lý các hành động bất đồng bộ (ví dụ: Fetching API).
`createAsyncThunk` nhận vào:

1. Một chuỗi định danh hành động (action type string).
2. Một hàm callback thực hiện tác vụ bất đồng bộ (trả về một Promise).

Nó sẽ tự động sinh ra 3 action tương ứng với trạng thái của Promise:

- `pending`: Khi API bắt đầu được gọi.
- `fulfilled`: Khi API thành công (Promise resolve).
- `rejected`: Khi API thất bại (Promise reject).

```javascript
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';

// 1. Tạo async thunk
export const fetchUserById = createAsyncThunk(
  'users/fetchById',
  async (userId, thunkAPI) => {
    const response = await fetch(`https://jsonplaceholder.typicode.com/users/${userId}`);
    if (!response.ok) {
      throw new Error('Không thể tải thông tin người dùng');
    }
    const data = await response.json();
    return data; // Đây sẽ là action.payload trong case fulfilled
  }
);

// 2. Định nghĩa slice và xử lý các trạng thái của async thunk thông qua extraReducers
const userSlice = createSlice({
  name: 'users',
  initialState: {
    data: null,
    loading: false,
    error: null
  },
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchUserById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchUserById.fulfilled, (state, action) => {
        state.loading = false;
        state.data = action.payload; // Nhận dữ liệu trả về từ callback
      })
      .addCase(fetchUserById.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message;
      });
  }
});

export default userSlice.reducer;
```

---

## 3. Ví dụ lập trình hoàn chỉnh bằng JavaScript với Redux Toolkit

Dưới đây là một ví dụ hoàn chỉnh kết hợp cả Synchronous Action (bộ đếm) và Asynchronous Action (fetch API) bằng Node.js sử dụng Redux Toolkit:

### Cài đặt thư viện

```bash
npm install @reduxjs/toolkit
```

### Mã nguồn minh họa (`demo-rtk.js`)

```javascript
const { configureStore, createSlice, createAsyncThunk } = require('@reduxjs/toolkit');

// 1. Tạo Async Thunk giả lập gọi API bất đồng bộ
const fetchUserData = createAsyncThunk(
  'user/fetchUserData',
  async (userId, thunkAPI) => {
    // Giả lập delay 1 giây
    await new Promise((resolve) => setTimeout(resolve, 1000));
    
    if (userId === 0) {
      throw new Error('ID người dùng không hợp lệ!');
    }
    return { id: userId, name: 'Nam Nguyen', role: 'Developer' };
  }
);

// 2. Tạo User Slice
const userSlice = createSlice({
  name: 'user',
  initialState: {
    profile: null,
    status: 'idle', // 'idle' | 'loading' | 'succeeded' | 'failed'
    error: null
  },
  reducers: {
    clearUser: (state) => {
      state.profile = null;
      state.status = 'idle';
    }
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchUserData.pending, (state) => {
        state.status = 'loading';
        console.log('--- Đang tải dữ liệu người dùng... ---');
      })
      .addCase(fetchUserData.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.profile = action.payload;
      })
      .addCase(fetchUserData.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.error.message;
      });
  }
});

// 3. Cấu hình Store
const store = configureStore({
  reducer: {
    user: userSlice.reducer
  }
});

// Lắng nghe thay đổi store
store.subscribe(() => {
  console.log('State hiện tại:', JSON.stringify(store.getState(), null, 2));
});

// 4. Chạy demo các action đồng bộ và bất đồng bộ
async function runDemo() {
  console.log('--- Bắt đầu Demo ---');
  
  // Dispatch một action đồng bộ
  console.log('\nDispatch action đồng bộ clearUser:');
  store.dispatch(userSlice.actions.clearUser());

  // Dispatch action bất đồng bộ thành công
  console.log('\nDispatch fetchUserData với ID = 1:');
  await store.dispatch(fetchUserData(1));

  // Dispatch action bất đồng bộ thất bại
  console.log('\nDispatch fetchUserData với ID = 0 (Lỗi):');
  await store.dispatch(fetchUserData(0));
}

runDemo();
```

---

Như vậy bạn đã nắm vững cách Redux Toolkit giúp code gọn gàng, xử lý bất biến an toàn và quản lý logic bất đồng bộ. Ở bài tiếp theo, chúng ta sẽ kết nối Redux Toolkit vào một ứng dụng React thực tế bằng **React Redux**.
