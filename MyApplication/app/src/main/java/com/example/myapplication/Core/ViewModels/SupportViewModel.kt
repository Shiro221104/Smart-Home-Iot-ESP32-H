package com.example.myapplication.Feature.Support

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.Core.Models.Device
import com.example.myapplication.Core.Models.DeviceType
import com.example.myapplication.Core.Models.Room
import com.example.myapplication.Core.repository.DeviceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SupportViewModel : ViewModel() {

    val messages = mutableStateListOf(
        ChatMessage(
            "Hello 👋\nHow can I help you today?",
            false
        )
    )

    private val deviceRepository = DeviceRepository()

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var deviceList: List<Device> = emptyList()
    private var roomList: List<Room> = emptyList()

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

        messages.add(
            ChatMessage(
                userText,
                true
            )
        )

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

                val intentName =
                    response.queryResult.intent
                        ?.displayName
                        .orEmpty()

                val dialogReply =
                    response.queryResult.fulfillmentText

                // Lấy parameters từ Dialogflow (room name)
                val parameters = response.queryResult.parameters

                val localReply = handleIntent(intentName, parameters)

                val finalReply = localReply.ifEmpty { dialogReply }

                withContext(Dispatchers.Main) {
                    messages.add(
                        ChatMessage(
                            finalReply,
                            false
                        )
                    )
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {
                    messages.add(
                        ChatMessage(
                            "Error: ${e.message}",
                            false
                        )
                    )
                }
            }
        }
    }

    private fun handleIntent(
        intentName: String,
        parameters: Map<String, Any>? = null
    ): String {


        val roomName = parameters?.get("room")
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotBlank() }


        val roomId = roomName?.let { name ->
            roomList.find { room ->
                room.name.equals(name, ignoreCase = true)
            }?.id
        }

        return when (intentName) {

            // ===== LIGHT =====

            "turn_on_light" ->
                controlDevice(DeviceType.LIGHT, "ON", roomId, roomName)

            "turn_off_light" ->
                controlDevice(DeviceType.LIGHT, "OFF", roomId, roomName)

            // ===== FAN =====

            "turn_on_fan" ->
                controlDevice(DeviceType.FAN, "ON", roomId, roomName)

            "turn_off_fan" ->
                controlDevice(DeviceType.FAN, "OFF", roomId, roomName)

            // ===== TURN ON ALL LIGHTS =====

            "turn_on_all_lights" -> {

                val lights = deviceList.filter {
                    it.type == DeviceType.LIGHT.toFirebase() &&
                            (roomId == null || it.room == roomId)
                }

                var changedCount = 0
                lights.forEach { device ->
                    if (device.status != "ON") {
                        deviceRepository.updateDeviceStatus(device.id, "ON")
                        changedCount++
                    }
                }

                val suffix = if (roomName != null) " ở $roomName" else ""
                when {
                    lights.isEmpty() -> "Không tìm thấy đèn$suffix"
                    changedCount == 0 -> "Tất cả đèn$suffix hiện đang bật"
                    else -> "Đã bật tất cả đèn$suffix"
                }
            }

            // ===== TURN OFF ALL LIGHTS =====

            "turn_off_all_light" -> {

                val lights = deviceList.filter {
                    it.type == DeviceType.LIGHT.toFirebase() &&
                            (roomId == null || it.room == roomId)
                }

                var changedCount = 0
                lights.forEach { device ->
                    if (device.status != "OFF") {
                        deviceRepository.updateDeviceStatus(device.id, "OFF")
                        changedCount++
                    }
                }

                val suffix = if (roomName != null) " ở $roomName" else ""
                when {
                    lights.isEmpty() -> "Không tìm thấy đèn$suffix"
                    changedCount == 0 -> "Tất cả đèn$suffix hiện đang tắt"
                    else -> "Đã tắt tất cả đèn$suffix"
                }
            }

            // ===== TURN ON ALL DEVICES (EXCEPT DOOR) =====

            "turn_on_all_devices" -> {

                val devices = deviceList.filter {
                    it.type != "door" &&
                            (roomId == null || it.room == roomId)
                }

                var changedCount = 0
                devices.forEach { device ->
                    if (device.status != "ON") {
                        deviceRepository.updateDeviceStatus(device.id, "ON")
                        changedCount++
                    }
                }

                val suffix = if (roomName != null) " ở $roomName" else ""
                when {
                    devices.isEmpty() -> "Không có thiết bị nào$suffix"
                    changedCount == 0 -> "Tất cả thiết bị$suffix hiện đang bật"
                    else -> "Đã bật tất cả thiết bị$suffix"
                }
            }

            // ===== TURN OFF ALL DEVICES =====

            "turn_off_all_devices" -> {

                val devices = deviceList.filter {
                    it.type != "door" &&
                            (roomId == null || it.room == roomId)
                }

                var changedCount = 0
                devices.forEach { device ->
                    if (device.status != "OFF") {
                        deviceRepository.updateDeviceStatus(device.id, "OFF")
                        changedCount++
                    }
                }

                val suffix = if (roomName != null) " ở $roomName" else ""
                when {
                    devices.isEmpty() -> "Không có thiết bị nào$suffix"
                    changedCount == 0 -> "Tất cả thiết bị$suffix hiện đang tắt"
                    else -> "Đã tắt tất cả thiết bị$suffix"
                }
            }

            else -> ""
        }
    }

    private fun controlDevice(
        type: DeviceType,
        status: String,
        roomId: String?,
        roomName: String?
    ): String {

        val device = if (roomId != null) {
            deviceList.find {
                it.type == type.toFirebase() && it.room == roomId
            }
        } else {
            deviceList.find {
                it.type == type.toFirebase()
            }
        }

        return if (device != null) {
            if (device.status == status) {
                if (status == "ON") "${device.name} hiện đang bật"
                else "${device.name} hiện đang tắt"
            } else {
                deviceRepository.updateDeviceStatus(device.id, status)
                if (status == "ON") "Đã bật ${device.name}"
                else "Đã tắt ${device.name}"
            }
        } else {
            val suffix = if (roomName != null) " ở $roomName" else ""
            "${type.displayName} không tìm thấy$suffix"
        }
    }
}