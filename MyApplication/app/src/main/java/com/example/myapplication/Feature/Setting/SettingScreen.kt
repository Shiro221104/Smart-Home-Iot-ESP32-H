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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.Feature.home.AddRoomDialog
import com.example.myapplication.Core.ViewModels.RoomViewModel
import com.example.myapplication.Core.ViewModels.AuthViewModel
import com.example.myapplication.Core.Models.Device
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.ui.res.painterResource
import com.example.myapplication.Feature.Intro.IntroActivity
import com.example.myapplication.R
import androidx.compose.material.icons.automirrored.filled.Logout
import com.example.myapplication.Feature.Setting.AccentBlue

// ─── Màu chủ đạo ───────────────────────────────────────────────────────────
private val BgColor     = Color(0xFFF7F8FA)
private val CardColor   = Color.White
private val AccentBlue  = Color(0xFF3D7EF5)
private val TextPrimary = Color(0xFF1A1A2E)
private val TextMuted   = Color(0xFF9098A3)
private val DangerRed   = Color(0xFFE53935)

@Composable
fun SettingScreen(
    roomViewModel: RoomViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val rooms by roomViewModel.rooms.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ─── State ────────────────────────────────────────────────────────────
    var isDarkMode by remember { mutableStateOf(false) }
    var showRoomDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPasswordSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var roomToDelete by remember { mutableStateOf<com.example.myapplication.Core.Models.Room?>(null) }
    var deleteErrorMessage by remember { mutableStateOf("") }

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
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Section: Giao diện ───────────────────────────────────────
            SectionLabel("Giao diện")
            ToggleRow(
                icon = Icons.Outlined.DarkMode,
                label = "Chế độ tối",
                checked = isDarkMode,
                onCheckedChange = { isDarkMode = it },
            )

            // ── Section: Bảo mật ────────────────────────────────────────
            SectionLabel("Bảo mật")
            SettingCard {
                ActionRow(
                    icon = Icons.Outlined.Lock,
                    label = "Đổi mật khẩu cửa",
                    onClick = { showPasswordSheet = true }
                )
            }

            // ── Section: Nhà ────────────────────────────────────────
            SectionLabel("Nhà")
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
                            Text(
                                "Quản lý phòng",
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                fontSize = 15.sp,
                            )
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
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Thêm phòng",
                            modifier = Modifier.size(18.dp)
                        )
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
                                        onClick = { 
                                            roomToDelete = room
                                            showDeleteConfirmDialog = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource( R.drawable.trash),
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

            // ── Section: Logout ───────────────────────────────────────
            SectionLabel("Đăng xuất")
            SettingCard {
                ActionRow(
                    Icons.AutoMirrored.Filled.Logout,
                    label = "Đăng xuất",
                    onClick = { showLogoutDialog = true },
                    tint = DangerRed
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
            onAdd = { showRoomDialog = false },
            existingRooms = rooms
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

    // ══════════════════════════════════════════════════════════════════════
    // DIALOG: Logout
    // ══════════════════════════════════════════════════════════════════════
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DangerRed.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Logout,
                        null,
                        tint = DangerRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = { Text("Đăng xuất", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "Bạn chắc chắn muốn đăng xuất khỏi ứng dụng?",
                    color = TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Hủy", color = TextMuted)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Đăng xuất
                        authViewModel.signOut()
                        // Quay lại IntroActivity
                        val intent = Intent(context, IntroActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Đăng xuất")
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    // DIALOG: Xác nhận xóa phòng
    // ══════════════════════════════════════════════════════════════════════
    if (showDeleteConfirmDialog && roomToDelete != null) {
        DeleteRoomConfirmDialog(
            room = roomToDelete!!,
            onDismiss = { 
                showDeleteConfirmDialog = false
                roomToDelete = null
                deleteErrorMessage = ""
            },
            onConfirm = { room ->
                // Check if room has devices
                val auth = FirebaseAuth.getInstance()
                val userId = auth.currentUser?.uid ?: return@DeleteRoomConfirmDialog
                
                val devicesRef = FirebaseDatabase.getInstance().getReference("users/$userId/devices")
                devicesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val devicesInRoom = snapshot.children.mapNotNull { 
                            it.getValue(Device::class.java) 
                        }.filter { it.room == room.id }
                        
                        if (devicesInRoom.isNotEmpty()) {
                            deleteErrorMessage = "Không thể xóa phòng vì nó chứa ${devicesInRoom.size} thiết bị. Vui lòng xóa thiết bị trước."
                        } else {
                            roomViewModel.deleteRoom(room.id)
                            showDeleteConfirmDialog = false
                            roomToDelete = null
                            deleteErrorMessage = ""
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        deleteErrorMessage = "Lỗi khi kiểm tra thiết bị. Vui lòng thử lại."
                    }
                })
            }
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    // DIALOG: Lỗi khi xóa phòng
    // ══════════════════════════════════════════════════════════════════════
    if (deleteErrorMessage.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { deleteErrorMessage = "" },
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DangerRed.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        null,
                        tint = DangerRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = { Text("Không thể xóa phòng", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    deleteErrorMessage,
                    color = TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { deleteErrorMessage = "" },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Đóng")
                }
            }
        )
    }

}
@OptIn(ExperimentalMaterial3Api::class)
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

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Lắng nghe Firebase — chỉ lấy device có type == "DOOR"
    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        if (userId != null) {
            val ref = FirebaseDatabase.getInstance().getReference("users/$userId/devices")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    doorDevices =
                        snapshot.children.mapNotNull { it.getValue(Device::class.java) }
                            .filter { it.type.uppercase() == "DOOR" }
                    if (selectedDoor == null) selectedDoor = doorDevices.firstOrNull()
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            ref.addValueEventListener(listener)
            onDispose { ref.removeEventListener(listener) }
        } else {
            onDispose {}
        }
    }

    // Validation
    val passwordMismatch = confirmPassword.isNotEmpty() && newPassword != confirmPassword
    val notFourDigits =
        newPassword.isNotEmpty() && (newPassword.length != 4 || !newPassword.all { it.isDigit() })
    val canSubmit = selectedDoor != null
            && newPassword.length == 4
            && newPassword.all { it.isDigit() }
            && newPassword == confirmPassword

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(text = "Đổi Mật Khẩu Cửa", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                if (doorDevices.isEmpty()) {
                    // Chưa có cửa nào
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Chưa có thiết bị cửa nào", color = TextMuted, fontSize = 14.sp)
                    }
                } else {
                    // Chọn cửa
                    ExposedDropdownMenuBox(
                        expanded = expandedDoor,
                        onExpandedChange = { expandedDoor = !expandedDoor }
                    ) {
                        OutlinedTextField(
                            value = selectedDoor?.let {
                                val roomName = roomMap[it.room] ?: "?"
                                "${it.name} · $roomName"
                            } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = { Icon(painter = painterResource(R.drawable.door), null, modifier = Modifier.size(25.dp)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedDoor) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                        )

                        ExposedDropdownMenu(
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
                                        errorMessage = ""
                                        isError = false
                                    }
                                )
                            }
                        }
                    }

                    // Mật khẩu mới
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                newPassword = it
                                isError = false
                                errorMessage = ""
                            }
                        },
                        label = { Text("Mật khẩu mới") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        isError = notFourDigits,
                        supportingText = {
                            if (notFourDigits) Text("Nhập đúng 4 chữ số")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        trailingIcon = {
                            IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                Icon(
                                    imageVector = if (showNewPassword)
                                        Icons.Default.VisibilityOff
                                    else
                                        Icons.Default.Visibility,
                                    contentDescription = if (showNewPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (showNewPassword)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Xác nhận mật khẩu
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                confirmPassword = it
                                isError = false
                                errorMessage = ""
                            }
                        },
                        label = { Text("Xác nhận mật khẩu") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        isError = passwordMismatch,
                        supportingText = {
                            if (passwordMismatch) Text("Mật khẩu không khớp")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    imageVector = if (showConfirmPassword)
                                        Icons.Default.VisibilityOff
                                    else
                                        Icons.Default.Visibility,
                                    contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (showConfirmPassword)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Lỗi
                    if (isError && errorMessage.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    DangerRed.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                null,
                                tint = DangerRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(errorMessage, color = DangerRed, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSubmit,
                onClick = {
                    val door = selectedDoor ?: return@Button
                    val auth = FirebaseAuth.getInstance()
                    val userId = auth.currentUser?.uid ?: return@Button

                    FirebaseDatabase.getInstance()
                        .getReference("users/$userId/devices")
                        .child(door.id)
                        .child("password")
                        .setValue(newPassword)
                        .addOnSuccessListener {
                            newPassword = ""
                            confirmPassword = ""
                            isError = false
                            errorMessage = ""
                            onDismiss()
                        }
                        .addOnFailureListener { e ->
                            isError = true
                            errorMessage = e.message ?: "Lỗi khi cập nhật"
                        }
                }
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
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
    onCheckedChange: (Boolean) -> Unit,

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
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentBlue
            )
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = AccentBlue
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
                IconBadge(icon, tint)
                Text(label, fontWeight = FontWeight.Medium, color = if (tint == AccentBlue) TextPrimary else tint, fontSize = 15.sp)
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

// ─────────────────────────────────────────────────────────────────────────────
// Dialog xác nhận xóa phòng
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DeleteRoomConfirmDialog(
    room: com.example.myapplication.Core.Models.Room,
    onDismiss: () -> Unit,
    onConfirm: (com.example.myapplication.Core.Models.Room) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DangerRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.trash),
                    null,
                    tint = DangerRed,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = { Text("Xóa phòng \"${room.name}\"?", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Text(
                "Bạn chắc chắn muốn xóa phòng này không?\n\nLưu ý: Tất cả thiết bị trong phòng này cũng sẽ bị xóa.",
                color = TextMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextMuted)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(room) },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Xóa")
            }
        }
    )
}