# Project Structure - Sxcution App

Dự án này là ứng dụng giả lập GPS **Sxcution** dành cho Android viết bằng Kotlin. Dưới đây là cấu trúc các file chính và vai trò của chúng trong dự án:

## App Source Code

- `app/src/main/java/com/sxcution/app/MainActivity.kt`: Lớp giao diện chính điều khiển Google Maps, bắt đầu/dừng Fake GPS service, lưu và chọn địa điểm đã lưu, và hiển thị Banner thông tin.
- `app/src/main/java/com/sxcution/app/services/LocationService.kt`: Service chính chạy ngầm để thực hiện Fake GPS bằng phương pháp Test Provider (Mocks GPS và Network providers) và tính toán xoay hướng mũi tên.
- `app/src/main/java/com/sxcution/app/service/ForegroundLocationService.kt`: Service chạy ngầm chịu trách nhiệm hiển thị thông báo (Notification) trạng thái Fake GPS lên thanh trạng thái.
- `app/src/main/java/com/sxcution/app/repository/SavedPlacesRepository.kt`: Lớp quản lý cơ sở dữ liệu hoặc Preferences lưu trữ danh sách địa điểm yêu thích của người dùng.
- `app/src/main/java/com/sxcution/app/data/SavedPlace.kt`: Data class mô tả đối tượng địa điểm đã lưu (tên, latitude, longitude).

## UI Layout Resources

- `app/src/main/res/layout/activity_main.xml`: File layout chính chứa bản đồ Google Map toàn màn hình, banner hiển thị ở trên cùng, panel hiển thị địa chỉ ở dưới cùng và các nút điều khiển.
- `app/src/main/res/layout/dialog_save_place.xml`: Layout của Dialog khi người dùng nhấn nút Save để lưu địa điểm mới.

## Project Configurations

- `app/build.gradle`: File cấu hình build của module app chứa các thư viện dependencies như Play Services Maps, Fused Location, Dexter, etc.
- `naming_registry.json`: Bản đăng ký các ID giao diện và biến dùng chung để AI theo dõi và đồng bộ.
