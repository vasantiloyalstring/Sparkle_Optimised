package com.loyalstring.rfid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.loyalstring.rfid.data.local.entity.FaceInfo

@Dao
interface FaceDao {

    @Query("SELECT * FROM face_info")
    suspend fun getAllFaces(): List<FaceInfo>

    @Insert
    suspend fun insertFace(faceInfo: FaceInfo)
}