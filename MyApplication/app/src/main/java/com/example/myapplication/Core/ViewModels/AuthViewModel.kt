package com.example.myapplication.Core.ViewModels

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.example.myapplication.Core.Models.AuthUser
import com.example.myapplication.Core.repository.AuthRepository
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class AuthViewModel : ViewModel() {
    
    private val repository = AuthRepository()
    private val auth = FirebaseAuth.getInstance()
    
    // State cho user hiện tại
    var currentUser = mutableStateOf<AuthUser?>(null)
        private set
    
    // State cho loading
    var isLoading = mutableStateOf(false)
        private set
    
    // State cho error message
    var errorMessage = mutableStateOf("")
        private set
    
    // State cho success message
    var successMessage = mutableStateOf("")
        private set
    
    // Kiểm tra user đã đăng nhập khi khởi tạo ViewModel
    init {
        checkCurrentUser()
        // Lắng nghe thay đổi của auth state
        auth.addAuthStateListener { firebaseAuth ->
            // Refresh profile để lấy displayName mới nhất từ Firebase
            firebaseAuth.currentUser?.reload()?.addOnCompleteListener {
                currentUser.value = repository.getCurrentUser()
            }
        }
    }
    
    private fun checkCurrentUser() {
        // Refresh profile trước khi lấy user
        auth.currentUser?.reload()?.addOnCompleteListener {
            currentUser.value = repository.getCurrentUser()
        }
    }
    
    fun signUp(email: String, password: String, displayName: String) {
        if (email.isEmpty() || password.isEmpty() || displayName.isEmpty()) {
            errorMessage.value = "Vui lòng điền tất cả các trường"
            return
        }
        
        if (password.length < 6) {
            errorMessage.value = "Mật khẩu phải có ít nhất 6 ký tự"
            return
        }
        
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = ""
            successMessage.value = ""
            
            val result = repository.signUp(email, password, displayName)
            
            result.onSuccess { user ->
                currentUser.value = user
                successMessage.value = "Đăng ký thành công"
                isLoading.value = false
            }
            
            result.onFailure { exception ->
                errorMessage.value = when {
                    exception.message?.contains("already in use") == true -> 
                        "Email này đã được đăng ký"
                    exception.message?.contains("invalid email") == true -> 
                        "Email không hợp lệ"
                    else -> exception.message ?: "Lỗi đăng ký"
                }
                isLoading.value = false
            }
        }
    }
    
    fun signIn(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            errorMessage.value = "Vui lòng điền email và mật khẩu"
            return
        }
        
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = ""
            successMessage.value = ""
            
            val result = repository.signIn(email, password)
            
            result.onSuccess { user ->
                currentUser.value = user
                successMessage.value = "Đăng nhập thành công"
                isLoading.value = false
            }
            
            result.onFailure { exception ->
                errorMessage.value = when {
                    exception.message?.contains("user not found") == true -> 
                        "Tài khoản không tồn tại"
                    exception.message?.contains("password is invalid") == true -> 
                        "Mật khẩu không chính xác"
                    exception.message?.contains("too many requests") == true -> 
                        "Quá nhiều lần thử. Vui lòng thử lại sau"
                    else -> exception.message ?: "Lỗi đăng nhập"
                }
                isLoading.value = false
            }
        }
    }
    
    fun signOut() {
        repository.signOut()
        currentUser.value = null
        errorMessage.value = ""
        successMessage.value = ""
    }
    
    fun sendPasswordResetEmail(email: String) {
        if (email.isEmpty()) {
            errorMessage.value = "Vui lòng nhập email"
            return
        }
        
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = ""
            successMessage.value = ""
            
            val result = repository.sendPasswordResetEmail(email)
            
            result.onSuccess {
                successMessage.value = "Email reset mật khẩu đã được gửi"
                isLoading.value = false
            }
            
            result.onFailure { exception ->
                errorMessage.value = exception.message ?: "Lỗi gửi email"
                isLoading.value = false
            }
        }
    }
    
    fun clearMessages() {
        errorMessage.value = ""
        successMessage.value = ""
    }
}
