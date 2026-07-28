import { createSlice } from "@reduxjs/toolkit";

const counterSlice = createSlice({
    name: "counter",
    initialState: parseInt(localStorage.getItem("counter")) || 0,
    reducers: {
        increment: (state) => state + 1,
        decrement: (state) => state - 1,
    }
})

export const { increment, decrement } = counterSlice.actions;
export default counterSlice.reducer;
