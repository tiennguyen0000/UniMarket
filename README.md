# UniMarket

Ứng dụng Android hỗ trợ sinh viên mua bán, trao đổi đồ dùng học tập và thiết bị cá nhân trong môi trường đại học.

## Kiến trúc hiện tại

Dự án đang dùng kiến trúc theo hướng phân tầng nhẹ:

- **Presentation layer**
  - `Activity`/`Fragment` cho UI và điều hướng.
  - `ViewModel` cho state và xử lý luồng dữ liệu ở các màn đã chuẩn hóa (`Home`, `Profile`).
  - Mẫu state/event:
    - `UiState`: trạng thái màn hình.
    - `UiEvent`: sự kiện một lần (thông báo, lỗi,...).
- **Data layer**
  - `data/model`: các model domain (`User`, `Product`, `Order`,...).
  - `data/service`: service theo từng entity (`UserService`, `ProductService`,...).
  - `data/service/base`:
    - `AsyncCrudService`: CRUD bất đồng bộ với Firebase Firestore.
    - `BaseCrudService`: base service chuẩn hóa API.
    - `Result`/`ResultCallback`: kiểu kết quả thống nhất cho data flow.

## Công nghệ

- **Ngôn ngữ:** Java 11 (Android)
- **Backend:** Firebase
  - Firebase Authentication (email/password, Google Sign-In)
  - Cloud Firestore
  - Firebase Analytics
- **UI/Navigation**
  - AndroidX Navigation Component
  - RecyclerView
  - Material Components
  - Glide (image loading)
- **Kiểm thử**
  - JUnit (unit test)

## Cấu trúc thư mục

```text
app/src/main/java/com/example/unimarket/
├── auth/                     # Login/Register/Verify/Forgot Password
├── data/
│   ├── model/                # Data models
│   └── service/
│       ├── base/             # AsyncCrudService, BaseCrudService, Result
│       └── ...               # UserService, ProductService, ...
├── pages/
│   ├── home/                 # HomeFragment + HomeViewModel + HomeUiState/Event
│   ├── profile/              # ProfileFragment + ProfileViewModel + ProfileUiState/Event
│   ├── search/
│   └── orders/
├── Controller.java           # Host bottom navigation
├── MainActivity.java         # Entry routing (onboarding/auth/main flow)
└── OnboardingActivity.java
```

## Luồng chính

- Mở app -> `MainActivity` quyết định flow (onboarding/auth/main).
- Đăng nhập/đăng ký qua Firebase Auth.
- Sau auth, profile người dùng đồng bộ lên Firestore collection `profiles`.
- Các màn feature đọc/ghi dữ liệu qua `service` và `AsyncCrudService`.

## Thiết lập và chạy dự án

### Yêu cầu

- Android Studio (khuyến nghị bản mới)
- JDK 11
- Android SDK theo cấu hình trong `app/build.gradle.kts`

### Cấu hình Firebase

1. Tạo Firebase project.
2. Bật **Authentication** (Email/Password, Google nếu cần).
3. Bật **Cloud Firestore**.
4. Tải `google-services.json` và đặt vào:
   - `app/google-services.json`
5. Đảm bảo `default_web_client_id` đúng cho Google Sign-In (nếu dùng).

### Build

- Build debug:
  - `./gradlew :app:assembleDebug`
- Compile Java:
  - `./gradlew :app:compileDebugJavaWithJavac`
- Run unit tests:
  - `./gradlew :app:testDebugUnitTest`




