package com.example.myapplication.Feature.home


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.Core.ViewModels.AuthViewModel

@Composable
fun HomeHeader(authViewModel: AuthViewModel = viewModel()) {
    var showNotiDropDown by remember { mutableStateOf(false) }
    var isClicked by remember { mutableStateOf(false) }
    
    // Lấy tên user từ AuthViewModel - quan sát sự thay đổi
    val currentUser = authViewModel.currentUser.value
    val displayName = if (currentUser?.displayName?.isNotEmpty() == true) {
        currentUser.displayName
    } else {
        currentUser?.email?.split("@")?.get(0) ?: "User"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Welcome Back, $displayName",
                fontSize = 18.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Have a nice day!",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }


    }
}


@Preview
@Composable
fun HomeHeaderPreview(){
  HomeHeader()
}
