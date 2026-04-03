package com.loyalstring.rfid.viewmodel

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.FaceInfo
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

    private val _matchedFace = MutableLiveData<FaceInfo?>()
    val matchedFace: LiveData<FaceInfo?> = _matchedFace

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun saveFace(faceInfo: FaceInfo) {
        viewModelScope.launch {
            try {
                faceRepository.insertFace(faceInfo)
                _message.value =localizedContext.getString(R.string.face_saved_successfully)
            } catch (e: Exception) {
                _message.value = e.message ?: localizedContext.getString(R.string.failed_to_save_face)
            }
        }
    }

    fun recogniseFace(inputEmbedding: FloatArray) {
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
    }

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
}