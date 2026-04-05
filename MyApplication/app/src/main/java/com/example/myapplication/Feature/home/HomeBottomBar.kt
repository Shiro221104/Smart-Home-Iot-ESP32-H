package com.example.myapplication.Feature.home
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.myapplication.R
import kotlinx.coroutines.*

@Composable
fun HomeBottomBar(selected: Int, onSelect: (Int) -> Unit){
    NavigationBar(
        contentColor = Color.White,
        tonalElevation = 3.dp,
        modifier = Modifier.height(60.dp),
        windowInsets = WindowInsets(0)
    ) {
        val titles = listOf<String>("Home","Profile","Support","Setting")
        val icons = listOf(R.drawable.bottom_btn1, R.drawable.bottom_btn2, R.drawable.bottom_btn3, R.drawable.bottom_btn4)
        titles.forEachIndexed { index, string ->
            NavigationBarItem(selected = selected == index, onClick = { onSelect(index) }, icon = {
                Icon(
                    painter = painterResource(icons[index]),
                    contentDescription = "",
                    modifier = Modifier.height(20.dp).width(20.dp),
                    tint = Color.Unspecified
                )
            }, label = {
                Text(text = string, fontSize = 10.sp, color = Color.Black)
            }, alwaysShowLabel = true )
        }
    }

}
@Preview
@Composable
fun HomeBottomBarPreview(){
    HomeBottomBar(selected = 0, onSelect = {})
}