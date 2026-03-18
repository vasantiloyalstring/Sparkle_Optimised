package com.loyalstring.rfid.ui.screens



import android.app.DatePickerDialog
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.sampleIn.SampleInFiledDailog

import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.worker.LocaleHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun SampleInDetailsDailog(
    onDismiss: () -> Unit,
    onConfirm: (SampleInFiledDailog) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val calendar = Calendar.getInstance()

    var date by remember { mutableStateOf("") }
    var returnDate by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language
    val localizedContext = LocaleHelper.applyLocale(context, currentLang ?: "en")

    Dialog(onDismissRequest = onDismiss) {
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
                // ✅ Header (same design)
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
                            contentDescription = localizedContext.getString(R.string.cd_sample_out_fields_icon),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = localizedContext.getString(R.string.samplein_details), // ✅ if you have string resource, replace
                            fontSize = 18.sp,
                            color = Color.White,
                            fontFamily = poppins
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ✅ Content
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {

                    FieldWithLabel(
                        label = localizedContext.getString(R.string.label_date),
                        value = date,
                        placeholder = localizedContext.getString(R.string.placeholder_select_date),
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selected = Calendar.getInstance().apply { set(year, month, day) }
                                    date = dateFormat.format(selected.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    FieldWithLabel(
                        label = localizedContext.getString(R.string.label_return_date),
                        value = returnDate,
                        placeholder = localizedContext.getString(R.string.placeholder_select_return_date),
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selected = Calendar.getInstance().apply { set(year, month, day) }
                                    returnDate = dateFormat.format(selected.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    InputField(
                        label = localizedContext.getString(R.string.label_description),
                        value = description,
                        onValueChange = { description = it }
                    )
                }

                Spacer(Modifier.height(10.dp))

                // ✅ Buttons (same)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GradientButtonIcon(
                        text = localizedContext.getString(R.string.btn_cancel),
                        onClick = onDismiss,
                        icon = painterResource(id = R.drawable.ic_cancel),
                        iconDescription = localizedContext.getString(R.string.btn_cancel),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .padding(end = 6.dp)
                    )

                    GradientButtonIcon(
                        text = localizedContext.getString(R.string.btn_ok),
                        onClick = {
                            if (date.isBlank() || returnDate.isBlank()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.msg_select_date_return),
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@GradientButtonIcon
                            }

                            onConfirm(
                                SampleInFiledDailog(
                                    date = date,
                                    returnDate = returnDate,
                                    description = description
                                )
                            )
                        },
                        icon = painterResource(id = R.drawable.check_circle),
                        iconDescription = localizedContext.getString(R.string.btn_ok),
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
