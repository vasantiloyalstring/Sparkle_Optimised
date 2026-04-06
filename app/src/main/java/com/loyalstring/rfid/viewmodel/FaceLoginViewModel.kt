package com.loyalstring.rfid.viewmodel

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.FaceInfo
import com.loyalstring.rfid.data.model.face.AllFaceResponse
import com.loyalstring.rfid.data.model.face.FaceData
import com.loyalstring.rfid.data.model.face.FaceResponse
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.repository.FaceRepository
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.worker.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class FaceLoginViewModel @Inject constructor(
    private val faceRepository: FaceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
 
    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    private val _matchedFace = MutableLiveData<FaceData?>()
    val matchedFace: LiveData<FaceData?> = _matchedFace

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveFaceResponse = MutableLiveData<Resource<FaceResponse>>()
    val saveFaceResponse: LiveData<Resource<FaceResponse>> = _saveFaceResponse

    private val _allFaceData = MutableLiveData<Resource<AllFaceResponse>>()
    val allFaceData: LiveData<Resource<AllFaceResponse>> = _allFaceData

   /* fun saveFace(faceInfo: FaceInfo) {
        viewModelScope.launch {
            try {
                faceRepository.insertFace(faceInfo)
                _message.value =localizedContext.getString(R.string.face_saved_successfully)
            } catch (e: Exception) {
                _message.value = e.message ?: localizedContext.getString(R.string.failed_to_save_face)
            }
        }
    }*/

   /* fun recogniseFace(inputEmbedding: FloatArray) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val savedFaces = faceRepository.getAllFaces()

                if (savedFaces.isEmpty()) {
                    _message.value =localizedContext.getString(R.string.no_saved_face_data_found)
                    _matchedFace.value = null
                    return@launch
                }

                var bestMatch: FaceInfo? = null
                var bestDistance = Float.MAX_VALUE

                for (face in savedFaces) {
                    val savedEmbedding = convertStringToFloatArray(face.embedding)

                    if (savedEmbedding.isEmpty() || savedEmbedding.size != inputEmbedding.size) {
                        continue
                    }

                    val distance = calculateEuclideanDistance(inputEmbedding, savedEmbedding)

                    if (distance < bestDistance) {
                        bestDistance = distance
                        bestMatch = face
                    }
                }

                val threshold = 1.0f

                if (bestMatch != null && bestDistance < threshold) {
                    _matchedFace.value = bestMatch
                    _message.value = localizedContext.getString(R.string.face_matched_successfully)
                } else {
                    _matchedFace.value = null
                    _message.value = localizedContext.getString(R.string.face_not_recognised)
                }

            } catch (e: Exception) {
                _matchedFace.value = null
                _message.value = e.message ?: localizedContext.getString(R.string.face_recognition_failed)
            } finally {
                _isLoading.value = false
            }
        }
    }*/
 /*  fun recogniseFace(inputEmbedding: FloatArray) {
       viewModelScope.launch {
           _isLoading.value = true
           try {
               when (val result = faceRepository.getAllFaceData()) {

                   is Resource.Success -> {
                       val savedFaces = result.data?.Data ?: emptyList()

                       if (savedFaces.isEmpty()) {
                           _message.value = localizedContext.getString(R.string.no_saved_face_data_found)
                           _matchedFace.value = null
                           return@launch
                       }

                       var bestMatch: FaceData? = null
                       var bestDistance = Float.MAX_VALUE

                       for (face in savedFaces) {
                           val savedEmbedding = convertStringToFloatArray(face.Embedding)

                           if (savedEmbedding.isEmpty() || savedEmbedding.size != inputEmbedding.size) {
                               continue
                           }

                           val distance = calculateEuclideanDistance(inputEmbedding, savedEmbedding)

                           if (distance < bestDistance) {
                               bestDistance = distance
                               bestMatch = face
                           }
                       }

                       val threshold = 1.0f

                       if (bestMatch != null && bestDistance < threshold) {
                           _matchedFace.value = bestMatch
                           _message.value = localizedContext.getString(R.string.face_matched_successfully)
                       } else {
                           _matchedFace.value = null
                           _message.value = localizedContext.getString(R.string.face_not_recognised)
                       }
                   }

                   is Resource.Error -> {
                       _matchedFace.value = null
                       _message.value = result.message
                           ?: localizedContext.getString(R.string.face_recognition_failed)
                   }

                   is Resource.Loading -> {
                   }
               }

           } catch (e: Exception) {
               _matchedFace.value = null
               _message.value = e.message ?: localizedContext.getString(R.string.face_recognition_failed)
           } finally {
               _isLoading.value = false
           }
       }
   }*/
    fun clearMatchedFace() {
        _matchedFace.value = null
    }

    private fun calculateEuclideanDistance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    private fun convertStringToFloatArray(embedding: String?): FloatArray {
        if (embedding.isNullOrBlank()) return floatArrayOf()

        return embedding
            .split(",")
            .mapNotNull { it.trim().toFloatOrNull() }
            .toFloatArray()
    }

    fun saveFaceToApi(request: FaceInfo) {
        viewModelScope.launch {
            _saveFaceResponse.value = Resource.Loading()
            _saveFaceResponse.value = faceRepository.saveFaceToApi(request)
        }
    }

    fun getAllFaceData(inputEmbedding: FloatArray) {
        viewModelScope.launch {
            _allFaceData.value = Resource.Loading()
            _isLoading.value = true

            try {
                val result = faceRepository.getAllFaceData()
                _allFaceData.value = result

                when (result) {
                    is Resource.Success -> {
                        val savedFaces = result.data?.Data ?: emptyList()

                        if (savedFaces.isEmpty()) {
                            _matchedFace.value = null
                            _message.value = localizedContext.getString(R.string.no_saved_face_data_found)
                            return@launch
                        }

                        var bestMatch: FaceData? = null
                        var bestDistance = Float.MAX_VALUE

                        for (face in savedFaces) {
                            val savedEmbedding = convertStringToFloatArray(face.Embedding)

                            if (savedEmbedding.isEmpty() || savedEmbedding.size != inputEmbedding.size) {
                                continue
                            }

                            val distance = calculateEuclideanDistance(inputEmbedding, savedEmbedding)

                            if (distance < bestDistance) {
                                bestDistance = distance
                                bestMatch = face
                            }
                        }

                        val threshold = 1.0f

                        if (bestMatch != null && bestDistance < threshold) {
                            _matchedFace.value = bestMatch
                            _message.value = localizedContext.getString(R.string.face_matched_successfully)
                        } else {
                            _matchedFace.value = null
                            _message.value = localizedContext.getString(R.string.face_not_recognised)
                        }
                    }

                    is Resource.Error -> {
                        _matchedFace.value = null
                        _message.value = result.message ?:localizedContext.getString(R.string.failed_to_save_face)
                    }

                    is Resource.Loading -> {

                }
            }} catch (e: Exception) {
                _matchedFace.value = null
                _message.value = e.message ?: localizedContext.getString(R.string.face_not_recognised)
                _allFaceData.value = Resource.Error(e.message ?: localizedContext.getString(R.string.failed_to_fetch_face_data))
            } finally {
                _isLoading.value = false
            }
        }
    }
}