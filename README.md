# UniMarket – Nền tảng thương mại điện tử cho sinh viên

**UniMarket** là ứng dụng di động giúp sinh viên trao đổi, mua bán sản phẩm (laptop, sách, đồ điện tử...) trong môi trường đại học một cách an toàn và tiện lợi.

---

## 🚀 Công nghệ sử dụng

- **Ngôn ngữ:** Java (Android SDK)
- **Kiến trúc:** Service-Oriented (Tách biệt logic dữ liệu và giao diện)
- **Backend:** [Supabase](https://supabase.com/) (PostgreSQL, Auth, Storage)
- **Thư viện chính:**
  - `OkHttp`: Giao tiếp mạng (REST API)
  - `Gson`: Xử lý dữ liệu JSON
  - `Glide`: Tải và hiển thị hình ảnh
  - `Navigation Component`: Điều hướng màn hình

---

## 📁 Cấu trúc dự án

```text
app/src/main/java/com/example/unimarket/
├── auth/           # Quản lý Đăng nhập, Đăng ký, Xác thực
├── data/
│   ├── model/      # Các lớp dữ liệu (Product, User, Order...)
│   └── service/    # Logic xử lý dữ liệu (CRUD operations)
├── network/        # Cấu hình kết nối API Supabase
├── pages/          # Các màn hình chính (Home, Search, Order, Profile)
└── utils/          # Tiện ích và hằng số dùng chung
```

---

## ✨ Chức năng chính

### 1. Quản lý tài khoản & Xác thực
- Đăng ký/Đăng nhập tài khoản sinh viên.
- Xác minh danh tính qua Email trường hoặc Thẻ sinh viên.
- Khôi phục mật khẩu qua email.

### 2. Mua bán & Trao đổi
- Xem danh sách sản phẩm theo danh mục (Laptop, Sách, Giáo trình...).
- Tìm kiếm và lọc sản phẩm thông minh.
- Đăng tin bán sản phẩm kèm hình ảnh.
- Quản lý giỏ hàng và đặt hàng.

### 3. Tương tác & Gợi ý
- Chat trực tuyến thời gian thực giữa người mua và người bán.
- Đánh giá chất lượng sản phẩm và người bán.
- Hệ thống gợi ý sản phẩm dựa trên nhu cầu sinh viên.

---

## 🛠 Hướng dẫn thiết lập

1.  **Yêu cầu:** Android Studio Flamingo (hoặc mới hơn), Java 11+.
2.  **Cấu hình Backend:**
    - Tạo dự án trên Supabase.
    - Chạy file `app/src/main/java/com/example/unimarket/data/init.sql` trong SQL Editor của Supabase.
    - Cập nhật `SUPABASE_URL` và `SUPABASE_ANON_KEY` trong `com.example.unimarket.utils.Constants`.
3.  **Build & Run:** Mở dự án trong Android Studio và nhấn **Run**.

---

## 📝 Giấy phép
Dự án được phát triển phục vụ mục đích học tập (Đồ án môn học).
