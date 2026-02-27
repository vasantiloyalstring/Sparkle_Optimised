package com.loyalstring.rfid.ui.screens

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.data.model.report.Branch
import com.loyalstring.rfid.data.model.report.Category
import com.loyalstring.rfid.data.model.report.Product
import com.loyalstring.rfid.data.model.report.StockVerificationReqReport
import com.loyalstring.rfid.viewmodel.StockVerificationViewModel
import com.loyalstring.rfid.viewmodel.UiState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.loyalstring.rfid.data.model.report.Design
import com.loyalstring.rfid.navigation.GradientTopBar
import androidx.compose.ui.platform.LocalContext
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.worker.LocaleHelper

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun StockVerificationReportScreen(
    onBack: () -> Unit,
    navController: NavHostController
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val viewModel: StockVerificationViewModel = hiltViewModel()
    var selectedDate by remember { mutableStateOf(getTodayDate()) }
    val state by viewModel.reportState.collectAsState()
    val context = LocalContext.current
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language
    val localizedContext = LocaleHelper.applyLocale(context, currentLang ?: "en")
    val employee =
        remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }

    LaunchedEffect(Unit) {
        viewModel.fetchStockVerificationReport(
            StockVerificationReqReport(
                ClientCode = employee?.clientCode.toString(),
                ReportDate = selectedDate
            )
        )
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = localizedContext.getString(R.string.stock_verification_report),
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                showCounter = false,
                selectedCount = 0,
                onCountSelected = {},
                titleTextSize = 20.sp
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when (state) {

                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(localizedContext.getString(R.string.loading))
                    }
                }

                is UiState.Success -> {
                    val data = (state as UiState.Success).data

                    Column {
                        DateSelector(
                            selectedDate = selectedDate,
                            onClick = { showDatePicker = true },
                            localizedContext=localizedContext
                        )
                        ReportHeaderRow(localizedContext=localizedContext)

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                        ) {
                            items(data.Branches ?: emptyList()) { branch ->
                                BranchItem(branch, navController,selectedDate)
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${(state as UiState.Error).message}")
                    }
                }

                else -> {}
            }


        }
    }
    if (showDatePicker) {

        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = formatDate(millis)
                            selectedDate = date

                            viewModel.fetchStockVerificationReport(
                                StockVerificationReqReport(
                                    ClientCode = employee?.clientCode.toString(),
                                    ReportDate = date
                                )
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(localizedContext.getString(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(localizedContext.getString(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

}
@Composable
fun DateSelector(
    selectedDate: String,
    onClick: () -> Unit,
    localizedContext: Context
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {

        OutlinedTextField(
            value = selectedDate,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text(localizedContext.getString(R.string.select_date)) },

            trailingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = null)
            },
            singleLine = true,
            interactionSource = interactionSource
        )

        // 👇 THIS makes whole field clickable
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect {
                onClick()
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    localizedContext:Context
) {

    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(formatDate(millis))
                    }
                }
            ) { Text(localizedContext.getString(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

fun getTodayDate(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date())
}

fun formatDate(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}


@Composable
fun ReportHeaderRow(localizedContext: Context) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        HeaderText(localizedContext.getString(R.string.branch), Modifier.weight(1.6f))
        HeaderText(localizedContext.getString(R.string.total_inv), Modifier.weight(1f))
        HeaderText(localizedContext.getString(R.string.matched), Modifier.weight(1f))
        HeaderText(localizedContext.getString(R.string.unmatched), Modifier.weight(1f))
     //   HeaderText("Match", Modifier.weight(1f))
       // HeaderText("Unm", Modifier.weight(1f))
    }
}

@Composable
fun HeaderText(text: String, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,   // 🔥 Bold text
        textAlign = TextAlign.Center,
        color = Color.White
    )
}

@Composable
fun BranchItem(branch: Branch, navController: NavHostController, selectedDate: String){

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },   // ✅ IMPORTANT
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    branch.BranchName ?: "",
                    modifier = Modifier.weight(1.6f),
                    fontSize = 14.sp
                )
                CenterBadge(
                    branch.TotalInventoryItems ?: 0,
                    Color(0xFFBBDEFB),
                    Modifier.weight(1f)
                ) {
                    navController.navigate(
                        "detail_screen/" +
                                "${branch.BranchId}/" +
                                "0/" +
                                "0/" +
                                "0/" +
                                "TOTAL/" +
                                selectedDate
                    )
                }

                CenterBadge(
                    branch.TotalScannedItems ?: 0,
                    Color(0xFFC8E6C9),
                    Modifier.weight(1f)
                ) {
                    navController.navigate(
                        "detail_screen/" +
                                "${branch.BranchId}/" +
                                "0/" +
                                "0/" +
                                "0/" +
                                "MATCHED/" +
                                selectedDate
                    )
                }

                CenterBadge(
                    branch.NotScannedItems ?: 0,
                    Color(0xFFFFCDD2),
                    Modifier.weight(1f)
                ) {
                    navController.navigate(
                        "detail_screen/" +
                                "${branch.BranchId}/" +
                                "0/" +
                                "0/" +
                                "0/" +
                                "UNMATCHED/" +
                                selectedDate
                    )
                }
               /* CenterBadge(branch.TotalInventoryItems ?: 0, Color(0xFFBBDEFB), Modifier.weight(1f))
                CenterBadge(branch.TotalScannedItems ?: 0, Color(0xFFC8E6C9), Modifier.weight(1f))
                CenterBadge(branch.NotScannedItems ?: 0, Color(0xFFFFCDD2), Modifier.weight(1f))*/
               // CenterBadge(0, Color(0xFFB2DFDB), Modifier.weight(1f))
                //CenterBadge(branch.UnmatchQty ?: 0, Color(0xFFFFE0B2), Modifier.weight(1f))
            }
        }

        // ✅ OUTSIDE CARD
        if (expanded) {
            branch.Categories?.forEach { category ->
                CategoryItem(
                    branch.BranchId!!.toInt(),category,navController,selectedDate)
            }
        }
    }
}

@Composable
fun CenterBadge(
    value: Int,
    color: Color,
    modifier: Modifier,
    onClick: (() -> Unit)? = null
) {

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .background(
                    color,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = onClick != null) {
                    onClick?.invoke()
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                value.toString(),
                fontSize = 12.sp
            )
        }
    }
}
@Composable
fun CategoryItem(
    branchId: Int,
    category: Category,
    navController: NavHostController,
    selectedDate: String
) {

    var expanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF2F2F2)
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 🔹 Small left indicator
                Box(
                    Modifier
                        .size(6.dp)
                        .background(Color.Gray, shape = androidx.compose.foundation.shape.CircleShape)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    category.CategoryName ?: "",
                    modifier = Modifier.weight(1.6f),
                    fontSize = 13.sp,
                    color = Color(0xFF444444)
                )

                // 🔵 TOTAL
                CenterBadge(
                    category.TotalInventoryItems ?: 0,
                    Color(0xFFBBDEFB),
                    Modifier.weight(1f)
                ) {
                    navController.navigate(
                        "detail_screen/" +
                                "$branchId/" +
                                "${category.CategoryId}/" +
                                "0/" +
                                "0/" +
                                "TOTAL/" +
                                selectedDate
                    )
                }

// 🟢 MATCHED
                CenterBadge(
                    category.TotalScannedItems ?: 0,
                    Color(0xFFC8E6C9),
                    Modifier.weight(1f)
                ) {
                    navController.navigate(
                        "detail_screen/" +
                                "$branchId/" +
                                "${category.CategoryId}/" +
                                "0/" +
                                "0/" +
                                "MATCHED/" +
                                selectedDate
                    )
                }

// 🔴 UNMATCHED
                CenterBadge(
                    category.NotScannedItems ?: 0,
                    Color(0xFFFFCDD2),
                    Modifier.weight(1f)
                ) {
                    navController.navigate(
                        "detail_screen/" +
                                "$branchId/" +
                                "${category.CategoryId}/" +
                                "0/" +
                                "0/" +
                                "UNMATCHED/" +
                                selectedDate
                    )
                }
            }
        }

        if (expanded) {
            category.Products?.forEach { product ->
                ProductItem(
                    branchId = branchId,
                    categoryId = category.CategoryId ?: 0,
                    product = product,
                    navController = navController,
                    selectedDate = selectedDate
                )
            }
        }
    }
}

@Composable
fun ProductItem(
    product: Product,
    branchId: Int,
    categoryId: Int?,
    navController: NavHostController,
    selectedDate: String
) {

    var expanded by remember { mutableStateOf(false) }

    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFAFA))
                .clickable { expanded = !expanded }
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ◦ product dot
            Box(
                Modifier
                    .size(5.dp)
                    .background(Color.Gray, shape = androidx.compose.foundation.shape.CircleShape)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                product.ProductName ?: "",
                modifier = Modifier.weight(1.6f),
                fontSize = 13.sp,   // 👈 slightly bigger
                color = Color(0xFF555555)
            )

            CenterBadge(
                product.TotalInventoryItems ?: 0,
                Color(0xFFBBDEFB),
                Modifier.weight(1f)
            ) {
                navController.navigate(
                    "detail_screen/" +
                            "$branchId/" +
                            "$categoryId/" +
                            "${product.ProductId}/" +
                            "0/" +
                            "TOTAL/" +
                            selectedDate
                )
            }
            CenterBadge(
                product.TotalScannedItems ?: 0,
                Color(0xFFC8E6C9),
                Modifier.weight(1f)
            ) {
                navController.navigate(
                    "detail_screen/" +
                            "$branchId/" +
                            "$categoryId/" +
                            "${product.ProductId}/" +
                            "0/" +
                            "MATCHED/" +
                            selectedDate
                )
            }
            CenterBadge(
                product.NotScannedItems ?: 0,
                Color(0xFFFFCDD2),
                Modifier.weight(1f)
            ) {
                navController.navigate(
                    "detail_screen/" +
                            "$branchId/" +
                            "$categoryId/" +
                            "${product.ProductId}/" +
                            "0/" +
                            "UNMATCHED/" +
                            selectedDate
                )
            }
        }

        if (expanded) {
           product.Designs?.forEach { design ->
                DesignItem( branchId = branchId,
                    categoryId = categoryId?: 0,
                    productId = product.ProductId,
                    design=design,
                    navController = navController,
                    selectedDate = selectedDate)
            }
        }
    }
}

@Composable
fun DesignItem( branchId: Int,
                categoryId: Int,
                productId: Int?,
                design: Design,
                navController: NavHostController,
                selectedDate: String) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFFFF))
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ◦◦ smaller design dot
        Box(
            Modifier
                .size(4.dp)
                .background(Color.LightGray, shape = androidx.compose.foundation.shape.CircleShape)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            design.DesignName ?: "",
            modifier = Modifier.weight(1.6f),
            fontSize = 12.sp,
            color = Color(0xFF777777)
        )

        // 🔵 TOTAL
        CenterBadge(
            design.TotalInventoryItems ?: 0,
            Color(0xFFBBDEFB),
            Modifier.weight(1f)
        ) {
            navController.navigate(
                "detail_screen/" +
                        "$branchId/" +
                        "$categoryId/" +
                        "${productId ?: 0}/" +
                        "${design.DesignId}/" +
                        "TOTAL/" +
                        selectedDate
            )
        }

// 🟢 MATCHED
        CenterBadge(
            design.TotalScannedItems ?: 0,
            Color(0xFFC8E6C9),
            Modifier.weight(1f)
        ) {
            navController.navigate(
                "detail_screen/" +
                        "$branchId/" +
                        "$categoryId/" +
                        "${productId ?: 0}/" +
                        "${design.DesignId}/" +
                        "MATCHED/" +
                        selectedDate
            )
        }

// 🔴 UNMATCHED
        CenterBadge(
            design.NotScannedItems ?: 0,
            Color(0xFFFFCDD2),
            Modifier.weight(1f)
        ) {
            navController.navigate(
                "detail_screen/" +
                        "$branchId/" +
                        "$categoryId/" +
                        "${productId ?: 0}/" +
                        "${design.DesignId}/" +
                        "UNMATCHED/" +
                        selectedDate
            )
        }
    }
}

@Composable
fun Badge(value: Int, color: Color, modifier: Modifier = Modifier) {

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .background(
                color,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(value.toString(), fontSize = 12.sp)
    }
}
