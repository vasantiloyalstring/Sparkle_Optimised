package com.loyalstring.rfid.viewmodel
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanResponse
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanNoRequest
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchRequest
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchResponse
import com.loyalstring.rfid.data.model.deliveryChallan.DeleteDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.DeleteDeliveryChallanResponse
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanItemPrint
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanPrintData
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanListRow
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanRequestList
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
import com.loyalstring.rfid.data.model.deliveryChallan.UpdateDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.toListRow
import com.loyalstring.rfid.data.model.deliveryChallan.toResponseList
import com.loyalstring.rfid.repository.DeliveryChallanRepository
import com.loyalstring.rfid.ui.screens.generateDeliveryChallanPdf
import com.loyalstring.rfid.ui.screens.openPdfPreview
import com.loyalstring.rfid.ui.utils.DeliveryChallanListCache
import com.loyalstring.rfid.ui.utils.DeliveryChallanListStreamer
import com.loyalstring.rfid.ui.utils.DeliveryChallanPrefetcher

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class DeliveryChallanViewModel @Inject constructor(
    private val repository: DeliveryChallanRepository,
    private val deliveryChallanPrefetcher: DeliveryChallanPrefetcher,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _challanList = MutableStateFlow<List<DeliveryChallanListRow>>(emptyList())
    val challanList: StateFlow<List<DeliveryChallanListRow>> = _challanList.asStateFlow()

    private val fullChallanById = ConcurrentHashMap<Int, DeliveryChallanResponseList>()
    private val _fullDataVersion = MutableStateFlow(0)
    val fullDataVersion: StateFlow<Int> = _fullDataVersion.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _listLoading = MutableStateFlow(false)
    val listLoading: StateFlow<Boolean> = _listLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _lastChallanNo = MutableStateFlow<Int?>(null)
    val lastChallanNo: StateFlow<Int?> = _lastChallanNo

    private val _addChallanResponse = MutableStateFlow<AddDeliveryChallanResponse?>(null)
    val addChallanResponse: StateFlow<AddDeliveryChallanResponse?> = _addChallanResponse

    private val _updateChallanResponse = MutableStateFlow<AddDeliveryChallanResponse?>(null)
    val updateChallanResponse: StateFlow<AddDeliveryChallanResponse?> = _updateChallanResponse

    private val _customerTunchList = MutableStateFlow<List<CustomerTunchResponse>>(emptyList())
    val customerTunchList: StateFlow<List<CustomerTunchResponse>> = _customerTunchList
    private val _selectedChallan = MutableStateFlow<DeliveryChallanResponseList?>(null)
    val selectedChallan: StateFlow<DeliveryChallanResponseList?> = _selectedChallan

    private var listFetchJob: Job? = null
    private var activeListFetchKey: String? = null

    fun setSelectedChallan(challan: DeliveryChallanResponseList?) {
        _selectedChallan.value = challan
    }


    fun getFullChallan(id: Int): DeliveryChallanResponseList? = fullChallanById[id]

    fun seedRowsIfEmpty(rows: List<DeliveryChallanListRow>) {
        if (rows.isEmpty()) return
        if (_challanList.value.isEmpty()) {
            _challanList.value = rows.sortedByDescending { it.Id }
            _listLoading.value = false
        }
    }

    fun mergeBootstrapRows(rows: List<DeliveryChallanListRow>) {
        if (rows.isEmpty()) return
        val merged = mergeCachedRows(_challanList.value, rows)
        _challanList.value = merged
        _listLoading.value = false
    }

    private fun applySavedChallanLocally(
        response: AddDeliveryChallanResponse,
        customerName: String?,
        cacheBranchId: Int,
        clientCodeOverride: String? = null,
    ) {
        val full = response.toResponseList(customerName)
        val clientCode = clientCodeOverride?.takeIf { it.isNotBlank() }
            ?: full.ClientCode.takeIf { it.isNotBlank() }
            ?: return
        val branchId = cacheBranchId
        val row = full.toListRow().copy(ClientCode = clientCode, BranchId = branchId)
        val mergedRows = DeliveryChallanListCache.mergeRowIntoMemory(
            clientCode = clientCode,
            branchId = branchId,
            row = row,
        )
        _challanList.value = mergedRows
        fullChallanById[full.Id] = full
        _fullDataVersion.value++
        // Allow list to keep this row even while a warm prefetch/API sync runs.
        deliveryChallanPrefetcher.markRowsReady(clientCode, branchId)

        viewModelScope.launch(Dispatchers.IO) {
            DeliveryChallanListCache.upsertChallan(
                context = appContext,
                clientCode = clientCode,
                branchId = branchId,
                full = full,
                row = row,
            )
        }
    }

    private fun branchIdOf(value: Any?): Int = when (value) {
        is Int -> value
        is Number -> value.toInt()
        else -> value?.toString()?.toIntOrNull() ?: 0
    }

    private fun mergeWithLocalRows(
        clientCode: String,
        branchId: Int,
        incoming: List<DeliveryChallanListRow>,
    ): List<DeliveryChallanListRow> {
        val local = DeliveryChallanListCache.readMemoryRows(clientCode, branchId)
            .orEmpty()
            .ifEmpty { _challanList.value }
        return (local + incoming).distinctBy { it.Id }.sortedByDescending { it.Id }
    }

    /** Show cached rows instantly; sync via streaming parser (never full Gson list).
     *  @param forceRefresh when true (list button / list open), always refresh from API
     *  and merge with locally saved rows so the newest challan is visible. */
    fun loadAllChallans(
        clientCode: String,
        branchId: Int,
        forceRefresh: Boolean = false,
    ) {
        if (clientCode.isBlank()) return

        // Always paint local/memory rows first (includes just-saved challan).
        applyMemoryRowsToUi(clientCode, branchId)

        val fetchKey = "$clientCode:$branchId"
        if (!forceRefresh && listFetchJob?.isActive == true && activeListFetchKey == fetchKey) return

        listFetchJob?.cancel()
        activeListFetchKey = fetchKey
        listFetchJob = viewModelScope.launch(Dispatchers.IO) {
            DeliveryChallanListCache.discardOversizedPrefsFile(appContext)
            DeliveryChallanListCache.purgeLegacyFullPrefs(appContext, clientCode, branchId)
            hydrateDiskRowsIfEmpty(clientCode, branchId)
            pullMemoryRowsToUi(clientCode, branchId)

            // Warm cache shortcut — skip only when not forcing a list refresh.
            if (!forceRefresh) {
                if (deliveryChallanPrefetcher.isPrefetchRunning(clientCode, branchId)) {
                    withContext(Dispatchers.Main) {
                        if (_challanList.value.isEmpty()) _listLoading.value = true
                        _error.value = null
                    }
                    while (deliveryChallanPrefetcher.isPrefetchRunning(clientCode, branchId)) {
                        pullMemoryRowsToUi(clientCode, branchId)
                        if (_challanList.value.isNotEmpty()) {
                            withContext(Dispatchers.Main) { _listLoading.value = false }
                        }
                        kotlinx.coroutines.delay(50)
                    }
                    pullMemoryRowsToUi(clientCode, branchId)
                    hydrateDiskRowsIfEmpty(clientCode, branchId)
                    withContext(Dispatchers.Main) { _listLoading.value = false }
                    if (_challanList.value.isNotEmpty() &&
                        deliveryChallanPrefetcher.isWarm(clientCode, branchId)
                    ) {
                        if (activeListFetchKey == fetchKey) activeListFetchKey = null
                        return@launch
                    }
                } else {
                    deliveryChallanPrefetcher.ensureCached(clientCode, branchId)
                    hydrateDiskRowsIfEmpty(clientCode, branchId)
                    pullMemoryRowsToUi(clientCode, branchId)
                    if (_challanList.value.isNotEmpty() &&
                        deliveryChallanPrefetcher.isWarm(clientCode, branchId)
                    ) {
                        withContext(Dispatchers.Main) { _listLoading.value = false }
                        if (activeListFetchKey == fetchKey) activeListFetchKey = null
                        return@launch
                    }
                }
            }

            val hadRows = _challanList.value.isNotEmpty()
            if (!hadRows) {
                withContext(Dispatchers.Main) { _listLoading.value = true }
            }
            withContext(Dispatchers.Main) { _error.value = null }

            try {
                val response = repository.getAllDeliveryChallansRaw(
                    DeliveryChallanRequestList(clientCode, branchId)
                )
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    val rows = DeliveryChallanListStreamer.streamList(
                        body = body,
                        progressEvery = 25,
                        onRowsProgress = { rowsSoFar ->
                            val merged = mergeWithLocalRows(clientCode, branchId, rowsSoFar)
                            DeliveryChallanListCache.putMemoryRows(clientCode, branchId, merged)
                            withContext(Dispatchers.Main) {
                                _challanList.value = merged
                                _listLoading.value = false
                            }
                        },
                        onRawObject = { id, raw ->
                            DeliveryChallanListCache.saveRawJsonById(
                                appContext,
                                clientCode,
                                branchId,
                                id,
                                raw,
                            )
                        },
                    )
                    val mergedFinal = mergeWithLocalRows(clientCode, branchId, rows)
                    withContext(Dispatchers.Main) {
                        _challanList.value = mergedFinal
                        _listLoading.value = false
                    }
                    DeliveryChallanListCache.saveRows(appContext, clientCode, branchId, mergedFinal)
                    deliveryChallanPrefetcher.markRowsReady(clientCode, branchId)
                } else if (_challanList.value.isEmpty()) {
                    withContext(Dispatchers.Main) { _error.value = response.message() }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_challanList.value.isEmpty()) {
                    withContext(Dispatchers.Main) { _error.value = e.message }
                }
            } finally {
                withContext(Dispatchers.Main) { _listLoading.value = false }
                if (activeListFetchKey == fetchKey) activeListFetchKey = null
            }
        }
    }

    private suspend fun pullMemoryRowsToUi(clientCode: String, branchId: Int) {
        val mem = DeliveryChallanListCache.readMemoryRows(clientCode, branchId) ?: return
        if (mem.isEmpty()) return
        withContext(Dispatchers.Main) {
            _challanList.value = mem.sortedByDescending { it.Id }
            _listLoading.value = false
        }
    }

    /** Paint local cache even when list already has older rows (after save). */
    private fun applyMemoryRowsToUi(clientCode: String, branchId: Int) {
        val mem = DeliveryChallanListCache.readMemoryRows(clientCode, branchId)
        if (!mem.isNullOrEmpty()) {
            _challanList.value = mem.sortedByDescending { it.Id }
            _listLoading.value = false
            return
        }
        if (_challanList.value.isNotEmpty()) {
            _listLoading.value = false
        }
    }

    private fun applyMemoryRowsIfEmpty(clientCode: String, branchId: Int) {
        if (_challanList.value.isNotEmpty()) {
            _listLoading.value = false
            return
        }
        DeliveryChallanListCache.readMemoryRows(clientCode, branchId)?.let { memRows ->
            if (memRows.isNotEmpty()) {
                _challanList.value = memRows.sortedByDescending { it.Id }
                _listLoading.value = false
            }
        }
    }

    private suspend fun hydrateDiskRowsIfEmpty(clientCode: String, branchId: Int) {
        if (_challanList.value.isNotEmpty()) return

        val diskRows = DeliveryChallanListCache.readDiskRowsOnly(appContext, clientCode, branchId)
        if (diskRows.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                if (_challanList.value.isEmpty()) {
                    _challanList.value = diskRows.sortedByDescending { it.Id }
                    _listLoading.value = false
                }
            }
        }
    }

    /** Refresh list from server in background (keeps cached rows visible). */
    fun syncListWithServer(clientCode: String, branchId: Any) {
        val branch = when (branchId) {
            is Int -> branchId
            is Number -> branchId.toInt()
            else -> branchId.toString().toIntOrNull() ?: 0
        }
        loadAllChallans(clientCode, branch)
    }

    /** Pull latest cached rows then sync with server (e.g. after save on another screen). */
    fun refreshFromCacheAndSync(clientCode: String, branchId: Int) {
        loadAllChallans(clientCode, branchId)
    }

    private fun mergeCachedRows(
        current: List<DeliveryChallanListRow>,
        cached: List<DeliveryChallanListRow>,
    ): List<DeliveryChallanListRow> =
        (current + cached).distinctBy { it.Id }.sortedByDescending { it.Id }

    private fun mergeFullChallans(list: List<DeliveryChallanResponseList>) {
        if (list.isEmpty()) return
        fullChallanById.clear()
        list.forEach { challan ->
            if (challan != null) fullChallanById[challan.Id] = challan
        }
        _fullDataVersion.value++
    }

    suspend fun ensureFullChallan(
        id: Int,
        clientCode: String,
        branchId: Int,
    ): DeliveryChallanResponseList? = withContext(Dispatchers.IO) {
        fullChallanById[id]?.let { return@withContext it }
        val fromFile = DeliveryChallanListCache.loadFullById(appContext, clientCode, branchId, id)
        if (fromFile != null) {
            fullChallanById[id] = fromFile
            return@withContext fromFile
        }
        null
    }

    private fun indexFullChallans(list: List<DeliveryChallanResponseList>) = mergeFullChallans(list)

    private fun toSortedRows(list: List<DeliveryChallanResponseList>): List<DeliveryChallanListRow> =
        list.map { it.toListRow() }.distinctBy { it.Id }.sortedByDescending { it.Id }

    private fun rowsChanged(
        current: List<DeliveryChallanListRow>,
        updated: List<DeliveryChallanListRow>,
    ): Boolean {
        if (current.size != updated.size) return true
        return current.indices.any { current[it] != updated[it] }
    }

    fun fetchAllChallans(clientCode: String, branchId: Any) {
        syncListWithServer(clientCode, branchId)
    }

    fun clearlastDeliveryChallanNo() {
        _lastChallanNo.value = null
    }

    /** ✅ Fetch last challan number */
    fun fetchLastChallanNo(clientCode: String, branchId: Any) {
        viewModelScope.launch {
            try {
                val request = ChallanNoRequest(clientCode, branchId as Int)
                val response = repository.getLastChallanNo(request)
                if (response.isSuccessful) {
                    _lastChallanNo.value = response.body()?.LastChallanNo
                } else {
                    _lastChallanNo.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _lastChallanNo.value = null
            }
        }
    }


    /** ✅ Add Delivery Challan */
    fun addDeliveryChallan(request: AddDeliveryChallanRequest) {
        viewModelScope.launch {
            try {
                val response = repository.addDeliveryChallan(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    // Set UI event first so toast/PDF always run even if cache mapping fails.
                    _addChallanResponse.value = body
                    _error.value = null
                    if (body == null) {
                        _error.value = "Challan saved but response was empty"
                    } else {
                        val branch = branchIdOf(request.BranchId)
                        runCatching {
                            applySavedChallanLocally(
                                response = body,
                                customerName = request.CustomerName,
                                cacheBranchId = branch,
                                clientCodeOverride = request.ClientCode,
                            )
                        }.onFailure { e ->
                            e.printStackTrace()
                        }
                        // Force refresh so list shows latest server + locally saved row.
                        loadAllChallans(request.ClientCode, branch, forceRefresh = true)
                    }
                } else {
                    _error.value = "Failed: ${response.message()}"
                    _addChallanResponse.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.localizedMessage ?: "Unknown error"
                _addChallanResponse.value = null
            }
        }
    }

    /** ✅ Update Delivery Challan */
    fun updateDeliveryChallan(request: UpdateDeliveryChallanRequest) {
        viewModelScope.launch {
            try {
                val response = repository.updateDeliveryChallan(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    _updateChallanResponse.value = body
                    _error.value = null
                    if (body == null) {
                        _error.value = "Challan updated but response was empty"
                    } else {
                        val branch = branchIdOf(request.BranchId)
                        runCatching {
                            applySavedChallanLocally(
                                response = body,
                                customerName = request.CustomerName,
                                cacheBranchId = branch,
                                clientCodeOverride = request.ClientCode,
                            )
                        }.onFailure { e ->
                            e.printStackTrace()
                        }
                        loadAllChallans(request.ClientCode, branch, forceRefresh = true)
                    }
                } else {
                    _error.value = "Failed: ${response.message()}"
                    _updateChallanResponse.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.localizedMessage ?: "Unknown error"
                _updateChallanResponse.value = null
            }
        }
    }

    fun fetchCustomerTunch(clientCode: String, employeeId: Int?) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val request = CustomerTunchRequest(clientCode)
                val response = repository.getAllCustomerTunch(request)

                if (response.isSuccessful && response.body() != null) {
                    _customerTunchList.value = response.body()!!
                } else {
                    _error.value = "Failed: ${response.message()}"
                    _customerTunchList.value = emptyList()
                }

            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Something went wrong"
                _customerTunchList.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteDeliveryChallan(clientCode: String, id: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = DeleteDeliveryChallanRequest(ClientCode = clientCode, Id = id)
                val response = repository.deleteDeliveryChallan(request)
                if (response.isSuccessful) {
                    val branchId = fullChallanById[id]?.BranchId
                        ?: _challanList.value.firstOrNull { it.Id == id }?.BranchId
                        ?: 0
                    fullChallanById.remove(id)
                    val remainingRows = _challanList.value.filterNot { it.Id == id }
                    withContext(Dispatchers.Main) { _challanList.value = remainingRows }
                    DeliveryChallanListCache.removeChallan(
                        context = appContext,
                        clientCode = clientCode,
                        branchId = branchId,
                        id = id,
                        remainingRows = remainingRows,
                    )
                    withContext(Dispatchers.Main) { onResult(true) }
                } else {
                    withContext(Dispatchers.Main) {
                        _error.value = "Failed: ${response.message()}"
                        onResult(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _error.value = e.localizedMessage ?: "Unknown error"
                    onResult(false)
                }
            }
        }
    }

  /*  fun printDeliveryChallan(
        context: Context,
        challan: DeliveryChallanResponseList
    ) {
        viewModelScope.launch {
            try {
                // ✅ If list item already has full details, use it
                // ✅ Otherwise fetch full challan from API inside getChallanForPrint(...)
                val printData = getChallanForPrint(
                    clientCode = challan.ClientCode ?: "",
                    challanId = challan.Id ?: 0
                )

                val uri = generateDeliveryChallanPdf(context, printData)
                if (uri != null) openPdfPreview(context, uri)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Print failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ You already mentioned you have this kind of function
    suspend fun getChallanForPrint(clientCode: String, challanId: Int): DeliveryChallanPrintData {
        // call API + map to DeliveryChallanPrintData
        // return printData
        TODO("Implement")
    }*/



    fun clearLastChallanNo() { _lastChallanNo.value = null }
    fun clearAddChallanResponse() { _addChallanResponse.value = null }
    fun clearUpdateChallanResponse() { _updateChallanResponse.value = null }
    fun clearError() { _error.value = null }


}






