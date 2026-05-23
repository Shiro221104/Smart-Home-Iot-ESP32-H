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
        ChatMessage("Hello 👋\nHow can I help you today?", false)
    )

    private val deviceRepository = DeviceRepository()
    private var deviceList: List<Device> = emptyList()

    init {
        deviceRepository.getDevices { devices ->
            deviceList = devices
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

                val intentName  = response.queryResult.intent?.displayName.orEmpty()
                val fulfillText = response.queryResult.fulfillmentText
                val actionReply = handleIntent(intentName)
                val finalReply  = actionReply.ifEmpty { fulfillText }

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

    private fun handleIntent(intentName: String): String {
        return when (intentName) {
            "turn_on_light"      -> controlDevice(DeviceType.LIGHT, "ON")
            "turn_off_light"     -> controlDevice(DeviceType.LIGHT, "OFF")
            "turn_on_fan"        -> controlDevice(DeviceType.FAN, "ON")
            "turn_off_fan"       -> controlDevice(DeviceType.FAN, "OFF")
            "turn_on_all_lights" -> {
                val lights = deviceList.filter { it.type == DeviceType.LIGHT.toFirebase() }
                lights.forEach { deviceRepository.updateDeviceStatus(it.id, "ON") }
                if (lights.isNotEmpty()) "Turned on all ${lights.size} lights!" else "No lights found"
            }
            else -> ""
        }
    }

    private fun controlDevice(type: DeviceType, status: String): String {
        val device = deviceList.find { it.type == type.toFirebase() }
        return if (device != null) {
            deviceRepository.updateDeviceStatus(device.id, status)
            val action = if (status == "ON") "on" else "off"
            "Turned $action ${type.displayName}: ${device.name}"
        } else {
            "${type.displayName} not found"
        }
    }
}