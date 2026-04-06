package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.local.dao.FaceDao
import com.loyalstring.rfid.data.local.entity.FaceInfo
import com.loyalstring.rfid.data.model.face.AllFaceResponse
import com.loyalstring.rfid.data.model.face.FaceRequest
import com.loyalstring.rfid.data.model.face.FaceResponse
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.data.remote.resource.Resource
import javax.inject.Inject

class FaceRepositoryImpl @Inject constructor(
    //private val faceDao: FaceDao,
    private val faceApiService: RetrofitInterface
) : FaceRepository {

    /*override suspend fun getAllFaces(): List<FaceInfo> {
        return faceDao.getAllFaces()
    }

    override suspend fun insertFace(faceInfo: FaceInfo) {
        faceDao.insertFace(faceInfo)
    }*/

    override suspend fun getAllFaceData(): Resource<AllFaceResponse> {
        return try {
            val request = FaceRequest(
                clientCode = ""
            )
            val response = faceApiService.getAllFaceData(request)

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(
                    response.message().ifBlank { "Failed to fetch face data" }
                )
            }
        } catch (e: Exception) {
            Resource.Error(
                e.message ?: "Something went wrong while fetching face data"
            )
        }
    }


    override suspend fun saveFaceToApi(request: FaceInfo): Resource<FaceResponse> {
        return try {
            val response = faceApiService.saveFace(request)

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(
                    response.message().takeIf { it.isNotBlank() }
                        ?: "Failed to save face"
                )
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Something went wrong")
        }
    }

}