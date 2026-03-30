package com.loyalstring.rfid.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.loyalstring.rfid.ui.utils.PrinterManager

import android.Manifest

import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanPrintData

@Composable
fun PrinterScreen(navController: NavHostController) {
    val context = LocalContext.current
    val activity = context as Activity
    val printerManager = remember { PrinterManager(context) }
    var status by remember { mutableStateOf("Not connected") }
    val printData = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<DeliveryChallanPrintData>("printer_print_data")
    Column(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = {
                logBondedDevices()
                if (hasBluetoothPermissions(activity)) {
                    printerManager.connectBluetooth("60:6E:41:BE:B7:99") { _, msg ->
                        status = msg
                    }
                } else {
                    requestBluetoothPermissions(activity)
                    status = "Please grant Bluetooth permissions"
                }
            }
        ) {
            Text("Connect Bluetooth Printer")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (printData != null) {
                    printerManager.printDeliveryChallanCompact(printData,"") { success, msg ->
                        status = msg
                    }
                } else {
                    status = "No print data found"
                }
            }
        ) {
            Text("Print Challan")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = status)
    }
}
fun logBondedDevices() {
    val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
    adapter?.bondedDevices?.forEach { device ->
        android.util.Log.d(
            "BT_BONDED",
            "name=${device.name}, address=${device.address}, type=${device.type}"
        )
    }
}

fun hasBluetoothPermissions(activity: Activity): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

fun requestBluetoothPermissions(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            ),
            1001
        )
    }
}

