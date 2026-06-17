package com.example.myapplication.Feature.Support

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.Core.Models.Device
import com.example.myapplication.Core.Models.DeviceType
import com.example.myapplication.Core.repository.DeviceRepository
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

    private var deviceList: List<Device> = emptyList()

    init {

        deviceRepository.getDevices { devices ->

            deviceList = devices
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

                // tên intent từ dialogflow
                val intentName =
                    response.queryResult.intent
                        ?.displayName
                        .orEmpty()

                // text trả lời từ dialogflow
                val dialogReply =
                    response.queryResult.fulfillmentText

                // xử lý local
                val localReply =
                    handleIntent(intentName)

                // nếu localReply rỗng -> dùng dialogflow
                val finalReply =
                    localReply.ifEmpty {
                        dialogReply
                    }

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
        intentName: String
    ): String {

        return when (intentName) {

            // ===== LIGHT =====

            "turn_on_light" ->

                controlDevice(
                    DeviceType.LIGHT,
                    "ON"
                )

            "turn_off_light" ->

                controlDevice(
                    DeviceType.LIGHT,
                    "OFF"
                )

            // ===== FAN =====

            "turn_on_fan" ->

                controlDevice(
                    DeviceType.FAN,
                    "ON"
                )

            "turn_off_fan" ->

                controlDevice(
                    DeviceType.FAN,
                    "OFF"
                )

            // ===== TURN ON ALL LIGHTS =====

            "turn_on_all_lights" -> {

                val lights = deviceList.filter {

                    it.type ==
                            DeviceType.LIGHT.toFirebase()
                }

                var changedCount = 0

                lights.forEach { device ->

                    if (device.status != "ON") {

                        deviceRepository.updateDeviceStatus(
                            device.id,
                            "ON"
                        )

                        changedCount++
                    }
                }

                when {

                    lights.isEmpty() -> {

                        "Không tìm thấy đèn"
                    }

                    changedCount == 0 -> {

                        "Tất cả đèn hiện đang bật"
                    }

                    else -> {

                        "Đã bật tất cả đèn"
                    }
                }
            }

            // ===== TURN OFF ALL LIGHTS =====

            "turn_off_all_light" -> {

                val lights = deviceList.filter {

                    it.type ==
                            DeviceType.LIGHT.toFirebase()
                }

                var changedCount = 0

                lights.forEach { device ->

                    if (device.status != "OFF") {

                        deviceRepository.updateDeviceStatus(
                            device.id,
                            "OFF"
                        )

                        changedCount++
                    }
                }

                when {

                    lights.isEmpty() -> {

                        "Không tìm thấy đèn"
                    }

                    changedCount == 0 -> {

                        "Tất cả đèn hiện đang tắt"
                    }

                    else -> {

                        "Đã tắt tất cả đèn"
                    }
                }
            }

            // ===== TURN ON ALL DEVICES (EXCEPT DOOR) =====

            "turn_on_all_devices" -> {

                val devices = deviceList.filter {

                    it.type != "door"
                }

                var changedCount = 0

                devices.forEach { device ->

                    if (device.status != "ON") {

                        deviceRepository.updateDeviceStatus(
                            device.id,
                            "ON"
                        )

                        changedCount++
                    }
                }

                when {

                    devices.isEmpty() -> {

                        "Không có thiết bị nào"
                    }

                    changedCount == 0 -> {

                        "Tất cả thiết bị hiện đang bật"
                    }

                    else -> {

                        "Đã bật tất cả thiết bị"
                    }
                }
            }

            // ===== TURN OFF ALL DEVICES =====

            "turn_off_all_devices" -> {

                val devices = deviceList.filter {

                    it.type != "door"
                }

                var changedCount = 0

                devices.forEach { device ->

                    if (device.status != "OFF") {

                        deviceRepository.updateDeviceStatus(
                            device.id,
                            "OFF"
                        )

                        changedCount++
                    }
                }

                when {

                    devices.isEmpty() -> {

                        "Không có thiết bị nào"
                    }

                    changedCount == 0 -> {

                        "Tất cả thiết bị hiện đang tắt"
                    }

                    else -> {

                        "Đã tắt tất cả thiết bị"
                    }
                }
            }

            else -> ""
        }
    }

    private fun controlDevice(
        type: DeviceType,
        status: String
    ): String {

        val device = deviceList.find {

            it.type == type.toFirebase()
        }

        return if (device != null) {

            // đã đúng trạng thái
            if (device.status == status) {

                if (status == "ON") {

                    "${device.name} hiện đang bật"

                } else {

                    "${device.name} hiện đang tắt"
                }

            } else {

                deviceRepository.updateDeviceStatus(
                    device.id,
                    status
                )

                if (status == "ON") {

                    "Đã bật ${device.name}"

                } else {

                    "Đã tắt ${device.name}"
                }
            }

        } else {

            "${type.displayName} không tìm thấy"
        }
    }
}