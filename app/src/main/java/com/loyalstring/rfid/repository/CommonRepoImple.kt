package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.stockVerification.ScanSessionResponse
import com.loyalstring.rfid.data.model.stockVerification.StockVerificationRequestData
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class CommonRepoImple ( private val api: RetrofitInterface
) : CommonRepository {

    override suspend fun stockVarificationNew(
        request: StockVerificationRequestData
    ): Result<ScanSessionResponse> {
        return try {
            val response = api.stockVarificationNew(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errMsg = response.errorBody()?.string()
                Result.failure(
                    Exception(
                        errMsg?.takeIf { it.isNotBlank() }
                            ?: "API error: ${response.code()} - ${response.message()}"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadStock(request: RequestBody): Result<ScanSessionResponse> {
        return try {
            val response = api.uploadStock(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errMsg = response.errorBody()?.string()
                Result.failure(
                    Exception(
                        errMsg?.takeIf { it.isNotBlank() }
                            ?: "API error: ${response.code()} - ${response.message()}"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadStockFile(
        clientCode: String,
        file: File
    ): Result<ScanSessionResponse> {

        val clientCodePart =
            clientCode.toRequestBody("text/plain".toMediaType())

        val fileRequestBody =
            file.asRequestBody("application/json".toMediaType())

        val filePart =
            MultipartBody.Part.createFormData(
                name = "jsonFile",   // MUST match backend
                filename = file.name,
                body = fileRequestBody
            )

        return runCatching {
            api.uploadStockVerificationFile(
                clientCode = clientCodePart,
                jsonFile = filePart
            )
        }
    }

}