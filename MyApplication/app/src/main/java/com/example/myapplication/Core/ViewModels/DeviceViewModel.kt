package com.example.myapplication.Core.ViewModels

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf
import  com.example.myapplication.Core.Models.Device
import  com.example.myapplication.Core.repository.DeviceRepository
class DeviceViewModel : ViewModel() {

    private val repository = DeviceRepository()

    // State cho UI
    var devices = mutableStateListOf<Device>()
        private set

    // Load dữ liệu từ Firebase
    fun loadDevices() {
        repository.getDevices { list ->
            devices.clear()
            devices.addAll(list)
        }
    }

    // Thêm thiết bị
    fun addDevice(device: Device) {
        repository.addDevice(device)
    }

    // Update trạng thái
    fun updateStatus(deviceId: String, status: String) {
        repository.updateDeviceStatus(deviceId, status)
    }
}