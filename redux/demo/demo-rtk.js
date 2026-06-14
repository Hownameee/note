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
        console.log('\n--- Đang tải dữ liệu người dùng... ---');
      })
      .addCase(fetchUserData.fulfilled, (state, action) => {
        console.log('\n--- Tải dữ liệu người dùng thành công... ---');
        state.status = 'succeeded';
        state.profile = action.payload;
      })
      .addCase(fetchUserData.rejected, (state, action) => {
        console.log('\n--- Tải dữ liệu người dùng thất bại... ---');
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
