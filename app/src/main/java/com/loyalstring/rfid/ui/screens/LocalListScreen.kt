package com.loyalstring.rfid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationListScreen(
    onBack: () -> Unit,
    navController: NavHostController, )
{
    val context = LocalContext.current
    val viewModel: SettingsViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val locations by viewModel.localLocations.collectAsState()
    var isLoading by remember { mutableStateOf(true) }

    // Fetch locations from DB when screen opens
    LaunchedEffect(Unit) {
        isLoading = true
        viewModel.fetchLocationsFromDb()
        scope.launch {
            kotlinx.coroutines.delay(500)
            isLoading = false
        }
    }

    Scaffold(
        topBar = {  GradientTopBar(
            title = "Location List",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            selectedCount = 0,
            titleTextSize = 20.sp
        ) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                locations.isEmpty() -> Text("No locations found", fontSize = 16.sp)
                else -> {

                    var selectedDate by remember { mutableStateOf<Date?>(null) }
                    var showDatePicker by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {

                        // 🔵 Date Selector Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Button(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    selectedDate?.let {
                                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                                    } ?: "Select Date"
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            IconButton(
                                onClick = {

                                    val selected = selectedDate ?: return@IconButton

                                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    val selectedStr = formatter.format(selected)

                                    val dayLocations = locations.filter {

                                        try {

                                            val parser = SimpleDateFormat(
                                                "yyyy-MM-dd'T'HH:mm:ss",
                                                Locale.getDefault()
                                            )

                                            val formatter = SimpleDateFormat(
                                                "dd/MM/yyyy",
                                                Locale.getDefault()
                                            )

                                            val createdDate = parser.parse(it.CreatedOn?.substring(0,19))

                                            formatter.format(createdDate!!) == selectedStr

                                        } catch (e: Exception) {
                                            false
                                        }
                                    }
                                    if (dayLocations.isNotEmpty()) {

                                        val first = dayLocations.first()
                                        val last = dayLocations.last()

                                       /* val route = buildString {

                                            append("https://www.google.com/maps/dir/")

                                            dayLocations.forEach {
                                                append("${it.Latitude},${it.Longitude}/")
                                            }
                                        }

                                        val uri = Uri.parse(route)

                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        intent.setPackage("com.google.android.apps.maps")

                                        context.startActivity(intent)*/
                                        if (dayLocations.size >= 2) {

                                            val origin = "${dayLocations.first().Latitude},${dayLocations.first().Longitude}"
                                            val destination = "${dayLocations.last().Latitude},${dayLocations.last().Longitude}"

                                            val waypoints = dayLocations
                                                .drop(1)
                                                .dropLast(1)
                                                .joinToString("|") { "${it.Latitude},${it.Longitude}" }

                                            val uri = Uri.parse(
                                                "https://www.google.com/maps/dir/?api=1" +
                                                        "&origin=$origin" +
                                                        "&destination=$destination" +
                                                        "&waypoints=$waypoints"
                                            )

                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                            intent.setPackage("com.google.android.apps.maps")

                                            context.startActivity(intent)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = "Route")
                            }
                        }

                        // 🔵 Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {

                            Text(
                                "S.no",
                                modifier = Modifier.weight(0.5f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )

                            Text(
                                "Date",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )

                            Text(
                                "UserId",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )

                            Text(
                                "Address",
                                modifier = Modifier.weight(1.5f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        Divider(color = Color.Gray)

                        // 🔵 Location List
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {

                            itemsIndexed(locations) { index, item ->

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Text(
                                        "${index + 1}",
                                        modifier = Modifier.weight(0.3f),
                                        fontSize = 12.sp
                                    )

                                    val date = try {
                                        val parser = SimpleDateFormat(
                                            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS",
                                            Locale.getDefault()
                                        )
                                        val formatter =
                                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                                        formatter.format(parser.parse(item.CreatedOn) ?: Date())

                                    } catch (e: Exception) {
                                        item.CreatedOn
                                    }

                                    Text(
                                        date.toString(),
                                        modifier = Modifier.weight(1f),
                                        fontSize = 12.sp
                                    )

                                    Text(
                                        item.UserId.toString(),
                                        modifier = Modifier.weight(1f),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        item.Address ?: "-",
                                        modifier = Modifier.weight(1.5f),
                                        fontSize = 12.sp
                                    )

                                    IconButton(
                                        onClick = {

                                            val latitude = item.Latitude
                                            val longitude = item.Longitude

                                            val label = "${date} - ${item.Address ?: ""}"

                                            val gmmIntentUri =
                                                Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
                                            val mapIntent =
                                                Intent(Intent.ACTION_VIEW, gmmIntentUri)

                                            mapIntent.setPackage("com.google.android.apps.maps")

                                            context.startActivity(mapIntent)
                                        }
                                    ) {

                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Open Map",
                                            tint = Color.Red
                                        )
                                    }
                                }

                                Divider()
                            }
                        }
                    }

                    // 🔵 Date Picker Dialog
                    if (showDatePicker) {

                        val datePickerState = rememberDatePickerState()

                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {

                                TextButton(onClick = {

                                    datePickerState.selectedDateMillis?.let {
                                        selectedDate = Date(it)
                                    }

                                    showDatePicker = false

                                }) {
                                    Text("OK")
                                }
                            }
                        ) {

                            DatePicker(state = datePickerState)
                        }
                    }
                }

            }
        }
    }
}

