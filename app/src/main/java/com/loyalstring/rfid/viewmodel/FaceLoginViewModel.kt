package com.loyalstring.rfid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.local.entity.FaceInfo
import com.loyalstring.rfid.repository.FaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class FaceLoginViewModel @Inject constructor(
    private val faceRepository: FaceRepository
) : ViewModel() {

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
                _message.value = "Face saved successfully"
            } catch (e: Exception) {
                _message.value = e.message ?: "Failed to save face"
            }
        }
    }

    fun recogniseFace(inputEmbedding: FloatArray) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val savedFaces = faceRepository.getAllFaces()

                if (savedFaces.isEmpty()) {
                    _message.value = "No saved face data found"
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
                    _message.value = "Face matched successfully"
                } else {
                    _matchedFace.value = null
                    _message.value = "Face not recognised"
                }

            } catch (e: Exception) {
                _matchedFace.value = null
                _message.value = e.message ?: "Face recognition failed"
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