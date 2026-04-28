package com.example.myapplication.Core.ViewModels

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import com.example.myapplication.Core.Models.Sensor
import com.example.myapplication.Core.repository.SensorRepository

class SensorViewModel : ViewModel() {

    private val repository = SensorRepository()

    var sensor = mutableStateOf(Sensor())
        private set

    init {
        loadSensor() // 🔥 auto load khi tạo ViewModel
    }

    private fun loadSensor() {
        repository.listenSensorData { data ->
            data?.let {
                sensor.value = it
            }
        }
    }
}