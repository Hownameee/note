import { createAsyncThunk, createSlice, type PayloadAction } from "@reduxjs/toolkit";

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

export const fetchUsers = createAsyncThunk(
    "user/fetchUsers",
    async () => {
        const response = await fetch("https://jsonplaceholder.typicode.com/users");
        return response.json();
    }
)

const userSlice = createSlice({
    name: "user",
    initialState: { users: [], loading: false, error: null } as UserState,
    reducers: {},
    extraReducers(builder) {
        builder
            .addCase(fetchUsers.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(fetchUsers.fulfilled, (state, action: PayloadAction<User[]>) => {
                state.loading = false;
                state.users = action.payload;
            })
            .addCase(fetchUsers.rejected, (state, action) => {
                state.loading = false;
                state.error = action.error.message;
            })
    }
})

export default userSlice.reducer;
export const { } = userSlice.actions;
