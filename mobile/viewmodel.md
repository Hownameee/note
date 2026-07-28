# View Model Datatype

## LiveData (Cái ống nước)

LiveData (Ống nước chỉ để nhìn):

- Bản chất: Là một lớp (class) chứa dữ liệu và có khả năng nhận biết vòng đời (Lifecycle-aware) của Activity/Fragment.
- Đặc điểm: Nó chỉ cho phép người ta đọc (Observe) dữ liệu chảy ra, chứ không cho phép sửa hay thêm dữ liệu mới.
- Tác dụng: Giúp Activity/Fragment của bạn tự động cập nhật UI khi có data mới, nhưng tự động "ngủ" khi View bị ẩn (Stop) và tự hủy khi View bị đóng (Destroy) -> Tránh triệt để lỗi Memory Leak và crash app do update UI khi View đã chết.

## MutableLiveData (Ống nước cho phép bơm nước)

- Bản chất: Kế thừa trực tiếp từ LiveData. "Mutable" nghĩa là có thể thay đổi.
- Đặc điểm: Nó có thêm 2 hàm setValue() và postValue() để bạn bơm dữ liệu mới vào ống.

Best Practice (Quy tắc ngầm): Trong ViewModel, người ta luôn dùng MutableLiveData ở dạng private (để tự do thay đổi data bên trong ViewModel), và bọc nó lại bằng một LiveData public đẩy ra ngoài cho Fragment/Activity (để Fragment chỉ được phép "đọc" chứ không được phép làm nhiễu data).

### setValue() vs postValue()

- **setValue(T value):**
  - **Môi trường:** CHỈ ĐƯỢC DÙNG trên Main Thread (Luồng chính / UI Thread).
  - **Đặc điểm:** Hoạt động đồng bộ (Synchronous) và ngay lập tức. Gọi lệnh xong là Observer nhận được data liền.
  - **Khi nào dùng:** Khi xử lý logic tính toán đơn giản, nhận sự kiện click button, hoặc update trạng thái UI trực tiếp từ luồng chính.

- **postValue(T value):**
  - **Môi trường:** Dùng trên Background Thread (Luồng chạy ngầm - Worker Thread).
  - **Đặc điểm:** Hoạt động bất đồng bộ (Asynchronous). Nó sẽ đóng gói giá trị mới và gửi một yêu cầu (task) lên Main Thread để cập nhật. Do đó, sẽ có một độ trễ cực nhỏ trước khi Observer nhận được.
  - **Khi nào dùng:** Khi vừa gọi API xong, query database (như Room/Retrofit) ở luồng ngầm hoàn tất và muốn đẩy kết quả về cho UI hiển thị.

## Observer (Vòi nước cuối đường ống)

- **Bản chất:** Là nơi hứng và lắng nghe dữ liệu chảy ra từ LiveData, thường được đặt trong Activity hoặc Fragment.
- **Cách hoạt động:** Hàm `observe(LifecycleOwner, Observer)` kết nối UI với LiveData thông qua 2 yếu tố:
  1. `LifecycleOwner` (thường dùng `getViewLifecycleOwner()` trong Fragment): Cung cấp nhận thức về vòng đời. Giúp LiveData biết khi nào View đang hiển thị để gửi data, khi nào View bị ẩn/hủy để tự động ngưng gửi hoặc ngắt kết nối hoàn toàn.
  2. `Observer`: Một khối lệnh (callback) chứa logic cập nhật giao diện (đổi text, ẩn hiện View, cập nhật RecyclerView...) sẽ được thực thi mỗi khi có dữ liệu mới chảy trong ống.

## Transformations & Transformations.switchMap() (Van chuyển hướng)

- **Transformations (Bộ lọc):** Đóng vai trò như các trạm trung chuyển gắn ở giữa đường ống. Nó lấy dữ liệu từ LiveData nguồn, biến đổi/xử lý, và đẩy ra một LiveData đích cho UI lắng nghe.
- **Transformations.switchMap():**
  - **Vấn đề giải quyết:** Tránh việc Observer bị đứt kết nối khi ViewModel phải gọi lại một hàm tạo ra một LiveData mới (Ví dụ: tính năng Search, tính năng Pull-to-Refresh).
  - **Cách hoạt động:** Cần một LiveData làm "cò súng" (Trigger). Mỗi khi Trigger thay đổi giá trị, `switchMap` sẽ kích hoạt một hàm (như query DB hoặc gọi API) và trả về một ống nước (LiveData) MỚI.
  - **Ưu điểm cực lớn:** Hệ thống sẽ tự động rút Observer từ ống nước cũ và cắm sang ống nước mới. Fragment/Activity bên ngoài chỉ cần thiết lập `observe` một lần duy nhất, code vừa sạch vừa không lo lỗi mất kết nối (Lost Observer).
