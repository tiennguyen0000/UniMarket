# UniMarket

UniMarket là ứng dụng Android dành cho sinh viên mua bán và trao đổi đồ dùng trong môi trường đại học. Dự án tập trung vào luồng giao dịch nội bộ campus: đăng tin, tìm kiếm sản phẩm, nhắn tin với người bán, thêm vào giỏ, đặt hàng, theo dõi đơn và quản lý hồ sơ người dùng.

## Highlights

- Xây dựng native Android với Java và Material Components
- Dùng Firebase Authentication, Cloud Firestore, Firebase Storage và Firebase Analytics
- Hỗ trợ flow mua bán tương đối đầy đủ: listing -> search -> wishlist/cart -> checkout -> orders -> chat -> review
- Có lớp kiểm soát quyền theo vai trò và xác thực sinh viên
- Có khu vực quản trị cho kiểm duyệt người dùng và yêu cầu xác minh

## Feature Set

### 1. Authentication & Onboarding

- Onboarding cho người dùng mới
- Đăng ký, đăng nhập, quên mật khẩu, xác minh email
- Hỗ trợ Google Sign-In trong flow xác thực

### 2. Marketplace Listing

- Đăng tin bán sản phẩm với tiêu đề, mô tả, giá, tình trạng và danh mục
- Upload nhiều ảnh sản phẩm lên Firebase Storage
- Kiểm tra quyền đăng tin dựa trên hồ sơ người dùng và trạng thái xác thực

### 3. Browse & Search

- Trang chủ hiển thị danh mục và entry points tới các luồng chính
- Màn tìm kiếm có lọc danh mục và các tiêu chí liên quan
- Hỗ trợ mở chi tiết sản phẩm qua bottom sheet

### 4. Product Detail & Engagement

- Xem chi tiết sản phẩm, hình ảnh, mô tả, tình trạng
- Thêm/xóa wishlist
- Xem và gửi đánh giá
- Nhắn tin trực tiếp với người bán từ chi tiết sản phẩm

### 5. Cart & Checkout

- Thêm sản phẩm vào giỏ
- Mở cart bottom sheet và quản lý line items
- Tạo đơn hàng từ sản phẩm
- Áp dụng discount code trong flow thanh toán
- Đồng bộ trạng thái sản phẩm và đơn hàng sau checkout

### 6. Orders

- Theo dõi đơn mua và đơn bán
- Lọc theo trạng thái
- Cập nhật vòng đời đơn hàng theo vai trò buyer/seller
- Mở lại chat hoặc mua lại từ order flow

### 7. Chat & Notifications

- Chat theo conversation gắn với sản phẩm
- Inbox hội thoại qua bottom sheet
- Tạo thông báo cho tin nhắn và cập nhật đơn hàng
- Badge thông báo chưa đọc ở giao diện chính

### 8. Profile, Verification & Admin

- Quản lý hồ sơ cá nhân
- Xem bài đăng, đơn hàng và sản phẩm đã lưu
- Gửi yêu cầu xác thực sinh viên
- Admin console cho reviewer/moderator quản lý người dùng và verification requests

## Tech Stack

- Language: Java 11 cho Android source compatibility
- UI: Android Views, Material Components, RecyclerView, BottomSheet, Navigation Component
- Architecture: Activity/Fragment + ViewModel + service layer
- Backend: Firebase Authentication, Cloud Firestore, Firebase Storage, Firebase Analytics
- Image loading: Glide
- Testing: JUnit

## Architecture Overview

Dự án đang dùng kiến trúc phân tầng nhẹ, ưu tiên dễ đọc và dễ mở rộng trong bối cảnh app Android dùng Firebase trực tiếp.

- Presentation layer
  - `Activity`, `Fragment`, `BottomSheetDialogFragment`
  - `ViewModel` cho các màn đã được chuẩn hóa state
  - Một số màn dùng `UiState` / `UiEvent` để tách state hiển thị và one-time events
- Data layer
  - `data/model` chứa các domain model như `User`, `Product`, `Order`, `Review`, `Notification`
  - `data/service` chứa service theo từng entity và các service orchestration như `CheckoutService`
  - `data/service/base` chứa `BaseCrudService`, `AsyncCrudService`, `Result`, `ResultCallback`
- Access & rules layer
  - `auth/AccessControl.java`
  - `firestore.rules`
  - `storage.rules`

## Project Structure

```text
app/src/main/java/com/example/unimarket/
├── auth/                    # Login, register, onboarding, access control
├── data/
│   ├── model/               # User, Product, Order, Cart, Review, ...
│   ├── service/             # CRUD services + CheckoutService
│   └── util/                # TimeUtils, FirestoreIds, constants helpers
├── pages/
│   ├── home/                # Home, product detail, cart, notifications
│   ├── search/              # Search and filters
│   ├── post/                # Create listing
│   ├── orders/              # Buyer/seller order flows
│   ├── chat/                # Product conversations and inbox
│   └── profile/             # Profile, verification, admin console
├── Controller.java          # Main host with bottom navigation
└── MainActivity.java        # Entry flow
```

## Backend Notes

- Firestore được dùng làm nguồn dữ liệu chính cho:
  - `profiles`
  - `products`
  - `carts`, `cart_items`
  - `orders`, `order_items`
  - `conversations`, `messages`
  - `notifications`
  - `wishlist`
  - `reviews`
  - `student_verifications`
  - `reports`, `user_behavior`, `discount_codes`
- Firebase Storage được dùng cho ảnh sản phẩm và avatar
- Quyền truy cập dữ liệu được định nghĩa tại:
  - [firestore.rules](/mnt/c/Git/Android/UniMarket/firestore.rules)
  - [storage.rules](/mnt/c/Git/Android/UniMarket/storage.rules)

## Getting Started

### Requirements

- Android Studio bản mới
- JDK phù hợp với Android Gradle Plugin
- Android SDK theo cấu hình Gradle hiện tại
- Firebase project để cấp Auth / Firestore / Storage

### Firebase Setup

1. Tạo Firebase project
2. Bật các dịch vụ cần dùng:
   - Authentication
   - Cloud Firestore
   - Firebase Storage
   - Analytics nếu muốn theo dõi sự kiện
3. Thêm Android app vào Firebase project
4. Đặt `google-services.json` vào:

```text
app/google-services.json
```

5. Kiểm tra lại cấu hình SHA, OAuth client và `default_web_client_id` nếu dùng Google Sign-In

### Local Setup

1. Clone repo
2. Mở bằng Android Studio
3. Kiểm tra `local.properties` trỏ đúng Android SDK
4. Sync Gradle

### Useful Commands

```bash
./gradlew :app:assembleDebug
./gradlew :app:compileDebugJavaWithJavac
./gradlew :app:testDebugUnitTest
```

## Current Scope

Repo hiện đã bao phủ phần lớn flow marketplace trong campus, nhưng vẫn thiên về application prototype / academic product hơn là production-hardened system.

Một số điểm cần lưu ý:

- Business rules đang nằm khá nhiều ở tầng client + Firestore rules
- Chưa có backend riêng ngoài Firebase
- Kiểm thử hiện mới tập trung ở mức unit test cơ bản
- Một số màn vẫn theo style Android View truyền thống thay vì architecture tách sâu hơn

## Why This Project Stands Out

- Không chỉ dừng ở CRUD listing cơ bản mà có cả cart, checkout, orders, chat và notifications
- Có mô hình role/verification giúp luồng đăng bán sát bài toán thực tế hơn
- Có admin console và moderation-oriented access control, hiếm hơn ở các đồ án marketplace Android cơ bản

