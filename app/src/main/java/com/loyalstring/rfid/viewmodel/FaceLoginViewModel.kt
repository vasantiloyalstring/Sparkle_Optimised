package com.loyalstring.rfid.viewmodel



import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.loyalstring.rfid.data.local.entity.FaceInfo
import com.loyalstring.rfid.repository.DropdownRepository
import com.loyalstring.rfid.repository.FaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FaceLoginViewModel @Inject constructor(
    private val  faceRepository: FaceRepository
) : ViewModel() {

    private val _matchedFace = MutableLiveData<FaceInfo?>()
    val matchedFace: LiveData<FaceInfo?> = _matchedFace

    fun fakeMatchFace() {
        _matchedFace.value = FaceInfo(
            id = 1,
            name = "Test User",
            employeeId = 10,
            username = "admin",
            clientCode = "LS000093",
            branchId = 1,
            embedding = ""
        )
    }
}