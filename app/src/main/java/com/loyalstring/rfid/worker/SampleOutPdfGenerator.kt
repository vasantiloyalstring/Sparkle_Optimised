package com.loyalstring.rfid.worker



import android.content.Context
import android.net.Uri
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.loyalstring.rfid.data.model.sampleOut.SampleOutDetails
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object SampleOutPdfGenerator {

    data class HeaderData(
        val companyName: String,
        val title: String = "Sample Out Print",
        val customerName: String,
        val addressCity: String,
        val contactNo: String,
        val sampleOutNo: String,
        val date: String,
        val returnDate: String
    )

    fun generatePdfUri(
        context: Context,
        header: HeaderData,
        items: List<SampleOutDetails>
    ): Uri {
        val file = generatePdfFile(context, header, items)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    private fun generatePdfFile(
        context: Context,
        header: HeaderData,
        items: List<SampleOutDetails>
    ): File {

        val pageWidth = 595
        val pageHeight = 842

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = 1.5f
        }
        val grayFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EEEEEE") }

        fun textPaint(size: Float, bold: Boolean = false): Paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = size
                typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            }

        val marginX = 28f
        var y = 55f

        // Header
        val pCompany = textPaint(16f, true)
        val companyW = pCompany.measureText(header.companyName)
        canvas.drawText(header.companyName, (pageWidth - companyW) / 2f, y, pCompany)

        y += 18f
        canvas.drawLine(marginX, y, pageWidth - marginX, y, linePaint)

        y += 35f
        val pTitle = textPaint(12f, true)
        val titleW = pTitle.measureText(header.title)
        canvas.drawText(header.title, (pageWidth - titleW) / 2f, y, pTitle)

        // Left/Right blocks
        y += 35f
        val leftX = marginX
        val rightX = pageWidth - marginX - 10f

        val pLabel = textPaint(10.5f, true)
        val pValue = textPaint(10.5f, false)

        fun drawKVLeft(label: String, value: String) {
            canvas.drawText("$label: ", leftX, y, pLabel)
            canvas.drawText(value, leftX + pLabel.measureText("$label: "), y, pValue)
            y += 16f
        }

        var yRight = y
        fun drawKVRight(label: String, value: String) {
            val txt = "$label: $value"
            val w = pLabel.measureText(txt)
            canvas.drawText(txt, rightX - w, yRight, pLabel)
            yRight += 16f
        }

        drawKVLeft("Customer Name", header.customerName)
        drawKVLeft("Address/City", header.addressCity)
        drawKVLeft("Contact No", header.contactNo)

        drawKVRight("Sample Out No", header.sampleOutNo)
        drawKVRight("Date", header.date)
        drawKVRight("ReturnDate", header.returnDate)

        y = maxOf(y, yRight)

        // Table
        y += 18f

        val tableLeft = marginX
        val tableRight = pageWidth - marginX
        val tableWidth = tableRight - tableLeft

        val colFractions = floatArrayOf(0.08f, 0.33f, 0.11f, 0.11f, 0.11f, 0.11f, 0.07f, 0.08f)
        val colW = colFractions.map { it * tableWidth }.toFloatArray()
        fun colX(i: Int): Float {
            var x = tableLeft
            repeat(i) { x += colW[it] }
            return x
        }

        val headerH = 34f
        val rowH = 34f

        canvas.drawRect(tableLeft, y, tableRight, y + headerH, grayFill)

        val headers = listOf("Sr.No", "Item Details", "Gross Wt", "Stone Wt", "Diamond\nWt", "Net Wt", "Pieces", "Status")
        val pTh = textPaint(10f, true)

        var x = tableLeft
        for (i in headers.indices) {
            canvas.drawRect(x, y, x + colW[i], y + headerH, linePaint)
            val lines = headers[i].split("\n")
            if (lines.size == 1) {
                canvas.drawText(lines[0], x + (colW[i] - pTh.measureText(lines[0])) / 2f, y + 22f, pTh)
            } else {
                canvas.drawText(lines[0], x + (colW[i] - pTh.measureText(lines[0])) / 2f, y + 16f, pTh)
                canvas.drawText(lines[1], x + (colW[i] - pTh.measureText(lines[1])) / 2f, y + 30f, pTh)
            }
            x += colW[i]
        }

        y += headerH

        fun safeD(s: String?) = s?.toDoubleOrNull() ?: 0.0
        fun fmt3(v: String?) = String.format(Locale.US, "%.3f", safeD(v))

        var totalGross = 0.0
        var totalStone = 0.0
        var totalDia = 0.0
        var totalNet = 0.0
        var totalPieces = 0

        val pTd = textPaint(10f, false)
        val pBold = textPaint(10f, true)

        fun buildItemDetails(it: SampleOutDetails): String {
            val category = it.CategoryName ?: ""
            val product = it.ProductName ?: ""
            val design = it.DesignName ?: ""
            val purity = it.Purity ?: ""
            val itemCode = it.ItemCode ?: ""
            return listOf(category, product, itemCode, purity, design).filter { s -> s.isNotBlank() }.joinToString(" - ")
        }

        items.forEachIndexed { idx, it ->
            totalGross += safeD(it.GrossWt)
            totalStone += safeD(it.StoneAmt)
            totalDia += safeD(it.DiamondWt)
            totalNet += safeD(it.NetWt)
            totalPieces += (it.qty ?: it.Pieces?.toIntOrNull() ?: 0)

            val row = listOf(
                (idx + 1).toString(),
                buildItemDetails(it),
                fmt3(it.GrossWt),
                fmt3(it.StoneAmt),
                fmt3(it.DiamondWt),
                fmt3(it.NetWt),
                (it.qty ?: it.Pieces?.toIntOrNull() ?: 0).toString(),
                (it.ChallanStatus ?: "Sample Out")
            )

            // borders
            var rx = tableLeft
            for (c in row.indices) {
                canvas.drawRect(rx, y, rx + colW[c], y + rowH, linePaint)
                rx += colW[c]
            }

            // text
            for (c in row.indices) {
                val cx = colX(c)
                if (c == 1) {
                    val txt = row[c]
                    val part1 = txt.take(36)
                    val part2 = txt.drop(36).take(36)
                    canvas.drawText(part1, cx + 6f, y + 15f, pTd)
                    if (part2.isNotBlank()) canvas.drawText(part2, cx + 6f, y + 30f, pTd)
                } else {
                    val txt = row[c]
                    canvas.drawText(txt, cx + (colW[c] - pTd.measureText(txt)) / 2f, y + 22f, pTd)
                }
            }

            y += rowH
        }

        // Total row
        val spanW = colW[0] + colW[1]
        canvas.drawRect(tableLeft, y, tableRight, y + rowH, linePaint)
        canvas.drawRect(tableLeft, y, tableLeft + spanW, y + rowH, linePaint)
        canvas.drawText("Total", tableLeft + (spanW - pBold.measureText("Total")) / 2f, y + 22f, pBold)

        fun drawTotal(colIndex: Int, value: String) {
            val cx = colX(colIndex)
            canvas.drawRect(cx, y, cx + colW[colIndex], y + rowH, linePaint)
            canvas.drawText(value, cx + (colW[colIndex] - pBold.measureText(value)) / 2f, y + 22f, pBold)
        }

        drawTotal(2, String.format(Locale.US, "%.3f", totalGross))
        drawTotal(3, String.format(Locale.US, "%.3f", totalStone))
        drawTotal(4, String.format(Locale.US, "%.3f", totalDia))
        drawTotal(5, String.format(Locale.US, "%.3f", totalNet))
        drawTotal(6, totalPieces.toString())

        document.finishPage(page)

        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(context.cacheDir, "SampleOut_$ts.pdf")
        FileOutputStream(outFile).use { document.writeTo(it) }
        document.close()

        return outFile
    }
}
