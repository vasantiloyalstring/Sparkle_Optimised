package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.local.entity.FaceInfo
import javax.inject.Inject

class FaceRepositoryImpl @Inject constructor(
    private val faceDao: FaceDao
) : FaceRepository {

    override suspend fun getAllFaces(): List<FaceInfo> {
        return faceDao.getAllFaces()
    }
}