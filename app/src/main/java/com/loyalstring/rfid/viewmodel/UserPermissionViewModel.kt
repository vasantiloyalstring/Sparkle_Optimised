package com.loyalstring.rfid.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.loyalstring.rfid.data.local.entity.UserPermissionEntity
import com.loyalstring.rfid.data.model.stockVerification.AccessibleCompany
import com.loyalstring.rfid.data.remote.data.UserPermissionResponse
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.repository.UserPermissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserPermissionViewModel @Inject constructor(
    private val repository: UserPermissionRepository
) : ViewModel() {

    private val _permissionResponse = MutableLiveData<Resource<Unit>>()
    val permissionResponse: LiveData<Resource<Unit>> = _permissionResponse

    private val _permissionResponseAll = MutableLiveData<Resource<Unit>>()
    val permissionResponseAll: LiveData<Resource<Unit>> = _permissionResponseAll

    private val _allEmployees = MutableLiveData<List<UserPermissionEntity>>()
    val allEmployees: LiveData<List<UserPermissionEntity>> = _allEmployees

    fun loadPermissions(clientCode: String, userId: Int) {
        viewModelScope.launch {
            _permissionResponse.value = Resource.Loading()
            _permissionResponse.value = repository.fetchAndSavePermissions(clientCode, userId)
        }
    }
/*
    fun loadPermissionsAll(clientCode: String) {
        viewModelScope.launch {
            _permissionResponseAll.value = Resource.Loading()
            _permissionResponseAll.value = repository.fetchAndSavePermissionsAll(clientCode)
        }
    }*/
fun loadPermissionsAll(clientCode: String) {
    viewModelScope.launch {
        Log.d("PERMISSION_VM", "loadPermissionsAll called, clientCode = $clientCode")

        val result = repository.fetchAndSavePermissionsAll(clientCode)

        when (result) {
            is Resource.Success -> {
                Log.d("PERMISSION_VM", "Success")
                Log.d("PERMISSION_VM", "result.data size = ${result.data?.size ?: 0}")
                Log.d("PERMISSION_VM", "result.data = ${result.data}")

                _allEmployees.value = result.data ?: emptyList()

                Log.d("PERMISSION_VM", "_allEmployees size = ${_allEmployees.value?.size ?: 0}")
            }

            is Resource.Error -> {
                Log.d("PERMISSION_VM", "Error = ${result.message}")
                _allEmployees.value = emptyList()
            }

            is Resource.Loading -> {
                Log.d("PERMISSION_VM", "Loading")
            }
        }
    }
}

    suspend fun getAccessibleBranches(): List<String> {
        val userPerm = repository.getUserPermission()
        val json = userPerm?.branchSelectionJson ?: return emptyList()

        return try {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val list: List<Map<String, Any>> = Gson().fromJson(json, type)
            list.mapNotNull { it["Name"]?.toString()?.trim() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAccessibleCompany(): List<UserPermissionEntity> {

        val userPerm = repository.getUserPermission() ?: return emptyList()

        return listOf(userPerm)
    }

/*    suspend fun getAccessibleCompany(): List<UserPermissionEntity> {

        val userPerm = repository.getUserPermission()
        val json = userPerm?.companySelectionJson ?: return emptyList()

        return try {

            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val list: List<Map<String, Any>> = Gson().fromJson(json, type)

            list.mapNotNull { item ->

                val id = (item["Id"] as? Double)?.toInt()
                val name = item["Name"]?.toString()?.trim()

                if (id != null && !name.isNullOrBlank()) {
                    AccessibleCompany(
                        id = id,
                        name = name
                    )
                } else null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }*/


}

