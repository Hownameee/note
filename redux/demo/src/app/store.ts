import { configureStore } from "@reduxjs/toolkit";
import counterReducer from "./slices/counterSlice";
import todoReducer from "./slices/todoSlice";
import cartReducer from "./slices/cartSlice";
import userReducer from "./slices/userSlice";
import { useDispatch, useSelector } from "react-redux";

const store = configureStore({
    reducer: {
        counter: counterReducer,
        todo: todoReducer,
        cart: cartReducer,
        user: userReducer,
    }
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch

export const useAppDispatch = useDispatch.withTypes<AppDispatch>();
export const useAppSelector = useSelector.withTypes<RootState>();

export default store