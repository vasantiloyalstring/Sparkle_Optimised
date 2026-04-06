package com.loyalstring.rfid.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.loyalstring.rfid.data.model.login.Employee

@Entity(tableName = "face_info")
data class FaceInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val name: String,
    val UserId : Int?=null,
    val employeeId: Int? = null,

    // this is what Room will save
    val employeeJson: String? = null,

    val username: String? = null,
    val clientCode: String? = null,
    val branchId: Int? = null,
    val userType: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val faceWidth: Int = 0,
    val faceHeight: Int = 0,
    val top: Int = 0,
    val left: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    val smilingProbability: Float = 0f,
    val leftEyeOpenProbability: Float = 0f,
    val rightEyeOpenProbability: Float = 0f,
    val timestamp: String = "",
    val time: Long = System.currentTimeMillis(),
    val embedding: String? = null
) {
    @Ignore
    var employee: Employee? = employeeJson?.let {
        try {
            Gson().fromJson(it, Employee::class.java)
        } catch (e: Exception) {
            null
        }
    }
}