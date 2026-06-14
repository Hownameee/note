import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

interface Todo {
    id: string,
    text: string,
    completed: boolean,
}

type TodoState = Todo[];

const todoSlice = createSlice({
    name: "todo",
    initialState: [] as TodoState,
    reducers: {
        addTodo: (state, action: PayloadAction<string>) => {
            state.push({
                id: crypto.randomUUID(),
                text: action.payload,
                completed: false,
            })
        },

        toggleTodo: (state, action: PayloadAction<string>) => {
            const todo = state.find((todo) => todo.id === action.payload)
            if (todo) {
                todo.completed = !todo.completed
            }
        },

        deleteTodo: (state, action: PayloadAction<string>) => {
            return state.filter((todo) => todo.id !== action.payload)
        }
    }
})

export const { addTodo, toggleTodo, deleteTodo } = todoSlice.actions;
export default todoSlice.reducer;