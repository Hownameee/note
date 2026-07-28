import { useAppDispatch, useAppSelector } from "./app/store";
import { increment, decrement } from "./app/slices/counterSlice";
import { addTodo, deleteTodo, toggleTodo } from "./app/slices/todoSlice";
import { useState, type FormEvent } from "react";
import { addToCart, clearCart, removeFromCart, type CartItem } from "./app/slices/cartSlice";
import UserDirectory from "./UserDirectory";

function App() {
  const dispatch = useAppDispatch();
  const count = useAppSelector((state) => state.counter);
  const todos = useAppSelector((state) => state.todo);
  const cart = useAppSelector((state) => state.cart);

  const [activeTab, setActiveTab] = useState<"playground" | "directory">("playground");
  const [todoInput, setTodoInput] = useState("");
  const [productName, setProductName] = useState("");
  const [productPrice, setProductPrice] = useState("");

  const handleAddTodo = (e: FormEvent) => {
    e.preventDefault();
    if (!todoInput.trim()) return;
    dispatch(addTodo(todoInput));
    setTodoInput("");
  };

  const handleAddToCart = (e: FormEvent) => {
    e.preventDefault();
    if (!productName.trim() || !productPrice) return;
    
    const priceNum = Number(productPrice);
    if (isNaN(priceNum) || priceNum <= 0) return;

    const item: CartItem = {
      id: Date.now().toString(),
      name: productName,
      price: priceNum,
      quantity: 1,
      totalItemPrice: priceNum,
    };
    
    dispatch(addToCart(item));
    setProductName("");
    setProductPrice("");
  };

  return (
    <div className="container">
      {/* Global Header */}
      <header className="header">
        <h1>Redux Toolkit Dashboard</h1>
        <p>Học tập & Thực hành Quản lý State trong React với Redux Toolkit</p>
      </header>

      {/* Tab Navigation */}
      <nav className="tab-navigation">
        <button 
          className={`tab-btn ${activeTab === "playground" ? "active" : ""}`}
          onClick={() => setActiveTab("playground")}
        >
          📊 Core & Toolkit Playground
        </button>
        <button 
          className={`tab-btn ${activeTab === "directory" ? "active" : ""}`}
          onClick={() => setActiveTab("directory")}
        >
          👥 Async User Directory
        </button>
      </nav>

      {/* Conditional Rendering */}
      {activeTab === "playground" ? (
        <main className="dashboard">
          
          {/* 1. Counter Card */}
          <section className="card">
            <h2 className="card-title">⏱️ Redux Counter</h2>
            <div className="counter-wrapper">
              <div className="counter-display">{count}</div>
              <div className="counter-controls">
                <button 
                  className="btn btn-secondary" 
                  onClick={() => dispatch(decrement())}
                  title="Giảm 1 đơn vị"
                >
                  -
                </button>
                <button 
                  className="btn btn-primary" 
                  onClick={() => dispatch(increment())}
                  title="Tăng 1 đơn vị"
                >
                  +
                </button>
              </div>
            </div>
          </section>

          {/* 2. Todo Card */}
          <section className="card">
            <h2 className="card-title">📝 Todo List</h2>
            
            <form onSubmit={handleAddTodo} className="form-group">
              <input 
                type="text" 
                className="input-field" 
                placeholder="Thêm công việc mới..." 
                value={todoInput} 
                onChange={(e) => setTodoInput(e.target.value)} 
              />
              <button type="submit" className="btn btn-primary">Thêm</button>
            </form>

            <div className="list-container">
              {todos.length === 0 ? (
                <p style={{ color: "var(--text-secondary)", textAlign: "center", marginTop: "20px", fontSize: "0.9rem" }}>
                  Chưa có công việc nào cần làm.
                </p>
              ) : (
                todos.map((todo) => (
                  <div key={todo.id} className="list-item">
                    <div 
                      className="list-item-content" 
                      onClick={() => dispatch(toggleTodo(todo.id))}
                    >
                      <div className={`checkbox-custom ${todo.completed ? "checked" : ""}`}></div>
                      <span className={`todo-text ${todo.completed ? "completed" : ""}`}>
                        {todo.text}
                      </span>
                    </div>
                    <button 
                      className="btn-icon" 
                      onClick={() => dispatch(deleteTodo(todo.id))}
                      title="Xóa công việc"
                    >
                      ✕
                    </button>
                  </div>
                ))
              )}
            </div>
          </section>

          {/* 3. Cart Card */}
          <section className="card">
            <h2 className="card-title">🛒 Giỏ Hàng</h2>
            
            <form onSubmit={handleAddToCart} className="form-group vertical">
              <div style={{ display: "flex", gap: "8px", width: "100%" }}>
                <input 
                  type="text" 
                  className="input-field" 
                  placeholder="Tên sản phẩm..." 
                  value={productName} 
                  onChange={(e) => setProductName(e.target.value)} 
                />
                <input 
                  type="number" 
                  className="input-field" 
                  placeholder="Giá ($)..." 
                  value={productPrice} 
                  onChange={(e) => setProductPrice(e.target.value)} 
                  style={{ width: "95px", flex: "none" }}
                />
              </div>
              <button type="submit" className="btn btn-primary" style={{ width: "100%" }}>
                Thêm vào Giỏ
              </button>
            </form>

            <div className="list-container" style={{ marginBottom: "20px" }}>
              {cart.items.length === 0 ? (
                <p style={{ color: "var(--text-secondary)", textAlign: "center", marginTop: "20px", fontSize: "0.9rem" }}>
                  Giỏ hàng trống.
                </p>
              ) : (
                cart.items.map((item) => (
                  <div key={item.id} className="list-item">
                    <div className="cart-item-info">
                      <span className="cart-item-name">{item.name}</span>
                      <span className="cart-item-meta">
                        ${item.price} × {item.quantity}
                      </span>
                    </div>
                    <span className="cart-item-price">${item.totalItemPrice}</span>
                    <button 
                      className="btn-icon" 
                      onClick={() => dispatch(removeFromCart(item.id))}
                      title="Bớt 1 sản phẩm"
                    >
                      ✕
                    </button>
                  </div>
                ))
              )}
            </div>

            <div className="cart-summary">
              <div className="summary-row">
                <span>Tổng số lượng:</span>
                <span style={{ fontWeight: 600 }}>{cart.totalQuantity}</span>
              </div>
              <div className="summary-row total">
                <span>Tổng thanh toán:</span>
                <span style={{ color: "var(--accent-pink)" }}>${cart.totalPrice}</span>
              </div>
              <button 
                className="btn btn-danger" 
                onClick={() => dispatch(clearCart())} 
                disabled={cart.items.length === 0}
                style={{ width: "100%", marginTop: "8px" }}
              >
                Xóa sạch Giỏ hàng
              </button>
            </div>
          </section>

        </main>
      ) : (
        <UserDirectory />
      )}
    </div>
  );
}

export default App;
