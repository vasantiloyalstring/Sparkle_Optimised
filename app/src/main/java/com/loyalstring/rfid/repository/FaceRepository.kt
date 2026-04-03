package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.local.entity.FaceInfo

interface FaceRepository {

    suspend fun getAllFaces(): List<FaceInfo>
    suspend fun insertFace(faceInfo: FaceInfo)
}