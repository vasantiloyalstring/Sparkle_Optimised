package com.loyalstring.rfid.data.model.deliveryChallan



import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanPrintData
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanItemPrint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import android.Manifest
import android.util.Log


object BluetoothThermalPrinterHelper {

    // Standard SPP UUID (most Bluetooth thermal printers use this)


    /**
     * Main function: connect to printer by name and print challan.
     */

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /*suspend fun printDeliveryChallan(
        activity: Activity,
        data: DeliveryChallanPrintData,
        printerName: String = "XP-Q600"
    ) = withContext(Dispatchers.IO) {

        // 1. Adapter check
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            showToast(activity, "Bluetooth not supported")
            return@withContext
        }

        // 2. Bluetooth on check
        if (!adapter.isEnabled) {
            showToast(activity, "Please enable Bluetooth")
            return@withContext
        }

        // 3. Permission check
        if (!checkBluetoothPermissions(activity)) {
            return@withContext
        }

        // 4. Find paired printer
        // Bluetooth permission check required BEFORE accessing bondedDevices
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                1001
            )
            return@withContext
        }

// NOW IT WILL NOT CRASH
        val device = adapter.bondedDevices.firstOrNull {
            it.name.equals(printerName, ignoreCase = true)
        }

        if (device == null) {
            showToast(activity, "Printer $printerName not paired")
            return@withContext
        }

        var socket: BluetoothSocket? = null

        try {
            // Try standard SPP UUID first
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)*/

    suspend fun printDeliveryChallan(
        context: Context,
        data: DeliveryChallanPrintData,
        printerName: String = "4B-2043PB-B799"
    ) = withContext(Dispatchers.IO) {
        //showToast(context, "Printed on  device1111$printerName")
        Log.d("@@","Printed on  device1111")
        // BluetoothAdapter
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return@withContext //showToast(context, "Bluetooth not supported")

        if (!adapter.isEnabled) {
            return@withContext //showToast(context, "Please enable Bluetooth")
        }

        // ❗ Must check permission but NOT request it (Context cannot request)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //showToast(context, "❌ Missing BLUETOOTH_CONNECT permission")
            Log.e("@BT", "BLUETOOTH_CONNECT permission not granted")
            return@withContext
        }

        // Now safe to access bonded devices
        val device = adapter.bondedDevices.firstOrNull {
            it.name.equals(printerName, ignoreCase = true)
        } ?: return@withContext //showToast(context, "Printer not paired")

        var socket: BluetoothSocket? = null

        try {
            Log.d("@@","Printed on  device11112222")
          //  showToast(context, "Printed on  device$printerName")
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            try {
                socket.connect()
            } catch (e: IOException) {
                // XP-Q600 fallback RFCOMM(1)
                val fallback = device.javaClass
                    .getMethod("createRfcommSocket", Int::class.java)
                    .invoke(device, 1) as BluetoothSocket

                fallback.connect()
                socket = fallback
            }

            val out = socket.outputStream

            // Initialize printer
            out.write(byteArrayOf(0x1B, 0x40))

            val builder = StringBuilder()

            builder.appendLine(centerText(data.branchName.uppercase(), 32))
            builder.appendLine(centerText(data.city.uppercase(), 32))
            builder.appendLine("--------------------------------")
            builder.appendLine("Created : ${data.createdDateTime}")
            builder.appendLine("Customer: ${data.customerName}")
            builder.appendLine("Quotation#: ${data.quotationNo}")
            builder.appendLine("Phone   : ${data.phone}")
            builder.appendLine("--------------------------------")
            builder.appendLine("ITEM DETAILS")
            builder.appendLine("--------------------------------")

            data.items.forEach { item ->
                builder.appendLine(item.itemName)
                builder.appendLine("Pur:${item.purity}  Pcs:${item.pcs}")
                builder.appendLine("G:${item.grossWt} S:${item.stoneWt} N:${item.netWt}")
                builder.appendLine("Rt:${item.ratePerGram} Wt:${item.wastage} Amt:${item.itemAmount}")
                builder.appendLine("--------------------------------")
            }

            builder.appendLine("Taxable Amt : ${data.taxableAmount}")
            builder.appendLine("CGST ${"%.2f".format(data.cgstPercent)}% : ${data.cgstAmount}")
            builder.appendLine("SGST ${"%.2f".format(data.sgstPercent)}% : ${data.sgstAmount}")
            builder.appendLine("--------------------------------")
            builder.appendLine("TOTAL NET AMT : ${data.totalNetAmount}")
            builder.appendLine("--------------------------------")
            builder.appendLine("Thank you!")

            out.write(builder.toString().toByteArray())
            out.write(byteArrayOf(0x0A, 0x0A, 0x0A))
            out.write(byteArrayOf(0x1D, 0x56, 0x01))
            out.flush()

           // showToast(context, "Printed on $printerName")

        } catch (e: Exception) {
           // showToast(context, "Print failed: ${e.message}")
        } finally {
            try {
                socket?.close()
            } catch (_: IOException) {
            }
        }
    }


    private fun checkBluetoothPermissions(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val neededPermissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            val missing = neededPermissions.any {
                ActivityCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missing) {
                ActivityCompat.requestPermissions(activity, neededPermissions, 1001)
                return false
            }
        }
        return true
    }
    private fun centerText(text: String, maxChars: Int): String {
        if (text.length >= maxChars) return text
        val padding = (maxChars - text.length) / 2
        return " ".repeat(padding) + text
    }

    private fun showToast(context: Context, msg: String) {
        android.os.Handler(context.mainLooper).post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
