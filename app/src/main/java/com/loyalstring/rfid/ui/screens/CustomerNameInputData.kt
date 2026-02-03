package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource

import com.loyalstring.rfid.worker.LocaleHelper
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.sparklepos.models.loginclasses.customerBill.AddEmployeeRequest
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.loyalstring.rfid.R
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.OrderViewModel
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerNameInputData(
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    onClear: () -> Unit,
    onAddCustomerClick: () -> Unit,
    filteredCustomers: List<EmployeeList>,
    isLoading: Boolean,
    onCustomerSelected: (EmployeeList) -> Unit,
    coroutineScope: CoroutineScope,
    fetchSuggestions: suspend () -> Unit,
    expanded: Boolean,
    onSaveCustomer: (AddEmployeeRequest) -> Unit,
    employeeClientCode: String? = null,
    employeeId: String? = null
) {

    var isExpanded by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    val orderViewModel: OrderViewModel = hiltViewModel()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val gradientBrush = Brush.horizontalGradient(
        listOf(Color(0xFF5231A7), Color(0xFFD32940))
    )

    val context: Context = LocalContext.current

    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language
    val localizedContext = LocaleHelper.applyLocale(context, currentLang ?: "en")

    // ✅ Filter using customerName only (same input)
    val visibleCustomers = remember(customerName, filteredCustomers) {
        val q = customerName.trim().lowercase(Locale.getDefault())
        if (q.isEmpty()) filteredCustomers
        else filteredCustomers.filter {
            it.FirstName.orEmpty().lowercase(Locale.getDefault()).contains(q) ||
                    it.LastName.orEmpty().lowercase(Locale.getDefault()).contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        // ✅ MAIN INPUT (ONLY ONE INPUT)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp)
                .border(1.dp, gradientBrush, RoundedCornerShape(10.dp))
                .background(Color.White, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))

            BasicTextField(
                value = customerName,
                onValueChange = {
                    onCustomerNameChange(it)
                    if (!isExpanded) isExpanded = true
                    coroutineScope.launch { fetchSuggestions() }
                },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        isExpanded = true
                        coroutineScope.launch { fetchSuggestions() }
                        keyboardController?.show()
                    },
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (customerName.isBlank()) {
                            Text(
                                text = localizedContext.getString(R.string.hint_enter_customer_name),
                                fontSize = 13.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        inner()
                    }
                }
            )

            if (customerName.isBlank()) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onAddCustomerClick()
                            showAddCustomerDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.vector_add),
                        contentDescription = "Add Customer",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        onClear()
                        isExpanded = false
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ✅ LIST BELOW INPUT (NO SECOND SEARCH BOX)
        if (isExpanded) {
            Spacer(Modifier.height(6.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 260.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                val listScroll = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(listScroll)
                ) {
                    when {
                        isLoading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(localizedContext.getString(R.string.label_loading))
                            }
                        }

                        visibleCustomers.isEmpty() -> {
                            Text(
                                text = localizedContext.getString(R.string.no_results_found),
                                modifier = Modifier.padding(12.dp),
                                color = Color.Gray
                            )
                        }

                        else -> {
                            visibleCustomers.forEach { customer ->
                                val fullName =
                                    "${customer.FirstName.orEmpty()} ${customer.LastName.orEmpty()}".trim()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onCustomerNameChange(fullName)
                                            onCustomerSelected(customer)

                                            isExpanded = false
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        }
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = fullName,
                                        fontSize = 13.sp,
                                        color = Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Divider(color = Color(0xFFEAEAEA))
                            }
                        }
                    }
                }
            }
        }
    }

    // ✅ Add Customer Dialog
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            localizedContext = localizedContext,
            onDismiss = { showAddCustomerDialog = false },
            onSaveCustomer = {
                onSaveCustomer(it)
                showAddCustomerDialog = false
                orderViewModel.getAllEmpList(employee?.clientCode.toString())
            },
            employeeClientCode = employeeClientCode,
            employeeId = employeeId
        )
    }
}
/*----------------------------------------------------------
   ADD CUSTOMER DIALOG (Localized)
-----------------------------------------------------------*/
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddCustomerDialog(
    localizedContext: Context,
    onDismiss: () -> Unit,
    onSaveCustomer: (AddEmployeeRequest) -> Unit,
    employeeClientCode: String? = null,
    employeeId: String? = null

) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    var expandedCountry by remember { mutableStateOf(false) }
    var expandedState by remember { mutableStateOf(false) }

    val stateOptions = listOf("Andhra Pradesh", "Bihar", "Goa", "Gujarat", "Karnataka",
        "Kerala", "Maharashtra", "Rajasthan", "Tamil Nadu", "Telangana")
    val countryOptions = listOf("India", "USA", "UK", "Canada")

    val scope = rememberCoroutineScope()
    val cityBiv = remember { BringIntoViewRequester() }

    Popup(alignment = Alignment.Center, properties = PopupProperties(focusable = true)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .fillMaxWidth(0.95f)
                    .heightIn(min = 300.dp, max = 600.dp)
                    .imePadding()                      // ✅ pushes content above keyboard

            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 🔹 Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color.DarkGray, shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            localizedContext.getString(R.string.title_customer_profile),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 🔹 Form Fields
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                            .imePadding()
                    ) {
                        fun fieldModifier() = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(horizontal = 10.dp)

                        CenteredTextField(name, { name = it }, localizedContext.getString(R.string.hint_customer_name), fieldModifier())
                        Spacer(Modifier.height(10.dp))
                        CenteredTextField(phone, { if (it.length <= 10) phone = it.filter(Char::isDigit) }, localizedContext.getString(R.string.hint_mobile_number), fieldModifier())
                        Spacer(Modifier.height(10.dp))
                        CenteredTextField(email, { email = it }, localizedContext.getString(R.string.hint_email), fieldModifier())
                        Spacer(Modifier.height(10.dp))
                        CenteredTextField(pan, { pan = it.uppercase().take(10) }, localizedContext.getString(R.string.hint_pan_number), fieldModifier())
                        Spacer(Modifier.height(10.dp))
                        CenteredTextField(gst, { gst = it.uppercase().take(15) }, localizedContext.getString(R.string.hint_gst_number), fieldModifier())
                        Spacer(Modifier.height(10.dp))
                        CenteredTextField(street, { street = it }, localizedContext.getString(R.string.hint_street_address), fieldModifier())
                        Spacer(Modifier.height(10.dp))

                        // 🌍 Dropdown Row
                        Row(Modifier.fillMaxWidth()) {
                            DropdownBox(
                                label = localizedContext.getString(R.string.hint_country),
                                value = country,
                                options = countryOptions,
                                expanded = expandedCountry,
                                onExpandedChange = { expandedCountry = it },
                                onSelect = { country = it },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            DropdownBox(
                                label = localizedContext.getString(R.string.hint_state),
                                value = state,
                                options = stateOptions,
                                expanded = expandedState,
                                onExpandedChange = { expandedState = it },
                                onSelect = { state = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        CenteredTextField(
                            value = city,
                            onValueChange = { city = it },
                            placeholder = localizedContext.getString(R.string.hint_city),
                            modifier = fieldModifier()
                                .bringIntoViewRequester(cityBiv)
                                .onFocusEvent { state ->
                                    if (state.isFocused) {
                                        scope.launch {
                                            delay(250) // keyboard open hone do
                                            cityBiv.bringIntoView() // ✅ scroll up so user can see typing
                                        }
                                    }
                                }
                        )
                       // CenteredTextField(city, { city = it }, localizedContext.getString(R.string.hint_city), fieldModifier())
                    }

                    // 🔹 Footer Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GradientButtonIcon(
                            text = localizedContext.getString(R.string.btn_cancel),
                            onClick = onDismiss,
                            icon = painterResource(id = R.drawable.ic_cancel),
                            iconDescription = "Cancel Icon",
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        )

                        GradientButtonIcon(
                            text = localizedContext.getString(R.string.btn_ok),
                            onClick = {
                                fun isValidEmail(email: String) =
                                    email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$".toRegex())

                                fun isValidPhone(phone: String) =
                                    phone.matches("^[0-9]{10}$".toRegex())

                                fun isValidPan(pan: String) =
                                    pan.matches("^[A-Z]{5}[0-9]{4}[A-Z]{1}$".toRegex())

                                fun isValidGst(gst: String) =
                                    gst.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[A-Z0-9]{1}[A-Z]{1}[0-9]{1}$".toRegex())

                                when {
                                    name.trim().isEmpty() -> showToast(context, context.getString(R.string.msg_enter_name))
                                    phone.trim().isEmpty() && email.trim().isEmpty() && pan.trim().isEmpty() && gst.trim().isEmpty()
                                            && street.trim().isEmpty() && city.trim().isEmpty() && state.trim().isEmpty() && country.trim().isEmpty() -> {
                                        val req = AddEmployeeRequest(name, "", "", "", "", "", "",
                                            0, 0, 0, "", "Active", "", "0", "0", "", "", "", "", "",
                                            "", "", "", "", "", "", "", "0", "0", "", "0", "0", "",
                                            employeeClientCode, 0, "", false, employeeId)
                                        onSaveCustomer(req); onDismiss()
                                    }
                                    phone.isNotEmpty() && !isValidPhone(phone) -> showToast(context, context.getString(R.string.msg_invalid_phone))
                                    email.isNotEmpty() && !isValidEmail(email) -> showToast(context, context.getString(R.string.msg_invalid_email))
                                    pan.isNotEmpty() && !isValidPan(pan) -> showToast(context, context.getString(R.string.msg_invalid_pan))
                                    gst.isNotEmpty() && !isValidGst(gst) -> showToast(context, context.getString(R.string.msg_invalid_gst))
                                    country.isNotEmpty() && state.isEmpty() -> showToast(context, context.getString(R.string.msg_select_state))
                                    state.isNotEmpty() && country.isEmpty() -> showToast(context, context.getString(R.string.msg_select_country))
                                    city.isEmpty() -> showToast(context, context.getString(R.string.msg_enter_city))
                                    else -> {
                                        val req = AddEmployeeRequest(name, "", "", email, "", "", "",
                                            0, 0, 0, phone, "Active", "", "0", "0", street, "", "", city,
                                            state, "", "", "", "", country, "", "", "0", "0", pan, "0", "0", gst,
                                            employeeClientCode, 0, "", false, employeeId)
                                        onSaveCustomer(req); onDismiss()
                                    }
                                }
                            },
                            icon = painterResource(id = R.drawable.check_circle),
                            iconDescription = "OK Icon",
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownBox(
    label: String,
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 10.dp)
            .clickable { onExpandedChange(!expanded) }
    ) {
        Text(
            text = if (value.isEmpty()) label else value,
            fontSize = 12.sp,
            color = if (value.isEmpty()) Color.Gray else Color.Black,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        if (expanded) {
            Popup(alignment = Alignment.TopStart, offset = IntOffset(0, 80), properties = PopupProperties(focusable = true)) {
                Column(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color.LightGray)
                        .width(140.dp)
                ) {
                    options.forEach {
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            color = Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(it)
                                    onExpandedChange(false)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CenteredTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = textSize, color = Color.Black),
        modifier = modifier,
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = Color.Gray, fontSize = 14.sp)
                inner()
            }
        }
    )
}

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
