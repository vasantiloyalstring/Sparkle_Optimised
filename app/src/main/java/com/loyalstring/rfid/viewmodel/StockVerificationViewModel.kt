package com.loyalstring.rfid.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.model.report.Item
import com.loyalstring.rfid.data.model.report.StockVerificationReqReport
import com.loyalstring.rfid.data.model.report.StockVerificationResponseReport
import com.loyalstring.rfid.repository.StockVerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockVerificationViewModel @Inject constructor(
    private val repository: StockVerificationRepository
) : ViewModel() {

    private val _reportState =
        MutableStateFlow<UiState<StockVerificationResponseReport>>(UiState.Idle)

    val reportState: StateFlow<UiState<StockVerificationResponseReport>> =
        _reportState
    private var fullReport: StockVerificationResponseReport? = null

    private val _detailState = MutableStateFlow<UiState<List<Item>>>(UiState.Idle)
    val detailState: StateFlow<UiState<List<Item>>> = _detailState

    fun fetchStockVerificationReport(request: StockVerificationReqReport) {
        viewModelScope.launch {

            _reportState.value = UiState.Loading

            try {
                val response = repository.getStockVerificationReport(request)
                _reportState.value = UiState.Success(response)
                fullReport=response

            } catch (e: Exception) {
                _reportState.value =
                    UiState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun fetchDetailItems(
        branchId: String?,
        type: String?,
        date: String?,
        categoryId: String?,
        productId: String?,
        designId: String?
    ) {

        viewModelScope.launch {

            _detailState.value = UiState.Loading

            try {

                Log.d(
                    "DETAIL_DEBUG",
                    "branch=$branchId | type=$type | date=$date | category=$categoryId | product=$productId | design=$designId"
                )

                if (date.isNullOrEmpty()) {
                    _detailState.value = UiState.Error("Invalid date")
                    return@launch
                }

                // 🔹 Fetch fresh report data
                val response = repository.getStockVerificationReport(
                    StockVerificationReqReport(
                        ClientCode = "LS000403",
                        ReportDate = date
                    )
                )

                if (response.Branches.isNullOrEmpty()) {
                    _detailState.value = UiState.Error("No data available")
                    return@launch
                }

                // 🔹 Convert IDs safely
                val branchIdInt = branchId?.toIntOrNull()
                val categoryIdInt = categoryId?.toIntOrNull()?.takeIf { it > 0 }
                val productIdInt = productId?.toIntOrNull()?.takeIf { it > 0 }
                val designIdInt = designId?.toIntOrNull()?.takeIf { it > 0 }

                // 🔹 Find Branch
                val branch = response.Branches
                    ?.firstOrNull { it.BranchId == branchIdInt }

                if (branch == null) {
                    _detailState.value = UiState.Error("Branch not found")
                    return@launch
                }

                // 🔹 Category Filter
                val categories = branch.Categories
                    ?.filter { category ->
                        categoryIdInt == null || category.CategoryId == categoryIdInt
                    }
                    .orEmpty()

                // 🔹 Product Filter
                val products = categories
                    .flatMap { it.Products.orEmpty() }
                    .filter { product ->
                        productIdInt == null || product.ProductId == productIdInt
                    }

                // 🔹 Design Filter
                val designs = products
                    .flatMap { it.Designs.orEmpty() }
                    .filter { design ->
                        designIdInt == null || design.DesignId == designIdInt
                    }

                // 🔹 Collect Items
                val items = designs
                    .flatMap { it.Items.orEmpty() }

                if (items.isEmpty()) {
                    _detailState.value = UiState.Success(emptyList())
                    return@launch
                }

                // 🔹 Status Filter
                val finalItems = when (type?.uppercase()) {

                    "TOTAL" -> items

                    "MATCHED" -> items.filter {
                        it.Status?.equals("Matched", true) == true
                    }

                    "UNMATCHED" -> items.filter {
                        it.Status?.equals("Unmatched", true) == true
                    }

                    else -> items
                }

                _detailState.value = UiState.Success(finalItems)

            } catch (e: Exception) {

                Log.e("DETAIL_ERROR", e.message ?: "Unknown error")

                _detailState.value =
                    UiState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun resetState() {
        _reportState.value = UiState.Idle
    }
}