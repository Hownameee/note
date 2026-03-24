# Details

## Master node

Không để chạy ứng dụng, chỉ điều phối:

- Api server: cửa giao tiếp, kubeadm init thực chất là 1 cái webserver (viết bằng Go), cần 1 địa chỉ ip và port (default là 6443) để  bind. kuctl cần biết ip đó để gửi lệnh.
- etcd: Cơ sở dữ liệu của K8s. Nó lưu trữ "trạng thái mong muốn" của toàn bộ hệ thống (ví dụ: "tôi muốn có 3 container chạy web").
- Scheduler: Người xếp chỗ. Khi bạn yêu cầu chạy một ứng dụng mới, Scheduler sẽ nhìn vào các Worker Node xem máy nào đang rảnh RAM, CPU để vứt ứng dụng đó sang.
- Controller manager: kubelet trên mỗi worker tự động gửi info (CPU, RAM, DISK) cho api server ở master mỗi n giây (maybe n = 10), Controller manager chỉ xem trong etcd thông qua api server nếu có worker quá lâu không gửi -> đánh dấu, sau 1 khoang tg không thấy thì remove node đó, di chuyển các pod sang worker khác.

kubeadm init khởi động 4 thành phần:

- kube-apiserver: Chạy như một web server liên tục mở cổng 6443 để nhận lệnh từ bạn và từ các Worker Node.
- etcd: Database chạy ngầm để lưu trữ trạng thái.
- kube-controller-manager: Chạy vòng lặp vô hạn (infinite loop) để liên tục soi xét hệ thống xem có máy nào sập, có Pod nào chết không.
- kube-scheduler: Chạy ngầm chờ chực xem có yêu cầu tạo Pod mới không để đi tìm máy Worker rảnh rỗi.

Hai thành phần nền tảng bắt buộc phải có:

- containerd (hoặc Docker): Vâng, Master Node cũng cần Container Runtime y hệt Worker Node. Tại sao? Vì 4 thành phần lõi ở trên thực chất cũng được đóng gói thành các Container!
- kubelet: Đây là điều bất ngờ nhất: Master Node cũng cài kubelet. Kubelet trên Master Node có nhiệm vụ rất đặc biệt: nó gọi thằng containerd để giữ cho 4 cái container của bộ não (apiserver, etcd, controller, scheduler) luôn luôn chạy. K8s gọi 4 cái container đặc biệt này là Static Pods.

## Worker Node (Phân xưởng sản xuất)

Trên mỗi Worker Node bắt buộc phải có **3 thành phần cốt lõi** đang chạy ngầm để nhận lệnh từ Master và thực thi ứng dụng:

### 1. Kubelet (Người quản đốc phân xưởng)

Đây là thành phần quan trọng nhất, đại diện cho Worker Node để giao tiếp với Master Node.

- **Nhận lệnh từ API Server:** Kubelet là thành phần duy nhất trên Worker Node nói chuyện với `kube-apiserver`. Khi nhận được "bản vẽ" (Pod Spec) yêu cầu chạy ứng dụng, nó sẽ đốc thúc Container Runtime thực thi.
- **Theo dõi sức khỏe Container (Health Check):** Kubelet liên tục giám sát các container (thông qua Liveness/Readiness Probe). Nếu một container bị lỗi hoặc crash, Kubelet sẽ tự động khởi động lại (restart) container đó ngay tại chỗ để đảm bảo ứng dụng luôn sống (Self-healing cấp độ Node).
- **Gửi Heartbeat (Báo cáo sinh tồn):** Cứ mỗi 10 giây, Kubelet gửi nhịp tim về cho Master Node kèm theo tình trạng tài nguyên (CPU, RAM, Disk đang trống bao nhiêu). Dựa vào đây, Master mới biết máy nào còn sống và rảnh rỗi để phân bổ Pod. Nếu ngắt kết nối quá thời gian quy định, Master sẽ đánh dấu Node này là `NotReady`.

### 2. Container Runtime (Cỗ máy bốc vác / Lắp ráp)

Kubernetes bản chất không biết cách chạy container, nó phải nhờ đến phần mềm chuyên trách (ví dụ: `containerd`, `CRI-O`, hoặc `Docker`).

- **Nhiệm vụ bốc vác:** Nhận lệnh từ Kubelet để đi kéo (pull) Image từ các kho chứa (như Docker Hub) về máy tính cục bộ.
- **Thực thi cách ly:** Giao tiếp sâu với nhân hệ điều hành Linux (sử dụng *cgroups* và *namespaces*) để tạo ra các môi trường cách ly (Container), bơm RAM/CPU vào đó và chạy ứng dụng lên.

### 3. Kube-proxy (Cảnh sát giao thông mạng)

Đảm bảo luồng dữ liệu mạng có thể tìm đúng đường đến các Pod, dù IP của Pod rất hay thay đổi (do bị xóa/tạo lại liên tục).

- **Quản lý quy tắc mạng:** Kube-proxy liên tục lắng nghe `API Server`. Khi bạn tạo ra một `Service` (một IP tĩnh đại diện cho nhiều Pod), Kube-proxy sẽ tiếp nhận thông tin này.
- **Sử dụng `iptables` hoặc `IPVS` của Linux:** Nó dịch lý thuyết thành hành động bằng cách viết các đạo luật (rules) thẳng vào tường lửa của hệ điều hành Linux.
  - *Ví dụ:* Nó thêm một luật rằng: *"Nếu có truy cập đi vào IP tĩnh của Service, hãy tự động điều hướng (NAT - bẻ lái) lưu lượng đó sang một trong các IP ảo của Pod đang chạy đằng sau"*. Điều này tạo ra cơ chế Cân bằng tải (Load Balancing) ngay dưới nền hệ điều hành.

## IP

### 1. IP của Pod (Số điện thoại nội bộ của nhân viên)

- **Nó là gì?** Đây là một địa chỉ IP **hoàn toàn ảo** do chính Kubernetes (cụ thể là các Plugin mạng như Flannel, Calico) tự tạo ra và quản lý. Lớp mạng ảo này được trải rộng bao phủ lên toàn bộ các máy Node trong cụm, gọi là **Overlay Network**.
- **Ai cấp phát?** Do Kubernetes cấp. Trong lệnh `kubeadm init`, tham số `--pod-network-cidr=172.29.0.0/16` chính là lúc bạn giao cho K8s quyền quản lý dải IP này:
  - Đây là một "kho số điện thoại nội bộ" khổng lồ (từ `172.29.0.1` đến `172.29.255.254`).
  - Cứ mỗi khi một Pod mới được sinh ra, K8s sẽ rút một IP trong kho này gắn cho nó (ví dụ: Pod A chạy web được cấp IP `172.29.1.5`). Khi Pod bị xóa, K8s thu lại IP này cất vào kho.
- **Nhiệm vụ:** Dùng để các ứng dụng (Pod) giao tiếp với nhau. K8s đảm bảo rằng mỗi Pod trong toàn bộ cụm sẽ có một IP duy nhất, không bao giờ trùng lặp, bất kể nó nằm ở Worker Node nào.

### 2. Sự kỳ diệu: Chúng giao tiếp với nhau như thế nào?

Vấn đề hóc búa nhất là đây: **Pod A (IP ảo: 172.29.1.5)** nằm ở **Worker 1 (IP thật: 192.168.56.11)**. Nó muốn gửi dữ liệu sang **Pod B (IP ảo: 172.29.2.10)** nằm ở **Worker 2 (IP thật: 192.168.56.12)**.

Làm sao cái IP ảo lại có thể truyền tín hiệu qua đường dây cáp vật lý?

K8s sử dụng một kỹ thuật gọi là **Đóng gói (Encapsulation)** thông qua các Plugin mạng (CNI - Container Network Interface). Kịch bản diễn ra trong tích tắc như sau:

1. Pod A muốn gửi một lá thư cho Pod B. Nó ghi ngoài phong bì: *Gửi `172.29.2.10`*.
2. Lá thư rớt xuống trạm bưu điện của Worker 1. Trạm bưu điện (Plugin mạng) nhận ra: *"À, cái IP ảo `172.29.2.10` này đang nằm ở máy Worker 2"*.
3. Trạm bưu điện lấy phong bì của Pod A, **nhét vào một cái hộp to hơn (đóng gói)**. Bên ngoài cái hộp to nó dán địa chỉ thật: *Từ máy `192.168.56.11` gửi máy `192.168.56.12`*.
4. Cái hộp to chạy qua dây cáp mạng thật (hoặc switch ảo của VirtualBox) sang đến Worker 2.
5. Worker 2 nhận hộp to, bóc ra thấy phong bì nhỏ bên trong ghi đích đến là `172.29.2.10`. Nó liền chuyển thẳng phong bì đó vào tận tay cho Pod B.

*Lưu ý: Toàn bộ quá trình bọc/mở hộp này diễn ra trong suốt dưới nền hệ điều hành mà ứng dụng của bạn không hề hay biết!*
