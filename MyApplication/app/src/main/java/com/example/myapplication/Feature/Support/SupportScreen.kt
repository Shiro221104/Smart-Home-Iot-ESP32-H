package com.example.myapplication.Feature.Support

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Purple1 = Color(0xFF7B2FF7)
private val Purple2 = Color(0xFF5F0A87)
private val Background = Color(0xFFF5F6FB)

fun getCurrentTime(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date())
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: String = getCurrentTime()
)

@Composable
fun SupportScreen(
    viewModel: SupportViewModel = viewModel()
) {
    var messageText by remember { mutableStateOf("") }
    val messages = viewModel.messages
    val listState = rememberLazyListState()


    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp)
        ) {
            items(messages) { message ->
                if (message.isUser) {
                    UserMessage(message = message.text, timestamp = message.timestamp)
                } else {
                    BotMessage(message = message.text, timestamp = message.timestamp)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .background(Color.White, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            BasicTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
                decorationBox = { innerTextField ->
                    if (messageText.isEmpty()) {
                        Text(
                            text = "Type your message here...",
                            color = Color.Gray,
                            fontSize = 15.sp
                        )
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Purple1, Purple2))),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        if (messageText.isNotEmpty()) {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun BotMessage(message: String, timestamp: String = getCurrentTime()) {
    Row(verticalAlignment = Alignment.Top) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Purple1, Purple2))),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.chatbot),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Box(
                modifier = Modifier
                    .background(
                        Color(0xFFEDEDED),
                        RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .padding(14.dp)
                    .widthIn(max = 250.dp)
            ) {
                Text(
                    text = message,
                    fontSize = 15.sp,
                    color = Color.DarkGray,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(text = timestamp, color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
fun UserMessage(message: String, timestamp: String = getCurrentTime()) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(listOf(Purple1, Purple2)),
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 0.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .padding(14.dp)
                    .widthIn(max = 270.dp)
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(text = timestamp, color = Color.Gray, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Image(
            painter = painterResource(R.drawable.avatar),
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SupportScreenPreview() {
    SupportScreen()
}