package com.loyalstring.rfid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.repository.SampleOutRepositoty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SampleOutViewModel @Inject constructor(
    private val repository: SampleOutRepositoty
) : ViewModel() {

    // Sirf list expose kar rahe hain
    private val _sampleOutList = MutableStateFlow<List<SampleOutListResponse>>(emptyList())
    val sampleOutList: StateFlow<List<SampleOutListResponse>> = _sampleOutList.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()


    fun loadSampleOut(
        clientCode: String,
        sampleStatus: String
    ) {
        viewModelScope.launch {

            _loading.value = true   // START LOADING

            try {
                val request = SampleOutListRequest(
                    ClientCode = clientCode,
                    SampleStatus = sampleStatus
                )

                val result = repository.getAllSampleOut(request)

                result.onSuccess { list ->
                    _sampleOutList.value = list
                    _error.value = null
                }

                result.onFailure { e ->
                    _sampleOutList.value = emptyList()
                    _error.value = e.message ?: "Something went wrong"
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "Unexpected error"
                _sampleOutList.value = emptyList()

            } finally {
                _loading.value = false  // ALWAYS STOP LOADING
            }
        }
    }

}
