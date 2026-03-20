package com.loyalstring.rfid.ui.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Close

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.loyalstring.rfid.R
import com.loyalstring.rfid.worker.LocaleHelper

@Composable
fun FilePickerDialog(
    onDismiss: () -> Unit,
    onFileSelected: () -> Unit,
    onConfirm: () -> Unit

)
{

    val context: Context = LocalContext.current

    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language
    val localizedContext = LocaleHelper.applyLocale(context, currentLang ?: "en")
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(25.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // ❌ Close button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = localizedContext.getString(R.string.close),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onDismiss() }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Upload area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Transparent)
                        .drawBehind {
                            val strokeWidth = 5.dp.toPx()
                            val dashWidth = 3.dp.toPx()
                            val dashGap = 3.dp.toPx()

                            drawRoundRect(
                                brush = BackgroundGradient,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidth,
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(dashWidth, dashGap), 0f
                                    )
                                ),
                                cornerRadius = CornerRadius(16.dp.toPx())
                            )
                        }
                        .clickable { onFileSelected() },
                    contentAlignment = Alignment.Center
                )  {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.Icon(
                            painter = painterResource(id = R.drawable.arrow_upload_progress),
                            contentDescription = "Upload",
                            modifier = Modifier.size(50.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(Modifier.height(8.dp))

                        Row {
                            Text(
                                localizedContext.getString(R.string.drag_and_drop_file_or),
                                fontSize = 14.sp
                            )
                            Text(
                                text = localizedContext.getString(R.string.choose_file),
                                fontSize = 14.sp,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable { onFileSelected() }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

// ✅ Info row outside red box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(localizedContext.getString(R.string.support_formats_xls_xlsx), fontSize = 10.sp, color = Color.Gray)
                    Text(localizedContext.getString(R.string.maximum_size_250_mb), fontSize = 10.sp, color = Color.Gray)
                }


                Spacer(Modifier.height(16.dp))

                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent // transparent background
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                brush = BackgroundGradient,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(id = R.drawable.ic_cancel), // ✅ your drawable
                                contentDescription = localizedContext.getString(R.string.cancel),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(localizedContext.getString(R.string.cancel), color = Color.White)
                        }
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent // transparent background
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                brush = BackgroundGradient,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(id = R.drawable.check_circle), // ✅ your drawable
                                contentDescription = localizedContext.getString(R.string.ok),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(localizedContext.getString(R.string.ok), color = Color.White)
                        }
                    }

                }
            }
        }
    }
}
