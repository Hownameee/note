# KUBERNETES KUBECTL CHEAT SHEET

## 1. Quản lý Hệ thống & Ngữ cảnh (Cluster & Context)

```bash
kubectl get nodes                                            # Xem danh sách và trạng thái các máy (Master, Worker)
kubectl config get-contexts                                  # Xem bạn đang đứng ở Context/Namespace nào
kubectl config set-context --current --namespace=<namespace> # Chuyển Namespace mặc định hiện tại
```

## 2. Quản lý Namespace (Phòng ban)

```bash
kubectl get namespaces                                       # Xem toàn bộ các Namespace đang có
kubectl create namespace dev                                 # Tạo một Namespace mới tên là 'dev'
kubectl delete namespace dev                                 # Xóa Namespace (LƯU Ý: Xóa SẠCH mọi tài nguyên bên trong)
```

## 3. Thao tác với Pod (Triển khai & Quản lý)

```bash
kubectl apply -f kube/nginx-pod.yaml                         # Tạo/Cập nhật Pod từ bản vẽ YAML
kubectl get pods                                             # Xem các Pod trong Namespace mặc định hiện tại
kubectl get pods -n kube-system                              # Xem Pod trong một Namespace cụ thể (VD: kube-system)
kubectl get pods --all-namespaces                            # Xem toàn bộ Pod trên tất cả các Namespace
kubectl get pods -A                                          # Gõ tắt của lệnh trên (--all-namespaces)
kubectl delete pod nginx-pod                                 # Xóa một Pod cụ thể
kubectl delete -f kube/nginx-pod.yaml                        # Xóa tất cả các tài nguyên được khai báo trong file YAML
```

## 4. Gỡ lỗi & Bắt bệnh (Troubleshooting)

```bash
kubectl describe pod nginx-pod                               # Khám bệnh: Xem chi tiết thông tin, lịch sử xếp chỗ và các lỗi của Pod
kubectl logs nginx-pod                                       # Xem log (console output) của ứng dụng trong Pod
kubectl logs -f nginx-pod                                    # Xem log chạy liên tục theo thời gian thực (giống 'tail -f')
kubectl exec -it nginx-pod -- /bin/sh                        # Đột nhập vào bên trong Container của Pod để gõ lệnh Linux (dùng bash nếu có)
```

## 5. Các cờ mở rộng hữu ích (Modifiers)

```bash
kubectl get pods -o wide                                     # Xem thêm IP ảo của Pod và tên Worker Node đang chạy Pod đó
kubectl get pods -w                                          # Theo dõi trạng thái Pod liên tục (màn hình tự cập nhật khi trạng thái đổi)
kubectl get pod nginx-pod -o yaml                            # Trích xuất toàn bộ cấu hình thực tế của Pod ra định dạng YAML
```

## 6. Xem thông số & Tài nguyên của Node (CPU, RAM, Disk)

> *(Dữ liệu tĩnh được Kubelet báo cáo và lưu trong etcd)*

```bash
# XEM CHI TIẾT (Dễ đọc nhất): Xem toàn bộ "hồ sơ" của một Node cụ thể
kubectl describe node worker-node

# LƯU Ý QUAN TRỌNG KHI ĐỌC LỆNH DESCRIBE NODE:
# 1. Tìm mục `Capacity`: Đây là tổng tài nguyên vật lý/ảo hóa của máy (Ví dụ: RAM 2GB, CPU 2 Core).
# 2. Tìm mục `Allocatable`: Đây là tài nguyên THỰC TẾ mà Pod được phép xài (Luôn nhỏ hơn Capacity vì K8s phải giữ lại một phần RAM/CPU để nuôi HĐH Linux và Kubelet).

# XEM DỮ LIỆU THÔ: Trích xuất toàn bộ cấu hình gốc của Node ra định dạng YAML
kubectl get node worker-node -o yaml
```

## 7. Giám sát tài nguyên Thời gian thực (Real-time Metrics)

> *LƯU Ý: Nhóm lệnh `top` giống như Task Manager của Windows. Để chạy được các lệnh này, cụm của bạn BẮT BUỘC phải cài đặt thêm một Add-on tên là "Metrics Server".*

```bash
# Xem Node nào đang bị quá tải (Hiển thị % CPU và RAM đang sử dụng của từng máy)
kubectl top nodes

# Xem Pod nào đang ngốn nhiều tài nguyên nhất trong Namespace hiện tại
kubectl top pods

# Xem mức độ tiêu thụ CPU/RAM của toàn bộ Pod trên hệ thống
kubectl top pods -A

# Sắp xếp các Pod theo thứ tự ngốn RAM nhiều nhất đến ít nhất
kubectl top pods --sort-by=memory
```

## 8. Mạng lưới & Phơi bày ứng dụng (Networking & Expose)

```bash
# Xuyên hầm (CỰC KỲ HỮU ÍCH): Kéo cổng 80 của Pod Nginx ra cổng 8080 của máy tính thật để xem thẳng trên trình duyệt
kubectl port-forward pod/nginx-pod 8080:80 --address 0.0.0.0

# Tạo nhanh một Service (Cảnh sát giao thông) cho Pod mà không cần viết file YAML
kubectl expose pod nginx-pod --port=80 --name=nginx-service --type=NodePort

# Xem danh sách các Service đang chạy (để lấy IP tĩnh hoặc Port)
kubectl get services
# Hoặc gõ tắt: kubectl get svc
```

## 9. Quản lý File (Sao chép dữ liệu In/Out)

```bash
# Copy file từ máy thật thả VÀO bên trong Container của Pod đang chạy
kubectl cp ./index.html nginx-pod:/usr/share/nginx/html/index.html

# Copy file TỪ bên trong Container của Pod lấy ra ngoài máy thật
kubectl cp nginx-pod:/etc/nginx/nginx.conf ./nginx.conf
```

## 10. Chỉnh sửa "Nóng" (Sửa trực tiếp không cần file)

```bash
# Mở cấu hình YAML của một tài nguyên đang chạy bằng trình soạn thảo (vi/nano). Lưu file lại là hệ thống tự cập nhật ngay lập tức!
kubectl edit pod nginx-pod
kubectl edit service nginx-service
```

## 11. Mở rộng & Cập nhật phiên bản (Deployments & Rollouts)

> *(Nhóm lệnh này dùng cho `Deployment` - Cấp quản lý cao hơn của Pod, thường dùng trong môi trường Production)*

```bash
# Xem danh sách các Deployments
kubectl get deployments
# Hoặc gõ tắt: kubectl get deploy

# SCALE: Mở rộng/Thu hẹp ứng dụng. (Ví dụ: Đang có 1 Pod, gọi thêm 4 Pod nữa chạy song song để gánh tải)
kubectl scale deployment my-app --replicas=5

# THEO DÕI: Xem tiến trình khi bạn vừa cập nhật phiên bản mới (Image mới) cho ứng dụng
kubectl rollout status deployment my-app

# LỊCH SỬ: Xem lại lịch sử các lần cập nhật phiên bản trước đó
kubectl rollout history deployment my-app

# QUAY XE (ROLLBACK): Nếu bản cập nhật mới bị lỗi sập web, lùi ngay về phiên bản chạy ổn định liền trước đó
kubectl rollout undo deployment my-app
```
