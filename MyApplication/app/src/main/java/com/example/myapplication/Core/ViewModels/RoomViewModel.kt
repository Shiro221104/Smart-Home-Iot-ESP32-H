package com.example.myapplication.Core.ViewModels

import androidx.lifecycle.ViewModel
import com.example.myapplication.Core.Models.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RoomViewModel : ViewModel() {

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listener: ValueEventListener? = null

    init {
        listenRooms()
    }

    private fun listenRooms() {
        val userId = auth.currentUser?.uid ?: return
        
        val ref = database.getReference("users/$userId/rooms")
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val list = mutableListOf<Room>()
                for (roomSnapshot in snapshot.children) {
                    val room = roomSnapshot.getValue(Room::class.java)
                    if (room != null) {
                        room.id = roomSnapshot.key ?: ""
                        list.add(room)
                    }
                }
                _rooms.value = list
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                _rooms.value = emptyList()
            }
        }
        ref.addValueEventListener(listener!!)
    }

    fun deleteRoom(roomId: String) {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users/$userId/rooms").child(roomId).removeValue()
    }

    // Thêm room mới
    fun addRoom(room: Room) {
        val userId = auth.currentUser?.uid ?: return
        
        val newRoom = room.copy(userId = userId)
        database.getReference("users/$userId/rooms").push().setValue(newRoom)
    }

    override fun onCleared() {
        super.onCleared()
        if (listener != null) {
            val userId = auth.currentUser?.uid ?: return
            database.getReference("users/$userId/rooms").removeEventListener(listener!!)
        }
    }
}