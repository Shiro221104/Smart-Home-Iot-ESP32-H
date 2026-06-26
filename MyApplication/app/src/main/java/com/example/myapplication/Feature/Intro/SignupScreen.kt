package com.example.myapplication.Feature.Intro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.Core.ViewModels.AuthViewModel

@Composable
fun SignupScreen(
    authViewModel: AuthViewModel = viewModel(),
    onBackClick: () -> Unit,
    onSignupSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    val isLoading = authViewModel.isLoading.value
    val errorMessage = authViewModel.errorMessage.value
    val successMessage = authViewModel.successMessage.value
    val currentUser = authViewModel.currentUser.value
    
    // Nếu đăng ký thành công, điều hướng
    LaunchedEffect(currentUser) {
        if (currentUser != null && successMessage.isNotEmpty()) {
            onSignupSuccess()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEEF2F7),
                        Color(0xFFE3F2FD)

                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable { onBackClick() }
                        .padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tiêu đề
            Text(
                text = "Tạo Tài Khoản",
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            

            
            // Display Name TextField
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Tên hiển thị") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Email TextField
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Password TextField
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                    )
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Confirm Password TextField
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Xác nhận mật khẩu") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.clickable { confirmPasswordVisible = !confirmPasswordVisible }
                    )
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Hiển thị lỗi
            if (errorMessage.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFC62828),
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp
                    )
                }
            }
            
            // Hiển thị success message
            if (successMessage.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    color = Color(0xFFC8E6C9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = successMessage,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp
                    )
                }
            }
            
            // Nút đăng ký
            Button(
                onClick = {
                    if (password != confirmPassword) {
                        // Hiển thị lỗi
                    } else {
                        authViewModel.signUp(email, password, displayName)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 16.dp),
                enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty() 
                    && confirmPassword.isNotEmpty() && displayName.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Đăng Ký", fontSize = 16.sp)
                }
            }
        }
    }
}
