package com.example.myapplication.Feature.Intro

import com.example.myapplication.MainActivity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.Core.ViewModels.AuthViewModel
import kotlinx.coroutines.*

class IntroActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        )

        setContent {
            IntroNavigation(
                onNavigateToMain = {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                    finish()
                }
            )
        }
    }

    @Composable
    fun IntroNavigation(onNavigateToMain: () -> Unit) {
        val authViewModel: AuthViewModel = viewModel()
        val currentAuthScreen = remember { mutableStateOf("splash") }

        when (currentAuthScreen.value) {
            "splash" -> {
                SplashScreen {
                    // Kiểm tra xem user đã đăng nhập chưa
                    if (authViewModel.currentUser.value != null) {
                        onNavigateToMain()
                    } else {
                        currentAuthScreen.value = "login"
                    }
                }
            }

            "login" -> {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        onNavigateToMain()
                    },
                    onSignupClick = {
                        currentAuthScreen.value = "signup"
                    }
                )
            }

            "signup" -> {
                SignupScreen(
                    authViewModel = authViewModel,
                    onBackClick = {
                        currentAuthScreen.value = "login"
                    },
                    onSignupSuccess = {
                        onNavigateToMain()
                    }
                )
            }
        }
    }

    @Composable
    fun SplashScreen(onNavigate: () -> Unit) {
        LaunchedEffect(Unit) {
            delay(1000)
            onNavigate()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEEF2F7),
                            Color(0xFFE3F2FD)
                        )
                    )
                )
        ) {

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-40).dp)

            )


            Box(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-60).dp, y = 60.dp)

            )


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(260.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "SMART HOME",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0A2540)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Smart Living Made Simple",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}