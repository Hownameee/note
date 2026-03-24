# Overview

## Bức tranh toàn cảnh kiến trúc Kubernetes

Hãy tưởng tượng bạn đang nhìn vào một sa bàn chiến thuật. Đây là bức tranh toàn cảnh của hệ thống Kubernetes mà bạn đang xây dựng:

### 1. Tầng Hạ tầng vật lý (Lớp nền móng)

Dưới cùng của bức tranh là những cỗ máy thực sự (hoặc máy ảo VirtualBox của bạn).

* **Các máy chủ (Nodes):** Bao gồm 1 Master và 1 (hoặc nhiều) Worker.
* **Mạng Node (Đường cao tốc thật):** Các máy này kết nối với nhau bằng cáp mạng thật (hoặc switch ảo Host-Only của VirtualBox). Chúng nhận diện nhau bằng **IP thật** (Ví dụ: `192.168.56.10` cho Master, `192.168.56.11` cho Worker).

### 2. Tầng Quản lý - Control Plane (Vương quốc của Master)

Nằm trên máy Master là bộ não điều phối, nơi **không** chạy ứng dụng của người dùng, mà chỉ chạy các phần mềm quản lý hệ thống:

* **`etcd` (Sổ vàng tĩnh lặng):** Cơ sở dữ liệu lưu trữ toàn bộ sự thật về cụm. Mọi cấu hình, mọi trạng thái lý tưởng ("Tôi muốn 3 Pod Nginx") đều nằm ở đây.
* **`kube-apiserver` (Trạm tiếp khách):** Cửa ngõ duy nhất. Bất kể là bạn gõ lệnh `kubectl` từ laptop, hay `kubelet` từ Worker muốn báo cáo, tất cả đều phải xếp hàng đi qua API Server. Không ai được phép nói chuyện trực tiếp với `etcd`.
* **`kube-scheduler` (Người xếp chỗ):** Liên tục dòm ngó. Thấy có Pod mới cần chạy mà chưa có chỗ, nó sẽ quét các Worker, tính toán RAM/CPU và quyết định gán Pod đó về Worker nào rảnh nhất.
* **`kube-controller-manager` (Đội phản ứng nhanh):** Liên tục đối chiếu thực tế với cuốn sổ `etcd`. Nếu thấy thiếu (do máy sập) thì nó ra lệnh bù vào. Nếu thấy thừa thì nó ra lệnh xóa bớt.

### 3. Tầng Thực thi - Data Plane (Công trường của Worker)

Nằm trên máy Worker là nơi đổ mồ hôi để chạy ứng dụng của bạn:

* **`kubelet` (Quản đốc công trường):** Liên tục gọi điện (Heartbeat) về cho API Server để báo cáo "Em vẫn sống". Nó nhận bản vẽ từ API Server và đốc thúc công nhân làm việc.
* **Container Runtime như `containerd` (Công nhân bốc vác):** Kẻ thực sự đi kéo Image (từ Docker Hub) về và nhốt vào trong các Container cách ly trên hệ điều hành Linux.
* **`kube-proxy` (Cảnh sát giao thông):** Can thiệp vào hệ thống mạng của Linux (iptables/IPVS) để đảm bảo luồng dữ liệu mạng đi đúng hướng đến đúng Pod.

### 4. Tầng Mạng ảo - Overlay Network (Hệ thống đường hầm bưu điện)

Phủ lên toàn bộ cụm máy tính này là một mạng lưới ma trận ảo (do Plugin mạng như Flannel hoặc Calico tạo ra).

* Mạng này cung cấp một dải **IP ảo** (ví dụ `172.29.x.x`).
* Mọi Pod sinh ra đều được cắm một cái IP ảo duy nhất này.
* Khi Pod A ở Worker 1 muốn gửi dữ liệu cho Pod B ở Worker 2, "Trạm bưu điện" CNI (Calico) sẽ lấy gói tin ảo, **đóng gói (encapsulate)** vào một gói tin thật chạy trên IP thật (`192.168.56.x`), ném qua đường cao tốc vật lý sang Worker 2, rồi bóc hộp ra đưa thẳng cho Pod B.

---

## Bức tranh chuyển động: Chuyện gì xảy ra khi bạn gõ 1 lệnh?

Hãy kết nối toàn bộ hệ thống này bằng một luồng công việc thực tế. Bạn gõ: `kubectl apply -f web.yaml` (Yêu cầu chạy 1 ứng dụng Web).

1. Lệnh của bạn bay đến **API Server**.
2. **API Server** kiểm tra quyền của bạn, rồi ghi yêu cầu này vào **`etcd`**.
3. **Scheduler** lập tức phát hiện: *"Có 1 Pod Web cần chạy, Worker 1 đang rảnh, ném về Worker 1"*. Lựa chọn này được ghi lại vào **`etcd`**.
4. **Kubelet** trên Worker 1 đang nghe ngóng API Server, thấy có tên mình được gọi: *"A, sếp giao chạy Pod Web"*.
5. **Kubelet** ra lệnh cho **`containerd`**: *"Kéo image Web về, bật lên"*.
6. Trạm bưu điện mạng (CNI) cấp cho Pod Web này một cái IP ảo (`172.29.1.100`).
7. **Kubelet** báo cáo lại cho **API Server**: *"Nhiệm vụ hoàn tất, Pod đang chạy, IP là 172.29.1.100"*. Thông tin này lại được ghi vào **`etcd`**.
8. **kube-proxy** trên tất cả các Node nhận được thông tin về IP mới, lập tức cập nhật biển báo giao thông (`iptables`): *"Từ nay ai hỏi đường tới Web thì rẽ vào 172.29.1.100 nhé"*.
9. **Kịch bản tự chữa lành:** Nếu đêm nay Worker 1 cháy ổ cứng chết tươi -> **Kubelet** ngừng gửi nhịp tim -> **Controller Manager** phát hiện sự cố -> Xóa Pod cũ, tạo yêu cầu Pod mới -> **Scheduler** lại xếp chỗ sang Worker 2 -> Quá trình lặp lại hoàn toàn tự động.

Bức tranh kiến trúc này biến hàng trăm cái máy tính rời rạc thành **một siêu máy tính duy nhất** có khả năng tự vận hành và tự chữa lành.
