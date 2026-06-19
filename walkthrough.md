# Walkthrough - Sxcution App Banner, Backup/Restore JSON and Core Features Report

Báo cáo này tổng hợp toàn bộ các thay đổi mới nhất (Màu chữ neon green, tối ưu Banner card, Backup/Restore JSON, Phân nhóm thông minh địa điểm) cùng với các tính năng kiến trúc cốt lõi quan trọng của ứng dụng **Sxcution** được trích xuất từ các tài liệu lưu trữ cũ để lưu trữ làm tài liệu tham khảo đồng bộ.

---

## I. Các thay đổi mới cập nhật (Banner, Màu chữ & Backup/Restore)

### 1. Sửa màu chữ Status chạy ngầm
- **Màu chữ mới**: Đã đổi màu chữ `"Location Services Running"` (cả trong file layout XML và file logic MainActivity khi cập nhật trạng thái động) thành màu xanh lá neon thuần khiết **`#00ff00`** theo yêu cầu để tăng tính nổi bật và dễ quan sát.

### 2. Tối ưu hóa thu nhỏ dải Banner trên cùng
- **Thu gọn kích thước**: Chiều rộng của banner đen được giảm đi 30% bằng cách đặt kích thước cố định là **`260dp`** và căn giữa (`layout_gravity="top|center_horizontal"`). Banner bây giờ hiển thị dưới dạng một chiếc Card nổi bo góc sang trọng trên bản đồ Google Maps, thay vì dải dài chiếm chỗ.
- **Giảm khoảng cách và đưa sát viền**:
  - Giảm padding trên dưới của banner từ `11dp` xuống còn **`2dp`** (padding trái phải là `8dp`).
  - Đặt `layout_marginTop` của dòng chữ trạng thái xuống **`0dp`** để đẩy cả cụm chữ sát lên viền trên nhất có thể, tối ưu tối đa diện tích hiển thị của bản đồ bên dưới.
  - Điều chỉnh font chữ tiêu đề xuống `15sp` và trạng thái xuống `12sp` để hài hòa với kích thước card mới.

### 3. Tính năng Backup và Restore danh sách địa điểm đã lưu (JSON)
Chúng tôi đã tích hợp thành công hai nút **Backup** và **Restore** gọn gàng vào Settings Dialog của ứng dụng:
- **Cách thức hoạt động**: Dữ liệu địa điểm đã lưu được trích xuất từ `SharedPreferences` và mã hóa thành một chuỗi JSON chuẩn.
- **Tính năng Backup**: Khi click nút, app tự động xuất và ghi đè danh sách địa điểm ra file JSON có tên **`sxcution_places_backup.json`** nằm ở thư mục **`Download/`** dùng chung của điện thoại (đường dẫn: `Download/sxcution_places_backup.json`). Bạn chỉ cần copy file này để chuyển sang thiết bị khác.
- **Tính năng Restore**: Khi click nút, app tìm và đọc file `Download/sxcution_places_backup.json`. Dữ liệu sẽ được tự động phân tích và ghi ngược lại vào app bằng cơ chế gộp thông minh (chỉ thêm những địa điểm mới có ID chưa tồn tại ở máy hiện tại, tránh trùng lặp dữ liệu và giữ lại các địa điểm cũ).

---

## II. Tư vấn kỹ thuật: Đưa app vào hệ thống (System App) và mức độ an toàn với WeChat

### 1. Có nên đưa app này vào ROM làm app hệ thống (System App) không?
**Câu trả lời là CÓ, rất nên làm** nếu bạn có thiết bị đã Root.
- **Lợi ích 1: Không cần kích hoạt "Mô phỏng vị trí" (Mock Locations)**. Các app quét gian lận vị trí như WeChat thường kiểm tra xem tùy chọn Developer Options (Cài đặt nhà phát triển) có đang bật hay không và có app nào đang được chọn làm "Mock Location App" hay không. Khi đưa Sxcution vào `/system/priv-app/`, ứng dụng có quyền hệ thống và có thể trực tiếp ghi đè tọa độ GPS toàn thiết bị mà không cần bật Mock Location trong cài đặt nhà phát triển. Điều này che giấu hoàn toàn hành vi giả lập.
- **Lợi ích 2: Chống bị tắt ngầm (Kill process)**. Hệ thống Android ưu tiên RAM và bộ nhớ cao nhất cho System App. App của bạn sẽ không bao giờ bị hệ thống tự động tắt khi chạy đa nhiệm hoặc treo máy qua đêm, giúp giữ kết nối GPS ổn định 24/7.

### 2. Có an toàn hơn đối với các ứng dụng nghiêm ngặt như WeChat không?
**An toàn hơn rất nhiều, tuy nhiên vẫn cần lưu ý các yếu tố sau:**
- ** WeChat phát hiện dựa trên những gì?**
  1. **Tùy chọn Mock Location**: WeChat quét rất gắt cái này. Đưa app vào System App sẽ giúp bạn vượt qua cơ chế quét này 100%.
  2. **Tốc độ di chuyển phi vật lý (Teleportation)**: Nếu bạn fake vị trí nhảy từ TP.HCM sang Bắc Kinh hoặc từ quận này sang quận khác cách nhau hàng chục km chỉ trong vài phút, server WeChat sẽ phát hiện ra sự bất thường về thời gian di chuyển vật lý và khóa tài khoản của bạn. 
     * *Giải pháp*: Luôn tắt fake GPS, đợi một khoảng thời gian hợp lý (bằng thời gian bay hoặc đi xe thực tế) trước khi start fake ở vị trí mới.
  3. **Kiểm tra trạng thái đứng yên**: Điện thoại thật luôn có độ rung nhẹ của la bàn (bearing). Tính năng **Bearing Rotation** tự động xoay nhẹ mũi tên khi đứng yên (2-5 giây/lần) trong Sxcution sinh ra chính là để qua mặt cơ chế quét này của WeChat.
  4. **Phát hiện máy đã Root**: Để đưa app vào hệ thống, bạn bắt buộc phải Root máy (Magisk). WeChat có thể quét xem điện thoại có bị Root hay cài Magisk hay không.
     * *Giải pháp*: Sử dụng Magisk bản mới nhất, kích hoạt **Zygisk**, cài đặt module **DenyList** (hoặc Shamiko) và đưa WeChat vào danh sách ẩn Root (DenyList) để WeChat không biết máy đã Root.

---

## III. Các tính năng cốt lõi quan trọng khác

### 1. Thuật toán xoay hướng mũi tên khi đứng yên (Bearing Rotation)
Để giả lập GPS hoạt động tự nhiên như thật giống app DNA, dải mũi tên màu xanh trên bản đồ sẽ tự động xoay nhẹ khi thiết bị đứng yên:
- **Ngưỡng nhận diện di chuyển**: Sử dụng sai số `0.000001` độ làm mốc. Nếu thay đổi tọa độ nhỏ hơn ngưỡng này, app coi như đang đứng yên.
- **Xoay ngẫu nhiên khi đứng yên**: Cập nhật góc xoay (bearing) sau mỗi khoảng thời gian ngẫu nhiên từ `2 đến 5 giây` với góc xoay ngẫu nhiên từ `0 đến 360 độ`.
- **Tính toán bearing khi di chuyển**: Khi tọa độ thay đổi vượt ngưỡng, góc xoay được tính toán chính xác dựa trên vector di chuyển thực tế theo công thức lượng giác:
  $$lat_1 = \text{toRadians}(fromLat), \quad lat_2 = \text{toRadians}(toLat)$$
  $$\Delta Lng = \text{toRadians}(toLng - fromLng)$$
  $$y = \sin(\Delta Lng) \cdot \cos(lat_2)$$
  $$x = \cos(lat_1) \cdot \sin(lat_2) - \sin(lat_1) \cdot \cos(lat_2) \cdot \cos(\Delta Lng)$$
  $$bearing = \text{toDegrees}(\text{atan2}(y, x))$$
  $$bearing\_final = (bearing + 360) \pmod{360}$$

### 2. Luồng yêu cầu quyền tối ưu (Sequential Permissions)
- **Loại bỏ các quyền không cần thiết**: Đã xóa bỏ các quyền đọc hình ảnh, video và quản lý bộ nhớ ngoài. Chỉ giữ lại quyền cơ bản cho GPS để tránh bị Google Play Protect đánh dấu nghi ngờ.
- **Yêu cầu quyền theo tuần tự**: Quyền vị trí -> Quyền thông báo (Android 13+) -> Quyền chạy nền để đảm bảo trải nghiệm người dùng mượt mà nhất.

---

## IV. Kết quả kiểm tra biên dịch và cài đặt

- **Biên dịch**: Dự án đã biên dịch thành công 100% bản **Release** chính thức được ký bằng file chữ ký của bạn `release.jks`.
- **Cài đặt**: Bản APK release (`app-release.apk`) đã qua tối ưu hóa ProGuard/R8 dung lượng nhẹ hơn (chỉ ~14.1MB) đã được cài đặt thành công lên thiết bị di động của bạn (`262bba890d037ece`).

---

## V. Đồng bộ lên GitHub
- Kho lưu trữ local đã được liên kết với Repository GitHub của bạn: [github.com/Sxcution/APK](https://github.com/Sxcution/APK).
- Đã đẩy toàn bộ mã nguồn sạch của dự án lên nhánh `main` thành công.

---

## VI. Tính năng phân nhóm địa điểm thông minh (Saved Places Grouping)

Chúng tôi đã thiết kế và triển khai cơ chế gom nhóm thông minh cho các địa điểm yêu thích của người dùng:

### 1. Cơ chế Tạo và Chọn Nhóm (Save Location Dialog)
- **Giao diện nâng cấp**: Hộp thoại lưu địa điểm giờ đây tích hợp một menu thả xuống (Spinner) nằm ngay cạnh ô nhập tên địa điểm.
- **Nút "+ Add Group"**: Cho phép người dùng tạo nhóm mới ngay lập tức qua một hộp thoại nhập tên đơn giản.
- **Chọn và Lưu**: Khi tạo nhóm mới thành công, Spinner sẽ tự động chọn nhóm đó. Khi nhấn Save, địa điểm được lưu kèm theo liên kết nhóm tương ứng.
- **Lưu trữ dữ liệu**: Nhóm mặc định là `"Default"`. Danh sách tên các nhóm được lưu dạng mảng JSON trong SharedPreferences dưới khóa `saved_groups_list`.

### 2. Giao diện Danh sách theo Nhóm (Saved Places Dialog)
- **Phân loại hiển thị**: Danh sách hiển thị dưới dạng phân nhóm tiêu đề (`item_group_header`). Từng nhóm sẽ chứa danh sách các địa điểm thuộc nhóm đó bên dưới.
- **Sắp xếp thứ tự**:
  - Các nhóm được sắp xếp theo thứ tự bảng chữ cái và số (A-Z, 0-9) từ trên xuống dưới.
  - Các địa điểm trong cùng một nhóm cũng được sắp xếp theo thứ tự bảng chữ cái (A-Z).
- **Trải nghiệm cuộn mượt mà không phân trang**:
  - Đã loại bỏ hoàn toàn các nút phân trang trước đây.
  - Sử dụng RecyclerView cuộn trực tiếp trên danh sách gộp phẳng (được Adapter tự động chia nhỏ thành Header và Place items).
  - **Tự động co giãn theo số lượng**: Nếu người dùng có ít hơn hoặc bằng 4 địa điểm, hộp thoại hiển thị chiều cao tự nhiên (`WRAP_CONTENT`). Khi danh sách dài hơn 4 địa điểm (hoặc nhiều nhóm), chiều cao dialog tự động chuyển sang tỷ lệ cố định (65% chiều cao màn hình) và kích hoạt chế độ cuộn RecyclerView. Điều này giúp ngăn chặn hoàn toàn hiện tượng tràn màn hình hoặc vỡ layout trên thiết bị.

### 3. Hiển thị Nhóm và Tên địa điểm trên Banner (Group Prefix Display)
- Banner tiêu đề ở trên cùng bản đồ và nhãn Marker của địa điểm được chọn hiện sẽ hiển thị kèm theo tên nhóm tương ứng theo định dạng: `[Tên Nhóm]: [Tên Địa Điểm]` (Ví dụ: `Q1: Bui Vien`).
- Nếu địa điểm không chỉ định nhóm, mặc định hệ thống hiển thị `"Default: [Tên Địa Điểm]"`.
