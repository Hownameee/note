# Hướng dẫn chi tiết về RTK Query (Data Fetching & Caching)

Trong phát triển ứng dụng web hiện đại, phần lớn state toàn cục của ứng dụng thực chất là **dữ liệu được lấy từ máy chủ (Server State)**.
Nếu dùng `createAsyncThunk` truyền thống, bạn sẽ phải:

1. Tự viết mã gọi API.
2. Quản lý trạng thái loading, error, success bằng tay trong `extraReducers`.
3. Tự viết logic tránh gọi trùng lặp API khi đã có dữ liệu cũ.
4. Tự viết logic tải lại dữ liệu (refetch) khi dữ liệu phía server thay đổi.

**RTK Query** là một add-on mạnh mẽ nằm trong gói `@reduxjs/toolkit` để tự động hóa hoàn toàn các công việc này. Nó lấy cảm hứng từ các thư viện quản lý dữ liệu server nổi tiếng như *React Query* hay *SWR*.

---

## 1. Các Tính Năng Nổi Bật của RTK Query

- **Tự động sinh ra React Hooks**: Dựa trên các endpoints bạn định nghĩa, RTK Query tự động tạo ra các hook như `useGetUsersQuery`, `useUpdateUserMutation`, giúp gọi API trực tiếp trong component.
- **Quản lý Cache thông minh**: Dữ liệu tải về sẽ được cache lại. Nếu hai component cùng yêu cầu một API, chỉ có 1 request thực tế được gửi đi.
- **Theo dõi trạng thái Request**: Cung cấp sẵn các biến trạng thái cực kỳ tiện lợi: `isLoading`, `isFetching`, `isSuccess`, `isError`, `error`.
- **Cơ chế Invalidation (Tags System)**: Tự động tải lại dữ liệu mới khi thực hiện các tác vụ thêm/sửa/xóa (Mutations) thông qua cơ chế gắn nhãn (Tags).
- **Optimistic Updates**: Cập nhật giao diện lập tức trước khi server phản hồi thành công để tăng trải nghiệm người dùng (UX).

---

## 2. Các Khái Niệm Quan Trọng

### A. `createApi()`

Điểm bắt đầu của RTK Query. Nó định nghĩa một tập hợp các endpoints để kết nối tới server.

```javascript
import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

export const pokemonApi = createApi({
  reducerPath: 'pokemonApi', // Tên định danh reducer trong store
  baseQuery: fetchBaseQuery({ baseUrl: 'https://pokeapi.co/api/v2/' }), // URL gốc
  endpoints: (builder) => ({
    // Endpoint lấy dữ liệu (Query)
    getPokemonByName: builder.query({
      query: (name) => `pokemon/${name}`,
    }),
  }),
});

// Tự động sinh ra hook: useGetPokemonByNameQuery
export const { useGetPokemonByNameQuery } = pokemonApi;
```

### B. Queries vs Mutations

- **Queries (Truy vấn)**: Được sử dụng để **đọc dữ liệu** từ server (thường là phương thức `GET`).
- **Mutations (Đột biến)**: Được sử dụng để **gửi dữ liệu** cập nhật lên server, tạo ra các thay đổi (thường là các phương thức `POST`, `PUT`, `PATCH`, `DELETE`).

### C. Cache Invalidation (Hệ thống Tags)

Đây là tính năng rất hay giúp đồng bộ dữ liệu UI và Server.

- **`providesTags`**: Gắn nhãn (tag) cho dữ liệu trả về từ một Query.
- **`invalidatesTags`**: Khai báo rằng Mutation này sẽ làm cho các tag tương ứng bị hết hạn (invalidated). Khi đó, các Query đang hiển thị trên UI có tag đó sẽ tự động refetch lại dữ liệu mới nhất từ server.

---

## 3. Ví dụ lập trình hoàn chỉnh với RTK Query

Dưới đây là mã nguồn xây dựng chức năng quản lý bài viết (Posts) đầy đủ CRUD:

### Bước 1: Khai báo API Service (`features/posts/postsApi.js`)

```javascript
import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

export const postsApi = createApi({
  reducerPath: 'postsApi',
  baseQuery: fetchBaseQuery({ baseUrl: 'https://jsonplaceholder.typicode.com/' }),
  // Đăng ký các tag định danh
  tagTypes: ['Post'],
  endpoints: (builder) => ({
    // 1. Query: Lấy danh sách posts
    getPosts: builder.query({
      query: () => 'posts?_limit=5',
      // Dữ liệu này sẽ được gắn nhãn 'Post'
      providesTags: (result) =>
        result
          ? [...result.map(({ id }) => ({ type: 'Post', id })), { type: 'Post', id: 'LIST' }]
          : [{ type: 'Post', id: 'LIST' }],
    }),
    
    // 2. Mutation: Thêm mới post
    createPost: builder.mutation({
      query: (newPost) => ({
        url: 'posts',
        method: 'POST',
        body: newPost,
      }),
      // Khi hành động này thành công, nhãn 'LIST' sẽ hết hạn, kích hoạt getPosts gọi lại API
      invalidatesTags: [{ type: 'Post', id: 'LIST' }],
    }),
  }),
});

// RTK Query tự tạo ra các hooks tương ứng
export const { useGetPostsQuery, useCreatePostMutation } = postsApi;
```

### Bước 2: Cấu hình Store (`app/store.js`)

Chúng ta cần khai báo api reducer và api middleware để RTK Query quản lý cache và các vòng đời request.

```javascript
import { configureStore } from '@reduxjs/toolkit';
import { postsApi } from '../features/posts/postsApi';

export const store = configureStore({
  reducer: {
    // Thêm reducer của RTK Query
    [postsApi.reducerPath]: postsApi.reducer,
  },
  // BẮT BUỘC thêm middleware của RTK Query để bật tính năng cache, polling, invalidation
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(postsApi.middleware),
});
```

### Bước 3: Sử dụng Hook trong React Component (`features/posts/PostsList.jsx`)

```jsx
import React, { useState } from 'react';
import { useGetPostsQuery, useCreatePostMutation } from './postsApi';

export function PostsList() {
  // 1. Gọi hook query
  const { data: posts, isLoading, isError, error, refetch } = useGetPostsQuery();
  
  // 2. Gọi hook mutation
  const [createPost, { isLoading: isCreating }] = useCreatePostMutation();
  const [title, setTitle] = useState('');

  const handleAddPost = async () => {
    if (!title.trim()) return;
    try {
      // Thực thi mutation gửi dữ liệu lên server
      await createPost({ title, body: 'Nội dung demo', userId: 1 }).unwrap();
      setTitle('');
      alert('Đã thêm bài viết thành công! Danh sách sẽ tự động được làm mới.');
    } catch (err) {
      console.error('Lỗi khi thêm bài viết:', err);
    }
  };

  if (isLoading) return <div>Đang tải danh sách bài viết...</div>;
  if (isError) return <div>Đã xảy ra lỗi: {error.message}</div>;

  return (
    <div style={{ padding: '20px', maxWidth: '600px', margin: 'auto' }}>
      <h2>Quản lý bài viết (RTK Query)</h2>
      
      {/* Form thêm bài viết */}
      <div style={{ marginBottom: '20px' }}>
        <input
          type="text"
          placeholder="Nhập tiêu đề bài viết..."
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          style={{ width: '70%', padding: '8px' }}
        />
        <button 
          onClick={handleAddPost} 
          disabled={isCreating}
          style={{ marginLeft: '10px', padding: '8px 15px' }}
        >
          {isCreating ? 'Đang thêm...' : 'Thêm mới'}
        </button>
      </div>

      <button onClick={refetch} style={{ marginBottom: '15px' }}>
        Tải lại thủ công (Refetch)
      </button>

      {/* Hiển thị danh sách bài viết */}
      <ul>
        {posts?.map((post) => (
          <li key={post.id} style={{ marginBottom: '10px', textAlign: 'left' }}>
            <strong>ID: {post.id}</strong> - {post.title}
          </li>
        ))}
      </ul>
    </div>
  );
}
```

---

## 4. Tóm tắt: Khi nào dùng Async Thunk vs RTK Query?

| Tiêu chí | `createAsyncThunk` | RTK Query |
| :--- | :--- | :--- |
| **Mục đích chính** | Logic bất đồng bộ đa dụng (lưu trữ file, thao tác local phức tạp, side effects chung). | Gọi API dạng REST hoặc GraphQL (CRUD dữ liệu từ Server). |
| **Quản lý Cache** | Phải tự viết logic lưu trữ và kiểm tra cache. | Tự động hoàn toàn dựa trên cấu hình endpoints. |
| **Theo dõi Loading/Error** | Phải tự viết reducers để quản lý các biến này. | Tự động trả về các biến boolean (`isLoading`, `isError`,...). |
| **Hỗ trợ UI Hooks** | Không. Chỉ trả về action creator. | Tự động sinh ra custom React Hooks. |

**Lời khuyên**: Đối với các tác vụ liên quan đến Fetching API, hãy ưu tiên dùng **RTK Query**. Chỉ dùng **Async Thunk** khi bạn cần làm các tác vụ bất đồng bộ ngoài luồng API (ví dụ: truy cập thiết bị phần cứng, thanh toán phức tạp nhiều bước, hoặc tích hợp thư viện bên ngoài).
