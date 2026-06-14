import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

export interface CartItem {
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

const cartSlice = createSlice({
    name: "cart",
    initialState: { items: [], totalQuantity: 0, totalPrice: 0 } as CartState,
    reducers: {
        addToCart: (state, action: PayloadAction<CartItem>) => {
            console.log(action.payload);
            const item = action.payload

            const existingItem = state.items.find((i) => i.name === item.name);

            if (existingItem) {
                existingItem.quantity++;
                existingItem.totalItemPrice += item.price;
            } else {
                state.items.push(item);
            }

            state.totalQuantity++;
            state.totalPrice += item.price;
        },

        removeFromCart: (state, action: PayloadAction<string>) => {
            const id = action.payload

            const existingItem = state.items.find((i) => i.id === id);

            if (existingItem) {
                existingItem.quantity--;
                existingItem.totalItemPrice -= existingItem.price;

                if (existingItem.quantity === 0) {
                    state.items = state.items.filter((i) => i.id !== id);
                }

                state.totalQuantity--;
                state.totalPrice -= existingItem.price;
            }
        },

        clearCart: (state) => {
            state.items = [];
            state.totalQuantity = 0;
            state.totalPrice = 0;
        },
    }
})

export default cartSlice.reducer
export const { addToCart, removeFromCart, clearCart } = cartSlice.actions