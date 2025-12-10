package com.loyalstring.rfid.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.DeliveryChallanItem
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanDetails
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.sampleOut.SampleOutDetails
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * DeliveryChallanDialogEditAndDisplay
 * A Delivery Challan dialog with the same fields & functionality as the Order details dialog.
 *
 * Usage: call it with the selected OrderItem, branch list and an onSave callback to persist changes.
 */
@Composable
fun SampleOutDialogEditAndDisplay(
    selectedItem: SampleOutDetails?,
    branchList: List<BranchModel>,
    salesmanList: UiState<List<EmployeeList>>,
    onDismiss: () -> Unit,
    edit: Int = 0,
    onSave: (SampleOutDetails) -> Unit,
    orderViewModel: OrderViewModel = hiltViewModel(),
    singleProductViewModel: SingleProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                orderViewModel.getAllEmpList(employee?.clientCode.toString())
                orderViewModel.getAllItemCodeList(ClientCodeRequest(employee?.clientCode.toString()))
                singleProductViewModel.getAllBranches(ClientCodeRequest(employee?.clientCode.toString()))
                singleProductViewModel.getAllPurity(ClientCodeRequest(employee?.clientCode.toString()))
                singleProductViewModel.getAllSKU(ClientCodeRequest(employee?.clientCode.toString()))
                orderViewModel.getDailyRate(ClientCodeRequest(employee?.clientCode))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ------------ state ------------
    var branch by remember { mutableStateOf("") }
    var exhibition by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }

    var purity by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
    var typeOfColors by remember { mutableStateOf("") }
    var screwType by remember { mutableStateOf("") }
    var polishType by remember { mutableStateOf("") }
    var finePercentage by remember { mutableStateOf("") }
    var wastage by remember { mutableStateOf("") }

    var productName by remember { mutableStateOf("") }
    var itemCode by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var NetWt by remember { mutableStateOf("") }
    var totalWt by remember { mutableStateOf("") }
    var packingWt by remember { mutableStateOf("") }
    var grossWT by remember { mutableStateOf("") }
    var stoneWt by remember { mutableStateOf("") }
    var dimondWt by remember { mutableStateOf("") }
    var ratePerGRam by remember { mutableStateOf("") }
    var hallMarkAmt by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var stoneAmt by remember { mutableStateOf("") }
    var itemAmt by remember { mutableStateOf("") }
    var finePlusWt by remember { mutableStateOf("") }

    // dropdown states
    var expandedPurity by remember { mutableStateOf(false) }
    var expandedColors by remember { mutableStateOf(false) }
    var expandedScrew by remember { mutableStateOf(false) }
    var expandedPolish by remember { mutableStateOf(false) }

    val colorsList = listOf(
        "Yellow Gold", "White Gold", "Rose Gold", "Green Gold",
        "Black Gold", "Blue Gold", "Purple Gold"
    )
    val screwList = listOf("Type 1", "Type 2", "Type 3")
    val polishList = listOf("High Polish", "Matte Finish", "Satin Finish", "Hammered")
    val baseUrl = "https://rrgold.loyalstring.co.in/"

    val inputFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    )
    fun formatDateSafe(dateString: String?): String {
        if (dateString.isNullOrBlank() || dateString.equals("null", true)) return ""
        for (format in inputFormats) {
            try {
                val parsed = format.parse(dateString)
                if (parsed != null) return dateFormatter.format(parsed)
            } catch (_: Exception) { }
        }
        return ""
    }

    // ---------- init from selected item ----------
    selectedItem?.let { s ->
        productName = s.ProductName.orEmpty()
        itemCode = s.ItemCode.orEmpty()
        sku = s.SKU.orEmpty()

        totalWt = s.TotalWt.orEmpty()
        packingWt = s.PackingWeight.orEmpty()
        grossWT = s.GrossWt.orEmpty()
        stoneWt = s.TotalStoneWeight.orEmpty()
        dimondWt = s.DiamondWeight.orEmpty()
        NetWt = s.NetWt.orEmpty()

        purity = s.Purity.orEmpty()
        size = s.Size.orEmpty()
        length = "" // no backing field in model

        typeOfColors = s.DiamondColour.orEmpty()
        screwType = ""
        polishType = ""
        finePercentage = s.FinePer.orEmpty()
        wastage = s.fixWastage.orEmpty()

        qty = s.qty.toString()
        hallMarkAmt = s.HallmarkAmount.orEmpty()
        mrp = s.MRP.orEmpty()
        ratePerGRam = s.totayRate.orEmpty()
        stoneAmt = s.StoneAmount.orEmpty()
        finePlusWt = s.FineWastageWt.orEmpty()

        exhibition = ""
        remark = ""

        itemAmt = if (!s.MRP.isNullOrEmpty()) s.MRP else s.ItemAmount.orEmpty()
        itemAmt = try { "%.2f".format(itemAmt.toDouble()) } catch (_: Exception) { itemAmt }
    }

    val dailyRates by orderViewModel.getAllDailyRate.collectAsState(initial = emptyList())
    LaunchedEffect(purity, dailyRates) {
        if (purity.isNotBlank() && dailyRates.isNotEmpty()) {
            val match = dailyRates.find { it.PurityName.equals(purity, ignoreCase = true) }
            match?.Rate?.let { rate ->
                ratePerGRam = rate
                val net = NetWt.toDoubleOrNull() ?: 0.0
                val totalRate = net * (rate.toDoubleOrNull() ?: 0.0)
                itemAmt = "%.2f".format(totalRate)
            }
        }
    }

    // purity list from vm
    val purityList by singleProductViewModel.purityResponse1.collectAsState()
    val purityNames = purityList.map { it.PurityName ?: "" }

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ---------- HEADER ----------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.order_edit_icon),
                            contentDescription = "Custom Order Icon",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Custom Order Fields",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = poppins
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // ---------- BODY (SCROLLABLE) ----------
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    // 📷 Image area (top)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = baseUrl + (selectedItem?.Image ?: ""),
                            contentDescription = "Product image",
                            placeholder = painterResource(R.drawable.add_photo),
                            error = painterResource(R.drawable.add_photo),
                            modifier = Modifier.size(110.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // 1️⃣ Product Name
                    FieldRow("Product Name", productName) { productName = it }
                    Spacer(Modifier.height(4.dp))

                    // 2️⃣ ItemCode
                    FieldRow("ItemCode", itemCode) { itemCode = it }
                    Spacer(Modifier.height(4.dp))

                    // 3️⃣ SKU
                    FieldRow("SKU", sku) { sku = it }
                    Spacer(Modifier.height(4.dp))

                    // 4️⃣ Purity (dropdown)
                    DropdownRow(
                        label = "Purity",
                        list = purityNames,
                        selected = purity,
                        expanded = expandedPurity,
                        onSelect = { purity = it },
                        onExpand = { expandedPurity = it }
                    )

                    Spacer(Modifier.height(6.dp))

                    // 5️⃣ Total Weight
                    FieldRow("Total Weight", totalWt) { newVal ->
                        totalWt = newVal
                        val totalValue = totalWt.toDoubleOrNull() ?: 0.0
                        val pack = packingWt.toDoubleOrNull() ?: 0.0
                        grossWT = String.format("%.3f", totalValue - pack)
                        val stone = stoneWt.toDoubleOrNull() ?: 0.0
                        val diamond = dimondWt.toDoubleOrNull() ?: 0.0
                        val stoneAmtVal = stoneAmt.toDoubleOrNull() ?: 0.0
                        NetWt = String.format("%.3f", totalValue - (stone + diamond + stoneAmtVal))
                        val net = NetWt.toDoubleOrNull() ?: 0.0
                        val rate = ratePerGRam.toDoubleOrNull() ?: 0.0
                        itemAmt = "%.2f".format(net * rate)
                    }

                    Spacer(Modifier.height(4.dp))

                    // 6️⃣ Packing Weight
                    FieldRow("Packing Weight", packingWt) { newVal ->
                        packingWt = newVal
                        val totalValue = totalWt.toDoubleOrNull() ?: 0.0
                        val pack = packingWt.toDoubleOrNull() ?: 0.0
                        grossWT = String.format("%.3f", totalValue - pack)
                        val stone = stoneWt.toDoubleOrNull() ?: 0.0
                        val diamond = dimondWt.toDoubleOrNull() ?: 0.0
                        NetWt = String.format(
                            "%.3f",
                            (totalValue - pack) - (stone + diamond + (stoneAmt.toDoubleOrNull() ?: 0.0))
                        )
                        val net = NetWt.toDoubleOrNull() ?: 0.0
                        val rate = ratePerGRam.toDoubleOrNull() ?: 0.0
                        itemAmt = "%.2f".format(net * rate)
                    }

                    Spacer(Modifier.height(4.dp))

                    // 7️⃣ Gross Weight
                    FieldRow("Gross Weight", grossWT) { newVal ->
                        grossWT = newVal
                        val g = grossWT.toDoubleOrNull() ?: 0.0
                        val s = stoneWt.toDoubleOrNull() ?: 0.0
                        val d = dimondWt.toDoubleOrNull() ?: 0.0
                        NetWt = "%.3f".format(g - s - d)
                        val net = NetWt.toDoubleOrNull() ?: 0.0
                        val rate = ratePerGRam.toDoubleOrNull() ?: 0.0
                        itemAmt = "%.2f".format(net * rate)
                    }

                    Spacer(Modifier.height(4.dp))

                    // 8️⃣ Stone Weight
                    FieldRow("Stone Weight", stoneWt) { newVal ->
                        stoneWt = newVal
                        val total = grossWT.toDoubleOrNull() ?: 0.0
                        val stone = stoneWt.toDoubleOrNull() ?: 0.0
                        val diamond = dimondWt.toDoubleOrNull() ?: 0.0
                        NetWt = String.format(
                            "%.3f",
                            total - (stone + diamond + (stoneAmt.toDoubleOrNull() ?: 0.0))
                        )
                        val net = NetWt.toDoubleOrNull() ?: 0.0
                        val rate = ratePerGRam.toDoubleOrNull() ?: 0.0
                        itemAmt = "%.2f".format(net * rate)
                    }

                    Spacer(Modifier.height(4.dp))

                    // 9️⃣ Dimond Weight
                    FieldRow("Dimond Weight", dimondWt) { newVal ->
                        dimondWt = newVal
                        val total = grossWT.toDoubleOrNull() ?: 0.0
                        val stone = stoneWt.toDoubleOrNull() ?: 0.0
                        val diamond = dimondWt.toDoubleOrNull() ?: 0.0
                        NetWt = String.format(
                            "%.3f",
                            total - (stone + diamond + (stoneAmt.toDoubleOrNull() ?: 0.0))
                        )
                        val net = NetWt.toDoubleOrNull() ?: 0.0
                        val rate = ratePerGRam.toDoubleOrNull() ?: 0.0
                        itemAmt = "%.2f".format(net * rate)
                    }

                    Spacer(Modifier.height(4.dp))

                    // 🔟 Net Weight (read-only)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Weight",
                            modifier = Modifier.weight(0.4f),
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontFamily = poppins
                        )
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .height(35.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = if (NetWt.isBlank()) "0.000" else NetWt,
                                fontSize = 13.sp,
                                fontFamily = poppins
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // 1️⃣1️⃣ Size
                    FieldRow("Size", size) { size = it }
                    Spacer(Modifier.height(4.dp))

                    // 1️⃣2️⃣ Length
                    FieldRow("Length", length) { length = it }
                    Spacer(Modifier.height(4.dp))

                    // 1️⃣3️⃣ Type of Color
                    DropdownRow(
                        label = "Type of Color",
                        list = colorsList,
                        selected = typeOfColors,
                        expanded = expandedColors,
                        onSelect = { typeOfColors = it },
                        onExpand = { expandedColors = it }
                    )

                    Spacer(Modifier.height(4.dp))

                    // 1️⃣4️⃣ Screw Type
                    DropdownRow(
                        label = "Screw Type",
                        list = screwList,
                        selected = screwType,
                        expanded = expandedScrew,
                        onSelect = { screwType = it },
                        onExpand = { expandedScrew = it }
                    )

                    Spacer(Modifier.height(4.dp))

                    // 1️⃣5️⃣ Polish Type
                    DropdownRow(
                        label = "Polish Type",
                        list = polishList,
                        selected = polishType,
                        expanded = expandedPolish,
                        onSelect = { polishType = it },
                        onExpand = { expandedPolish = it }
                    )

                    Spacer(Modifier.height(4.dp))

                    // 1️⃣6️⃣ Rate/Gm
                    FieldRow("Rate/Gm", ratePerGRam) { newVal ->
                        ratePerGRam = newVal
                        val net = NetWt.toDoubleOrNull() ?: 0.0
                        val rate = ratePerGRam.toDoubleOrNull() ?: 0.0
                        itemAmt = "%.2f".format(net * rate)
                    }

                    Spacer(Modifier.height(4.dp))

                    // 1️⃣7️⃣ Fine%
                    FieldRow("Fine%", finePercentage) { finePercentage = it }

                    Spacer(Modifier.height(4.dp))

                    // 1️⃣8️⃣ Wastage%
                    FieldRow("Wastage%", wastage) { wastage = it }

                    Spacer(Modifier.height(4.dp))

                    // 1️⃣9️⃣ Quantity
                    FieldRow("Quantity", qty) { qty = it }

                    Spacer(Modifier.height(4.dp))

                    // 2️⃣0️⃣ Hallmark Amount
                    FieldRow("Hallmark Amount", hallMarkAmt) { newVal ->
                        hallMarkAmt = newVal
                        val newHall = hallMarkAmt.toDoubleOrNull() ?: 0.0
                        val baseAmt =
                            (NetWt.toDoubleOrNull() ?: 0.0) * (ratePerGRam.toDoubleOrNull() ?: 0.0)
                        itemAmt = String.format("%.2f", baseAmt + newHall)
                    }

                    Spacer(Modifier.height(4.dp))

                    // 2️⃣1️⃣ MRP
                    FieldRow("MRP", mrp) { newVal ->
                        mrp = newVal
                        val mrpValue = mrp.toDoubleOrNull()
                        if (mrpValue != null && mrpValue > 0) {
                            itemAmt = String.format("%.2f", mrpValue)
                        }
                    }

                    // (optional) extra fields after sequence
                    Spacer(Modifier.height(6.dp))
                   // FieldRow("Exhibition", exhibition) { exhibition = it }
                    //Spacer(Modifier.height(4.dp))
                    //FieldRow("Remark", remark) { remark = it }
                }

                // ---------- BUTTONS ----------
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GradientButtonIcon(
                        text = "Cancel",
                        onClick = { onDismiss() },
                        icon = painterResource(id = R.drawable.ic_cancel),
                        iconDescription = "Cancel",
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(end = 4.dp)
                    )

                    GradientButtonIcon(
                        text = "Save",
                        onClick = {
                            val s = selectedItem
                            if (s == null) {
                                Toast.makeText(context, "No item selected", Toast.LENGTH_SHORT)
                                    .show()
                                onDismiss()
                                return@GradientButtonIcon
                            }

                            // same calc block you already had (shortened as-is)
                            val gross = grossWT.toDoubleOrNull() ?: 0.0
                            val stoneW = stoneWt.toDoubleOrNull() ?: 0.0
                            val diamondW = dimondWt.toDoubleOrNull() ?: 0.0
                            val rate = ratePerGRam.toDoubleOrNull() ?: 0.0
                            val stoneAmtVal = stoneAmt.toDoubleOrNull() ?: 0.0
                            val diamondAmtVal = (s.DiamondSellAmount.toDoubleOrNull()
                                ?: s.DiamondSellAmount.toDoubleOrNull()
                                ?: 0.0)

                            val makingPerGramVal = (s.MakingPerGram.toDoubleOrNull() ?: 0.0)
                            val makingPercentVal = (s.MakingPercentage.toDoubleOrNull() ?: 0.0)
                            val fixMakingVal = (s.MakingFixedAmt.toDoubleOrNull() ?: 0.0)

                            val wastageWtVal = wastage.toDoubleOrNull() ?: 0.0
                            val finePercentVal = finePercentage.toDoubleOrNull() ?: 0.0

                            val netWtCalc = gross - stoneW - diamondW
                            val netWtStr = "%.3f".format(netWtCalc.coerceAtLeast(0.0))

                            val metalAmtVal = netWtCalc * rate
                            val metalAmtStr = "%.2f".format(metalAmtVal)

                            val makingByGram = netWtCalc * makingPerGramVal
                            val makingByPercent = metalAmtVal * (makingPercentVal / 100.0)
                            val fixWastageAmt = wastageWtVal * rate

                            val totalMakingVal =
                                makingByGram + fixMakingVal + makingByPercent + fixWastageAmt
                            val makingAmtStr = "%.2f".format(totalMakingVal)

                            val fineWtVal = netWtCalc * (finePercentVal / 100.0)
                            val fineWtStr = "%.3f".format(fineWtVal)

                            val itemAmtVal =
                                stoneAmtVal + diamondAmtVal + metalAmtVal + totalMakingVal
                            val itemAmtStr = "%.2f".format(itemAmtVal)

                            NetWt = netWtStr
                            finePlusWt = fineWtStr
                            itemAmt = itemAmtStr

                            val updated = s.copy(
                                GrossWt = grossWT,
                                NetWt = netWtStr,
                                TotalWt = totalWt,
                                PackingWeight = packingWt,
                                TotalStoneWeight = stoneWt,
                                DiamondWeight = dimondWt,
                                StoneAmount = stoneAmt,
                                TotalStoneAmount = stoneAmt,
                                RatePerGram = ratePerGRam,
                                MetalRate = ratePerGRam,
                                MetalAmount = metalAmtStr,
                                MakingCharg = makingAmtStr,
                                HallmarkAmount = hallMarkAmt,
                                MRP = mrp,
                                ItemAmount = itemAmtStr,
                                TotalItemAmount = itemAmtStr,
                                Amount = itemAmtStr,
                                TotalAmount = itemAmtStr,
                                Purity = purity,
                                Size = size,
                                FineWastageWt = fineWtStr,
                                FinePercentage = finePercentage,
                                DiamondColour = typeOfColors,
                                Description = remark,
                                MakingPerGram = makingPerGramVal.toString(),
                                MakingPercentage = makingPercentVal.toString(),
                                MakingFixedAmt = fixMakingVal.toString(),
                                fixWastage = wastage,
                                Quantity = qty,
                                qty = qty.toIntOrNull() ?: s.qty
                            )

                            onSave(updated)
                            onDismiss()
                        },
                        icon = painterResource(id = R.drawable.check_circle),
                        iconDescription = "Save",
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(start = 4.dp)
                    )
                }
            }
        }
    }
}



