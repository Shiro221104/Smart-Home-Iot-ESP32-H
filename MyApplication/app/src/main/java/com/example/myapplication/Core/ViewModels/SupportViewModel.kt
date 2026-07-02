package com.example.myapplication.Feature.Support

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.Core.Models.Device
import com.example.myapplication.Core.Models.DeviceType
import com.example.myapplication.Core.Models.Room
import com.example.myapplication.Core.Models.Schedule
import com.example.myapplication.Core.repository.DeviceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.util.Calendar

class SupportViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext get() = getApplication<Application>()

    val messages = mutableStateListOf(
        ChatMessage(
            "Hello 👋\nHow can I help you today?",
            false
        )
    )

    private val deviceRepository = DeviceRepository()

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    @Volatile private var deviceList: List<Device> = emptyList()
    @Volatile private var roomList: List<Room> = emptyList()

    // ===== CONTEXT =====
    private var lastIntent: String = ""
    private var lastParameters: Map<String, Any>? = null

    init {
        deviceRepository.getDevices { devices ->
            deviceList = devices
        }

        val userId = auth.currentUser?.uid
        if (userId != null) {
            val ref = database.getReference("users/$userId/rooms")
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Room>()
                    for (roomSnapshot in snapshot.children) {
                        val room = roomSnapshot.getValue(Room::class.java)
                        if (room != null) {
                            room.id = roomSnapshot.key ?: ""
                            list.add(room)
                        }
                    }
                    roomList = list
                }

                override fun onCancelled(error: DatabaseError) {
                    roomList = emptyList()
                }
            })
        }
    }

    fun sendMessage(userText: String) {
        messages.add(ChatMessage(userText, true))

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.sendMessage(
                    request = DialogflowRequest(
                        QueryInput(
                            TextInput(
                                text = userText,
                                languageCode = "vi"
                            )
                        )
                    )
                )

                val intentName = response.queryResult.intent?.displayName.orEmpty()
                val dialogReply = response.queryResult.fulfillmentText
                val parameters = response.queryResult.parameters

                val localReply = handleIntent(intentName, parameters, userText)

                if (intentName != "turn_off_last_device" && intentName != "turn_on_last_device") {
                    lastIntent = intentName
                    lastParameters = parameters
                }

                val finalReply = when {
                    localReply.isNotEmpty() -> localReply
                    !dialogReply.isNullOrBlank() -> dialogReply
                    else -> "Đã thực hiện xong."
                }

                withContext(Dispatchers.Main) {
                    messages.add(ChatMessage(finalReply, false))
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messages.add(ChatMessage("Error: ${e.message}", false))
                }
            }
        }
    }

    // ===== HELPER: Parse danh sách phòng từ parameters =====

    private data class RoomTarget(val name: String, val id: String)

    // ===== HELPER: Tách câu thành các cụm "device room" riêng biệt =====
    // Dùng cho trường hợp "bật đèn phòng khách và quạt phòng bếp"
    // (mỗi cụm có ĐÚNG 1 device + 1 room)

    private data class DeviceRoomClause(val type: DeviceType, val roomId: String?, val roomName: String?)

    private fun parseClausesFromRawText(rawText: String): List<DeviceRoomClause>? {
        // Tách theo "và" hoặc dấu phẩy
        val clauses = rawText.split(Regex("\\s+và\\s+|,\\s*"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (clauses.size < 2) return null // không có nhiều cụm -> để Dialogflow xử lý bình thường

        val result = mutableListOf<DeviceRoomClause>()

        for (clause in clauses) {
            val lower = clause.lowercase()

            val type = when {
                lower.contains("đèn") || lower.contains("light") -> DeviceType.LIGHT
                lower.contains("quạt") || lower.contains("fan")  -> DeviceType.FAN
                else -> null
            } ?: return null // cụm này không nhận diện được device -> bỏ, fallback Dialogflow

            val room = roomList.find { lower.contains(it.name.lowercase()) }

            // Nếu cụm không có room riêng (vd: chỉ "đèn"), coi như chưa rõ ràng -> fallback
            result += DeviceRoomClause(type, room?.id, room?.name)
        }

        // Chỉ áp dụng cách parse này nếu MỌI cụm đều xác định được room riêng
        // (đảm bảo đây thực sự là "mỗi cụm 1 device-room", không phải câu mơ hồ)
        if (result.any { it.roomId == null }) return null

        return result
    }

    private fun parseRoomTargets(
        parameters: Map<String, Any>?,
        error: (String) -> Unit
    ): List<RoomTarget?>? {
        val roomNames: List<String> = when (val raw = parameters?.get("room")) {
            is List<*> -> raw.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotBlank() } }
            is String  -> raw.trim().takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList()
            else       -> emptyList()
        }

        if (roomNames.isEmpty()) return listOf(null) // null = toàn bộ nhà

        val notFound = roomNames.filter { name ->
            roomList.none { it.name.equals(name, ignoreCase = true) }
        }
        if (notFound.isNotEmpty()) {
            val names = notFound.joinToString(", ") { "\"$it\"" }
            error("Không tìm thấy phòng $names trong hệ thống.")
            return null
        }

        return roomNames.map { name ->
            RoomTarget(
                name = name,
                id = roomList.first { it.name.equals(name, ignoreCase = true) }.id
            )
        }
    }

    // ===== HELPER: Parse danh sách device type từ parameters =====

    private fun parseDeviceTypes(parameters: Map<String, Any>?): List<DeviceType> {
        val deviceParam = when (val raw = parameters?.get("device")) {
            is List<*> -> raw.mapNotNull { it?.toString()?.trim()?.lowercase() }
            is String  -> listOf(raw.trim().lowercase())
            else       -> emptyList()
        }

        if (deviceParam.isEmpty()) return listOf(DeviceType.LIGHT, DeviceType.FAN)

        val types = mutableListOf<DeviceType>()
        if (deviceParam.any { it.contains("đèn") || it.contains("light") }) types += DeviceType.LIGHT
        if (deviceParam.any { it.contains("quạt") || it.contains("fan") }) types += DeviceType.FAN
        return types.ifEmpty { listOf(DeviceType.LIGHT, DeviceType.FAN) }
    }

    // ===== HELPER: Chạy action cho từng phòng, gom kết quả =====

    private fun forEachRoom(
        targets: List<RoomTarget?>,
        action: (roomId: String?, roomName: String?) -> String
    ): String = targets
        .mapNotNull { room -> action(room?.id, room?.name).takeIf { it.isNotBlank() } }
        .joinToString("\n")

    // ===== HELPER: Parse giờ từ parameter Dialogflow ("time", dạng ISO 8601) =====

    private fun parseDialogflowTime(parameters: Map<String, Any>?): Calendar? {
        val raw = when (val v = parameters?.get("time")) {
            is List<*> -> v.firstOrNull()?.toString()
            is String  -> v
            else       -> null
        }?.trim()?.takeIf { it.isNotBlank() } ?: return null

        return try {
            val odt = OffsetDateTime.parse(raw)
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, odt.hour)
                set(Calendar.MINUTE, odt.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    // ===== HELPER: Lưu lịch vào Firebase rồi đặt báo thức (AlarmManager) =====

    private fun saveScheduleAndArm(schedule: Schedule) {
        val userId = auth.currentUser?.uid ?: return
        val ref = database.getReference("users/$userId/schedules").push()
        schedule.id = ref.key ?: return
        ref.setValue(schedule)
        ScheduleManager.scheduleAlarm(appContext, schedule)
    }

    /** Gọi từ màn hình quản lý lịch (nếu có) để huỷ + xoá 1 lịch đã đặt. */
    fun deleteSchedule(schedule: Schedule) {
        val userId = auth.currentUser?.uid ?: return
        ScheduleManager.cancelAlarm(appContext, schedule)
        database.getReference("users/$userId/schedules/${schedule.id}").removeValue()
    }

    // ===== MAIN INTENT HANDLER =====

    private fun handleIntent(
        intentName: String,
        parameters: Map<String, Any>? = null,
        rawText: String = ""
    ): String {

        var roomError = ""
        val roomTargets = parseRoomTargets(parameters) { roomError = it }
        if (roomTargets == null) return roomError

        val singleRoomName = roomTargets.firstOrNull()?.name
        val singleRoomId   = roomTargets.firstOrNull()?.id

        return when (intentName) {

            // ===== TURN ON MULTI DEVICES =====

            "turn_on_multi_devices" -> {
                // Ưu tiên: thử tách câu gốc thành từng cụm "device room" riêng biệt
                val clauses = parseClausesFromRawText(rawText)
                if (clauses != null) {
                    clauses.joinToString("\n") { clause ->
                        controlDeviceMulti(clause.type, "ON", clause.roomId, clause.roomName)
                    }
                } else {
                    // Fallback: cross product như cũ
                    val deviceTypes = parseDeviceTypes(parameters)
                    forEachRoom(roomTargets) { roomId, roomName ->
                        deviceTypes
                            .map { controlDeviceMulti(it, "ON", roomId, roomName) }
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                    }.ifBlank { "Đã thực hiện xong." }
                }
            }

            // ===== TURN OFF MULTI DEVICES =====

            "turn_off_multi_devices" -> {
                val clauses = parseClausesFromRawText(rawText)
                if (clauses != null) {
                    clauses.joinToString("\n") { clause ->
                        controlDeviceMulti(clause.type, "OFF", clause.roomId, clause.roomName)
                    }
                } else {
                    val deviceTypes = parseDeviceTypes(parameters)
                    forEachRoom(roomTargets) { roomId, roomName ->
                        deviceTypes
                            .map { controlDeviceMulti(it, "OFF", roomId, roomName) }
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                    }.ifBlank { "Đã thực hiện xong." }
                }
            }

            // ===== TURN ON ALL LIGHTS =====

            "turn_on_all_lights" -> {
                val lights = deviceList.filter {
                    it.type == DeviceType.LIGHT.toFirebase() &&
                            (singleRoomId == null || it.room == singleRoomId)
                }
                var changedCount = 0
                lights.forEach { device ->
                    if (device.status != "ON") {
                        deviceRepository.updateDeviceStatus(device.id, "ON")
                        changedCount++
                    }
                }
                val suffix = if (singleRoomName != null) " ở $singleRoomName" else ""
                when {
                    lights.isEmpty()  -> "Không tìm thấy đèn$suffix"
                    changedCount == 0 -> "Tất cả đèn$suffix hiện đang bật"
                    else              -> "Đã bật tất cả đèn$suffix"
                }
            }

            // ===== TURN OFF ALL LIGHTS =====

            "turn_off_all_lights" -> {
                val lights = deviceList.filter {
                    it.type == DeviceType.LIGHT.toFirebase() &&
                            (singleRoomId == null || it.room == singleRoomId)
                }
                var changedCount = 0
                lights.forEach { device ->
                    if (device.status != "OFF") {
                        deviceRepository.updateDeviceStatus(device.id, "OFF")
                        changedCount++
                    }
                }
                val suffix = if (singleRoomName != null) " ở $singleRoomName" else ""
                when {
                    lights.isEmpty()  -> "Không tìm thấy đèn$suffix"
                    changedCount == 0 -> "Tất cả đèn$suffix hiện đang tắt"
                    else              -> "Đã tắt tất cả đèn$suffix"
                }
            }

            // ===== TURN ON ALL DEVICES =====

            "turn_on_all_devices" -> {
                val devices = deviceList.filter {
                    it.type != "door" &&
                            (singleRoomId == null || it.room == singleRoomId)
                }
                var changedCount = 0
                devices.forEach { device ->
                    if (device.status != "ON") {
                        deviceRepository.updateDeviceStatus(device.id, "ON")
                        changedCount++
                    }
                }
                val suffix = if (singleRoomName != null) " ở $singleRoomName" else ""
                when {
                    devices.isEmpty() -> "Không có thiết bị nào$suffix"
                    changedCount == 0 -> "Tất cả thiết bị$suffix hiện đang bật"
                    else              -> "Đã bật tất cả thiết bị$suffix"
                }
            }

            // ===== TURN OFF ALL DEVICES =====

            "turn_off_all_devices" -> {
                val devices = deviceList.filter {
                    it.type != "door" &&
                            (singleRoomId == null || it.room == singleRoomId)
                }
                var changedCount = 0
                devices.forEach { device ->
                    if (device.status != "OFF") {
                        deviceRepository.updateDeviceStatus(device.id, "OFF")
                        changedCount++
                    }
                }
                val suffix = if (singleRoomName != null) " ở $singleRoomName" else ""
                when {
                    devices.isEmpty() -> "Không có thiết bị nào$suffix"
                    changedCount == 0 -> "Tất cả thiết bị$suffix hiện đang tắt"
                    else              -> "Đã tắt tất cả thiết bị$suffix"
                }
            }

            // ===== QUERY DEVICE STATUS =====

            "query_device_status" -> {
                // Hỗ trợ cả 2 key parameter: "device_type" (string) hoặc "device" (string/list)
                val rawDeviceParam = parameters?.get("device_type") ?: parameters?.get("device")

                val deviceTypeName: String? = when (rawDeviceParam) {
                    is List<*> -> rawDeviceParam.firstOrNull()?.toString()?.trim()
                    is String  -> rawDeviceParam.trim()
                    else       -> null
                }?.takeIf { it.isNotBlank() }

                val deviceType = when (deviceTypeName?.lowercase()) {
                    "đèn", "light" -> DeviceType.LIGHT
                    "quạt", "fan"  -> DeviceType.FAN
                    else           -> null
                } ?: return "Không nhận ra loại thiết bị."

                val device = deviceList.find {
                    it.type == deviceType.toFirebase() &&
                            (singleRoomId == null || it.room == singleRoomId)
                }

                val suffix = if (singleRoomName != null) " ở $singleRoomName" else ""
                when {
                    device == null         -> "${deviceType.displayName} không tìm thấy$suffix"
                    device.status == "ON"  -> "${device.name}$suffix đang BẬT"
                    device.status == "OFF" -> "${device.name}$suffix đang TẮT"
                    else                   -> "${device.name}$suffix: ${device.status}"
                }
            }

            // ===== QUERY ACTIVE DEVICES =====

            "query_active_devices" -> {
                val activeDevices = deviceList.filter {
                    it.status == "ON" &&
                            it.type != "door" &&
                            (singleRoomId == null || it.room == singleRoomId)
                }
                val suffix = if (singleRoomName != null) " ở $singleRoomName" else ""
                if (activeDevices.isEmpty()) {
                    "Không có thiết bị nào đang bật$suffix"
                } else {
                    val list = activeDevices.joinToString("\n") { "• ${it.name}" }
                    "Các thiết bị đang bật$suffix:\n$list"
                }
            }

            // ===== SCHEDULE DEVICE: "bật đèn lúc 9h", "tắt quạt phòng ngủ lúc 22h30" =====

            "schedule_device" -> {
                val cal = parseDialogflowTime(parameters)
                    ?: return "Không hiểu giờ bạn muốn đặt lịch. Vui lòng nói rõ giờ, ví dụ \"lúc 9 giờ tối\"."

                val deviceTypes = parseDeviceTypes(parameters)
                val lowerText = rawText.lowercase()
                val action = if (lowerText.contains("tắt")) "OFF" else "ON"
                val actionWord = if (action == "ON") "bật" else "tắt"

                val hourStr = cal.get(Calendar.HOUR_OF_DAY)
                val minuteStr = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
                val suffix = if (singleRoomName != null) " ở $singleRoomName" else ""

                val results = deviceTypes.map { type ->
                    val schedule = Schedule(
                        deviceType = type.toFirebase(),
                        roomId = singleRoomId,
                        roomName = singleRoomName,
                        action = action,
                        hour = hourStr,
                        minute = cal.get(Calendar.MINUTE),
                        repeatDaily = true,
                        enabled = true
                    )
                    saveScheduleAndArm(schedule)
                    type.toVietnamese()
                }

                "Đã đặt lịch $actionWord ${results.joinToString(", ")}$suffix lúc $hourStr:$minuteStr hằng ngày."
            }

            // ===== FOLLOW-UP: TẮT THIẾT BỊ VỪA BẬT =====

            "turn_off_last_device" -> {
                val (prevRoomId, prevRoomName) = resolveLastRoom()
                val suffix = if (prevRoomName != null) " ở $prevRoomName" else ""
                when (lastIntent) {
                    "turn_on_multi_devices" -> {
                        // Tắt đúng loại thiết bị đã bật trước đó
                        val deviceTypes = parseDeviceTypes(lastParameters)
                        deviceTypes
                            .map { controlDeviceMulti(it, "OFF", prevRoomId, prevRoomName) }
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                            .ifBlank { "Đã tắt thiết bị$suffix" }
                    }
                    "turn_on_all_lights" -> controlDeviceMulti(DeviceType.LIGHT, "OFF", prevRoomId, prevRoomName)
                        .ifEmpty { "Đã tắt đèn$suffix" }
                    "turn_on_all_devices" -> {
                        val devices = deviceList.filter {
                            it.type != "door" && (prevRoomId == null || it.room == prevRoomId)
                        }
                        devices.forEach { deviceRepository.updateDeviceStatus(it.id, "OFF") }
                        if (devices.isEmpty()) "Không có thiết bị nào$suffix"
                        else "Đã tắt tất cả thiết bị$suffix"
                    }
                    else -> "Không có thiết bị nào đang được điều khiển"
                }
            }

            // ===== FOLLOW-UP: BẬT THIẾT BỊ VỪA TẮT =====

            "turn_on_last_device" -> {
                val (prevRoomId, prevRoomName) = resolveLastRoom()
                val suffix = if (prevRoomName != null) " ở $prevRoomName" else ""
                when (lastIntent) {
                    "turn_off_multi_devices" -> {
                        val deviceTypes = parseDeviceTypes(lastParameters)
                        deviceTypes
                            .map { controlDeviceMulti(it, "ON", prevRoomId, prevRoomName) }
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                            .ifBlank { "Đã bật thiết bị$suffix" }
                    }
                    "turn_off_all_lights" -> controlDeviceMulti(DeviceType.LIGHT, "ON", prevRoomId, prevRoomName)
                        .ifEmpty { "Đã bật đèn$suffix" }
                    "turn_off_all_devices" -> {
                        val devices = deviceList.filter {
                            it.type != "door" && (prevRoomId == null || it.room == prevRoomId)
                        }
                        devices.forEach { deviceRepository.updateDeviceStatus(it.id, "ON") }
                        if (devices.isEmpty()) "Không có thiết bị nào$suffix"
                        else "Đã bật tất cả thiết bị$suffix"
                    }
                    else -> "Không có thiết bị nào đang được điều khiển"
                }
            }

            else -> ""
        }
    }

    // ===== Resolve phòng từ lastParameters (dùng cho follow-up) =====

    private fun resolveLastRoom(): Pair<String?, String?> {
        val rawRoom = lastParameters?.get("room")
        val name: String? = when (rawRoom) {
            is List<*> -> rawRoom.firstOrNull()?.toString()?.trim()
            is String  -> rawRoom.trim()
            else       -> null
        }?.takeIf { it.isNotBlank() }

        val id = name?.let { n -> roomList.find { it.name.equals(n, ignoreCase = true) }?.id }
        return Pair(id, name)
    }

    // ===== controlDeviceMulti: bật/tắt TẤT CẢ thiết bị cùng loại trong phòng =====

    private fun controlDeviceMulti(
        type: DeviceType,
        status: String,
        roomId: String?,
        roomName: String?
    ): String {
        val suffix = if (roomName != null) " ở $roomName" else ""
        val typeName = type.toVietnamese()
        val devices = deviceList.filter {
            it.type == type.toFirebase() &&
                    (roomId == null || it.room == roomId)
        }

        if (devices.isEmpty()) return "$typeName không tìm thấy$suffix"

        val toChange = devices.filter { it.status != status }
        toChange.forEach { deviceRepository.updateDeviceStatus(it.id, status) }

        val action = if (status == "ON") "bật" else "tắt"
        return when {
            toChange.isEmpty()            -> "$typeName$suffix hiện đang $action rồi"
            toChange.size == devices.size -> "Đã $action $typeName$suffix"
            else                          -> "Đã $action ${toChange.size}/${devices.size} $typeName$suffix"
        }
    }
}

private fun DeviceType.toVietnamese(): String = when (this) {
    DeviceType.LIGHT -> "đèn"
    DeviceType.FAN   -> "quạt"
    else             -> this.displayName
}