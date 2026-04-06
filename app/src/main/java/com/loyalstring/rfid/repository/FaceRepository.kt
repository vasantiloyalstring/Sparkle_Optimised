package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.local.entity.FaceInfo
import com.loyalstring.rfid.data.model.face.AllFaceResponse
import com.loyalstring.rfid.data.model.face.FaceData
import com.loyalstring.rfid.data.model.face.FaceResponse
import com.loyalstring.rfid.data.remote.resource.Resource

interface FaceRepository {

    //suspend fun getAllFaces(): List<FaceData>
    //suspend fun insertFace(faceInfo: FaceInfo)

    suspend fun saveFaceToApi(request: FaceInfo): Resource<FaceResponse>

    suspend fun getAllFaceData(): Resource<AllFaceResponse>
}