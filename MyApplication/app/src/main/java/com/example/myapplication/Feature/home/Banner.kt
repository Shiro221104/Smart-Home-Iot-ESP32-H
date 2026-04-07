package com.example.myapplication.Feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R

@Composable
fun Banner(temp: String, humidity: String) {

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {


        Row(
            modifier = Modifier
                .weight(1f)
                .height(110.dp)
                .background(
                    color = Color(0xFF37C9BB),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                painter = painterResource(id = R.drawable.temperature),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text("Temperature", color = Color.White.copy(alpha = 0.8f))
                Text(temp, fontSize = 24.sp, color = Color.White)
            }
        }


        Row(
            modifier = Modifier
                .weight(1f)
                .height(110.dp)
                .background(
                    color = Color(0xFF4FC3F7),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                painter = painterResource(id = R.drawable.humidity),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text("Humidity", color = Color.White.copy(alpha = 0.8f))
                Text(humidity, fontSize = 24.sp, color = Color.White)
            }
        }
    }
}
