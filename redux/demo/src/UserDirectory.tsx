import { useAppDispatch, useAppSelector } from "./app/store";
import { fetchUsers } from "./app/slices/userSlice";

export default function UserDirectory() {
  const dispatch = useAppDispatch();
  const { users, loading, error } = useAppSelector((state) => state.user);

  // Helper to get initials from name (e.g., "Nam Nguyen" -> "NN")
  const getInitials = (name: string) => {
    if (!name) return "?";
    const parts = name.split(" ");
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  return (
    <div className="card" style={{ minHeight: "400px", width: "100%" }}>
      {/* Top Control Bar */}
      <div style={{ 
        display: "flex", 
        justifyContent: "space-between", 
        alignItems: "center",
        flexWrap: "wrap",
        gap: "16px",
        borderBottom: "1px solid rgba(255, 255, 255, 0.08)",
        paddingBottom: "16px",
        marginBottom: "20px"
      }}>
        <h2 style={{ fontSize: "1.35rem", fontWeight: 600, color: "#fff", display: "flex", alignItems: "center", gap: "10px" }}>
          👥 Danh Sách Thành Viên ({users.length})
        </h2>
        <button 
          className="btn btn-primary" 
          onClick={() => dispatch(fetchUsers())}
          disabled={loading}
        >
          {loading ? "Đang tải..." : "Tải Danh Sách"}
        </button>
      </div>

      {/* Dynamic Content Area */}
      {loading ? (
        <div className="spinner-wrapper">
          <div className="spinner"></div>
          <p style={{ color: "var(--text-secondary)", fontSize: "0.95rem" }}>
            Đang tải danh sách từ JSONPlaceholder API...
          </p>
        </div>
      ) : error ? (
          <div style={{
            background: "rgba(239, 68, 68, 0.1)",
            border: "1px solid rgba(239, 68, 68, 0.2)",
            borderRadius: "12px",
            padding: "20px",
            color: "#fca5a5",
            textAlign: "center",
            marginTop: "20px"
          }}>
            <span style={{ fontSize: "1.5rem", display: "block", marginBottom: "8px" }}>⚠️ Lỗi tải dữ liệu</span>
            <p style={{ fontSize: "0.95rem" }}>{error}</p>
          </div>
      ) : users.length === 0 ? (
        <div style={{ 
          display: "flex", 
          flexDirection: "column", 
          alignItems: "center", 
          justifyContent: "center", 
          flexGrow: 1, 
          padding: "60px 0",
          color: "var(--text-secondary)"
        }}>
          <span style={{ fontSize: "3rem", marginBottom: "16px" }}>📭</span>
          <p style={{ fontSize: "1rem", marginBottom: "20px", textAlign: "center" }}>
            Chưa có dữ liệu thành viên trong Store.
          </p>
          <button 
            className="btn btn-secondary" 
            onClick={() => dispatch(fetchUsers())}
          >
            Lấy Dữ Liệu Ngay
          </button>
        </div>
      ) : (
        /* Users Cards Grid */
        <div className="users-list-grid">
          {users.map((user) => (
            <div key={user.id} className="user-card">
              <div className="user-avatar-circle">
                {getInitials(user.name)}
              </div>
              <h3 className="user-name-text">{user.name}</h3>
              <span className="user-email-text" title={user.email}>
                {user.email}
              </span>
              <span className="user-phone-text">
                📞 {user.phone.split(" ")[0]}
              </span>
              <div style={{ marginTop: "auto" }}>
                <span className="user-company-badge">
                  💼 {(user as any).company?.name || "Independent"}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}