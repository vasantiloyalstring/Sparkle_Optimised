package com.loyalstring.rfid.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList

import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
import com.loyalstring.rfid.data.model.deliveryChallan.InvoiceFields
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.ui.utils.GradientButtonIcon

// Agar FieldWithLabel & InputField dusre file me hain:
import com.loyalstring.rfid.ui.screens.FieldWithLabel
import com.loyalstring.rfid.ui.screens.InputField

// Agar SampleOutFields data class alag package me hai:
import com.loyalstring.rfid.data.model.sampleOut.SampleOutFields
import com.loyalstring.rfid.viewmodel.UiState

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun SampleOutFieldsDialog(
    onDismiss: () -> Unit,
    onConfirm: (SampleOutFields) -> Unit,
    branchList: List<BranchModel>,
    salesmanList: UiState<List<EmployeeList>>
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val calendar = Calendar.getInstance()

    var date by remember { mutableStateOf("") }
    var returnDate by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
            ) {
                // 🔹 Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3A3A3A))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.stylus_note),
                            contentDescription = "Sample Out Fields",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Sample Out Fields",
                            fontSize = 18.sp,
                            color = Color.White,
                            fontFamily = poppins
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 🔹 Content
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Date
                    FieldWithLabel(
                        label = "Date",
                        value = date,
                        placeholder = "Select date",
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selected = Calendar.getInstance().apply {
                                        set(year, month, day)
                                    }
                                    date = dateFormat.format(selected.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    // Return Date
                    FieldWithLabel(
                        label = "Return Date",
                        value = returnDate,
                        placeholder = "Select return date",
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selected = Calendar.getInstance().apply {
                                        set(year, month, day)
                                    }
                                    returnDate = dateFormat.format(selected.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    // Description
                    InputField(
                        label = "Description",
                        value = description,
                        onValueChange = { description = it }
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 🔹 Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GradientButtonIcon(
                        text = "Cancel",
                        onClick = onDismiss,
                        icon = painterResource(id = R.drawable.ic_cancel),
                        iconDescription = "Cancel",
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .padding(end = 6.dp)
                    )
                    GradientButtonIcon(
                        text = "Ok",
                        onClick = {
                            if (date.isBlank() || returnDate.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Please select Date & Return Date",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@GradientButtonIcon
                            }

                            onConfirm(
                                SampleOutFields(
                                    date = date,
                                    returnDate = returnDate,
                                    description = description
                                )
                            )
                        },
                        icon = painterResource(id = R.drawable.check_circle),
                        iconDescription = "OK",
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .padding(start = 6.dp)
                    )
                }
            }
        }
    }
}
