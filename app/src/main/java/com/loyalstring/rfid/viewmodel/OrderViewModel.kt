package com.loyalstring.rfid.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sparklepos.models.loginclasses.customerBill.AddEmployeeRequest
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeResponse
import com.google.gson.Gson
import com.loyalstring.rfid.data.local.dao.PendingEmployeeDao
import com.loyalstring.rfid.data.local.dao.PendingOrderDao
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.data.local.entity.PendingEmployeeEntity
import com.loyalstring.rfid.data.local.entity.PendingOrderEntity
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.order.CustomOrderRequest
import com.loyalstring.rfid.data.model.order.CustomOrderResponse
import com.loyalstring.rfid.data.model.order.CustomOrderUpdateResponse
import com.loyalstring.rfid.data.model.order.Customer
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.data.model.order.LastOrderNoResponse
import com.loyalstring.rfid.data.remote.data.DailyRateResponse
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.repository.OrderRepository
import com.loyalstring.rfid.ui.utils.OfflineCustomerMatch
import com.loyalstring.rfid.worker.PendingEmployeeSyncWorker
import com.loyalstring.rfid.worker.PendingOrderSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
    object Idle : UiState<Nothing>()
}

@HiltViewModel
class OrderViewModel @Inject constructor(
    @ApplicationContext private val context1: Context,
    private val pendingOrderDao: PendingOrderDao,
    private val repository: OrderRepository // or whatever your dependency is
) : ViewModel() {
    private val _addEmpResponse = MutableLiveData<Resource<EmployeeResponse>>()
    val addEmpReposnes: LiveData<Resource<EmployeeResponse>> = _addEmpResponse

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading


    /* private val _empListResponse = MutableLiveData<List<EmployeeList>>()
    val empListResponse: LiveData<List<EmployeeList>> = _empListResponse
    val empListFlow = _empListResponse.asStateFlow()*/

    private val _empListFlow = MutableStateFlow<UiState<List<EmployeeList>>>(UiState.Loading)
    val empListFlow: StateFlow<UiState<List<EmployeeList>>> = _empListFlow
    val isEmpListLoading = MutableStateFlow(false)


    private val _itemCodeResponse = MutableStateFlow<List<ItemCodeResponse>>(emptyList())
    val itemCodeResponse: StateFlow<List<ItemCodeResponse>> = _itemCodeResponse
    val isItemCodeLoading = MutableStateFlow(false)


    private val _lastOrderNOResponse = MutableStateFlow(LastOrderNoResponse())
    val lastOrderNoresponse: StateFlow<LastOrderNoResponse> = _lastOrderNOResponse

    private val _orderResponse = MutableStateFlow<CustomOrderResponse?>(null)
    val orderResponse: StateFlow<CustomOrderResponse?> = _orderResponse

    private val _orderUpdateResponse = MutableStateFlow<CustomOrderUpdateResponse?>(null)
    val orderUpdateResponse: StateFlow<CustomOrderUpdateResponse?> = _orderUpdateResponse

    private val _allOrderItems = MutableStateFlow<List<OrderItem>>(emptyList())
    val allOrderItems: StateFlow<List<OrderItem>> = _allOrderItems


    private val _insertOrderOffline = MutableStateFlow<CustomOrderRequest?>(null)
    val insertOrderOffline: StateFlow<CustomOrderRequest?> = _insertOrderOffline


    private val _getAllOrderList = MutableStateFlow<List<CustomOrderResponse>>(emptyList())
    val getAllOrderList: StateFlow<List<CustomOrderResponse>> = _getAllOrderList

    private val _getAllDailyRate = MutableStateFlow<List<DailyRateResponse>>(emptyList())
    val getAllDailyRate: StateFlow<List<DailyRateResponse>> = _getAllDailyRate

  /*  private val _nextOrderNo = MutableStateFlow(0)
    val nextOrderNo: StateFlow<Int> = _nextOrderNo*/

//    private val _orderPlaced = mutableStateOf(false)
//    val orderPlaced: State<Boolean> = _orderPlaced

    fun setOrderResponse(response: CustomOrderResponse) {
        _orderResponse.value = response
    }

    suspend fun clearOrderItems() {
        repository.clearOrderItems()
        _allOrderItems.value = emptyList()
    }

    fun clearOrderRequest() {
        _insertOrderOffline.value = null
    }

    fun clearOrderResponse() {
        _orderResponse.value = null
    }

    fun clearLastOrderNo() {
        _lastOrderNOResponse.value = LastOrderNoResponse()
    }

    fun clearOrderItemsAsync() {
        viewModelScope.launch { clearOrderItems() }
    }

//    fun placeOrder(request: ClientCodeRequest, order: CustomOrderRequest) {
//        viewModelScope.launch {
//            val response = repository.submitOrder(request, order)
//            _orderPlaced.value = response.isSuccessful
//        }
//    }
//
//    fun resetOrderPlaced() {
//        _orderPlaced.value = false
//    }

    /*add employee*/
    fun addEmployee(request: AddEmployeeRequest) {
        viewModelScope.launch {
            _addEmpResponse.value = Resource.Loading()
            // Always push to JewelMarts ClientAdd in parallel (online / best-effort).
            // Does not affect main LoyalString customer-add success/failure.
            pushClientAddBestEffort(request)
            try {
                val gson = Gson()
                val json = gson.toJson(request)
                Log.d("AddEmployeeRequestJSON", json)
                val response = repository.AAddAllEmployeeDetails(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("orderViewModel", "Response Body: $body")

                    if (body?.message != null) {
                        // Logical error from server — still keep offline so order can continue.
                        saveEmployeeOffline(request, "Customer saved offline (${body.message})")
                    } else if (body?.Id != null && body.Id > 0) {
                        // Online success: persist locally and refresh list.
                        val emp = request.toEmployeeList().copy(Id = body.Id)
                        withContext(Dispatchers.IO) {
                            repository.insertEmployeeToRoom(emp)
                        }
                        getAllEmpList(request.ClientCode.orEmpty())
                        _addEmpResponse.value =
                            Resource.Success(body, "Customer added successfully")
                        enqueueCustomerThenOrderSync(context1)
                    } else {
                        saveEmployeeOffline(request)
                    }
                } else {
                    saveEmployeeOffline(request)
                }
            } catch (e: Exception) {
                Log.e("orderViewModel", "Exception: ${e.message}")
                saveEmployeeOffline(request)
            }
        }
    }

    /** Fire-and-forget JewelMarts ClientAdd — never fails UI / main save. */
    private fun pushClientAddBestEffort(request: AddEmployeeRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.pushClientAddToJewelMarts(request)
        }
    }


    fun setLocalOrderList(data: List<CustomOrderResponse>) {
        _getAllOrderList.value = data
    }

    fun removeOrderById(orderId: Int) {
        // CustomOrderId==0 is used by many offline rows — never wipe all of them.
        if (orderId <= 0) return
        _getAllOrderList.value = _getAllOrderList.value.filterNot { it.CustomOrderId == orderId }
    }

    /** Remove one order from UI list (server id or local OrderNo). */
    fun removeOrderFromUi(order: CustomOrderResponse) {
        _getAllOrderList.value = _getAllOrderList.value.filterNot { sameOrder(it, order) }
    }

    private fun sameOrder(a: CustomOrderResponse, b: CustomOrderResponse): Boolean {
        if (a.CustomOrderId > 0 && b.CustomOrderId > 0) {
            return a.CustomOrderId == b.CustomOrderId
        }
        val aNo = a.OrderNo.orEmpty()
        val bNo = b.OrderNo.orEmpty()
        return aNo.isNotBlank() && aNo == bNo
    }


    /*emp list function*/

    /*  fun getAllEmpList(clientCode: String) {
          viewModelScope.launch {
             // delay(1000)
              isEmpListLoading.value = true

              try {
                  val response = repository.getAllEmpList(ClientCodeRequest(clientCode)) // API call

                  if (response.isSuccessful && response.body() != null && response.body()!!.isNotEmpty()) {
                      val data = response.body()

                      // Save to Room
                      // repository.clearAllEmployees()
                    // repository.saveEmpListToRoom(data!!)

                    *//*  if (!data.isNullOrEmpty()) {
                        withContext(Dispatchers.IO) {
                            val chunkSize = 10 // try with 500–1000
                            data.chunked(chunkSize).forEach { chunk ->
                                repository.saveEmpListToRoom(chunk)
                            }
                        }
                    }*//*
                   // repository.saveEmpListToRoom(data!!)
                    _empListFlow.value = UiState.Success(data!!)

                } else {
                    val localData = repository.getAllEmpListFromRoom(ClientCodeRequest(clientCode))
                    _empListFlow.value = UiState.Success(localData)

                    // API failed => try loading from local DB
                  *//*  val localData = repository.getAllEmpListFromRoom(ClientCodeRequest(clientCode))
                    val employeeList = localData.map { it.toEmployeeList() }  // Convert to List<EmployeeList>
                    _empListFlow.value = UiState.Success(employeeList!!)*//*
                }

            } catch (e: Exception) {
                val localData = repository.getAllEmpListFromRoom(ClientCodeRequest(clientCode))
                _empListFlow.value = UiState.Success(localData)
                // Exception (e.g., no internet) => try loading from local DB
              *//*  val localData = repository.getAllEmpListFromRoom(ClientCodeRequest(clientCode))
                val employeeList = localData.map { it.toEmployeeList() }  // Convert to List<EmployeeList>
                _empListFlow.value = UiState.Success(employeeList!!)*//*
            } finally {
                isEmpListLoading.value = false
            }
        }
    }
*/

    fun getAllEmpList(clientCode: String) {
        viewModelScope.launch {
            isEmpListLoading.value = true
            val request = ClientCodeRequest(clientCode)

            try {
                val cached = repository.getAllEmpListFromRoom(request)
                if (cached.isNotEmpty()) {
                    _empListFlow.value = UiState.Success(cached)
                }

                val response = repository.getAllEmpList(request)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    // Prefer Room (includes preserved offline Id=0 customers).
                    val merged = repository.getAllEmpListFromRoom(request)
                    _empListFlow.value = UiState.Success(
                        merged.ifEmpty { response.body()!! }
                    )
                } else if (cached.isEmpty()) {
                    _empListFlow.value = UiState.Success(cached)
                }
            } catch (e: Exception) {
                val localData = repository.getAllEmpListFromRoom(request)
                _empListFlow.value = UiState.Success(localData)
            } finally {
                isEmpListLoading.value = false
            }
        }
    }
    fun AddEmployeeRequest.toEmployeeList(): EmployeeList {
        return EmployeeList(
            custId = 0, // auto-generate in Room
            Id = this.Id?.toIntOrNull() ?: 0,
            FirstName = this.FirstName.orEmpty(),
            LastName = this.LastName.orEmpty(),
            PerAddStreet = this.PerAddStreet.orEmpty(),
            CurrAddStreet = this.CurrAddStreet.orEmpty(),
            Mobile = this.Mobile.orEmpty(),
            Email = this.Email.orEmpty(),
            Password = this.Password.orEmpty(),
            CustomerLoginId = this.CustomerLoginId.orEmpty(),
            DateOfBirth = this.DateOfBirth.orEmpty(),
            MiddleName = this.MiddleName.orEmpty(),
            PerAddPincode = this.PerAddPincode.orEmpty(),
            Gender = this.Gender,
            OnlineStatus = this.OnlineStatus,
            CurrAddTown = this.PerAddTown, // using PerAddTown because CurrAddTown not in API
            CurrAddPincode = this.CurrAddPincode.orEmpty(),
            CurrAddState = this.CurrAddState.orEmpty(),
            PerAddTown = this.PerAddTown.orEmpty(),
            PerAddState = this.PerAddState,
            GstNo = this.GstNo.orEmpty(),
            PanNo = this.PanNo.orEmpty(),
            AadharNo = this.AadharNo.orEmpty(),
            BalanceAmount = this.BalanceAmount ?: "0.0",
            AdvanceAmount = this.AdvanceAmount.orEmpty(),
            Discount = this.Discount.orEmpty(),
            CreditPeriod = this.CreditPeriod,
            FineGold = this.FineGold.orEmpty(),
            FineSilver = this.FineSilver.orEmpty(),
            ClientCode = this.ClientCode.orEmpty(),
            VendorId = this.VendorId ?: 0,
            AddToVendor = this.AddToVendor == true,
            CustomerSlabId = this.CustomerSlabId ?: 0,
            CreditPeriodId = this.CreditPeriodId ?: 0,
            RateOfInterestId = this.RateOfInterestId ?: 0,
            CustomerSlab = null,
            RateOfInterest = null,
            CreatedOn = "", // not in API, can be current time or empty
            LastUpdated = "", // not in API
            StatusType = true,
            Remark = this.Remark.orEmpty(),
            Area = this.Area.orEmpty(),
            City = this.City.orEmpty(),
            Country = this.Country.orEmpty()
        )
    }

    /*get all item code list*/
    fun getAllItemCodeList(request: ClientCodeRequest) {
        viewModelScope.launch {
            val hadCached = _itemCodeResponse.value.isNotEmpty()
            try {
                val localData = repository.getAllItemCodeFromRoom(request)
                if (localData.isNotEmpty()) {
                    _itemCodeResponse.value = localData
                } else if (!hadCached) {
                    isItemCodeLoading.value = true
                }

                val response = repository.getAllItemCodeList(request)
                if (response.isSuccessful && response.body() != null) {
                    _itemCodeResponse.value = response.body()!!
                    repository.saveAllItemCodeToRoom(response.body()!!)
                } else if (_itemCodeResponse.value.isEmpty()) {
                    _itemCodeResponse.value = localData
                }
            } catch (e: Exception) {
                if (_itemCodeResponse.value.isEmpty()) {
                    _itemCodeResponse.value = repository.getAllItemCodeFromRoom(request)
                }
            } finally {
                isItemCodeLoading.value = false
            }
        }
    }

    /*customer order*/
    fun addOrderCustomer(request: CustomOrderRequest) {
        viewModelScope.launch {
            try {
                val response = repository.addOrder(request)
                if (response.isSuccessful && response.body() != null) {
                    _orderResponse.value = response.body()!!
                    Log.d("OrderViewModel", "Custom Order: ${response.body()}")
                } else {
                    _orderResponse.value = response.body()// ✅ Use default object
                    Log.e("OrderViewModel", "Custom Order Response error: ${response.code()}")
                }
            } catch (e: Exception) {
                _orderResponse.value = _orderResponse.value
                Log.e("OrderViewModel", "Custom Order Exception: ${e.message}")
            }
        }
    }

    /*last order no*/
    fun fetchLastOrderNo(request: ClientCodeRequest) {
        viewModelScope.launch {
            try {
                val response = repository.getLastOrderNo(request)
                if (response.isSuccessful && response.body() != null) {
                    _lastOrderNOResponse.value = response.body()!!
                    //repository.clearLastOrderNo()
                    repository.saveLastOrderNoToRoom(response.body()!!)
                    Log.d("OrderViewModel", "Last Order No: ${response.body()}")
                } else {
                    Log.e("OrderViewModel", "Error: ${response.code()} ${response.message()}")
                    val localData = repository.getLastOrderNoFromRoom(request)
                    _lastOrderNOResponse.value = localData
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Exception: ${e.message}")
                val localData = repository.getLastOrderNoFromRoom(request)
                _lastOrderNOResponse.value = localData
            }
        }
    }

    fun deleteOrder(request: ClientCodeRequest, id: Int) {


    }

    /*get All order list in list screen*/
    /*last order no*/
   /* fun fetchAllOrderListFromApi(request: ClientCodeRequest) {
     *//*   viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = repository.getAllOrderList(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()
                    _getAllOrderList.value = response.body()!!
                    try {
                        repository.saveCustomerOrder(body!!)
                    } catch (e: Exception) {
                        Log.d("OrderViewModel", "Error: ${e.printStackTrace()}")
                    }
                    Log.d("OrderViewModel", "get All order list: ${response.body()}")
                    _isLoading.value = false
                } else {
                    Log.e("OrderViewModel", "Error: ${response.code()} ${response.message()}")
                    val localData = repository.getAllCustomerOrders(request.clientcode.toString())
                    //_getAllOrderList.value = mappedData
                    // _getAllOrderList.value = localData
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Exception: ${e.message}")
                repository.getAllCustomerOrders(request.clientcode.toString())
                _isLoading.value = false
            }
        }*//*

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = repository.getAllOrderList(request)
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val list = res.body()!!

                    _getAllOrderList.value = list

                    repository.saveOrderListCache(
                        clientCode = request.clientcode.orEmpty(),
                        list = list
                    )
                } else {
                    val cached = repository.getOrderListCache(request.clientcode.orEmpty())
                    _getAllOrderList.value = cached
                }
            } catch (e: Exception) {
                val cached = repository.getOrderListCache(request.clientcode.orEmpty())
                _getAllOrderList.value = cached
            } finally {
                _isLoading.value = false
            }
        }
    }*/

    fun fetchAllOrderListFromApi(request: ClientCodeRequest) {
        viewModelScope.launch {
            val clientCode = request.clientcode.orEmpty()

            try {
                val pendingLocal = getPendingOrderResponses(clientCode)

                val cachedServer = repository.getOrderListCache(clientCode)
                if (pendingLocal.isNotEmpty() || cachedServer.isNotEmpty()) {
                    _getAllOrderList.value = mergeOrders(pendingLocal, cachedServer)
                } else {
                    _isLoading.value = true
                }

                val serverList = try {
                    val res = repository.getAllOrderList(request)
                    if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        val list = res.body()!!
                        repository.saveOrderListCache(clientCode, list)
                        list
                    } else {
                        cachedServer
                    }
                } catch (e: Exception) {
                    cachedServer
                }

                _getAllOrderList.value = mergeOrders(pendingLocal, serverList)
            } finally {
                _isLoading.value = false
            }
        }
    }
    private fun mergeOrders(
        pendingLocal: List<CustomOrderResponse>,
        serverList: List<CustomOrderResponse>
    ): List<CustomOrderResponse> {

        val serverIds = serverList.map { it.CustomOrderId }.toSet()

        val safePending = pendingLocal
            .filter { it.CustomOrderId == 0 || it.CustomOrderId !in serverIds }
            .sortedByDescending { it.CreatedOn }   // ✅ latest first (offline)

        val safeServer = serverList
            .sortedByDescending { it.CreatedOn }   // optional: server also latest first

        return safePending + safeServer
    }


    fun CustomOrderResponse.toRequest(): CustomOrderRequest {
        return CustomOrderRequest(
            CustomOrderId = this.CustomOrderId,
            CustomerId = this.CustomerId.toString(),
            ClientCode = this.ClientCode,
            OrderId = this.OrderId,
            TotalAmount = this.TotalAmount,
            PaymentMode = this.PaymentMode,
            Offer = this.Offer,
            Qty = this.Qty,
            GST = this.GST,
            OrderStatus = this.OrderStatus,
            MRP = this.MRP,
            VendorId = this.VendorId,
            TDS = this.TDS,
            PurchaseStatus = this.PurchaseStatus,
            GSTApplied = this.GSTApplied,
            Discount = this.Discount,
            TotalNetAmount = this.TotalNetAmount,
            TotalGSTAmount = this.TotalGSTAmount,
            TotalPurchaseAmount = this.TotalPurchaseAmount,
            ReceivedAmount = this.ReceivedAmount,
            TotalBalanceMetal = this.TotalBalanceMetal,
            BalanceAmount = this.BalanceAmount?:"0.0",
            TotalFineMetal = this.TotalFineMetal.toString(),
            CourierCharge = this.CourierCharge,
            SaleType = this.SaleType,
            OrderDate = this.OrderDate,
            OrderCount = this.OrderCount,
            AdditionTaxApplied = this.AdditionTaxApplied,
            CategoryId = this.CategoryId,
            OrderNo = this.OrderNo.toString(),
            DeliveryAddress = this.DeliveryAddress,
            BillType = this.BillType,
            UrdPurchaseAmt = this.UrdPurchaseAmt,
            BilledBy = this.BilledBy,
            SoldBy = this.SoldBy,
            CreditSilver = this.CreditSilver,
            CreditGold = this.CreditGold,
            CreditAmount = this.CreditAmount,
            BalanceAmt = this.BalanceAmt,
            BalanceSilver = this.BalanceSilver,
            BalanceGold = this.BalanceGold,
            TotalSaleGold = this.TotalSaleGold,
            TotalSaleSilver = this.TotalSaleSilver,
            TotalSaleUrdGold = this.TotalSaleUrdGold,
            TotalSaleUrdSilver = this.TotalSaleUrdSilver,
            FinancialYear = this.FinancialYear,
            BaseCurrency = this.BaseCurrency,
            TotalStoneWeight = this.TotalStoneWeight,
            TotalStoneAmount = this.TotalStoneAmount,
            TotalStonePieces = this.TotalStonePieces,
            TotalDiamondWeight = this.TotalDiamondWeight,
            TotalDiamondPieces = this.TotalDiamondPieces,
            TotalDiamondAmount = this.TotalDiamondAmount,
            FineSilver = this.FineSilver,
            FineGold = this.FineGold,
            DebitSilver = this.DebitSilver,
            DebitGold = this.DebitGold,
            PaidMetal = this.PaidMetal,
            PaidAmount = this.PaidAmount,
            TotalAdvanceAmt = this.TotalAdvanceAmt,
            TaxableAmount = this.TaxableAmount,
            TDSAmount = this.TDSAmount,
            CreatedOn = this.CreatedOn,
            StatusType = this.StatusType,
            FineMetal = this.FineMetal?:"",
            BalanceMetal = this.BalanceMetal?:"",
            AdvanceAmt = this.AdvanceAmt?:"",
            PaidAmt = this.PaidAmt?:"",
            TaxableAmt = this.TaxableAmt?:"",
            GstAmount = this.GstAmount?:"",
            GstCheck = this.GstCheck?:"",
            Category = this.Category?:"",
            TDSCheck = this.TDSCheck?:"",
            Remark = this.Remark?: "",
            OrderItemId = this.OrderItemId,
            StoneStatus = this.StoneStatus?: "",
            DiamondStatus = this.DiamondStatus?: "",
            BulkOrderId = this.BulkOrderId?: "",
            CustomOrderItem = this.CustomOrderItem,
            Payments = this.Payments,
            uRDPurchases = emptyList(), // You can map if needed
            Customer = this.Customer,
            Id = this.Id,
            syncStatus = this.syncStatus,
            LastUpdated = this.LastUpdated,
            HallmarkAmount = this.HallmarkAmount,
            WeightCatogories = this.WeightCategories,
            SKUId = this.SKUId,
            RfidCode = this.RfidCode,
            TidNumber = this.TidNumber
        )
    }


    /*insert order item locally*/
    fun insertOrderItemToRoom(item: OrderItem) {
        viewModelScope.launch {
            try {

                if (!item.rfidCode.equals("null")) {
                    repository.insertOrderItems(item)
                    Log.d("OrderViewModel", "Order item inserted into Room: $item")
                }

            } catch (e: Exception) {
                Log.e("OrderViewModel", "Room Insert Error: ${e.message}")
            }
        }
    }

    fun insertOrderItemsBatch(items: List<OrderItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                items.filter { !it.rfidCode.equals("null") }.forEach { item ->
                    repository.insertOrderItems(item)
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Room batch insert error: ${e.message}")
            }
        }
    }

    /*getAll order items from the roomdatbase*/

    fun getAllOrderItemsFromRoom() {
        viewModelScope.launch {
            repository.getAllOrderItems().collect { items ->
                // Filter out items with null or empty rfidCode
                val filteredItems = items.filter { !it.rfidCode.isNullOrBlank() }
                _allOrderItems.value = filteredItems
                Log.d(
                    "OrderViewModel",
                    "Fetched ${filteredItems.size} order items with valid RFID code"
                )
            }
        }
    }

    /*delete all order*/
    fun deleteOrders(
        request: ClientCodeRequest,
        id: Int,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Offline-only rows have CustomOrderId=0 — API delete is invalid.
                if (id <= 0) {
                    onResult(false)
                    return@launch
                }
                val response = repository.deleteOrder(request, id)
                if (response.isSuccessful) {
                    removeOrderById(id)
                    withContext(Dispatchers.IO) {
                        repository.deleteOrderFromCache(request.clientcode.orEmpty(), id)
                        // Drop any pending UPDATE/DELETE row for this server order.
                        pendingOrderDao.hardDeleteByLocalId("SRV-$id")
                    }
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    /*insert order item or update locally*/
    fun insertOrderItemToRoomORUpdate(item: OrderItem) {
        viewModelScope.launch {
            try {
                repository.insertORUpdate(item)
                Log.d("OrderViewModel", "Order item updated into Room: $item")
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Room update Error: ${e.message}")
            }
        }
    }


    /*    *//*sync data to server*//*
    // Save the customer order to Room
    fun saveOrder(customerOrderRequest: CustomOrderRequest) {
        viewModelScope.launch {
            try {
                repository.saveCustomerOrder(customerOrderRequest)
                _insertOrderOffline.value = (customerOrderRequest)
                repository.saveCustomerOrder(customerOrderRequest)
                Log.d("orderViewModel", "orderViewModel" + customerOrderRequest)
            } catch (e: Exception) {
                _insertOrderOffline.value = (customerOrderRequest)
                Log.d("orderViewModel", "orderViewModel" + e.toString())
            }
        }
    }*/

    // Fetch all customer orders based on the client code
    fun getAllOrders(clientCode: String) {
        viewModelScope.launch {
            try {
                repository.getAllCustomerOrders(clientCode)
                // _orderResponse.value = (orders)
            } catch (e: Exception) {
                // _orderResponse.value =("Failed to fetch orders: ${e.message}")
            }
        }
    }

    // Delete unsynced orders (syncStatus = 0)
    fun deleteUnsyncedOrders() {
        viewModelScope.launch {
            try {
                repository.deleteUnsyncedOrders()
                //_orderResponse.value = ("Unsynced orders deleted successfully.")
            } catch (e: Exception) {
                // _orderResponse.value = UiState.Error("Failed to delete unsynced orders: ${e.message}")
            }
        }
    }

    fun syncDataWhenOnline() {
        viewModelScope.launch {
            val unsyncedOrders = repository.getAllCustomerOrders("LS000241")
            try {
                Log.e("unsyncedOrders", "Successfully done" + unsyncedOrders.get(0).Category)
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync: ${e.message}")
            }
            for (order in unsyncedOrders) {
                try {
                    val response = repository.addOrder(order)
                    if (response.isSuccessful) {
                        repository.addOrder(order)
                        Log.e("Sync", "Successfully done")
                    }
                } catch (e: Exception) {
                    Log.e("Sync", "Failed to sync: ${e.message}")
                }
            }
        }
    }

    /*update customer Order*/
    /*customer order*/
    fun updateOrderCustomer(request: CustomOrderRequest) {
        viewModelScope.launch {
            try {
                val response = repository.updateOrder(request)
                if (response.isSuccessful && response.body() != null) {
                    _orderUpdateResponse.value = response.body()!!
                    Log.d("OrderViewModel", "Custom Order: ${response.body()}")
                } else {
                    _orderUpdateResponse.value = response.body()
                    Log.e(
                        "OrderViewModel",
                        "Custom Order Response error: ${response.code()} ${response.errorBody()?.string()}"
                    )
                }
            } catch (e: Exception) {
              //  _orderUpdateResponse.value = _orderResponse.value.OrderStatus.toString()
                Log.e("OrderViewModel", "Custom Order Exception: ${e.message}")
            }
        }
    }

    fun clearUpdateResponse() {
        _orderUpdateResponse.value = null
    }

    // PERF-FIX: @SuppressLint added because _addEmpResponse is MutableLiveData<Resource<EmployeeResponse>>
    // (non-nullable type param) but null is intentionally used here as a "reset/consumed" sentinel.
    @SuppressLint("NullSafeMutableLiveData")
    fun clearAddEmpResponse() {
        _addEmpResponse.value = null
    }

    /*update customer Order*/
    /*customer order*/
    fun getDailyRate(request: ClientCodeRequest) {
        viewModelScope.launch {
            try {
                val response = repository.dailyRate(request)
                if (response.isSuccessful && response.body() != null) {
                    _getAllDailyRate.value = response.body()!!
                    Log.d("OrderViewModel", "Custom Order: ${response.body()}")
                } else {
                    Log.e("OrderViewModel", "Daily rate response error: ${response.code()}")
                }
            } catch (e: Exception) {
                //  _orderUpdateResponse.value = _orderResponse.value.OrderStatus.toString()
                Log.e("OrderViewModel", "Custom Order Exception: ${e.message}")
            }
        }
    }

    fun saveOrderOffline(req: CustomOrderRequest, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val localId = UUID.randomUUID().toString()
            val gson = Gson()

            val frozenReq = freezeOfflineOrderCustomer(gson, req).copy(OrderNo = "LOCAL-$localId")
            val json = gson.toJson(frozenReq)
            val localCustomerKey =
                OfflineCustomerMatch.extractLocalCustId(frozenReq.Customer.Remark).orEmpty()
            val first = frozenReq.Customer.FirstName.trim()
            val last = frozenReq.Customer.LastName.trim()
            val mobile = frozenReq.Customer.Mobile.trim()

            Log.d(
                "OrderOfflineSave",
                "localId=$localId customer=$first key=$localCustomerKey mobile=$mobile"
            )

            pendingOrderDao.upsert(
                PendingOrderEntity(
                    localId = localId,
                    clientCode = frozenReq.ClientCode.orEmpty(),
                    customerId = frozenReq.CustomerId.toIntOrNull() ?: 0,
                    payloadJson = json,
                    status = "PENDING",
                    createdAt = System.currentTimeMillis(),
                    op = "CREATE",
                    updatedAt = System.currentTimeMillis(),
                    localCustomerKey = localCustomerKey,
                    customerFirstName = first,
                    customerLastName = last,
                    customerMobile = mobile,
                )
            )

            val localItem = frozenReq.toLocalResponse(localId, frozenReq.Customer)
            launch(Dispatchers.Main) {
                _getAllOrderList.value = listOf(localItem) + _getAllOrderList.value
            }

            enqueueCustomerThenOrderSync(context)
        }
    }

    /**
     * Snapshot customer fields onto the pending payload and attach the correct
     * LOCAL_CUST tag by matching pending offline employees to this order's customer.
     */
    private suspend fun freezeOfflineOrderCustomer(
        gson: Gson,
        req: CustomOrderRequest,
    ): CustomOrderRequest {
        val first = req.Customer.FirstName.trim()
        val last = req.Customer.LastName.trim()
        val mobile = req.Customer.Mobile.trim()
        val display = listOf(first, last).filter { it.isNotEmpty() }.joinToString(" ")

        var localCustId: String? = null
        val pending = pendingEmployeeDao.getPending()
            .filter {
                req.ClientCode.isNullOrBlank() ||
                    it.clientCode.equals(req.ClientCode, ignoreCase = true)
            }
        val remarkKey = OfflineCustomerMatch.extractLocalCustId(req.Customer.Remark)
        if (!remarkKey.isNullOrBlank() && pending.any { it.localId == remarkKey }) {
            // Selected offline customer tag is valid — use it (1:1 link).
            localCustId = remarkKey
        } else {
            // Resolve by THIS order's name/mobile against pending employees only.
            for (row in pending) {
                val emp = runCatching {
                    gson.fromJson(row.payloadJson, AddEmployeeRequest::class.java)
                }.getOrNull() ?: continue
                val empMobile = emp.Mobile.orEmpty().trim()
                val empFirst = emp.FirstName.orEmpty().trim()
                val empLast = emp.LastName.orEmpty().trim()
                val mobileMatch = mobile.isNotEmpty() && empMobile.isNotEmpty() &&
                    mobile.equals(empMobile, ignoreCase = true)
                val nameMatch = first.isNotEmpty() &&
                    first.equals(empFirst, ignoreCase = true) &&
                    last.equals(empLast, ignoreCase = true)
                if (mobileMatch || nameMatch) {
                    localCustId = row.localId
                    break
                }
            }
        }

        val customer = req.Customer.copy(
            FirstName = first,
            LastName = last,
            Mobile = mobile,
            Id = req.Customer.Id.takeIf { it > 0 } ?: 0,
            Remark = OfflineCustomerMatch.mergeRemark(req.Customer.Remark, localCustId),
        )
        return req.copy(
            CustomerId = (customer.Id.takeIf { it > 0 } ?: 0).toString(),
            Customer = customer,
            CustomOrderItem = req.CustomOrderItem.map { item ->
                item.copy(
                    CustomerId = customer.Id.takeIf { it > 0 } ?: 0,
                    CustomerName = display.ifBlank { item.CustomerName },
                )
            },
        )
    }

    /** Unique chained work: sync offline customers, then offline orders. */
    fun enqueueCustomerThenOrderSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val employeeWork = OneTimeWorkRequestBuilder<PendingEmployeeSyncWorker>()
            .setConstraints(constraints)
            .build()
        val orderWork = OneTimeWorkRequestBuilder<PendingOrderSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .beginUniqueWork(
                "pending_customer_order_sync",
                ExistingWorkPolicy.REPLACE,
                employeeWork,
            )
            .then(orderWork)
            .enqueue()
    }

    fun enqueuePendingSync(context: Context) {
        // Keep API: always run customer→order chain so CustomerId is ready.
        enqueueCustomerThenOrderSync(context)
    }

    @Inject
    lateinit var pendingEmployeeDao: PendingEmployeeDao // constructor me inject karo

    fun saveEmployeeOffline(
        req: AddEmployeeRequest,
        successMessage: String = "Customer saved offline. Will sync when online.",
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val localId = UUID.randomUUID().toString()

            val json = Gson().toJson(req)
            pendingEmployeeDao.upsert(
                PendingEmployeeEntity(
                    localId = localId,
                    clientCode = req.ClientCode.orEmpty(),
                    payloadJson = json,
                    status = "PENDING"
                )
            )

            // Tag remark so we can correlate; keep selectable in UI with Id=0.
            val emp = req.toEmployeeList().copy(
                Remark = listOf(req.Remark.orEmpty(), "LOCAL_CUST:$localId")
                    .filter { it.isNotBlank() }
                    .joinToString(" | ")
            )
            repository.insertEmployeeToRoom(emp)

            val localList = repository.getAllEmpListFromRoom(
                ClientCodeRequest(req.ClientCode.orEmpty())
            )
            val offlineResponse = EmployeeResponse(
                FirstName = req.FirstName.orEmpty(),
                LastName = req.LastName.orEmpty(),
                Mobile = req.Mobile.orEmpty(),
                Email = req.Email.orEmpty(),
                ClientCode = req.ClientCode.orEmpty(),
                City = req.City.orEmpty(),
                CurrAddState = req.CurrAddState.orEmpty(),
                Country = req.Country.orEmpty(),
                GstNo = req.GstNo.orEmpty(),
                PanNo = req.PanNo.orEmpty(),
                Id = 0,
                Remark = OfflineCustomerMatch.localCustTag(localId),
            )

            withContext(Dispatchers.Main) {
                _empListFlow.value = UiState.Success(localList)
                _addEmpResponse.value = Resource.Success(offlineResponse, successMessage)
            }

            enqueueCustomerThenOrderSync(context1)
        }
    }

    fun enqueuePendingEmployeeSync(context: Context) {
        enqueueCustomerThenOrderSync(context)
    }
    private fun CustomOrderRequest.toLocalResponse(
        localId: String,
        fallbackCustomer: Customer ,
    ): CustomOrderResponse {

        fun s(v: Any?): String = v?.toString()?.trim().orEmpty()
        fun s0(v: Any?): String = s(v).ifBlank { "0" }
        fun s00(v: Any?): String = s(v).ifBlank { "0.0" }

        val now = LocalDateTime.now().toString()
        val orderNoStr = this.OrderNo?.toString()?.takeIf { it.isNotBlank() } ?: "LOCAL-$localId"

        return CustomOrderResponse(
            Id = 0, // autoGenerate true, but Room will ignore if 0
            CustomOrderId = 0,
            CustomerId = this.CustomerId?.toIntOrNull() ?: 0,
            ClientCode = s(this.ClientCode),
            OrderId = this.OrderId ?: 0,

            TotalAmount = s00(this.TotalAmount),
            PaymentMode = s(this.PaymentMode),
            Offer = this.Offer, // nullable
            Qty = s0(this.Qty),

            GST = s(this.GST), // your model wants String ("true"/"false" or "1"/"0")
            OrderStatus = this.OrderStatus?.takeIf { it.isNotBlank() } ?: "PENDING (OFFLINE)",

            MRP = this.MRP, // nullable
            VendorId = this.VendorId,
            TDS = this.TDS,
            PurchaseStatus = this.PurchaseStatus,

            GSTApplied = s(this.GSTApplied),
            Discount = s00(this.Discount),

            TotalNetAmount = s00(this.TotalNetAmount),
            TotalGSTAmount = s00(this.TotalGSTAmount),
            TotalPurchaseAmount = s00(this.TotalPurchaseAmount),

            ReceivedAmount = s00(this.ReceivedAmount),
            TotalBalanceMetal = s00(this.TotalBalanceMetal),
            BalanceAmount = s00(this.BalanceAmount),
            TotalFineMetal = s00(this.TotalFineMetal),

            CourierCharge = this.CourierCharge,
            SaleType = this.SaleType,

            OrderDate = this.OrderDate?.toString()?.takeIf { it.isNotBlank() } ?: now,
            OrderCount = s0(this.OrderCount),

            AdditionTaxApplied = s(this.AdditionTaxApplied),
            CategoryId = this.CategoryId ?: 0,
            OrderNo = orderNoStr,

            DeliveryAddress = this.DeliveryAddress,
            BillType = s(this.BillType),

            UrdPurchaseAmt = this.UrdPurchaseAmt,
            BilledBy = s(this.BilledBy),
            SoldBy = s(this.SoldBy),

            CreditSilver = this.CreditSilver,
            CreditGold = this.CreditGold,
            CreditAmount = this.CreditAmount,

            BalanceAmt = s00(this.BalanceAmt),
            BalanceSilver = this.BalanceSilver,
            BalanceGold = this.BalanceGold,

            TotalSaleGold = this.TotalSaleGold,
            TotalSaleSilver = this.TotalSaleSilver,
            TotalSaleUrdGold = this.TotalSaleUrdGold,
            TotalSaleUrdSilver = this.TotalSaleUrdSilver,

            FinancialYear = s(this.FinancialYear),
            BaseCurrency = s(this.BaseCurrency),

            TotalStoneWeight = s00(this.TotalStoneWeight),
            TotalStoneAmount = s00(this.TotalStoneAmount),
            TotalStonePieces = s0(this.TotalStonePieces),

            TotalDiamondWeight = s00(this.TotalDiamondWeight),
            TotalDiamondPieces = s0(this.TotalDiamondPieces),
            TotalDiamondAmount = s00(this.TotalDiamondAmount),

            FineSilver = s00(this.FineSilver),
            FineGold = s00(this.FineGold),

            DebitSilver = this.DebitSilver,
            DebitGold = this.DebitGold,

            PaidMetal = s00(this.PaidMetal),
            PaidAmount = s00(this.PaidAmount),

            TotalAdvanceAmt = this.TotalAdvanceAmt,
            TaxableAmount = s00(this.TaxableAmount),
            TDSAmount = this.TDSAmount,

            CreatedOn = now,
            LastUpdated = now,
            StatusType = true,

            FineMetal = s00(this.FineMetal),
            BalanceMetal = s00(this.BalanceMetal),
            AdvanceAmt = s00(this.AdvanceAmt),
            PaidAmt = s00(this.PaidAmt),
            TaxableAmt = s00(this.TaxableAmt),

            GstAmount = s00(this.GstAmount),
            GstCheck = s(this.GstCheck),

            Category = s(this.Category),
            TDSCheck = s(this.TDSCheck),

            // ✅ important: store localId inside remark (since model has no localId field)
            Remark = (this.Remark ?: "").ifBlank { "LOCAL_ID:$localId" }
                .let { r -> if (r.contains("LOCAL_ID:")) r else "$r | LOCAL_ID:$localId" },

            OrderItemId = this.OrderItemId?: 0,
            StoneStatus = this.StoneStatus,
            DiamondStatus = this.DiamondStatus,
            BulkOrderId = this.BulkOrderId,

            CustomOrderItem = this.CustomOrderItem ?: emptyList(),
            Payments = this.Payments ?: emptyList(),

            // ⚠️ Customer is NON-NULL in your model
            Customer = this.Customer ?: fallbackCustomer,

            syncStatus = false,

            // non-null String
            ProductName = ""?.toString().orEmpty(),
            HallmarkAmount=this.HallmarkAmount.toString(),
            WeightCategories=this.WeightCatogories.toString(),
            SKUId=this.SKUId?:0,
            RfidCode = this.RfidCode.toString(),
            TidNumber=this.TidNumber

        )
    }


    suspend fun getPendingOrderResponses(clientCode: String): List<CustomOrderResponse> {
        val pending = pendingOrderDao.getPendingByClientCode(clientCode)
        return pending.mapNotNull { entity ->
            try {
                // Deleted / pending-delete rows must not reappear in the list on refresh.
                if (entity.isDeleted || entity.op.equals("DELETE", ignoreCase = true)) {
                    return@mapNotNull null
                }
                val req = Gson().fromJson(entity.payloadJson, CustomOrderRequest::class.java)

                val fallbackCustomer = req.Customer

                req.toLocalResponse(entity.localId, fallbackCustomer)
            } catch (e: Exception) {
                null
            }
        }
    }


    suspend fun updateOfflineCreatedOrder(localId: String, updatedReq: CustomOrderRequest) {
        val row = pendingOrderDao.getByLocalId(localId) ?: return
        // serverOrderId null => CREATE stays CREATE
        pendingOrderDao.updatePayload(
            localId = localId,
            payloadJson = Gson().toJson(updatedReq),
            op = "CREATE"
        )
    }

    suspend fun updateSyncedOrderOffline(serverOrderId: Int, clientCode: String, updatedReq: CustomOrderRequest) {
        val localId = "SRV-$serverOrderId"   // ✅ stable id
        pendingOrderDao.upsert(
            PendingOrderEntity(
                localId = localId,
                clientCode = clientCode,
                op = "UPDATE",
                payloadJson = Gson().toJson(updatedReq),
                serverOrderId = serverOrderId,
                status = "PENDING",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                customerId =updatedReq.Customer.Id,
                lastError = "ERROR",
                attempts = 1,
                serverOrderNo = serverOrderId.toString()
            )
        )
    }

    fun deleteOrderOffline(order: CustomOrderResponse, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {

            val clientCode = order.ClientCode.orEmpty()
                .ifBlank { /* keep going with empty — localId still works */ "" }

            // ✅ localId decide
            val localId = when {
                order.CustomOrderId == 0 && order.OrderNo?.startsWith("LOCAL-") == true ->
                    order.OrderNo!!.removePrefix("LOCAL-")

                order.CustomOrderId > 0 ->
                    "SRV-${order.CustomOrderId}"

                else -> {
                    // fallback: try remark LOCAL_ID
                    extractLocalIdFromRemark(order.Remark)
                }
            }

            if (localId.isNullOrBlank()) {
                // Still remove from UI so user isn't stuck; cannot queue sync without id.
                withContext(Dispatchers.Main) {
                    removeOrderFromUi(order)
                    onResult?.invoke(false)
                }
                return@launch
            }

            val existing = pendingOrderDao.getByLocalId(localId)
            val hasServerId = (existing?.serverOrderId ?: 0) > 0 || order.CustomOrderId > 0

            if (!hasServerId) {
                // Never synced CREATE — remove locally only (no server delete needed).
                pendingOrderDao.hardDeleteByLocalId(localId)
            } else {
                val serverId = existing?.serverOrderId?.takeIf { it > 0 }
                    ?: order.CustomOrderId.takeIf { it > 0 }
                pendingOrderDao.upsert(
                    PendingOrderEntity(
                        localId = localId,
                        clientCode = clientCode.ifBlank { order.ClientCode.orEmpty() },
                        customerId = existing?.customerId ?: order.CustomerId,
                        payloadJson = existing?.payloadJson
                            ?: Gson().toJson(order.toRequest()),
                        status = "PENDING",
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        op = "DELETE",
                        serverOrderId = serverId,
                        serverOrderNo = existing?.serverOrderNo ?: order.OrderNo,
                        isDeleted = true,
                        attempts = existing?.attempts ?: 0,
                        lastError = null,
                        localCustomerKey = existing?.localCustomerKey.orEmpty(),
                        customerFirstName = existing?.customerFirstName.orEmpty(),
                        customerLastName = existing?.customerLastName.orEmpty(),
                        customerMobile = existing?.customerMobile.orEmpty(),
                    )
                )
                if (serverId != null && serverId > 0) {
                    repository.deleteOrderFromCache(
                        clientCode.ifBlank { order.ClientCode.orEmpty() },
                        serverId
                    )
                }
            }

            withContext(Dispatchers.Main) {
                removeOrderFromUi(order)
                onResult?.invoke(true)
            }
        }
    }

    private fun extractLocalIdFromRemark(remark: String?): String? {
        if (remark.isNullOrBlank()) return null
        val key = "LOCAL_ID:"
        val idx = remark.indexOf(key)
        if (idx == -1) return null
        return remark.substring(idx + key.length).trim().split(" ", "|").firstOrNull()
    }






}








