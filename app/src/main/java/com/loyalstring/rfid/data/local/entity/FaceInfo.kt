package com.loyalstring.rfid.data.local.entity

import androidx.room.PrimaryKey

data class FaceInfo(   @PrimaryKey(autoGenerate = true)
                       val id: Int? = null,
                       val name: String,
                       val employeeId: Int? = null,
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
    )
