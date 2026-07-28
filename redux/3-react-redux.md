# Hướng dẫn kết nối Redux với React qua React Redux

Bản thân Redux là một thư viện quản lý state chung, nó không quan tâm bạn đang viết code bằng React, Angular, Vue hay JavaScript thuần.
**React Redux** chính là thư viện kết nối chính thức (Official Bridge) giúp các component trong React có thể đọc dữ liệu và gửi actions tới Redux Store một cách dễ dàng và tối ưu nhất.

---

## 1. Cơ Chế Hoạt Động của React Redux

React Redux hoạt động dựa trên cơ chế React Context ngầm để truyền đối tượng `store` tới tất cả các component con mà không cần prop drilling.

```text
[ App Root ] (<Provider store={store}>)
     |
     +--> [ Parent Component ] (Không cần biết về Redux)
               |
               +--> [ Child Component ] (Sử dụng useSelector / useDispatch)
```

---

## 2. Các API và Hooks Quan Trọng Nhất

### A. `<Provider>`

Component bao bọc toàn bộ ứng dụng React của bạn ở file gốc (thường là `main.jsx` hoặc `index.jsx`). Nó nhận vào prop `store` được khởi tạo từ `configureStore`.

```jsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { Provider } from 'react-redux';
import { store } from './app/store';
import App from './App';

ReactDOM.createRoot(document.getElementById('root')).render(
  <Provider store={store}>
    <App />
  </Provider>
);
```

### B. Hook `useSelector()`

Dùng để trích xuất (đọc) dữ liệu từ Redux store state vào component React.

```jsx
import { useSelector } from 'react-redux';

const CounterValue = () => {
  // Lấy ra giá trị value từ slice counter
  const count = useSelector((state) => state.counter.value);
  
  return <div>Giá trị hiện tại: {count}</div>;
};
```

#### ⚠️ Lưu ý CỰC KỲ QUAN TRỌNG về hiệu năng với `useSelector`

- Mỗi khi có **bất kỳ** action nào được dispatch, `useSelector` sẽ chạy lại hàm selector.
- Nếu giá trị trả về của selector **khác với lần chạy trước**, component sẽ bị re-render.
- React Redux so sánh giá trị cũ và mới bằng toán tử so sánh nghiêm ngặt **`===` (Reference Equality - so sánh tham chiếu)**.

**Hậu quả**: Nếu bạn trả về một object hoặc array mới trong selector, ví dụ:

```jsx
// SAI: Tạo object mới ở mỗi lần chạy. Component sẽ LUÔN RE-RENDER mỗi khi store thay đổi bất kỳ thứ gì!
const { name, age } = useSelector(state => ({ name: state.user.name, age: state.user.age }));
```

**Giải pháp**:

1. Gọi `useSelector` nhiều lần cho từng giá trị nguyên thủy (primitive values như string, number, boolean):

   ```jsx
   const name = useSelector(state => state.user.name);
   const age = useSelector(state => state.user.age);
   ```

2. Sử dụng hàm so sánh nông `shallowEqual` đi kèm của `react-redux`:

   ```jsx
   import { useSelector, shallowEqual } from 'react-redux';
   
   const { name, age } = useSelector(
     state => ({ name: state.user.name, age: state.user.age }),
     shallowEqual // So sánh nông các thuộc tính bên trong object thay vì so sánh tham chiếu của cả object
   );
   ```

3. Sử dụng các memoized selector được tạo từ hàm `createSelector` (được tích hợp sẵn trong `@reduxjs/toolkit` từ thư viện Reselect).

### C. Hook `useDispatch()`

Trả về hàm `dispatch` từ Redux store. Bạn dùng nó để phát đi các action nhằm thay đổi state.

```jsx
import { useDispatch } from 'react-redux';
import { increment } from './counterSlice';

const CounterButtons = () => {
  const dispatch = useDispatch();

  return (
    <button onClick={() => dispatch(increment())}>
      Tăng giá trị
    </button>
  );
};
```

---

## 3. Tìm hiểu về Connect API (Cách làm cũ trước khi có React Hooks)

Trước khi React Hooks ra đời (React Redux v7.1.0), lập trình viên phải kết nối component với store thông qua một Higher-Order Component (HOC) tên là `connect()`. Bạn sẽ bắt gặp cách viết này trong các dự án cũ (Legacy Code):

```jsx
import React from 'react';
import { connect } from 'react-redux';
import { increment } from './counterSlice';

function Counter({ count, increment }) {
  return (
    <div>
      <p>{count}</p>
      <button onClick={increment}>Tăng</button>
    </div>
  );
}

// Map state từ store thành props của component
const mapStateToProps = (state) => ({
  count: state.counter.value
});

// Map action creators thành props tự động dispatch
const mapDispatchToProps = {
  increment
};

// Kết nối component
export default connect(mapStateToProps, mapDispatchToProps)(Counter);
```

*Khuyên dùng: Ở các dự án mới, hãy dùng 100% Hooks (`useSelector`, `useDispatch`) vì nó giúp code ngắn gọn, dễ đọc và dễ chia sẻ logic (Custom Hooks) hơn rất nhiều.*

---

## 4. Ví dụ luồng hoạt động hoàn chỉnh trong ứng dụng React

Dưới đây là cấu trúc thư mục tiêu chuẩn của một ứng dụng React + Redux Toolkit:

```text
src/
├── app/
│   └── store.js         # Khởi tạo Redux Store
├── features/
│   └── counter/
│       ├── counterSlice.js   # Logic Redux (Actions, Reducers)
│       └── Counter.jsx        # UI Component React
├── App.jsx
└── main.jsx
```

### Bước 1: Thiết lập Slice (`features/counter/counterSlice.js`)

```javascript
import { createSlice } from '@reduxjs/toolkit';

export const counterSlice = createSlice({
  name: 'counter',
  initialState: { value: 0 },
  reducers: {
    increment: (state) => { state.value += 1; },
    decrement: (state) => { state.value -= 1; },
    incrementByAmount: (state, action) => { state.value += action.payload; }
  }
});

export const { increment, decrement, incrementByAmount } = counterSlice.actions;
export default counterSlice.reducer;
```

### Bước 2: Thiết lập Store (`app/store.js`)

```javascript
import { configureStore } from '@reduxjs/toolkit';
import counterReducer from '../features/counter/counterSlice';

export const store = configureStore({
  reducer: {
    counter: counterReducer
  }
});
```

### Bước 3: Cung cấp Store cho ứng dụng (`main.jsx`)

```jsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { Provider } from 'react-redux';
import { store } from './app/store';
import App from './App';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </React.StrictMode>
);
```

### Bước 4: Sử dụng trong UI Component (`features/counter/Counter.jsx`)

```jsx
import React, { useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { increment, decrement, incrementByAmount } from './counterSlice';

export function Counter() {
  const count = useSelector((state) => state.counter.value);
  const dispatch = useDispatch();
  const [amount, setAmount] = useState(2);

  return (
    <div style={{ textAlign: 'center', marginTop: '50px' }}>
      <h1>Bộ đếm: {count}</h1>
      <div>
        <button onClick={() => dispatch(decrement())}>Giảm -</button>
        <button onClick={() => dispatch(increment())} style={{ marginLeft: '10px' }}>Tăng +</button>
      </div>
      <div style={{ marginTop: '20px' }}>
        <input 
          type="number" 
          value={amount} 
          onChange={(e) => setAmount(Number(e.target.value))} 
        />
        <button onClick={() => dispatch(incrementByAmount(amount))} style={{ marginLeft: '10px' }}>
          Tăng thêm {amount}
        </button>
      </div>
    </div>
  );
}
```

---

Như vậy bạn đã đi qua 3 phần quan trọng nhất của hệ sinh thái Redux. Để hoàn thiện kiến thức nâng cao hiện đại, hãy sang bài thứ 4 để học về **RTK Query** - công cụ tối ưu cho việc Fetch dữ liệu từ API thay vì dùng `createAsyncThunk` thông thường.
