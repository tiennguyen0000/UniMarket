# UniMarket

UniMarket là ứng dụng Android dành cho sinh viên mua bán, trao đổi và quản lý giao dịch đồ dùng trong môi trường đại học. Dự án tập trung vào trải nghiệm marketplace nội bộ campus: đăng tin, tìm kiếm sản phẩm, nhắn tin, giỏ hàng, đặt hàng, theo dõi đơn, xác thực sinh viên và quản trị nội dung.

## Tính năng chính

- Onboarding, đăng ký, đăng nhập, quên mật khẩu và xác thực email.
- Đăng nhập email/mật khẩu và hỗ trợ Google Sign-In khi Firebase OAuth được cấu hình đúng.
- Trang chủ với nhiệm vụ nhận ưu đãi, mã giảm giá, lối tắt mua/bán và gợi ý sản phẩm.
- Đăng tin sản phẩm với ảnh, tiêu đề, mô tả, giá, số lượng, danh mục và tình trạng.
- Tìm kiếm sản phẩm với bộ lọc, sắp xếp, tìm kiếm gần đây, sản phẩm đã lưu và chế độ riêng cho người bán.
- Chi tiết sản phẩm dạng bottom sheet, hỗ trợ lưu, đánh giá, chat, thêm giỏ hàng và mua ngay.
- Giỏ hàng với thông tin nhận hàng, số điện thoại, vị trí, phương thức giao, lời nhắc người bán và mã giảm giá.
- Quản lý đơn mua/đơn bán, cập nhật trạng thái và chỉnh thông tin giao hàng khi đơn còn hợp lệ.
- Chat giữa người mua và người bán, inbox hội thoại và thông báo trong app.
- Hồ sơ cá nhân, chỉnh sửa thông tin, tìm kiếm đã lưu, bài đăng, đơn hàng và sản phẩm đã lưu.
- Xác thực sinh viên bằng MSSV, họ tên và ảnh hai mặt thẻ sinh viên.
- Bảng quản trị cho admin/moderator để xem thống kê, quản lý người dùng và duyệt xác thực.

## Công nghệ sử dụng

- Java cho Android native.
- Android Views, Material Components, RecyclerView, BottomSheet và Navigation Component.
- Firebase Authentication, Cloud Firestore và Firebase Storage.
- Glide cho tải ảnh.
- JUnit cho unit test.

## Kiến trúc tổng quan

Dự án dùng kiến trúc phân tầng nhẹ, phù hợp với Android app dùng Firebase trực tiếp.

- Presentation layer: `Activity`, `Fragment`, `BottomSheetDialogFragment`, adapter và ViewModel.
- Data model: các model như `User`, `Product`, `Order`, `Cart`, `Review`, `Notification`, `DiscountCode`.
- Service layer: các service theo entity và các service điều phối như `CheckoutService`.
- Rules layer: `firestore.rules` và `storage.rules` để kiểm soát quyền truy cập dữ liệu.

## Cấu trúc thư mục chính

```text
app/src/main/java/com/example/unimarket/
├── auth/                    # Đăng nhập, đăng ký, onboarding, phân quyền
├── data/
│   ├── model/               # User, Product, Order, Cart, Review, DiscountCode, ...
│   ├── service/             # Service thao tác Firebase và checkout flow
│   └── util/                # Helper dùng chung
├── pages/
│   ├── home/                # Trang chủ, chi tiết sản phẩm, giỏ hàng, thông báo
│   ├── search/              # Tìm kiếm, bộ lọc, trạng thái danh sách sản phẩm
│   ├── post/                # Đăng tin và cập nhật tin đăng
│   ├── orders/              # Đơn mua, đơn bán, cập nhật đơn hàng
│   └── profile/             # Hồ sơ, xác thực sinh viên, quản trị
├── Controller.java          # Activity khung chính với bottom navigation
└── MainActivity.java        # Entry flow của ứng dụng
```

## Firebase

Firestore đang được dùng cho các collection chính:

- `profiles`
- `products`
- `carts`, `cart_items`
- `orders`, `order_items`
- `conversations`, `messages`
- `notifications`
- `wishlist`
- `reviews`
- `student_verifications`
- `discount_codes`
- `reports`, `user_behavior`

Firebase Storage được dùng cho ảnh sản phẩm, ảnh xác thực sinh viên và các tài nguyên người dùng tải lên.

## Cài đặt local

1. Clone repository.
2. Mở project bằng Android Studio.
3. Tạo Firebase project và bật Authentication, Cloud Firestore, Firebase Storage.
4. Thêm Android app vào Firebase project.
5. Đặt file cấu hình Firebase tại:

```text
app/google-services.json
```

6. Kiểm tra SHA-1/SHA-256 và OAuth client nếu dùng Google Sign-In.
7. Sync Gradle và chạy app trên thiết bị hoặc emulator.

## Lệnh thường dùng

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:testDebugUnitTest
```

## Lưu ý bảo mật

Không commit các file chứa khóa hoặc dữ liệu máy cá nhân như `local.properties`, service account JSON, ảnh chụp màn hình tạm, thư mục build, file IDE local hoặc các file sinh ra khi chạy script.
