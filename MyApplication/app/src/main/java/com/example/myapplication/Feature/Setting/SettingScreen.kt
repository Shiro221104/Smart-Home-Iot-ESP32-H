package com.example.myapplication.Feature.Setting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.Feature.home.AddRoomDialog
import com.example.myapplication.Core.ViewModels.RoomViewModel
import com.example.myapplication.Core.Models.Device
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// ─── Màu chủ đạo ───────────────────────────────────────────────────────────
private val BgColor     = Color(0xFFF7F8FA)
private val CardColor   = Color.White
private val AccentBlue  = Color(0xFF3D7EF5)
private val TextPrimary = Color(0xFF1A1A2E)
private val TextMuted   = Color(0xFF9098A3)
private val DangerRed   = Color(0xFFE53935)

@Composable
fun SettingScreen(
    roomViewModel: RoomViewModel = viewModel()
) {
    val rooms by roomViewModel.rooms.collectAsStateWithLifecycle()

    // ─── State ────────────────────────────────────────────────────────────
    var isDarkMode        by remember { mutableStateOf(false) }
    var showRoomDialog    by remember { mutableStateOf(false) }
    var showAboutDialog   by remember { mutableStateOf(false) }
    var showPasswordSheet by remember { mutableStateOf(false) }

    Scaffold(containerColor = BgColor) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Tiêu đề ─────────────────────────────────────────────────
            Text(
                text = "Cài đặt",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Tuỳ chỉnh ứng dụng của bạn",
                fontSize = 14.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Section: Giao diện ───────────────────────────────────────
            SectionLabel("Giao diện")
            SettingCard {
                ToggleRow(
                    icon = Icons.Outlined.DarkMode,
                    label = "Chế độ tối",
                    checked = isDarkMode,
                    onCheckedChange = { isDarkMode = it }
                )
            }

            // ── Section: Bảo mật ────────────────────────────────────────
            SectionLabel("Bảo mật")
            SettingCard {
                ActionRow(
                    icon = Icons.Outlined.Lock,
                    label = "Đổi mật khẩu cửa",
                    onClick = { showPasswordSheet = true }
                )
            }

            // ── Section: Ngôi nhà ────────────────────────────────────────
            SectionLabel("Ngôi nhà")
            SettingCard {
                // Header phòng
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconBadge(Icons.Outlined.Home, AccentBlue)
                        Column {
                            Text("Quản lý phòng", fontWeight = FontWeight.Medium, color = TextPrimary, fontSize = 15.sp)
                            Text(
                                text = if (rooms.isEmpty()) "Chưa có phòng nào" else "${rooms.size} phòng",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                    // Nút thêm phòng
                    FilledTonalIconButton(
                        onClick = { showRoomDialog = true },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = AccentBlue.copy(alpha = 0.1f),
                            contentColor = AccentBlue
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm phòng", modifier = Modifier.size(18.dp))
                    }
                }

                // Danh sách phòng
                AnimatedVisibility(
                    visible = rooms.isNotEmpty(),
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 260.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            userScrollEnabled = true
                        ) {
                            items(rooms, key = { it.id }) { room ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BgColor)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(AccentBlue)
                                        )
                                        Text(room.name, fontSize = 14.sp, color = TextPrimary)
                                    }
                                    IconButton(
                                        onClick = { roomViewModel.deleteRoom(room.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Xóa",
                                            tint = DangerRed.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Section: Thông tin ───────────────────────────────────────
            SectionLabel("Thông tin")
            SettingCard {
                ActionRow(
                    icon = Icons.Outlined.Info,
                    label = "Về ứng dụng",
                    onClick = { showAboutDialog = true }
                )
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // DIALOG: Thêm phòng
    // ══════════════════════════════════════════════════════════════════════
    if (showRoomDialog) {
        AddRoomDialog(
            onDismiss = { showRoomDialog = false },
            onAdd = { showRoomDialog = false }
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    // DIALOG: About
    // ══════════════════════════════════════════════════════════════════════
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Outlined.Info, null, tint = AccentBlue) },
            title = { Text("Về ứng dụng", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Smart Home App\nPhiên bản 1.0\nKết nối ESP32 + Firebase",
                    color = TextMuted,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Đóng") }
            }
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    // DIALOG: Đổi mật khẩu
    // ══════════════════════════════════════════════════════════════════════
    if (showPasswordSheet) {
        ChangePasswordDialog(
            rooms = rooms,
            onDismiss = { showPasswordSheet = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialog đổi mật khẩu CỬA (Firebase Database)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChangePasswordDialog(
    rooms: List<com.example.myapplication.Core.Models.Room>,
    onDismiss: () -> Unit
) {
    // Map roomId → roomName để tra cứu nhanh
    val roomMap = remember(rooms) { rooms.associateBy({ it.id }, { it.name }) }

    // Load danh sách thiết bị DOOR từ Firebase
    var doorDevices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var selectedDoor by remember { mutableStateOf<Device?>(null) }
    var expandedDoor by remember { mutableStateOf(false) }

    var newPassword     by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showNew         by remember { mutableStateOf(false) }
    var showConfirm     by remember { mutableStateOf(false) }

    var isLoading      by remember { mutableStateOf(false) }
    var errorMessage   by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Lắng nghe Firebase — chỉ lấy device có type == "DOOR"
    DisposableEffect(Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("devices")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                doorDevices = snapshot.children.mapNotNull { it.getValue(Device::class.java) }
                    .filter { it.type.uppercase() == "DOOR" }
                if (selectedDoor == null) selectedDoor = doorDevices.firstOrNull()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    // Validation
    val passwordMismatch = confirmPassword.isNotEmpty() && newPassword != confirmPassword
    val notFourDigits    = newPassword.isNotEmpty() && (newPassword.length != 4 || !newPassword.all { it.isDigit() })
    val canSubmit        = selectedDoor != null
            && newPassword.length == 4
            && newPassword.all { it.isDigit() }
            && newPassword == confirmPassword
            && !isLoading

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Lock, null, tint = AccentBlue, modifier = Modifier.size(24.dp))
            }
        },
        title = {
            Text("Đổi mật khẩu cửa", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                if (doorDevices.isEmpty()) {
                    // Chưa có cửa nào
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        Text("Chưa có thiết bị cửa nào", color = TextMuted, fontSize = 14.sp)
                    }
                } else {
                    // 🚪 Chọn cửa
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedDoor?.let {
                                val roomName = roomMap[it.room] ?: "?"
                                "${it.name}  ·  $roomName"
                            } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Chọn cửa", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.SensorDoor, null) },
                            trailingIcon = {
                                IconButton(onClick = { expandedDoor = !expandedDoor }) {
                                    Icon(
                                        if (expandedDoor) Icons.Default.KeyboardArrowUp
                                        else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )
                        DropdownMenu(
                            expanded = expandedDoor,
                            onDismissRequest = { expandedDoor = false }
                        ) {
                            doorDevices.forEach { door ->
                                val roomName = roomMap[door.room] ?: "Không rõ phòng"
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(door.name, fontSize = 14.sp, color = TextPrimary)
                                            Text(roomName, fontSize = 12.sp, color = TextMuted)
                                        }
                                    },
                                    onClick = {
                                        selectedDoor = door
                                        expandedDoor = false
                                        newPassword = ""
                                        confirmPassword = ""
                                        successMessage = null
                                        errorMessage = null
                                    }
                                )
                            }
                        }
                    }

                    // 🔐 Mật khẩu mới
                    PasswordField(
                        value = newPassword,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                newPassword = it
                                errorMessage = null
                                successMessage = null
                            }
                        },
                        label = "Mật khẩu mới (4 chữ số)",
                        visible = showNew,
                        onToggleVisible = { showNew = !showNew },
                        isError = notFourDigits,
                        supportingText = if (notFourDigits) "Nhập đúng 4 chữ số" else null,
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    )

                    // 🔐 Xác nhận mật khẩu mới
                    PasswordField(
                        value = confirmPassword,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                confirmPassword = it
                                errorMessage = null
                                successMessage = null
                            }
                        },
                        label = "Xác nhận mật khẩu",
                        visible = showConfirm,
                        onToggleVisible = { showConfirm = !showConfirm },
                        isError = passwordMismatch,
                        supportingText = if (passwordMismatch) "Mật khẩu không khớp" else null,
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    )
                }

                // Lỗi
                if (errorMessage != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = DangerRed, modifier = Modifier.size(16.dp))
                        Text(errorMessage!!, color = DangerRed, fontSize = 13.sp)
                    }
                }

                // Thành công
                if (successMessage != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircleOutline, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Text(successMessage!!, color = Color(0xFF4CAF50), fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSubmit,
                onClick = {
                    val door = selectedDoor ?: return@Button
                    isLoading = true
                    errorMessage = null
                    // Cập nhật field password của device trên Firebase
                    FirebaseDatabase.getInstance()
                        .getReference("devices")
                        .child(door.id)
                        .child("password")
                        .setValue(newPassword)
                        .addOnSuccessListener {
                            isLoading = false
                            successMessage = "Đã đổi mật khẩu \"${door.name}\" thành công!"
                            newPassword = ""
                            confirmPassword = ""
                        }
                        .addOnFailureListener { e ->
                            isLoading = false
                            errorMessage = e.message ?: "Lỗi khi cập nhật"
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Lưu")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Huỷ", color = TextMuted)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextMuted,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun IconBadge(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon, AccentBlue)
            Text(label, fontWeight = FontWeight.Medium, color = TextPrimary, fontSize = 15.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(icon, AccentBlue)
                Text(label, fontWeight = FontWeight.Medium, color = TextPrimary, fontSize = 15.sp)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        isError = isError,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        supportingText = if (supportingText != null) {
            { Text(supportingText, fontSize = 12.sp) }
        } else null,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = Color(0xFFE5E7EB),
            errorBorderColor = DangerRed
        )
    )
}