package com.example.myapplication.Feature.home

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MainScreen(

){
    var selectedBottom by remember { mutableStateOf(value = 0) }
    Scaffold(contentColor = Color.White, bottomBar = {
        HomeBottomBar(
            selected = selectedBottom,
            onSelect = {selectedBottom=it}
        )
    } ) { inner -> LazyColumn(contentPadding = inner) {
          item { HomeHeader() }
        item { Banner() }
    }
    }
}
@Preview
@Composable
fun MainScreenPreview(){
    MainScreen()
}