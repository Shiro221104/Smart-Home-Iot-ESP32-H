package com.example.myapplication.Feature.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.Core.ViewModels.NotificationViewModel
import com.example.myapplication.R
import com.example.myapplication.Core.utils.TimeUtils

@Composable
fun NotificationScreen() {

    val viewModel: NotificationViewModel = viewModel()
    val list = viewModel.notifications.value
    val todayList = list.filter { TimeUtils.isToday(it.time) }
    val yesterdayList = list.filter { TimeUtils.isYesterday(it.time) }
    val olderList = list.filter {
        !TimeUtils.isToday(it.time) && !TimeUtils.isYesterday(it.time)
    }
    Scaffold(
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->

        Column(modifier = Modifier.padding(padding)) {

            // HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thông Báo",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }

            // LIST
            LazyColumn(
                modifier = Modifier.padding(12.dp)
            ) {

                if (todayList.isNotEmpty()) {
                    item {
                        Text(
                            text = "Hôm nay",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(todayList) { noti ->
                        NotificationCard(
                            time = TimeUtils.timeAgo(noti.time),
                            content = noti.message,
                            isHighlight = noti.title.contains("GAS")
                        )
                    }
                }

                if (yesterdayList.isNotEmpty()) {
                    item {
                        Text(
                            text = "Hôm qua",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(yesterdayList) { noti ->
                        NotificationCard(
                            time = TimeUtils.timeAgo(noti.time),
                            content = noti.message,
                            isHighlight = noti.title.contains("GAS")
                        )
                    }
                }

                if (olderList.isNotEmpty()) {
                    item {
                        Text(
                            text = "Cũ hơn",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(olderList) { noti ->
                        NotificationCard(
                            time = TimeUtils.timeAgo(noti.time),
                            content = noti.message,
                            isHighlight = noti.title.contains("GAS")
                        )
                    }
                }
            }
        }
    }
}