// ScanDisplayScreen.kt
package com.loyalstring.rfid.ui.screens
import kotlinx.coroutines.yield
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.MainActivity
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.stockVerification.Item
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.GradientButton
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.ProductListViewModel
import com.loyalstring.rfid.viewmodel.ScanDisplayViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.delay


// column widths
val colCategoryWidth = 72.dp
val colQtyWidth = 44.dp
val colWeightWidth = 62.dp
val colMatchedQtyWidth = 44.dp
val colMatchedWtWidth = 62.dp
val colStatusWidth = 54.dp
val colDesignNameWidth = 80.dp
val colRfidWidth = 100.dp
val colItemCodeWidth = 90.dp
val colGWtWidth = 50.dp
val colStatusIconWidth = 54.dp

private const val MENU_ALL = "ALL"
private const val MENU_MATCHED = "MATCHED"
private const val MENU_UNMATCHED = "UNMATCHED"
private const val MENU_SEARCH = "SEARCH"

@SuppressLint("UnrememberedMutableState")
@Composable
fun ScanDisplayScreen(onBack: () -> Unit, navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
    val productListViewModel: ProductListViewModel = hiltViewModel()
    val scanDisplayViewModel: ScanDisplayViewModel = hiltViewModel()
    val bulkViewModel: BulkViewModel = hiltViewModel()
    val context: Context = LocalContext.current
    var shouldNavigateBack by remember { mutableStateOf(false) }
    val emailStatus by scanDisplayViewModel.emailStatus.collectAsState()

    val loading by scanDisplayViewModel.loading.collectAsState()
    val error by scanDisplayViewModel.error.collectAsState()
    val successMsg by scanDisplayViewModel.successMsg.collectAsState()
    var isSending by remember { mutableStateOf(false) }





    // Handle back navigation with delay to allow ripple animation to complete
    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50) // Small delay for ripple animation
            onBack()
        }
    }

    var showRfidDialog by remember { mutableStateOf(false) }
    if (showRfidDialog) {
        AlertDialog(
            onDismissRequest = {
                showRfidDialog = false
                navController.popBackStack()
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    GradientButton(
                        text = "OK",
                        onClick = {
                            showRfidDialog = false
                            navController.popBackStack()
                        }
                    )
                }
            },
            title = { Text("Missing Data", fontFamily = poppins, fontSize = 18.sp) },
            text = {
                Text(
                    "RFID sheet not uploaded. Please contact administrator.",
                    fontFamily = poppins,
                    fontSize = 14.sp
                )
            }
        )
    }
    val isLoading by productListViewModel.isLoading.collectAsState()
    val allItems by productListViewModel.productList.collectAsState(initial = emptyList())
    // incoming filter from previous screen (branch/box/counter/exhibition)
    val filterTypeName = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<String>("filterType")
    val filterValue = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<String>("filterValue")

    LaunchedEffect(filterTypeName, filterValue) {
        if (!filterTypeName.isNullOrEmpty() && !filterValue.isNullOrEmpty()) {
            bulkViewModel.setFilteredItemsByType(filterTypeName, filterValue)
        }
    }


    val navFilteredItems = remember(allItems, filterTypeName, filterValue) {
        if (filterTypeName.isNullOrEmpty() || filterValue.isNullOrEmpty()) {
            allItems
        } else {
            when (filterTypeName.lowercase()) {
                "box" -> allItems.filter { it.boxName == filterValue }
                "counter" -> allItems.filter { it.counterName == filterValue }
                "branch" -> allItems.filter { it.branchName == filterValue }
                "exhibition" -> allItems.filter { it.branchType == filterTypeName && it.branchName == filterValue }
                else -> allItems
            }
        }
    }
    /*  remember(allItems, filterTypeName, filterValue) {
          if (filterTypeName.isNullOrEmpty() || filterValue.isNullOrEmpty()) {
              allItems
          } else {
              when (filterTypeName.lowercase()) {
                  "box" -> allItems.filter { it.boxName == filterValue }
                  "counter" -> allItems.filter { it.counterName == filterValue }
                  "branch" -> allItems.filter { it.branchName == filterValue }
                  "branch" -> allItems.filter { it.branchName == filterValue }
                  "exhibition" -> allItems.filter { it.branchType == filterTypeName && it.branchName == filterValue }
                  else -> allItems
              }
          }
      }*/

    LaunchedEffect(emailStatus) {
        when (emailStatus) {
            "success" -> Toast.makeText(context, "Email Sent successfully", Toast.LENGTH_LONG).show()
            null -> Unit
            else -> Toast.makeText(context, emailStatus ?: "Error", Toast.LENGTH_LONG).show()
        }
    }


    // Multi-select filters
    val selectedCategories = remember { mutableStateListOf<String>() }
    val selectedProducts = remember { mutableStateListOf<String>() }
    val selectedDesigns = remember { mutableStateListOf<String>() }


// stable snapshot keys
    val selectedCategoriesKey = selectedCategories.toList()
    val selectedProductsKey = selectedProducts.toList()
    val selectedDesignsKey = selectedDesigns.toList()


    val allCategories = remember(navFilteredItems) {
        navFilteredItems.mapNotNull { it.category }.distinct().sorted()
    }


    val allProducts = remember(navFilteredItems, selectedCategoriesKey) {
        navFilteredItems
            .filter { selectedCategoriesKey.isEmpty() || it.category in selectedCategoriesKey }
            .mapNotNull { it.productName }
            .distinct()
            .sorted()
    }


    val allDesigns = remember(navFilteredItems, selectedCategoriesKey, selectedProductsKey) {
        navFilteredItems
            .filter { selectedCategoriesKey.isEmpty() || it.category in selectedCategoriesKey }
            .filter { selectedProductsKey.isEmpty() || it.productName in selectedProductsKey }
            .mapNotNull { it.design }
            .distinct()
            .sorted()
    }


    var currentLevel by rememberSaveable { mutableStateOf("Category") }
    var currentCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var currentProduct by rememberSaveable { mutableStateOf<String?>(null) }
    var currentDesign by rememberSaveable { mutableStateOf<String?>(null) }

    val scannedFiltered by bulkViewModel.scannedFilteredItems

  //  val scannedFiltered by bulkViewModel.scannedFilteredItems.collectAsState(initial = emptySet())





    val matchedEpcs by bulkViewModel.matchedEpcSet.collectAsState(initial = emptySet())
/*
    val summaryItems by remember(navFilteredItems, matchedEpcs) {
        derivedStateOf {
            navFilteredItems.map { original ->
                val keyEpc = original.epc?.trim()?.uppercase()
                val status =
                    if (keyEpc != null && matchedEpcs.contains(keyEpc)) "Matched" else "Unmatched"
                original.copy(scannedStatus = status)
            }
        }
    }*/

    val summaryItems by remember(navFilteredItems, matchedEpcs) {
        derivedStateOf {
            navFilteredItems.map { original ->
                val key = original.tagKey() // ✅ EPC or RFID both
                val status = if (key != null && matchedEpcs.contains(key)) "Matched" else "Unmatched"
                original.copy(scannedStatus = status)
            }
        }
    }


    var showMenu by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf("Category") }

    var selectedMenu by rememberSaveable { mutableStateOf(MENU_ALL) }


    var selectedItem by remember { mutableStateOf<BulkItem?>(null) }
    var showItemDialog by remember { mutableStateOf(false) }


    var selectedPower by remember { mutableIntStateOf(30) }

    LaunchedEffect(Unit) {
        selectedPower = UserPreferences.getInstance(context).getInt(
            UserPreferences.KEY_INVENTORY_COUNT,
            30
        )
    }
    var _isResetting by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var savedEmails by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedEmail by remember { mutableStateOf<String?>(null) }
    var newEmail by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

 //   val scannedFiltered by bulkViewModel.scannedFilteredItems.collectAsState(initial = emptySet())

    val effectiveMatchedSet by remember(isScanning, scannedFiltered, matchedEpcs) {
        derivedStateOf {
            if (isScanning) {
                scannedFiltered
                    .mapNotNull { it.tagKey() }   // EPC/RFID normalize
                    .toSet()
            } else {
                matchedEpcs
            }
        }
    }

    LaunchedEffect(Unit) {
        savedEmails = scanDisplayViewModel.getAllEmails()
    }

    LaunchedEffect(key1 = _isResetting) {
        if (_isResetting) {
            delay(1000L) // Add your desired delay here
            _isResetting = false
        }
    }

  //  val scannedFiltered by bulkViewModel.scannedFilteredItems
    //val matchedEpcs by bulkViewModel.matchedEpcSet.collectAsState(initial = emptySet())
    val currentPage by bulkViewModel.currentPage.collectAsState()
    val pageSize by bulkViewModel.pageSize.collectAsState()
    val totalItems by bulkViewModel.totalItems.collectAsState()
    val isLoadingPage by bulkViewModel.isLoadingPage.collectAsState()

    // ✅ show toast ONLY once
    LaunchedEffect(successMsg) {
        successMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
           // scanDisplayViewModel.clearSuccessMsg()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
           // scanDisplayViewModel.clearError()
        }
    }

    // scopeItems overlay scanned status on filtered base set

    val scopeItems by remember(
        navFilteredItems,
        selectedCategoriesKey,
        selectedProductsKey,
        selectedDesignsKey,
        matchedEpcs,           // ✅ ADD THIS
        scannedFiltered
    ) {
        derivedStateOf {
            if (navFilteredItems.isEmpty()) emptyList()
            else {
                navFilteredItems.mapNotNull { original ->
                  /*  val keyEpc = original.epc?.trim()?.uppercase()
                    val status =
                        if (keyEpc != null && matchedEpcs.contains(keyEpc)) "Matched" else "Unmatched"*/

                    val key = original.tagKey()
                    val status = if (key != null && matchedEpcs.contains(key)) "Matched" else "Unmatched"

                    val withScan = original.copy(scannedStatus = status)

                    if ((selectedCategoriesKey.isEmpty() || withScan.category in selectedCategoriesKey) &&
                        (selectedProductsKey.isEmpty() || withScan.productName in selectedProductsKey) &&
                        (selectedDesignsKey.isEmpty() || withScan.design in selectedDesignsKey)
                    ) withScan else null
                }
            }
        }
    }


    // displayItems respects selectedMenu and sticky unmatched ids (existing logic preserved)

    val stickyUnmatchedIds by bulkViewModel.filteredUnmatchedIds.collectAsState()

    val displayItems by remember(scopeItems, selectedMenu, stickyUnmatchedIds) {
        derivedStateOf {
            if (scopeItems.isEmpty()) emptyList()
            else when (selectedMenu) {
                MENU_MATCHED -> scopeItems.filter { it.scannedStatus == "Matched" }

                MENU_UNMATCHED -> {
                    val unmatchedNow = scopeItems.filter { it.scannedStatus == "Unmatched" }
                    val sticky = scopeItems.filter {
                       /* val id = it.epc?.trim()?.uppercase()
                        id != null && stickyUnmatchedIds.contains(id)*/

                        val id = it.tagKey()
                        id != null && stickyUnmatchedIds.contains(id)
                    }
                    (unmatchedNow + sticky).distinctBy { it.tagKey() }
                }

                else -> scopeItems
            }
        }
    }


   /* val allMatched by remember(scopeItems) {
        derivedStateOf { scopeItems.isNotEmpty() && scopeItems.all { it.scannedStatus == "Matched" } }
    }*/

 /*   val currentSelectionItems by remember(displayItems, selectedCategoriesKey, selectedProductsKey, selectedDesignsKey) {
        derivedStateOf {
            displayItems.filter {
                (selectedCategoriesKey.isEmpty() || it.category in selectedCategoriesKey) &&
                        (selectedProductsKey.isEmpty() || it.productName in selectedProductsKey) &&
                        (selectedDesignsKey.isEmpty() || it.design in selectedDesignsKey)
            }
        }
    }*/

    val scanScopeItems by remember(scopeItems) {
        derivedStateOf { scopeItems }
    }

/*    val allMatchedForFilter by remember(scanScopeItems) {
        derivedStateOf {
            val matchable = scanScopeItems.filter { it.tagKey() != null }
            matchable.isNotEmpty() && matchable.all { it.scannedStatus == "Matched" }
        }
    }*/
    val scannedKeys by bulkViewModel.scannedKeySet

    val allMatchedForFilter by remember(isScanning, scanScopeItems, scannedKeys, matchedEpcs) {
        derivedStateOf {
            if (!isScanning) return@derivedStateOf false

            val filterKeys = scanScopeItems.mapNotNull { it.tagKey() }.toSet()
            if (filterKeys.isEmpty()) return@derivedStateOf false

            // ✅ stop when ALL filtered keys are scanned
            filterKeys.all { it in scannedKeys || it in matchedEpcs }
        }
    }


    /*    val allMatchedForSelection by remember(currentSelectionItems) {
            derivedStateOf {
                currentSelectionItems.isNotEmpty() &&
                        currentSelectionItems.all { it.scannedStatus == "Matched" }
            }
        }*/

    val activity = LocalContext.current as? MainActivity

    DisposableEffect(Unit) {
        val listener = object : ScanKeyListener {
            override fun onBarcodeKeyPressed() {
                bulkViewModel.startBarcodeScanning(context)
            }

            override fun onRfidKeyPressed() {
                if (!isScanning) {
                    isScanning = true
                    bulkViewModel.setFilteredItems(scanScopeItems)
                    bulkViewModel.startScanningInventory(selectedPower)
                } else {
                    isScanning = false
                    bulkViewModel.stopScanningAndCompute()
                }
            }
        }
        activity?.registerScanKeyListener(listener)
        onDispose { activity?.unregisterScanKeyListener() }
    }

    /*LaunchedEffect(isScanning, allMatched) {
        if (isScanning && allMatched) {
            bulkViewModel.stopScanningAndCompute()
            isScanning = false
            Toast.makeText(context, "All items matched. Scan stopped.", Toast.LENGTH_SHORT).show()
        }
    }*/

   /* LaunchedEffect(Unit) {
        snapshotFlow { isScanning to allMatched }
            .collect { (isScanningValue, allMatchedValue) ->

                if (isScanningValue && allMatchedValue) {

                    currentCategory = null
                    currentProduct = null
                    currentDesign = null
                    selectedCategories.clear()
                    selectedProducts.clear()
                    selectedDesigns.clear()

                    bulkViewModel.stopScanningAndCompute()
                    isScanning = false

                    Toast.makeText(context, "All items matched. Scan stopped.", Toast.LENGTH_SHORT).show()
                }
            }
    }*/
    /*LaunchedEffect(Unit) {
        snapshotFlow { isScanning to allMatchedForFilter }
            .collect { (isScanningValue, allMatchedValue) ->
                Log.d("AUTO_STOP", "isScanning=$isScanningValue allMatched=$allMatchedValue matched=${scanScopeItems.count{it.scannedStatus=="Matched"}} total=${scanScopeItems.size}")
                if (isScanningValue && allMatchedValue) {
                    bulkViewModel.stopScanningAndCompute()
                    isScanning = false
                    selectedMenu = MENU_MATCHED
                    Toast.makeText(context, "Filtered items matched. Scan stopped.", Toast.LENGTH_SHORT).show()
                }
            }
    }*/

    LaunchedEffect(isScanning, allMatchedForFilter) {
        if (isScanning && allMatchedForFilter) {
            bulkViewModel.stopScanningAndCompute()
            isScanning = false
            selectedMenu = MENU_MATCHED
            Toast.makeText(context, "Filtered items matched. Scan stopped.", Toast.LENGTH_SHORT).show()
        }
    }


  /*  LaunchedEffect(isScanning, scopeItems.size) {
        if (isScanning && scopeItems.isEmpty()) {
            bulkViewModel.stopScanningAndCompute()
            isScanning = false
        }
    }*/

    LaunchedEffect(isScanning, scanScopeItems.size) {
        if (isScanning && scanScopeItems.isEmpty()) {
            bulkViewModel.stopScanningAndCompute()
            isScanning = false
        }
    }

    DisposableEffect(Unit) { onDispose { bulkViewModel.stopScanningAndCompute()

    } }



    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    LaunchedEffect(Unit) {
        employee?.clientCode?.let { singleProductViewModel.fetchAllDropdownData(ClientCodeRequest(it)) }
        //bulkViewModel.getAllItems()
        // Initialize with pagination
        scope.launch {
            bulkViewModel.loadTotalCount()
            bulkViewModel.loadPage(0) // Load first page
        }
    }

    // Avoid feeding UI projection back into VM during scanning to reduce churn

    Scaffold(
        topBar = {
            filterValue?.let {
                GradientTopBar(
                    title = it,
                    navigationIcon = {
                        IconButton(onClick = { shouldNavigateBack = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    showCounter = true,
                    selectedCount = selectedPower,
                    onCountSelected = { selectedPower = it },
                    titleTextSize = 20.sp
                )
            }
        },
        bottomBar = {
            Column {

              //  SummaryRow(currentLevel, displayItems, selectedMenu)
                SummaryRow(currentLevel, displayItems, selectedMenu)
                ScanBottomBarInventory(
                    onSave = { /* save */

                        val clientCode = employee?.clientCode       // 🔁 replace

                        val scannedItems = buildItemsForUpload(scopeItems)

                        Log.d("SAVE_DEBUG", "scopeItems=${scopeItems.size} " +
                                "matched=${scopeItems.count { it.scannedStatus == "Matched" }} " +
                                "unmatched=${scopeItems.count { it.scannedStatus == "Unmatched" }}")

                        Log.d("SAVE_DEBUG", "payload=${scannedItems.size} " +
                                "payloadMatched=${scannedItems.count { it.status == "Matched" }} " +
                                "payloadUnmatched=${scannedItems.count { it.status == "Unmatched" }}")
                        //val scannedItems = buildItemsForUpload(scopeItems, matchedEpcs)
                        //val scannedItems: List<Item> = buildItemsForUpload(scopeItems)// 🔁 replace with your actual list
                        scanDisplayViewModel.uploadStockVerification(clientCode.toString(), scannedItems, batchSize = 500)
                    },
                    onList = { showMenu = true },
                    onScan = {
                        if (!isScanning) {
                            isScanning = true
                            // bulkViewModel.resetScanResults()
                            bulkViewModel.setFilteredItems(scanScopeItems)   // ✅ only current scope
                            bulkViewModel.startScanningInventory(selectedPower)
                        } else {
                            isScanning = false
                            bulkViewModel.stopScanningAndCompute()
                        }
                    },
                    onEmail = {
                        showEmailDialog = true

                    },
                    onReset = {
                        Log.d("ScanDisplayScreen", "Calling")
                        if (!_isResetting) {
                            Log.d("ScanDisplayScreen", "Called")
                            _isResetting = true;
                            isScanning = false
                            try {
                                selectedCategories.clear()
                                selectedProducts.clear()
                                selectedDesigns.clear()

                                bulkViewModel.setFilteredItems(allItems) // ✅ reset to full DB
                                bulkViewModel.resetScanResults()

                                selectedMenu = MENU_ALL
                                currentLevel = "Category"
                                currentCategory = null
                                currentProduct = null
                                currentDesign = null
                                bulkViewModel.stopScanningAndCompute()
                                Log.d("ScanDisplayScreen", "Completed")
                            } finally {
                                Log.d("ScanDisplayScreen", "Finally")
                            }
                        }
                    },
                    isScanning = isScanning

                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(Modifier.padding(innerPadding)) {
                FilterRow(
                    selectedCategories,
                    selectedProducts,
                    selectedDesigns,
                    onCategoryClick = {
                        filterType = "Category"
                        // entering filter dialog resets drill-down navigation
                        currentLevel = "Category"
                        currentCategory = null
                        currentProduct = null
                        currentDesign = null
                        showDialog = true
                    },
                    onProductClick = {
                        if (selectedCategories.isNotEmpty() || allCategories.isNotEmpty()) {
                            filterType = "Product"
                            // entering filter dialog resets drill-down navigation
                            currentLevel = "Category"
                            currentCategory = null
                            currentProduct = null
                            currentDesign = null
                            showDialog = true
                        } else Toast.makeText(context, "Select category first", Toast.LENGTH_SHORT)
                            .show()
                    },
                    onDesignClick = {
                        if (selectedProducts.isNotEmpty() || allProducts.isNotEmpty()) {
                            filterType = "Design"
                            currentLevel = "Category"
                            currentCategory = null
                            currentProduct = null
                            currentDesign = null
                            showDialog = true
                        } else Toast.makeText(context, "Select product first", Toast.LENGTH_SHORT)
                            .show()
                    }
                )

                TableHeader(currentLevel)

                // ---------- L A Z Y   C O L U M N   (drill-down + multi-select friendly) ----------
                LazyColumn(modifier = Modifier.weight(1f)) {
                    when (currentLevel) {
                        "Category" -> {
                            val grouped = displayItems.groupBy { it.category ?: "Unknown" }
                            grouped.forEach { (label, items) ->
                                item {
                                    TableDataRow(TableRow(label, items), currentLevel) {
                                        // drill down to product for the chosen category (single-select drill)
                                        currentCategory = label
                                        selectedCategories.clear()
                                        selectedCategories.add(label)
                                        selectedProducts.clear()
                                        selectedDesigns.clear()
                                        currentLevel = "Product"
                                    }
                                }
                            }
                        }

                        "Product" -> {
                            val grouped = displayItems
                                .filter { selectedCategories.isEmpty() || it.category in selectedCategories }
                                .groupBy { it.productName ?: "Unknown" }

                            grouped.forEach { (label, items) ->
                                item {
                                    TableDataRow(TableRow(label, items), currentLevel) {
                                        // when clicking product row, drill to design level
                                        currentProduct = label
                                        if (!selectedProducts.contains(label)) selectedProducts.add(
                                            label
                                        )
                                        selectedDesigns.clear()
                                        currentLevel = "Design"
                                    }
                                }
                            }
                        }

                        "Design" -> {
                            val grouped = displayItems
                                .filter {
                                    (selectedCategories.isEmpty() || it.category in selectedCategories) &&
                                            (selectedProducts.isEmpty() || it.productName in selectedProducts)
                                }
                                .groupBy { it.design ?: "Unknown" }

                            grouped.forEach { (label, items) ->
                                item {
                                    TableDataRow(TableRow(label, items), currentLevel) {
                                        currentDesign = label
                                        if (!selectedDesigns.contains(label)) selectedDesigns.add(
                                            label
                                        )
                                        currentLevel = "DesignItems"
                                    }
                                }
                            }
                        }

                        "DesignItems" -> {
                            val itemsList = displayItems.filter {
                                (selectedCategories.isEmpty() || it.category in selectedCategories) &&
                                        (selectedProducts.isEmpty() || it.productName in selectedProducts) &&
                                        (selectedDesigns.isEmpty() || it.design in selectedDesigns)
                            }

                            // Use pagination for large lists
                            items(
                                itemsList,
                                key = {
                                    it.epc ?: it.itemCode ?: it.design ?: it.hashCode().toString()
                                }) { item ->
                                DesignItemRow(item) { clickedItem ->
                                    selectedItem = clickedItem
                                    showItemDialog = true
                                }
                            }

                            // Load more items when reaching the end (for pagination)
                            if (itemsList.size >= pageSize && !isLoadingPage) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    bulkViewModel.loadNextPage()
                                                }
                                            }
                                        ) {
                                            Text("Load More Items")
                                        }
                                    }
                                }
                            }

                            // Show loading indicator
                            if (isLoadingPage) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
                // -----------------------------------------------------------------------------------
            }
        }
        // 🔹 Loader overlay (TOP LEVEL) - reuse existing shared loader (productListViewModel / bulkViewModel)
        val bulkIsLoading by bulkViewModel.isLoading.collectAsState()
        if (isLoading || bulkIsLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (bulkIsLoading) "Please wait..." else "Loading products...",
                        color = Color.White,
                        fontFamily = poppins
                    )
                }
            }
        }
    }

            if (showEmailDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isSending) showEmailDialog = false },

                    title = { Text("Send Report", fontFamily = poppins) },

                    text = {
                        Column {
                            if (savedEmails.isNotEmpty()) {
                                Text("Saved Emails:", fontFamily = poppins)
                                savedEmails.forEach { email ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isSending) {
                                                selectedEmail = email
                                                newEmail = email // optional: auto fill
                                            }
                                            .background(
                                                if (selectedEmail == email) Color.LightGray else Color.Transparent
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Text(email, fontFamily = poppins)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            OutlinedTextField(
                                value = newEmail,
                                onValueChange = { newEmail = it },
                                enabled = !isSending,
                                label = { Text("Add New Email", fontFamily = poppins) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },

                    confirmButton = {
                        GradientButtonNew(
                            text = "Send",
                            enabled = true,
                            isLoading = isSending,
                            onClick = {
                                val finalEmail = when {
                                    newEmail.isNotBlank() -> newEmail.trim()
                                    !selectedEmail.isNullOrBlank() -> selectedEmail!!.trim()
                                    else -> null
                                }

                                if (finalEmail.isNullOrBlank()) {
                                    Toast.makeText(context, "Please enter or select an email", Toast.LENGTH_SHORT).show()
                                    return@GradientButtonNew
                                }

                                // ✅ keep dialog open + show loader
                                isSending = true

                                // ✅ copy state now (important)
                                val itemsForReport = displayItems.toList()
                                val emailToSend = finalEmail
                                val newEmailToSave = newEmail.trim()

                                scope.launch {
                                    try {
                                        // ✅ allow UI to draw loader first
                                        yield()

                                        // save new email (IO)
                                        if (newEmailToSave.isNotBlank() && !savedEmails.contains(newEmailToSave)) {
                                            withContext(Dispatchers.IO) { scanDisplayViewModel.saveEmail(newEmailToSave) }
                                            savedEmails = withContext(Dispatchers.IO) { scanDisplayViewModel.getAllEmails() }
                                            selectedEmail = newEmailToSave
                                            newEmail = ""
                                        }

                                        // build + generate file (IO)
                                        val reportFile = withContext(Dispatchers.IO) {
                                            val summaryList = scanDisplayViewModel.buildSummary(itemsForReport)
                                            val (matched, unmatched) = scanDisplayViewModel.buildDetailedLists(itemsForReport)

                                            scanDisplayViewModel.generateScanReportExcel(
                                                context, summaryList, matched, unmatched
                                            )
                                        }

                                        // send email (IO)
                                        withContext(Dispatchers.IO) {
                                            scanDisplayViewModel.sendEmailHostinger(
                                                sendEmail = "android@loyalstring.com",
                                                sendPass = "Loyal@123",
                                                recipients = listOf(emailToSend),
                                                subject = "Inventory Scan Report",
                                                body = "<h2>Here is your scan report</h2><p>Details attached.</p>",
                                                type = "text/html",
                                                attachments = mapOf(reportFile.name to reportFile.absolutePath)
                                            )
                                        }

                                       // Toast.makeText(context, "Report sent to $emailToSend", Toast.LENGTH_LONG).show()
                                        showEmailDialog = false

                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSending = false
                                    }
                                }
                            }
                        )
                    },

                    dismissButton = {
                        GradientButtonNew(
                            text = "Cancel",
                            enabled = !isSending,
                            isLoading = false,
                            onClick = { showEmailDialog = false }
                        )
                    }
                )
            }



    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Success", fontFamily = poppins) },
            text = { Text("Email sent successfully!", fontFamily = poppins) },
            confirmButton = {
                GradientButton(
                    text = "OK",
                    onClick = { showSuccessDialog = false }
                )
            }
        )
    }

    // ---------- Filter Dialog ----------
    if (showDialog) {
        val items = when (filterType) {
            "Category" -> allCategories
            "Product" -> allProducts
            "Design" -> allDesigns
            else -> emptyList()
        }
        val selected = when (filterType) {
            "Category" -> selectedCategories
            "Product" -> selectedProducts
            "Design" -> selectedDesigns
            else -> mutableStateListOf()
        }

        FilterSelectionDialog(
            title = filterType,
            items = items,
            selectedItems = selected,
            onDismiss = { showDialog = false },
            onConfirm = {
                when (filterType) {
                    "Category" -> {
                        currentLevel = "Product"
                        currentCategory = null
                        selectedProducts.clear()
                        selectedDesigns.clear()
                    }
                    "Product" -> {
                        currentLevel = "Design"
                        currentProduct = null
                        selectedDesigns.clear()
                    }
                    "Design" -> {
                        currentLevel = "DesignItems"
                        currentDesign = null
                    }
                }

                // ❌ Don’t reset to allItems, just close dialog
                showDialog = false
            }

        )
    }

    // Menu (Matched/Unmatched/etc.)
/*
    val summaryItems by remember(navFilteredItems, matchedEpcs) {
        derivedStateOf {
            navFilteredItems.map { original ->
                val keyEpc = original.epc?.trim()?.uppercase()
                val status =
                    if (keyEpc != null && matchedEpcs.contains(keyEpc)) "Matched" else "Unmatched"
                original.copy(scannedStatus = status)
            }
        }
    }*/
   /* var matchedCount = remember(scopeItems) { scopeItems.count { it.scannedStatus == "Matched" } }
    var unmatchedCount = remember(scopeItems) { scopeItems.count { it.scannedStatus == "Unmatched" } }
    val totalCount = remember(scopeItems) { scopeItems.size }*/
    val matchedCount = remember(summaryItems) { summaryItems.count { it.scannedStatus == "Matched" } }
    val unmatchedCount = remember(summaryItems) { summaryItems.count { it.scannedStatus == "Unmatched" } }
    val totalCount = remember(summaryItems) { summaryItems.size }   // ✅ THIS LINE FIXED

    if (showMenu) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
                .clickable { showMenu = false }
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 60.dp, bottom = 70.dp)
                    .width(180.dp)
                    .fillMaxHeight()
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                VerticalMenu(
                    matchedCount = matchedCount,
                    unmatchedCount = unmatchedCount,
                    totalCount = totalCount
                ) { menuItem ->
                        when (menuItem.title) {
                            "UnMatched Items" -> {
                                scope.launch {
                                   /* bulkViewModel.setLoading(true)
                                    delay(1000)
                                    bulkViewModel.loadUnmatchedFast(scopeItems)
                                    selectedMenu = MENU_UNMATCHED
                                    currentLevel = "DesignItems"*/
                                    selectedMenu = MENU_UNMATCHED
                                   /* currentLevel = "DesignItems"

                                    currentCategory = null
                                    currentProduct = null
                                    currentDesign = null
                                    selectedCategories.clear()
                                    selectedProducts.clear()
                                    selectedDesigns.clear()*/

                                    // optional: sticky ids set (agar tumhe sticky behaviour chahiye)
                                    val ids = scopeItems.asSequence()
                                        .filter { it.scannedStatus.equals("Unmatched", true) }
                                        .mapNotNull { it.tagKey() }
                                        .distinct()
                                        .toList()
                                    bulkViewModel.rememberUnmatchedIds(ids)
                                }

                                // Compute unmatched ids off the main thread to avoid freezing UI on large lists
                                /*bulkViewModel.setLoading(true)
                            scope.launch(Dispatchers.IO) {
                                val ids = withContext(Dispatchers.Default) {
                                    scopeItems.asSequence()
                                        .filter { it.scannedStatus.equals("Unmatched", true) }
                                        .mapNotNull { it.epc?.trim()?.uppercase() }
                                        .distinct()
                                        .toList()
                                }
                                // Update ViewModel and UI on the main thread to avoid snapshot mutations from background
                                //bulkViewModel.rememberUnmatchedIds(ids) // Now updates _filteredUnmatchedIds directly on a background thread
                                withContext(Dispatchers.Main) { // Move UI updates back to Main thread
                                    selectedMenu = MENU_UNMATCHED
                                    // ✅ Always show items when unmatched mode
                                    currentLevel = "DesignItems"
                                    currentCategory = null
                                    currentProduct = null
                                    currentDesign = null

                                    selectedCategories.clear()
                                    selectedProducts.clear()
                                    selectedDesigns.clear()
                                    bulkViewModel.setLoading(false)
                                }
                            }*/

                            }

                            "Matched Items" -> {
                               /* selectedMenu = MENU_MATCHED
                                bulkViewModel.clearStickyUnmatched()

                                // ✅ Always show items when matched mode
                                currentLevel = "DesignItems"
                                currentCategory = null
                                currentProduct = null
                                currentDesign = null

                                selectedCategories.clear()
                                selectedProducts.clear()
                                selectedDesigns.clear()*/
                                selectedMenu = MENU_MATCHED
                                bulkViewModel.clearStickyUnmatched()

                               /* currentLevel = "DesignItems"
                                currentCategory = null
                                currentProduct = null
                                currentDesign = null
                                selectedCategories.clear()
                                selectedProducts.clear()
                                selectedDesigns.clear()*/
                            }

                            "Unlabelled Items" -> {
                                selectedMenu = MENU_ALL
                                bulkViewModel.clearStickyUnmatched()
                            }
                            // In ScanDisplayScreen
                            "Search" -> {
                                scope.launch {
                                    bulkViewModel.setLoading(true)
                                    // Remove artificial delay
                                    // delay(1000)

                                    val latestUnmatched = withContext(Dispatchers.Default) {
                                        scopeItems
                                            .filter { it.scannedStatus.equals("Unmatched", true) }
                                            .distinctBy { it.epc?.trim()?.uppercase() }
                                    }

                                    if (latestUnmatched.isNotEmpty()) {
                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                            "unmatchedItems",
                                            ArrayList(latestUnmatched)
                                        )
                                        navController.navigate("search_screen/unmatched") {
                                            // This is the callback from SearchScreen
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    } else {
                                        navController.navigate("search_screen/normal") {
                                            launchSingleTop = true
                                            restoreState = true
                                        }


                                    }
                                    //delay(1000)
                                    bulkViewModel.setLoading(false)
                                    //showToast(context, "End")
                                }

                            }
                        }

                    showMenu = false
                }
            }
        }
    }

    if (showItemDialog && selectedItem != null) {
        ItemDetailsDialog(item = selectedItem!!, onDismiss = { showItemDialog = false })
    }
    /*if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Select or Add Email") },
            text = {
                Column {
                    if (savedEmails.isNotEmpty()) {
                        Text("Saved Emails:")
                        savedEmails.forEach { emailEntity ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                                    .background(
                                        if (selectedEmail == emailEntity) Color.LightGray else Color.Transparent
                                    )
                                    .clickable {
                                        selectedEmail = emailEntity
                                        newEmail = emailEntity // ✅ populate text field
                                    }
                            ) {
                                Text(emailEntity, modifier = Modifier.padding(8.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Add New Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                GradientButton(
                    text = "Send",
                    onClick = {
                        val finalEmail = when {
                            newEmail.isNotBlank() -> newEmail
                            !selectedEmail.isNullOrBlank() -> selectedEmail
                            else -> null
                        }
                        if (finalEmail.isNullOrBlank()) {
                            Toast.makeText(
                                context,
                                "Please enter or select an email",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@GradientButton
                        }

                        scope.launch {
                            // save email if new
                            if (newEmail.isNotBlank() && !savedEmails.contains(newEmail)) {
                                scanDisplayViewModel.saveEmail(newEmail)
                                savedEmails = scanDisplayViewModel.getAllEmails()
                                selectedEmail = newEmail
                            }

                            // generate + send
                            val summaryList = scanDisplayViewModel.buildSummary(displayItems)
                            val (matched, unmatched) = scanDisplayViewModel.buildDetailedLists(
                                displayItems
                            )

                           *//* val pdfFile = scanDisplayViewModel.generateScanReportPdf(
                                context,
                                summaryList,
                                matched,
                                unmatched
                            )*//*

                            val pdfFile = scanDisplayViewModel.generateScanReportExcel(
                                context,
                                summaryList,
                                matched,
                                unmatched
                            )


                            scanDisplayViewModel.sendEmailHostinger(
                                sendEmail = "android@loyalstring.com",
                                sendPass = "Loyal@123",
                                recipients = listOf(finalEmail),
                                subject = "Inventory Scan Report",
                                body = "<h2>Here is your inventory scan report</h2><p>Details attached.</p>",
                                type = "text/html",
                                attachments = mapOf(pdfFile.name to pdfFile.absolutePath)
                            )
                        }

                        showEmailDialog = false
                    }
                )
            },
            dismissButton = {
                GradientButton(
                    text = "Cancel",
                    onClick = { showEmailDialog = false }
                )
            }
        )
    }*/

}

@Composable
fun GradientButtonNew(
    text: String,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFD32940), Color(0xFF5231A7))
    )

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradient, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Sending...", color = Color.White, fontFamily = poppins)
                } else {
                    Text(text, color = Color.White, fontFamily = poppins)
                }
            }
        }
    }
}


typealias StockItem = com.loyalstring.rfid.data.model.stockVerification.Item

private fun String.normalizeTagKey(): String? =
    this.trim().uppercase().takeIf { it.isNotBlank() }

private fun BulkItem.tagKey(): String? =
    (this.epc ?: this.rfid)?.normalizeTagKey()

private fun buildItemsForUpload(all: List<BulkItem>): List<Item> {
    return all
        .distinctBy { it.tagKey() ?: it.itemCode ?: it.hashCode().toString() }
        .map { b ->
            val status = when (b.scannedStatus?.trim()?.lowercase()) {
                "matched" -> "match"
                "unmatched" -> "unmatch"
                "match" -> "match"
                "unmatch" -> "unmatch"
                else -> "unmatch"
            }

            Log.d("@@","status"+status)
            b.toItem(status)
        }
        .filter { !it.itemCode.isNullOrBlank() }
}


/*
private fun buildItemsForUpload(all: List<BulkItem>): List<com.loyalstring.rfid.data.model.stockVerification.Item> {
    return all
        .distinctBy { it.epc?.trim()?.uppercase() ?: it.itemCode ?: it.hashCode().toString() }
        .map { it.toItem() }
        .filter { !it.itemCode.isNullOrBlank() }
}
*/


private fun BulkItem.toItem(status1: String): Item {
    return Item(
        itemCode = this.itemCode ?: "",
        status = status1,

        grossWeight = this.grossWeight?.toDoubleOrNull() ?: 0.0,
        netWeight = this.netWeight?.toDoubleOrNull() ?: 0.0,
        quantity = 1, // agar BulkItem me qty hai to yaha use kar dena

        counterName = this.counterName,
        categoryName = this.category,          // ✅ aapke BulkItem me category field hai (use ho rahi)
        productName = this.productName,
        designName = this.design,
        purityName = this.purity,          // agar purityName nahi hai to this.purity use karo
        companyName = "",
        branchName = this.branchName,

        counterId = this.counterId,
        categoryId = this.categoryId,
        productId = this.productId,
        designId = this.designId,
        purityId = 0,
        companyId =0,
        branchId = this.branchId
    )
}


@Composable
fun FilterRow(
    selectedCategories: List<String>,
    selectedProducts: List<String>,
    selectedDesigns: List<String>,
    onCategoryClick: () -> Unit,
    onProductClick: () -> Unit,
    onDesignClick: () -> Unit
) {
    val catLabel = selectedCategories.joinToString(", ").ifBlank { "Category" }
    val prodLabel = selectedProducts.joinToString(", ").ifBlank { "Product" }
    val designLabel = selectedDesigns.joinToString(", ").ifBlank { "Design" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(
            onClick = onCategoryClick,
            modifier = Modifier
                .width(100.dp)
                .height(40.dp),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0xFF3053F0), Color(0xFFE82E5A)))
            )
        ) {
            Text(
                catLabel,
                color = Color.DarkGray,
                fontFamily = poppins,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedButton(
            onClick = onProductClick,
            modifier = Modifier
                .width(100.dp)
                .height(40.dp),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0xFF3053F0), Color(0xFFE82E5A)))
            )
        ) {
            Text(
                prodLabel,
                color = Color.DarkGray,
                fontFamily = poppins,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedButton(
            onClick = onDesignClick,
            modifier = Modifier
                .width(100.dp)
                .height(40.dp),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0xFF3053F0), Color(0xFFE82E5A)))
            )
        ) {
            Text(
                designLabel,
                color = Color.DarkGray,
                fontFamily = poppins,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DesignItemRow(item: BulkItem, onClick: (BulkItem) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp)
            .clickable { onClick(item) },
        horizontalArrangement = Arrangement.Start
    ) {
        TableCell(item.design ?: "-", colDesignNameWidth)
        TableCell(item.rfid ?: "-", colRfidWidth)
        TableCell(item.itemCode ?: "-", colItemCodeWidth)

        // ✅ format weight properly
        val formattedGwt = formatTo3Decimals(parseWeightToBigDecimal(item.grossWeight))
        TableCell(formattedGwt, colGWtWidth)

        StatusIconCell(item.scannedStatus, colStatusIconWidth)
    }
}



/* -----------------------
   Filter selection dialog
   ----------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSelectionDialog(
    title: String,
    items: List<String>,
    selectedItems: SnapshotStateList<String>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select $title",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = poppins
            )
        },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    items(items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedItems.contains(item)) selectedItems.remove(item)
                                    else selectedItems.add(item)
                                }
                                .padding(vertical = 6.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                Checkbox(
                                    checked = selectedItems.contains(item),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedItems.add(item) else selectedItems.remove(
                                            item
                                        )
                                    },
                                    modifier = Modifier.size(20.dp),
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF3053F0),
                                        uncheckedColor = Color.Gray
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                fontFamily = poppins,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    GradientButton(text = "CANCEL", onClick = onDismiss)
                    Spacer(modifier = Modifier.width(12.dp))
                    GradientButton(text = "OK", onClick = onConfirm)
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

/* -----------------------
   Summary row (simplified)
   ----------------------- */
/*@Composable
fun SummaryRow(currentLevel: String, items: List<BulkItem>, selectedMenu: String) {

    val totalQty = items.size
    val totalGwtBD =
        items.fold(BigDecimal.ZERO) { acc, it -> acc + parseWeightToBigDecimal(it.grossWeight) }

    val matchedItems = items.filter { it.scannedStatus == "Matched" }
    val totalMatchedQty = matchedItems.size
    val totalMatchedWtBD =
        matchedItems.fold(BigDecimal.ZERO) { acc, it -> acc + parseWeightToBigDecimal(it.grossWeight) }

    val unmatchedItems = items.filter { it.scannedStatus == "Unmatched" }
    unmatchedItems.size
    val unmatchedWtBD =
        unmatchedItems.fold(BigDecimal.ZERO) { acc, it -> acc + parseWeightToBigDecimal(it.grossWeight) }
    Log.d("SummryRow","SummryRow")
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF3B363E))
            .padding(vertical = 4.dp)
    ) {
        when {
            selectedMenu == MENU_UNMATCHED -> {
                TableHeaderCell("Total", colDesignNameWidth)
                TableHeaderCell("$totalQty", colRfidWidth)
                TableHeaderCell("$totalMatchedQty", colItemCodeWidth)
                TableHeaderCell(formatTo3Decimals(unmatchedWtBD), colGWtWidth)
                TableHeaderCell("", colStatusIconWidth)
            }

            currentLevel == "DesignItems" -> {
                TableHeaderCell("Total", colDesignNameWidth)
                TableHeaderCell("$totalQty", colRfidWidth)
                TableHeaderCell("$totalMatchedQty", colItemCodeWidth)
                TableHeaderCell(formatTo3Decimals(totalMatchedWtBD), colGWtWidth)
                TableHeaderCell("", colStatusIconWidth)
            }

            else -> {
                TableHeaderCell("Total", colCategoryWidth)
                TableHeaderCell("$totalQty", colQtyWidth)
                TableHeaderCell(formatTo3Decimals(totalGwtBD), colWeightWidth)
                TableHeaderCell("$totalMatchedQty", colMatchedQtyWidth)
                TableHeaderCell(formatTo3Decimals(totalMatchedWtBD), colMatchedWtWidth)
                TableHeaderCell("", colStatusWidth)
            }
        }
    }
}*/

@Composable
fun SummaryRow(currentLevel: String, items: List<BulkItem>, selectedMenu: String) {

    val totals = remember(items) {
        val totalQty = items.size
        val totalWt = items.fold(BigDecimal.ZERO) { acc, it -> acc + parseWeightToBigDecimal(it.grossWeight) }

        val matched = items.filter { it.scannedStatus == "Matched" }
        val matchedQty = matched.size
        val matchedWt = matched.fold(BigDecimal.ZERO) { acc, it -> acc + parseWeightToBigDecimal(it.grossWeight) }

        val unmatchedQty = totalQty - matchedQty
        val unmatchedWt = totalWt.subtract(matchedWt)

        Triple(
            Triple(totalQty, totalWt, matchedQty),
            Triple(matchedWt, unmatchedQty, unmatchedWt),
            Unit
        )
    }

    val totalQty = totals.first.first
    val totalWtBD = totals.first.second
    val matchedQty = totals.first.third
    val matchedWtBD = totals.second.first
    val unmatchedQty = totals.second.second
    val unmatchedWtBD = totals.second.third

    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF3B363E))
            .padding(vertical = 4.dp)
    ) {

        // ✅ DesignItems view: show Total + Matched + Unmatched (filterwise)
        if (currentLevel == "DesignItems" || selectedMenu == MENU_MATCHED || selectedMenu == MENU_UNMATCHED) {
            TableHeaderCell("Total", colDesignNameWidth)
            TableHeaderCell("$totalQty", colRfidWidth)

            // 2-line info in same cell (M + U)
            Box(
                modifier = Modifier.width(colItemCodeWidth).padding(horizontal = 2.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M:$matchedQty\nU:$unmatchedQty",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    maxLines = 2
                )
            }

            TableHeaderCell(formatTo3Decimals(totalWtBD), colGWtWidth)
            TableHeaderCell("", colStatusIconWidth)
        } else {
            // ✅ Category/Product/Design summary (same as before but now always filterwise)
            TableHeaderCell("Total", colCategoryWidth)
            TableHeaderCell("$totalQty", colQtyWidth)
            TableHeaderCell(formatTo3Decimals(totalWtBD), colWeightWidth)
            TableHeaderCell("$matchedQty", colMatchedQtyWidth)
            TableHeaderCell(formatTo3Decimals(matchedWtBD), colMatchedWtWidth)
            TableHeaderCell("", colStatusWidth)
        }
    }
}



@Composable
fun TableHeaderCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

/* Reused simple TableHeader */
@Composable
fun TableHeader(currentLevel: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF3B363E))
            .padding(vertical = 2.dp)) {
        if (currentLevel == "DesignItems") {
            TableHeaderCell("Design", colDesignNameWidth)
            TableHeaderCell("RFID No", colRfidWidth)
            TableHeaderCell("Item Code", colItemCodeWidth)
            TableHeaderCell("G.Wt", colGWtWidth)
            TableHeaderCell("Status", colStatusIconWidth)
        } else {
            TableHeaderCell(currentLevel, colCategoryWidth)
            TableHeaderCell("Qty", colQtyWidth)
            TableHeaderCell("G.Wt", colWeightWidth)
            TableHeaderCell("M.Qty", colMatchedQtyWidth)
            TableHeaderCell("M.Wt", colMatchedWtWidth)
            TableHeaderCell("Status", colStatusWidth)
        }
    }
}

@Composable
fun TableDataRow(row: TableRow, currentLevel: String, onRowClick: () -> Unit) {
    val qty = row.items.size
    val matchedItems = row.items.filter { it.scannedStatus == "Matched" }
    val matchedQty = matchedItems.size
    val grossWeight = row.items
        .sumOf { it.grossWeight?.toDoubleOrNull() ?: 0.0 }
        .toBigDecimal()

    val matchedWeight = matchedItems
        .sumOf { it.grossWeight?.toDoubleOrNull() ?: 0.0 }
        .toBigDecimal()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 4.dp)
            .clickable { onRowClick() }
    ) {
        TableCell(row.label, colCategoryWidth)
        TableCell("$qty", colQtyWidth)
        TableCell(formatTo3Decimals(grossWeight), colWeightWidth)
        TableCell("$matchedQty", colMatchedQtyWidth)
        TableCell(formatTo3Decimals(matchedWeight), colMatchedWtWidth)
        val status = when {
            row.items.all { it.scannedStatus == "Matched" } -> "Matched"
            row.items.all { it.scannedStatus == "Unmatched" } -> "Unmatched"
            else -> "Unmatched"
        }
        StatusIconCell(status, colStatusWidth)
    }

}

@Composable
fun TableCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 11.sp, color = Color.DarkGray, fontFamily = poppins)
    }
}

@Composable
fun StatusIconCell(status: String?, width: Dp) {
    val iconRes = when (status) {
        "Matched" -> R.drawable.ic_matched
        "Unmatched" -> R.drawable.ic_unmatched
        else -> R.drawable.ic_unmatched
    }
    Box(modifier = Modifier
        .width(width)
        .padding(2.dp), contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun VerticalMenu( matchedCount: Int,
                  unmatchedCount: Int,
                  totalCount: Int,
                  onMenuClick: (MenuItem) -> Unit) {
    val menuItems = listOf(
        MenuItem("Matched Items", R.drawable.ic_list_matched, matchedCount),
        MenuItem("UnMatched Items", R.drawable.ic_list_unmatched, unmatchedCount),
        MenuItem("Unlabelled Items", R.drawable.ic_list_unlabelled, totalCount),
        MenuItem("Resume Scan", R.drawable.ic_resume_scan, null),
        MenuItem("Search", R.drawable.search_gr_svg, unmatchedCount)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        menuItems.forEach { item -> MenuCard(item = item, onClick = { onMenuClick(item) }) }
    }
}

@Composable
fun MenuCard(item: MenuItem, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(Color(0xFF3053F0), Color(0xFFE82E5A)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = item.title,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                fontFamily = poppins
            )
        }
    }
}

fun parseWeightToBigDecimal(weight: String?): BigDecimal {
    if (weight.isNullOrBlank()) return BigDecimal.ZERO
    return try {
        val cleaned = weight.replace(Regex("[^0-9.]"), "")
        if (cleaned.isBlank()) BigDecimal.ZERO else BigDecimal(cleaned)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }
}

fun formatTo3Decimals(b: BigDecimal): String {
    return b.setScale(3, RoundingMode.HALF_UP).toPlainString()
}

data class MenuItem(val title: String, val iconRes: Int,val count: Int? = null)
data class TableRow(val label: String, val items: List<BulkItem>)
