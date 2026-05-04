package com.example.myapplication.Feature.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.myapplication.R

@Composable
fun NotificationCard(
    time: String,
    content: String,
    isHighlight: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(
                color = if (isHighlight) Color(0xFFEAEAEA) else Color.White,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // TIME
        Text(
            text = time,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.width(60.dp)
        )

        // CONTENT
        Text(
            text = content,
            fontSize = 15.sp,
            color = Color(0xFF333333)
        )
    }
}