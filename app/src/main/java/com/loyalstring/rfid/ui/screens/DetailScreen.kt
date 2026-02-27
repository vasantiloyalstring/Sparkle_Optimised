package com.loyalstring.rfid.ui.screens
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loyalstring.rfid.data.model.report.Item
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.viewmodel.StockVerificationViewModel
import com.loyalstring.rfid.viewmodel.UiState
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    branchId: String?,
    type: String?,
    date: String?,
    categoryId: String?,
    productId: String?,
    designId: String?,
    onBack: () -> Unit
){

    val viewModel: StockVerificationViewModel = hiltViewModel()

    LaunchedEffect(branchId, type, date, categoryId, productId, designId) {
        viewModel.fetchDetailItems(
            branchId,
            type,
            date,
            categoryId,
            productId,
            designId
        )
    }
    var searchQuery by remember { mutableStateOf("") }

    val state by viewModel.detailState.collectAsState()

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "$type Items",
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {



                // 🔍 SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = {
                    Text("Search Item / RFID / Product")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear"
                            )
                        }
                    }
                },
                singleLine = true
            )

            when (state) {

                is UiState.Success -> {

                    val allItems = (state as UiState.Success).data

                    val items = if (searchQuery.isBlank()) {
                        allItems
                    } else {
                        allItems.filter {
                            it.ItemCode?.contains(searchQuery, true) == true ||
                                    it.RFIDCode?.contains(searchQuery, true) == true ||
                                    it.ProductName?.contains(searchQuery, true) == true ||
                                    it.CategoryName?.contains(searchQuery, true) == true
                        }
                    }
                    val horizontalScrollState = rememberScrollState()

                    LazyColumn {

                        // ✅ Sticky Header
                        stickyHeader {
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF212121))
                                    .horizontalScroll(horizontalScrollState)
                            ) {
                                DetailHeaderRow()
                            }
                        }

                        // ✅ Scrollable Rows
                        items(items) { item ->
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(horizontalScrollState)
                            ) {
                                DetailRow(item)
                            }
                        }
                    }
                }

                is UiState.Success -> {

                    val items = (state as UiState.Success).data
                    val horizontalScrollState = rememberScrollState()

                    LazyColumn {

                        item {
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(horizontalScrollState)
                            ) {
                                DetailHeaderRow()
                            }
                        }

                        items(items) { item ->
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(horizontalScrollState)
                            ) {
                                DetailRow(item)
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Error loading data")
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
fun DetailHeaderRow() {

    Row(
        modifier = Modifier
            .background(Color(0xFF212121)) // 🔥 Dark background
            .padding(vertical = 10.dp)
    ) {

        HeaderCell("Item")
        HeaderCell("RFID")
        HeaderCell("Category")
        HeaderCell("Product")
        HeaderCell("Gr WT")
        HeaderCell("Net WT")
        HeaderCell("Status")
    }
}

@Composable
fun RowScope.HeaderCell(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .width(90.dp) // 🔥 fixed width for clean alignment
            .padding(horizontal = 8.dp),
        fontSize = 12.sp,
        color = Color.White,
        textAlign = TextAlign.Center
    )
}

@Composable
fun DetailRow(item: Item) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        DataCell(item.ItemCode)
        DataCell(item.RFIDCode)
        DataCell(item.CategoryName)
        DataCell(item.ProductName)

        DataCell(item.GrossWeight?.toString())
        DataCell(item.NetWeight?.toString())

        DataCell(item.Status)
    }
}

@Composable
fun RowScope.DataCell(value: String?) {
    Text(
        text = value ?: "-",
        modifier = Modifier
            .width(90.dp) // 🔥 same width as header
            .padding(horizontal = 8.dp),
        fontSize = 12.sp,
        color = Color.Black,
        textAlign = TextAlign.Center
    )
}