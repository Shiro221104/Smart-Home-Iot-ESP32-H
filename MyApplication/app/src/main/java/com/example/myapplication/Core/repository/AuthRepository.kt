package com.example.myapplication.Core.repository

import com.example.myapplication.Core.Models.AuthUser
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    // Đăng ký tài khoản mới
    suspend fun signUp(email: String, password: String, displayName: String): Result<AuthUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            
            user?.let {
                // Cập nhật display name
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                it.updateProfile(profileUpdates).await()
                
                val authUser = AuthUser(
                    uid = it.uid,
                    email = it.email ?: "",
                    displayName = displayName,

                )
                
                Result.success(authUser)
            } ?: Result.failure(Exception("Không thể tạo user"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Đăng nhập
    suspend fun signIn(email: String, password: String): Result<AuthUser> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val user = auth.currentUser
            
            user?.let {
                val authUser = AuthUser(
                    uid = it.uid,
                    email = it.email ?: "",
                    displayName = it.displayName ?: ""
                )
                Result.success(authUser)
            } ?: Result.failure(Exception("Không thể đăng nhập"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Đăng xuất
    fun signOut() {
        auth.signOut()
    }

    // Lấy user hiện tại
    fun getCurrentUser(): AuthUser? {
        val user = auth.currentUser
        return user?.let {
            AuthUser(
                uid = it.uid,
                email = it.email ?: "",
                displayName = it.displayName ?: "",

            )
        }
    }

    // Kiểm tra user đã đăng nhập chưa
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Gửi email reset mật khẩu
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cập nhật tên hiển thị
    suspend fun updateUserProfile(displayName: String): Result<Unit> {
        return try {
            val user = auth.currentUser
            user?.let {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                
                it.updateProfile(profileUpdates).await()
                
                Result.success(Unit)
            } ?: Result.failure(Exception("Không có user hiện tại"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Xóa tài khoản
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser
            user?.let {
                it.delete().await()
                Result.success(Unit)
            } ?: Result.failure(Exception("Không có user hiện tại"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
