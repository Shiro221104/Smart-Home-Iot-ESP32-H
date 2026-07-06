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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    private var lastDeviceRoomClauses: List<DeviceRoomClause>? = null

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

                lastDeviceRoomClauses = if (intentName == "turn_on_multi_devices" || intentName == "turn_off_multi_devices") {
                    parseClausesFromRawText(userText)
                } else {
                    null
                }

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

    private fun parseDeviceTypesForCancel(parameters: Map<String, Any>?): List<DeviceType>? {
        val deviceParam = when (val raw = parameters?.get("device")) {
            is List<*> -> raw.mapNotNull { it?.toString()?.trim()?.lowercase() }
            is String  -> listOf(raw.trim().lowercase())
            else       -> emptyList()
        }

        if (deviceParam.isEmpty()) return null

        val types = mutableListOf<DeviceType>()
        if (deviceParam.any { it.contains("đèn") || it.contains("light") }) types += DeviceType.LIGHT
        if (deviceParam.any { it.contains("quạt") || it.contains("fan") }) types += DeviceType.FAN
        if (deviceParam.any { it.contains("cửa") || it.contains("door") }) types += DeviceType.DOOR
        return types.ifEmpty { null }
    }

    private fun scheduleMatch(
        schedule: Schedule,
        deviceTypes: List<DeviceType>?,
        roomIds: List<String>?,
        time: Calendar?
    ): Boolean {
        if (deviceTypes != null && deviceTypes.isNotEmpty()) {
            val firebaseTypes = deviceTypes.map { it.toFirebase() }
            if (schedule.deviceType !in firebaseTypes) return false
        }

        if (roomIds != null && schedule.roomId !in roomIds) return false

        if (time != null) {
            if (schedule.hour != time.get(Calendar.HOUR_OF_DAY) ||
                schedule.minute != time.get(Calendar.MINUTE)
            ) return false

            schedule.scheduleDate?.let {
                if (formatDateToISO(time) != it) return false
            }
        }

        return true
    }

    private fun formatScheduleDescription(schedule: Schedule): String {
        val deviceName = DeviceType.fromString(schedule.deviceType).toVietnamese()
        val roomName = schedule.roomId?.let { id ->
            roomList.find { it.id == id }?.name ?: id
        }
        val roomSuffix = roomName?.let { " ở $it" } ?: ""
        val time = "${schedule.hour.toString().padStart(2, '0')}:${schedule.minute.toString().padStart(2, '0')}"
        val dateText = when {
            schedule.repeatDaily -> " mỗi ngày"
            schedule.scheduleDate != null -> " vào ${formatDateForDisplay(schedule.scheduleDate)}"
            else -> ""
        }
        val actionWord = if (schedule.action == "ON") "Bật" else "Tắt"
        return "$actionWord $deviceName$roomSuffix lúc $time$dateText"
    }

    private fun formatDateForDisplay(isoDate: String): String {
        return try {
            val parts = isoDate.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            "ngày $day tháng $month"
        } catch (e: Exception) {
            isoDate
        }
    }

    private suspend fun getSchedules(): List<Schedule> = suspendCancellableCoroutine { cont ->
        val userId = auth.currentUser?.uid
        if (userId == null) {
            cont.resume(emptyList())
            return@suspendCancellableCoroutine
        }

        val ref = database.getReference("users/$userId/schedules")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val schedules = mutableListOf<Schedule>()
                for (item in snapshot.children) {
                    val schedule = item.getValue(Schedule::class.java)
                    if (schedule != null) {
                        schedule.id = item.key ?: schedule.id
                        schedules.add(schedule)
                    }
                }
                cont.resume(schedules)
            }

            override fun onCancelled(error: DatabaseError) {
                cont.resumeWithException(error.toException())
            }
        }

        ref.addListenerForSingleValueEvent(listener)
        cont.invokeOnCancellation { ref.removeEventListener(listener) }
    }

    // ===== HELPER: Chạy action cho từng phòng, gom kết quả =====

    private fun forEachRoom(
        targets: List<RoomTarget?>,
        action: (roomId: String?, roomName: String?) -> String
    ): String = targets
        .mapNotNull { room -> action(room?.id, room?.name).takeIf { it.isNotBlank() } }
        .joinToString("\n")

    // ===== HELPER: Kiểm tra xem người dùng có ý định đặt lịch hay không =====

    private fun isScheduleIntent(rawText: String, hasTimeParameter: Boolean): Boolean {
        val lowerText = rawText.lowercase()
        val scheduleKeywords = listOf(
            "lúc", "giờ", "h:", "h ", "và tắt", "mỗi ngày", "hôm nay", "ngày mai",
            "thứ", "đặt lịch", "set schedule", "every day", "daily", "phút nữa", "giờ nữa",
            "phút", "giờ", "sau"
        )
        return hasTimeParameter || scheduleKeywords.any { lowerText.contains(it) }
    }

    private fun parseRelativeDelayToCalendar(rawText: String, now: Calendar = Calendar.getInstance()): Calendar? {
        val lowerText = rawText.lowercase()
        val pattern = Regex("""(?:sau|trong|vào)?\s*(\d+)\s*(phút|phut|min|m|giờ|hour|h)\s*(nữa|sau|đó)?""")
        val match = pattern.find(lowerText) ?: return null

        return try {
            val amount = match.groupValues[1].toInt()
            val unit = match.groupValues[2]
            (now.clone() as Calendar).apply {
                when (unit) {
                    "phút", "phut", "min", "m" -> add(Calendar.MINUTE, amount)
                    "giờ", "hour", "h" -> add(Calendar.HOUR_OF_DAY, amount)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    // ===== HELPER: Parse ngày từ text (hôm nay, ngày mai, ngày kia, thứ 2, tuần sau, ngày 5 tháng 7, 5/7, 24h nữa, sau X giờ, etc.) =====

    internal fun parseDateFromText(rawText: String, now: Calendar = Calendar.getInstance()): Pair<Calendar?, Boolean>? {
        // Returns: Pair(Calendar object, shouldRepeat) or null
        val lowerText = rawText.lowercase()
        val today = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        parseRelativeDelayToCalendar(rawText, now)?.let { relativeTime ->
            return Pair(relativeTime, false)
        }

        // Kiểm tra "24h nữa", "sau 24h", "trong 1 ngày"
        val hoursPattern = Regex("""(?:sau|trong|giờ nữa|h nữa|sau\s+)?(\d+)\s*(?:h|giờ|hour)""")
        val hoursMatch = hoursPattern.find(lowerText)
        if (hoursMatch != null) {
            try {
                val hours = hoursMatch.groupValues[1].toInt()
                if (hours >= 24) {
                    val daysToAdd = hours / 24
                    val result = (today.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_MONTH, daysToAdd)
                    }
                    return Pair(result, false)
                }
            } catch (e: Exception) {
                // Continue to check other patterns
            }
        }

        // Kiểm tra "sau X ngày", "trong X ngày"
        val daysPattern = Regex("""(?:sau|trong)\s+(\d+)\s*(?:ngày|day)""")
        val daysMatch = daysPattern.find(lowerText)
        if (daysMatch != null) {
            try {
                val days = daysMatch.groupValues[1].toInt()
                val result = (today.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_MONTH, days)
                }
                return Pair(result, false)
            } catch (e: Exception) {
                // Continue to check other patterns
            }
        }

        // Kiểm tra ngày/tháng cụ thể: "ngày 5 tháng 7", "5 tháng 7", "5/7", "5-7"
        val datePattern = Regex("""(?:ngày\s+)?(\d{1,2})(?:\s+tháng|\s+\/|-)?(\d{1,2})(?:\s+năm\s+(\d{4}))?""")
        val dateMatch = datePattern.find(lowerText)
        if (dateMatch != null) {
            try {
                val day = dateMatch.groupValues[1].toInt()
                val month = dateMatch.groupValues[2].toInt()
                val year = dateMatch.groupValues[3].takeIf { it.isNotEmpty() }?.toInt()
                    ?: today.get(Calendar.YEAR)

                val result = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (result.get(Calendar.DAY_OF_MONTH) == day) {
                    return Pair(result, false)
                }
            } catch (e: Exception) {
                // Invalid date, continue to check other patterns
            }
        }

        return when {
            lowerText.contains("mỗi ngày") || lowerText.contains("hằng ngày") ||
            lowerText.contains("every day") || lowerText.contains("daily") ->
                Pair(null, true)

            lowerText.contains("hôm nay") || lowerText.contains("today") ->
                Pair(today.clone() as Calendar, false)

            lowerText.contains("ngày mai") || lowerText.contains("tomorrow") ->
                Pair((today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }, false)

            lowerText.contains("ngày kia") ->
                Pair((today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 2) }, false)

            lowerText.contains("tuần sau") || lowerText.contains("next week") ->
                Pair((today.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, 1) }, false)

            else -> null
        }
    }

    // ===== HELPER: Tự động chọn hôm nay hoặc ngày mai dựa trên thời gian =====
    
    private fun getSmartScheduleDate(scheduledHour: Int, scheduledMinute: Int): Pair<String?, Boolean> {
        // Returns: Pair(scheduleDate in ISO format, shouldRepeat)
        // Nếu giờ muốn đặt > giờ hiện tại → hôm nay (không lặp)
        // Nếu giờ muốn đặt <= giờ hiện tại → ngày mai (không lặp)
        
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        
        val today = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val isTimeInFuture = (scheduledHour > currentHour) || 
                             (scheduledHour == currentHour && scheduledMinute > currentMinute)
        
        val targetDate = if (isTimeInFuture) {
            today  // Hôm nay
        } else {
            (today.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, 1)  // Ngày mai
            }
        }
        
        return Pair(formatDateToISO(targetDate), false)  // false = không lặp
    }
    
    // ===== HELPER: Format ngày thành ISO string =====
    
    private fun formatDateToISO(cal: Calendar): String {
        val year = cal.get(Calendar.YEAR)
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$year-$month-$day"
    }

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

    private suspend fun handleIntent(
        intentName: String,
        parameters: Map<String, Any>? = null,
        rawText: String = ""
    ): String {

        var roomError = ""
        val roomTargets = parseRoomTargets(parameters) { roomError = it }
        if (roomTargets == null) return roomError

        val singleRoomName = roomTargets.firstOrNull()?.name
        val singleRoomId = roomTargets.filterNotNull().map { it.id }.takeIf { it.size == 1 }?.firstOrNull()
        val singleRoomIds = roomTargets.filterNotNull().map { it.id }.ifEmpty { null }

        return when (intentName) {

            // ===== LIST SCHEDULES =====

            "list_schedules" -> {
                val schedules = getSchedules()
                val filtered = schedules.filter { scheduleMatch(it, null, singleRoomIds, null) }
                if (filtered.isEmpty()) {
                    if (singleRoomName != null) "Không có lịch nào trong phòng $singleRoomName." else "Không có lịch nào."
                } else {
                    val lines = filtered.mapIndexed { index, schedule ->
                        "${index + 1}. ${formatScheduleDescription(schedule)}"
                    }
                    "Danh sách lịch đã đặt:\n${lines.joinToString("\n")}" 
                }
            }

            // ===== CANCEL SCHEDULE =====

            "cancel_schedule" -> {
                val explicitTime = parseDialogflowTime(parameters)
                val relativeTime = parseRelativeDelayToCalendar(rawText)
                val filterTime = explicitTime ?: relativeTime
                val deviceTypes = parseDeviceTypesForCancel(parameters)

                val schedules = getSchedules()
                val matched = schedules.filter { scheduleMatch(it, deviceTypes, singleRoomIds, filterTime) }
                if (matched.isEmpty()) {
                    "Không tìm thấy lịch phù hợp để huỷ."
                } else {
                    matched.forEach { deleteSchedule(it) }
                    val count = matched.size
                    val suffix = when {
                        singleRoomName != null -> " trong phòng $singleRoomName"
                        filterTime != null -> " lúc ${filterTime.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')}:${filterTime.get(Calendar.MINUTE).toString().padStart(2, '0')}"
                        else -> ""
                    }
                    "Đã huỷ $count lịch$suffix."
                }
            }

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

            // ===== SCHEDULE DEVICE: "bật đèn lúc 9h", "tắt quạt phòng ngủ lúc 22h30", "bật lúc 11h49 và tắt lúc 11h50" =====

            "schedule_device" -> {
                val lowerText = rawText.lowercase()
                val hasTimeParam = parameters?.containsKey("time") == true
                
                // Nếu không có time parameter và không phải schedule request, thì chỉ bật/tắt ngay
                if (!hasTimeParam && !isScheduleIntent(lowerText, hasTimeParam)) {
                    val action = if (lowerText.contains("tắt")) "OFF" else "ON"
                    val deviceTypes = parseDeviceTypes(parameters)
                    val actionWord = if (action == "ON") "bật" else "tắt"
                    val suffix = if (singleRoomName != null) " ở $singleRoomName" else ""
                    
                    return forEachRoom(roomTargets) { roomId, roomName ->
                        deviceTypes
                            .map { controlDeviceMulti(it, action, roomId, roomName) }
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                    }.ifBlank { "Đã thực hiện xong." }
                }
                
                // Kiểm tra nếu có cấu trúc "bật lúc X và tắt lúc Y"
                val andSchedulePattern = Regex("""(bật|tắt|mở|đóng)\s+.*?lúc\s+(\d{1,2})(?::|h)\s*(\d{0,2})\s*(?:và|,)\s*(bật|tắt|mở|đóng)\s+.*?lúc\s+(\d{1,2})(?::|h)\s*(\d{0,2})""", RegexOption.IGNORE_CASE)
                val matchResult = andSchedulePattern.find(lowerText)
                
                if (matchResult != null) {
                    // Trường hợp: "bật lúc 11h49 và tắt lúc 11h50"
                    val deviceTypes = parseDeviceTypes(parameters)
                    val suffix = if (singleRoomName != null) " ở $singleRoomName" else ""
                    
                    try {
                        val actionWord1 = matchResult.groupValues[1].lowercase()
                        val hour1 = matchResult.groupValues[2].toInt()
                        val minute1 = matchResult.groupValues[3].takeIf { it.isNotEmpty() }?.toInt() ?: 0
                        val action1 = if (actionWord1 == "tắt" || actionWord1 == "đóng") "OFF" else "ON"
                        
                        val actionWord2 = matchResult.groupValues[4].lowercase()
                        val hour2 = matchResult.groupValues[5].toInt()
                        val minute2 = matchResult.groupValues[6].takeIf { it.isNotEmpty() }?.toInt() ?: 0
                        val action2 = if (actionWord2 == "tắt" || actionWord2 == "đóng") "OFF" else "ON"
                        
                        // Parse ngày (nếu có người dùng nói rõ)
                        val dateInfo = parseDateFromText(lowerText)
                        val (finalScheduleDate, finalShouldRepeat) = if (dateInfo != null) {
                            Pair(
                                dateInfo.first?.let { formatDateToISO(it) },
                                dateInfo.second
                            )
                        } else {
                            // Không nói ngày cụ thể → tự động chọn hôm nay hoặc ngày mai
                            getSmartScheduleDate(hour1, minute1)
                        }
                        
                        val dateDescription = when {
                            lowerText.contains("hôm nay") -> " hôm nay"
                            lowerText.contains("ngày mai") -> " ngày mai"
                            lowerText.contains("ngày kia") -> " ngày kia"
                            lowerText.contains("24h") || (lowerText.contains("sau 1") && lowerText.contains("ngày")) -> " ngày mai"
                            lowerText.contains("mỗi ngày") || lowerText.contains("hằng ngày") -> " hằng ngày"
                            finalScheduleDate != null && !finalShouldRepeat -> {
                                val today = Calendar.getInstance()
                                today.set(Calendar.HOUR_OF_DAY, 0)
                                today.set(Calendar.MINUTE, 0)
                                today.set(Calendar.SECOND, 0)
                                today.set(Calendar.MILLISECOND, 0)
                                
                                val target = Calendar.getInstance()
                                SimpleDateFormat("yyyy-MM-dd").parse(finalScheduleDate)?.let { target.time = it }
                                
                                when {
                                    target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) && 
                                    target.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> " hôm nay"
                                    target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) + 1 && 
                                    target.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> " ngày mai"
                                    target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) + 2 && 
                                    target.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> " ngày kia"
                                    else -> {
                                        val day = target.get(Calendar.DAY_OF_MONTH)
                                        val month = target.get(Calendar.MONTH) + 1
                                        " ngày $day tháng $month"
                                    }
                                }
                            }
                            else -> ""
                        }
                        
                        // Tạo 2 schedule riêng
                        val schedules = mutableListOf<Pair<Schedule, String>>()
                        deviceTypes.forEach { type ->
                            schedules.add(Schedule(
                                deviceType = type.toFirebase(),
                                roomId = singleRoomId,
                                action = action1,
                                hour = hour1,
                                minute = minute1,
                                repeatDaily = finalShouldRepeat,
                                scheduleDate = finalScheduleDate,
                                enabled = true
                            ) to "${if (action1 == "ON") "bật" else "tắt"} lúc $hour1:${minute1.toString().padStart(2, '0')}")
                            
                            schedules.add(Schedule(
                                deviceType = type.toFirebase(),
                                roomId = singleRoomId,
                                action = action2,
                                hour = hour2,
                                minute = minute2,
                                repeatDaily = finalShouldRepeat,
                                scheduleDate = finalScheduleDate,
                                enabled = true
                            ) to "${if (action2 == "ON") "bật" else "tắt"} lúc $hour2:${minute2.toString().padStart(2, '0')}")
                        }
                        
                        schedules.forEach { (schedule, _) ->
                            saveScheduleAndArm(schedule)
                        }
                        
                        val deviceList = deviceTypes.map { it.toVietnamese() }
                        val actionTexts = schedules.map { it.second }
                        return "Đã đặt lịch ${deviceList.joinToString(", ")}$suffix: ${actionTexts.joinToString(" và ")}$dateDescription."
                    } catch (e: Exception) {
                        return "Không hiểu lịch bạn muốn đặt. Vui lòng nói rõ, ví dụ \"bật lúc 9 giờ và tắt lúc 22 giờ mỗi ngày\"."
                    }
                }
                
                // Trường hợp thường: "bật đèn lúc 9h", "tắt quạt lúc 22h30", "bật đèn 30 phút nữa"
                val explicitTime = parseDialogflowTime(parameters)
                val relativeTime = parseRelativeDelayToCalendar(rawText)
                val cal = explicitTime ?: relativeTime
                    ?: return "Không hiểu giờ bạn muốn đặt lịch. Vui lòng nói rõ giờ, ví dụ \"lúc 9 giờ tối\"."

                val deviceTypes = parseDeviceTypes(parameters)
                val action = if (lowerText.contains("tắt")) "OFF" else "ON"
                val actionWord = if (action == "ON") "bật" else "tắt"

                val hourStr = cal.get(Calendar.HOUR_OF_DAY)
                val minuteStr = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
                val suffix = if (singleRoomName != null) " ở $singleRoomName" else ""

                // Parse ngày (nếu có)
                val dateInfo = parseDateFromText(lowerText, cal)
                val (finalScheduleDate, finalShouldRepeat) = when {
                    // Người dùng nói rõ ngày (hôm nay, ngày mai, ngày 5/7, etc.)
                    dateInfo != null -> Pair(
                        dateInfo.first?.let { formatDateToISO(it) },
                        dateInfo.second
                    )
                    // Là thời gian tương đối (30 phút nữa, 2 giờ nữa, etc.) → không lặp
                    relativeTime != null -> Pair(null, false)
                    // Là giờ cụ thể (lúc 9h) → tự động chọn hôm nay hoặc ngày mai
                    explicitTime != null -> getSmartScheduleDate(hourStr, cal.get(Calendar.MINUTE))
                    else -> Pair(null, true)  // Fallback: lặp mỗi ngày
                }
                
                val dateDescription = when {
                    lowerText.contains("hôm nay") -> " hôm nay"
                    lowerText.contains("ngày mai") -> " ngày mai"
                    lowerText.contains("ngày kia") -> " ngày kia"
                    lowerText.contains("24h") || (lowerText.contains("sau 1") && lowerText.contains("ngày")) -> " ngày mai"
                    relativeTime != null -> {
                        // Thời gian tương đối - tính toán mô tả
                        val now = Calendar.getInstance()
                        val tomorrow = (now.clone() as Calendar).apply {
                            add(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        
                        when {
                            cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) && 
                            cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> " hôm nay"
                            cal.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) && 
                            cal.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) -> " ngày mai"
                            else -> ""
                        }
                    }
                    finalScheduleDate != null && !finalShouldRepeat -> {
                        val today = Calendar.getInstance()
                        today.set(Calendar.HOUR_OF_DAY, 0)
                        today.set(Calendar.MINUTE, 0)
                        today.set(Calendar.SECOND, 0)
                        today.set(Calendar.MILLISECOND, 0)

                        val target = Calendar.getInstance()
                        SimpleDateFormat("yyyy-MM-dd").parse(finalScheduleDate)?.let { target.time = it }

                        when {
                            target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) && 
                            target.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> " hôm nay"
                            target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) + 1 && 
                            target.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> " ngày mai"
                            target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) + 2 && 
                            target.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> " ngày kia"
                            else -> {
                                val day = target.get(Calendar.DAY_OF_MONTH)
                                val month = target.get(Calendar.MONTH) + 1
                                " ngày $day tháng $month"
                            }
                        }
                    }
                    finalShouldRepeat -> " hằng ngày"
                    else -> ""
                }

                val results = deviceTypes.map { type ->
                    val schedule = Schedule(
                        deviceType = type.toFirebase(),
                        roomId = singleRoomId,
                        action = action,
                        hour = hourStr,
                        minute = cal.get(Calendar.MINUTE),
                        repeatDaily = finalShouldRepeat,
                        scheduleDate = finalScheduleDate,
                        enabled = true
                    )
                    saveScheduleAndArm(schedule)
                    type.toVietnamese()
                }

                "Đã đặt lịch $actionWord ${results.joinToString(", ")}$suffix lúc $hourStr:$minuteStr$dateDescription."
            }

            // ===== FOLLOW-UP: TẮT THIẾT BỊ VỪA BẬT =====

            "turn_off_last_device" -> {
                val (prevRoomId, prevRoomName) = resolveLastRoom()
                val suffix = if (prevRoomName != null) " ở $prevRoomName" else ""
                when (lastIntent) {
                    "turn_on_multi_devices" -> {
                        lastDeviceRoomClauses?.let { clauses ->
                            clauses
                                .map { controlDeviceMulti(it.type, "OFF", it.roomId, it.roomName) }
                                .filter { it.isNotBlank() }
                                .joinToString("\n")
                                .ifBlank { "Đã tắt thiết bị$suffix" }
                        } ?: run {
                            val deviceTypes = parseDeviceTypes(lastParameters)
                            deviceTypes
                                .map { controlDeviceMulti(it, "OFF", prevRoomId, prevRoomName) }
                                .filter { it.isNotBlank() }
                                .joinToString(", ")
                                .ifBlank { "Đã tắt thiết bị$suffix" }
                        }
                    }
                    "query_device_status" -> {
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
                        lastDeviceRoomClauses?.let { clauses ->
                            clauses
                                .map { controlDeviceMulti(it.type, "ON", it.roomId, it.roomName) }
                                .filter { it.isNotBlank() }
                                .joinToString("\n")
                                .ifBlank { "Đã bật thiết bị$suffix" }
                        } ?: run {
                            val deviceTypes = parseDeviceTypes(lastParameters)
                            deviceTypes
                                .map { controlDeviceMulti(it, "ON", prevRoomId, prevRoomName) }
                                .filter { it.isNotBlank() }
                                .joinToString(", ")
                                .ifBlank { "Đã bật thiết bị$suffix" }
                        }
                    }
                    "query_device_status" -> {
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