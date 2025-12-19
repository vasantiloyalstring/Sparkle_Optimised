package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.google.gson.Gson
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutDetails
import com.loyalstring.rfid.data.model.sampleOut.SampleOutFields
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.DeliveryChallanViewModel
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.ProductListViewModel
import com.loyalstring.rfid.viewmodel.SampleInViewModel
import com.loyalstring.rfid.viewmodel.SampleOutViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.UiState

@SuppressLint("UnrememberedMutableState")
@Composable
fun SampleInScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    Id: Int? = null,
    SampleOutNo: String? =null
) {



    val viewModel: BulkViewModel = hiltViewModel()
    val orderViewModel: OrderViewModel = hiltViewModel()
    val context = LocalContext.current
    var selectedPower by remember { mutableStateOf(10) }
    var isScanning by remember { mutableStateOf(false) }
    //var showSuccessDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var firstPress by remember { mutableStateOf(false) }
    var shouldNavigateBack by remember { mutableStateOf(false) }
    val productListViewModel: ProductListViewModel = hiltViewModel()
    val sampleOutViewModel: SampleOutViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    val dailyRates by orderViewModel.getAllDailyRate.collectAsState()
    // Customer input fields
    var customerName by remember { mutableStateOf("") }
    var customerId by remember { mutableStateOf<Int?>(null) }
    var expandedCustomer by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<EmployeeList?>(null) }
    var showInvoiceDialog by remember { mutableStateOf(false) }

    var itemCode by remember { mutableStateOf(TextFieldValue("")) }
    var showDropdownItemcode by remember { mutableStateOf(false) }
    val allItems by productListViewModel.productList.collectAsState(initial = emptyList())
    var selectedItem by remember { mutableStateOf<ItemCodeResponse?>(null) }
    val isLoading by orderViewModel.isItemCodeLoading.collectAsState()

    val branchList = singleProductViewModel.branches
    var sampleOutFields by remember { mutableStateOf<SampleOutFields?>(null) }
    val productList = remember { mutableStateListOf<SampleOutDetails>() }
    val deliveryChallanViewModel: DeliveryChallanViewModel = hiltViewModel()
    val tags by viewModel.scannedTags.collectAsState()
    val scanTrigger by viewModel.scanTrigger.collectAsState()
    var baseTotal by remember { mutableStateOf(0.0) }
    var gstAmount by remember { mutableStateOf(0.0) }
    var totalWithGst by remember { mutableStateOf(0.0) }


    val errorMsg by sampleOutViewModel.error.collectAsState()
    val loading by sampleOutViewModel.loading.collectAsState()

    val customerSuggestions by orderViewModel.empListFlow.collectAsState(UiState.Loading)

    val viewModelSampleIn: SampleInViewModel = hiltViewModel()
    val challanList by viewModelSampleIn.sampleInList.collectAsState()

    val filteredCustomers by derivedStateOf {
        when (customerSuggestions) {
            is UiState.Success<*> -> {
                val items = (customerSuggestions as UiState.Success<Any?>).data as List<EmployeeList>
                if (customerName.isBlank()) {
                    items.take(20) // show first 20 when no input
                } else {
                    items.filter {
                        val fullName = "${it.FirstName} ${it.LastName}".trim().lowercase()
                        fullName.contains(customerName.trim().lowercase())
                    }.take(20)
                }
            }
            else -> emptyList()
        }
    }

    LaunchedEffect(customerSuggestions) {
        if (customerSuggestions is UiState.Success) {

            val data = (customerSuggestions as UiState.Success<List<EmployeeList>>).data
            Log.d("CustomerList", Gson().toJson(data))


        }
    }

    LaunchedEffect(scanTrigger) {
        scanTrigger?.let { type ->
            when (type) {
                "scan" -> if (productList.size != 1) viewModel.startScanning(30)
                "barcode" -> viewModel.startBarcodeScanning(context)
            }
            viewModel.clearScanTrigger()
        }
    }
    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Sample In",
                navigationIcon = {
                    IconButton(
                        onClick = { shouldNavigateBack = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {

                },
                showCounter = true,
                selectedCount = selectedPower,
                onCountSelected = {
                    selectedPower = it
                }
            )
        },
        bottomBar = {


            ScanBottomBar(

                onSave = {
                  },
                onList = { navController.navigate(Screens.SampleInListScreen.route) },
                onScan = {
                    viewModel.startSingleScan(20)
                },
                onGscan = {
                    if (isScanning) {
                        viewModel.stopScanning()
                        isScanning = false
                    } else {
                        viewModel.startScanning(selectedPower)
                        isScanning = true
                    }

                    // viewModel.toggleScanning(selectedPower)

                },
                onReset = {
                    firstPress = false

                    resetAllFields(
                        onResetCustomerName = { customerName = it },
                        onResetCustomerId = { customerId = it },
                        onResetSelectedCustomer = { selectedCustomer = it },
                        onResetExpandedCustomer = { expandedCustomer = it },
                        onResetItemCode = { itemCode = it },
                        onResetSelectedItem = { selectedItem = it },
                        onResetDropdownItemcode = { showDropdownItemcode = it },
                        onResetProductList = { productList.clear() },
                        onResetScanning = { isScanning = it },
                        viewModel = viewModel,
                        deliveryChallanViewModel = deliveryChallanViewModel

                    ) // 🧹 Clear everything in one call
                    viewModel.resetProductScanResults()
                    viewModel.stopBarcodeScanner()
                },
                isScanning = isScanning,
                isEditMode = isEditMode,
                isScreen=false

            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            val coroutineScope = rememberCoroutineScope()

            CustomerNameInputData(
                customerName = customerName,
                onCustomerNameChange = { customerName = it },
                onClear = { customerName = "" },
                onAddCustomerClick = { /* open popup handled internally */ },
                filteredCustomers = filteredCustomers,
                isLoading = false,
                onCustomerSelected = { customerName = "${it.FirstName.orEmpty()} ${it.LastName.orEmpty()}".trim()
                    customerId = it.Id ?: 0},
                coroutineScope = coroutineScope,
                fetchSuggestions = {orderViewModel.getAllEmpList(clientCode = employee?.clientCode.toString()) },
                expanded = false,
                onSaveCustomer = { request -> orderViewModel.addEmployee(request) },
                employeeClientCode = employee?.clientCode,
                employeeId = employee?.employeeId?.toString()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🔹 Left side → Enter RFID / Itemcode box
               Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(35.dp) // ✅ Adjusted height to align with button
                ) {
                    SampleOutInputRowData(
                        itemCode = itemCode,
                        onItemCodeChange = { itemCode = it },
                        showDropdown = showDropdownItemcode,
                        setShowDropdown = { showDropdownItemcode = it },
                        context = context,
                        onScanClicked = {   // Start RFID scan when QR icon clicked
                            viewModel.startBarcodeScanning(context)
                        },
                        onClearClicked = { itemCode = TextFieldValue("") },
                        filteredList = challanList,
                        isLoading = isLoading,
                        onItemSelected = { selectedItem = it }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 🔹 Right side → Invoice Fields button
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .height(35.dp) // ✅ same height as RFID box
                        .gradientBorderBox()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            showInvoiceDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Invoice Fields",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.filter_gary),
                            contentDescription = "Add",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            SampleOutListTableComponent(
                productList = productList,
                onTotalsChange = { base, gst, final ->
                    baseTotal = base
                    gstAmount = gst
                    totalWithGst = final
                },
                onItemUpdated = { index, updated ->
                    // ✅ sirf ek item update
                    productList[index] = updated
                },
                onDeleteItem = { index ->
                    if (index in productList.indices) {
                        productList.removeAt(index)   // ✅ yaha se item hatao
                    }
                }
            )




        }
    }
}