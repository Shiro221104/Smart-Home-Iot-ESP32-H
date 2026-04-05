package com.example.myapplication.Feature.home

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Banner(){
    Row(
        modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.Center

    ){
        Column(modifier = Modifier.weight(0.8f).height(170.dp).background(color = Color(android.graphics.Color.parseColor("#37c9bb")), shape = RoundedCornerShape(20.dp)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
               Text(
                   text = "Temperature",
                   fontSize = 18.sp,
                   modifier = Modifier.padding(top = 12.dp),
                   fontWeight = FontWeight.Bold,
                   color = Color.White
               )
        }
    }
}
@Preview
@Composable
fun BannerPreview(){
Banner()
}