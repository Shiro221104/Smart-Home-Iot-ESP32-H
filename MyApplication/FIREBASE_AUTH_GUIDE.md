## Hướng Dẫn Cấu Hình Firebase Authentication

### 1. Firebase Setup (Đã có sẵn)

Dự án của bạn đã cấu hình Firebase Authentication. Kiểm tra:
- ✅ `google-services.json` có trong `app/` folder
- ✅ Firebase dependencies được thêm vào `build.gradle.kts`
- ✅ Google Services plugin được kích hoạt

### 2. Cấu Trúc Mã

**Repositories:**
- `AuthRepository.kt` - Xử lý tất cả Firebase Auth operations
  - `signUp()` - Đăng ký tài khoản mới
  - `signIn()` - Đăng nhập
  - `signOut()` - Đăng xuất
  - `sendPasswordResetEmail()` - Gửi email reset password
  - `updateUserProfile()` - Cập nhật profile
  - `deleteAccount()` - Xóa tài khoản

**ViewModels:**
- `AuthViewModel.kt` - Quản lý trạng thái Authentication
  - `currentUser` - User hiện tại
  - `isLoading` - Trạng thái loading
  - `errorMessage` - Thông báo lỗi
  - `successMessage` - Thông báo thành công

**Models:**
- `AuthUser.kt` - Data class cho user info

**UI Screens:**
- `LoginScreen.kt` - Giao diện đăng nhập
- `SignupScreen.kt` - Giao diện đăng ký
- `Intro.kt` - Navigation giữa Splash, Login, Signup

### 3. Luồng Hoạt Động

```
Splash (1 giây)
  ↓
[User đã đăng nhập?]
  ├─ Yes → MainActivity
  └─ No → LoginScreen
            ↓
          [Có tài khoản?]
            ├─ Yes → Đăng nhập → MainActivity
            ├─ No → Đăng ký → SignupScreen → MainActivity
            └─ Forgot Password → Reset Email
```

### 4. Sử Dụng trong Code

#### Đăng Nhập:
```kotlin
authViewModel.signIn("email@example.com", "password123")
```

#### Đăng Ký:
```kotlin
authViewModel.signUp(
    "email@example.com", 
    "password123", 
    "Tên hiển thị"
)
```

#### Lấy User Hiện Tại:
```kotlin
val user = authViewModel.currentUser.value
if (user != null) {
    // User đã đăng nhập
    println("${user.displayName} - ${user.email}")
}
```

#### Đăng Xuất:
```kotlin
authViewModel.signOut()
```

### 5. Kiểm Tra Firebase Setup

1. Vào [Firebase Console](https://console.firebase.google.com/)
2. Chọn project của bạn
3. Vào "Authentication" → "Sign-in method"
4. Bật "Email/Password"
5. Vào "Firestore Database" → Tạo database (nếu chưa có)

### 6. Firestore Rules (Bảo mật)

Thêm rules vào Firestore để bảo mật user data:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users chỉ có thể đọc/viết dữ liệu của chính họ
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
    }
  }
}
```

### 7. Thêm Tính Năng Khác (Optional)

- **Social Login**: Google Sign-In, Facebook Login
- **Phone Authentication**: Đăng nhập bằng số điện thoại
- **Email Verification**: Xác minh email
- **Two-Factor Authentication**: Xác thực 2 lớp

### 8. Troubleshooting

**Lỗi: "java.lang.IllegalStateException: Failed to initialize Firebase"**
- Kiểm tra `google-services.json` được đặt đúng vị trí
- Rebuild project: `Build → Clean Build Folder`

**Lỗi: "Permission denied for path /users"**
- Cập nhật Firestore Rules theo hướng dẫn trên

**Email không nhận được reset password**
- Kiểm tra email trong spam folder
- Kiểm tra SMTP configuration trong Firebase Console

---

## 📝 Ghi Chú Quan Trọng

✅ Tất cả passwords được mã hóa bởi Firebase
✅ User info được lưu trong Firestore
✅ Authentication state được quản lý tự động
✅ Token tự động renew
✅ Secure connection (HTTPS) được sử dụng
