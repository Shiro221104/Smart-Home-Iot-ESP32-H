package com.example.myapplication.Core.ViewModels

import androidx.lifecycle.ViewModel
import com.example.myapplication.Core.Models.Room
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RoomViewModel : ViewModel() {

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    private val dbRef = FirebaseDatabase.getInstance().getReference("rooms")
    private var listener: ValueEventListener? = null

    init {
        listenRooms()
    }

    private fun listenRooms() {
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    child.getValue(Room::class.java)
                        ?.copy(id = child.key ?: return@mapNotNull null)
                }
                _rooms.value = list
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        dbRef.addValueEventListener(listener!!)
    }

    fun deleteRoom(roomId: String) {
        dbRef.child(roomId).removeValue()
    }

    override fun onCleared() {
        super.onCleared()
        listener?.let { dbRef.removeEventListener(it) }
    }
}