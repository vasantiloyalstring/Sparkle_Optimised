package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.loyalstring.rfid.data.model.quotation.QuotationPrintData
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

fun GenerateQuotationPdf(context: Context, data: QuotationPrintData) {
    val pdfDocument = PdfDocument()

    // A4 size for PdfDocument (approx points)
    val pageWidth = 595
    val pageHeight = 842
    val margin = 30

    // ---------- Paints ----------
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        color = Color.BLACK
    }

    // Outer borders (table)
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    // Light grid paint like screenshot (gst box)
    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DADADA")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }

    val tableCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        color = Color.BLACK
    }

    // Totals row style
    val totalRowFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F3F3F3")
        style = Paint.Style.FILL
    }

    val totalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }

    // ---------- Table columns ----------
    // [Image | Particulars | GrossWt | NetWt | Qty | Rate/Gm | Making/Gm | Amount]
    val tableX = margin
    val tableW = pageWidth - (margin * 2)

    val colWidths = intArrayOf(
        (tableW * 0.10).toInt(), // Image
        (tableW * 0.28).toInt(), // Particulars
        (tableW * 0.10).toInt(), // Gross
        (tableW * 0.10).toInt(), // Net
        (tableW * 0.08).toInt(), // Qty
        (tableW * 0.11).toInt(), // Rate/Gm
        (tableW * 0.12).toInt(), // Making/Gm
        (tableW * 0.11).toInt()  // Amount
    )

    val headers = listOf(
        "Image",
        "Particulars",
        "Gross Wt",
        "Net Wt",
        "Quantity",
        "Rate/Gm",
        "Making/Gm",
        "Amount"
    )

    fun drawRowLines(canvas: Canvas, yTop: Int, rowHeight: Int) {
        var x = tableX
        canvas.drawLine(tableX.toFloat(), yTop.toFloat(), (tableX + tableW).toFloat(), yTop.toFloat(), linePaint)
        canvas.drawLine(tableX.toFloat(), (yTop + rowHeight).toFloat(), (tableX + tableW).toFloat(), (yTop + rowHeight).toFloat(), linePaint)
        canvas.drawLine(tableX.toFloat(), yTop.toFloat(), tableX.toFloat(), (yTop + rowHeight).toFloat(), linePaint)

        for (w in colWidths) {
            x += w
            canvas.drawLine(x.toFloat(), yTop.toFloat(), x.toFloat(), (yTop + rowHeight).toFloat(), linePaint)
        }
    }

    fun drawTextInCell(canvas: Canvas, text: String, x: Int, y: Int, w: Int, paint: Paint) {
        val clipped = text.take(50)
        canvas.drawText(clipped, (x + 6).toFloat(), y.toFloat(), paint)
    }

    fun safeStr(s: String?): String = s?.trim().orEmpty()
    fun safeNumStr(s: String?, fallback: String = "0.00"): String {
        val v = s?.toDoubleOrNull()
        return if (v == null) fallback else String.format("%.3f", v)
    }
    fun safeIntStr(s: String?, fallback: String = "0"): String {
        val v = s?.toDoubleOrNull()
        return if (v == null) fallback else String.format("%.0f", v)
    }

    // ---------- GST Summary Box like screenshot ----------
    fun drawGstSummaryBox(
        canvas: Canvas,
        boxX: Int,
        boxY: Int,
        boxW: Int,
        rowHeights: IntArray,
        dividerX: Int,
        labels: List<String>,
        values: List<String>
    ) {
        val labelBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
        }

        val valueRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = Color.BLACK
            textAlign = Paint.Align.RIGHT
        }

        val boxH = rowHeights.sum()

        // outer rect
        canvas.drawRect(
            boxX.toFloat(), boxY.toFloat(),
            (boxX + boxW).toFloat(), (boxY + boxH).toFloat(),
            gridPaint
        )

        // divider
        canvas.drawLine(
            dividerX.toFloat(), boxY.toFloat(),
            dividerX.toFloat(), (boxY + boxH).toFloat(),
            gridPaint
        )

        var yTop = boxY
        for (i in rowHeights.indices) {
            val h = rowHeights[i]

            // row line (top)
            canvas.drawLine(
                boxX.toFloat(), yTop.toFloat(),
                (boxX + boxW).toFloat(), yTop.toFloat(),
                gridPaint
            )

            val textY = yTop + (h / 2) + 5
            canvas.drawText(labels[i], (boxX + 12).toFloat(), textY.toFloat(), labelBold)
            canvas.drawText(values[i], (boxX + boxW - 12).toFloat(), textY.toFloat(), valueRight)

            yTop += h
        }

        // bottom line
        canvas.drawLine(
            boxX.toFloat(), (boxY + boxH).toFloat(),
            (boxX + boxW).toFloat(), (boxY + boxH).toFloat(),
            gridPaint
        )
    }

    // ---------- Pagination ----------
    val itemsPerPage = 14
    val pages = if (data.items.isEmpty()) 1 else ((data.items.size + itemsPerPage - 1) / itemsPerPage)

    var itemIndex = 0

    // Precompute totals for totals row
    val totalGross = data.items.sumOf { it.grossWt?.toDoubleOrNull() ?: 0.0 }
    val totalNet = data.items.sumOf { it.netWt?.toDoubleOrNull() ?: 0.0 }
    val totalQty = data.items.sumOf { it.qty?.toDoubleOrNull() ?: 0.0 }
    val totalAmt = data.items.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }

    for (pageNo in 0 until pages) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo + 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // ✅ Avoid any viewer “black” artifacts: fill white background
        canvas.drawColor(Color.WHITE)

        var y = margin + 10

        // -------- Title --------
        canvas.drawText("QUOTATION", (pageWidth / 2).toFloat(), y.toFloat(), titlePaint)
        y += 25

        // -------- Top fields --------
        val leftX = margin
        val rightX = pageWidth / 2 + 10

        fun drawPair(x: Int, label: String, value: String, yPos: Int) {
            canvas.drawText(label, x.toFloat(), yPos.toFloat(), labelPaint)
            canvas.drawText(value, (x + 120).toFloat(), yPos.toFloat(), valuePaint)
        }

        val y4 = y + 18
        drawPair(leftX, "Customer Name:", safeStr(data.customerName), y4)
        drawPair(rightX, "Remark:", safeStr(data.remark), y4)

        val y5 = y4 + 18
        drawPair(leftX, "Mobile:", safeStr(data.customerMobile), y5)

        val y6 = y5 + 18
        drawPair(leftX, "Customer Address:", safeStr(data.customerAddress), y6)

        y = y6 + 20

        // separator
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), linePaint)
        y += 14

        // -------- Table header --------
        val headerHeight = 24
        drawRowLines(canvas, y, headerHeight)
        var x = tableX
        for (i in headers.indices) {
            drawTextInCell(canvas, headers[i], x, y + 16, colWidths[i], tableHeaderPaint)
            x += colWidths[i]
        }
        y += headerHeight

        // -------- Table rows --------
        val rowHeight = 36
        val endIndex = min(itemIndex + itemsPerPage, data.items.size)

        if (data.items.isEmpty()) {
            drawRowLines(canvas, y, rowHeight)
            x = tableX
            drawTextInCell(canvas, "-", x, y + 20, colWidths[0], tableCellPaint)
            x += colWidths[0]
            drawTextInCell(canvas, "-", x, y + 20, colWidths[1], tableCellPaint)
            y += rowHeight
        } else {
            for (i in itemIndex until endIndex) {
                val it = data.items[i]
                drawRowLines(canvas, y, rowHeight)

                var cx = tableX

                // Image column left blank / "-"
                drawTextInCell(canvas, "-", cx, y + 20, colWidths[0], tableCellPaint)
                cx += colWidths[0]

                // ✅ All columns (INCLUDING QTY) aligned
                drawTextInCell(canvas, safeStr(it.particulars).ifBlank { "-" }, cx, y + 20, colWidths[1], tableCellPaint); cx += colWidths[1]
                drawTextInCell(canvas, safeStr(it.grossWt).ifBlank { "-" }, cx, y + 20, colWidths[2], tableCellPaint); cx += colWidths[2]
                drawTextInCell(canvas, safeStr(it.netWt).ifBlank { "-" }, cx, y + 20, colWidths[3], tableCellPaint); cx += colWidths[3]
                drawTextInCell(canvas, safeStr(it.qty).ifBlank { "-" }, cx, y + 20, colWidths[4], tableCellPaint); cx += colWidths[4]
                drawTextInCell(canvas, safeStr(it.ratePerGm).ifBlank { "-" }, cx, y + 20, colWidths[5], tableCellPaint); cx += colWidths[5]
                drawTextInCell(canvas, safeStr(it.makingPerGm).ifBlank { "-" }, cx, y + 20, colWidths[6], tableCellPaint); cx += colWidths[6]
                drawTextInCell(canvas, safeStr(it.amount).ifBlank { "-" }, cx, y + 20, colWidths[7], tableCellPaint)

                y += rowHeight
            }
        }

        itemIndex = endIndex

        // ✅ Totals row inside table like screenshot (only on last page after last item)
        if (pageNo == pages - 1 && itemIndex == data.items.size && data.items.isNotEmpty()) {
            // background
            canvas.drawRect(
                tableX.toFloat(),
                y.toFloat(),
                (tableX + tableW).toFloat(),
                (y + rowHeight).toFloat(),
                totalRowFillPaint
            )

            drawRowLines(canvas, y, rowHeight)

            var cx = tableX
            drawTextInCell(canvas, "-", cx, y + 20, colWidths[0], totalTextPaint); cx += colWidths[0]
            drawTextInCell(canvas, "-", cx, y + 20, colWidths[1], totalTextPaint); cx += colWidths[1]

            drawTextInCell(canvas, String.format("%.3f", totalGross), cx, y + 20, colWidths[2], totalTextPaint); cx += colWidths[2]
            drawTextInCell(canvas, String.format("%.3f", totalNet), cx, y + 20, colWidths[3], totalTextPaint); cx += colWidths[3]
            drawTextInCell(canvas, String.format("%.0f", totalQty), cx, y + 20, colWidths[4], totalTextPaint); cx += colWidths[4]

            // Rate/Making blank like screenshot
            drawTextInCell(canvas, "", cx, y + 20, colWidths[5], totalTextPaint); cx += colWidths[5]
            drawTextInCell(canvas, "", cx, y + 20, colWidths[6], totalTextPaint); cx += colWidths[6]

            drawTextInCell(canvas, String.format("%.3f", totalAmt), cx, y + 20, colWidths[7], totalTextPaint)

            y += rowHeight
        }

        // -------- Bottom-right Summary box like screenshot (only last page) --------
        if (pageNo == pages - 1) {
            val cgstVal = data.cgst.toDoubleOrNull() ?: 0.0
            val sgstVal = data.sgst.toDoubleOrNull() ?: 0.0
            val igstVal = data.igst.toDoubleOrNull() ?: 0.0

            val baseTotal = data.totalAmount.toDoubleOrNull() ?: totalAmt
            val afterGst = baseTotal + cgstVal + sgstVal + igstVal

            val boxW = 420
            val rowHeights = intArrayOf(34, 34, 34, 34, 44)
            val boxH = rowHeights.sum()

            val boxX = pageWidth - margin - boxW
            val boxY = pageHeight - margin - boxH - 40
            val dividerX = boxX + (boxW * 0.40).toInt()

            drawGstSummaryBox(
                canvas = canvas,
                boxX = boxX,
                boxY = boxY,
                boxW = boxW,
                rowHeights = rowHeights,
                dividerX = dividerX,
                labels = listOf("CGST:", "SGST:", "IGST:", "After GST:", "Total Amount:"),
                values = listOf(
                    String.format("%.2f", cgstVal),
                    String.format("%.2f", sgstVal),
                    String.format("%.2f", igstVal),
                    String.format("%.2f", afterGst),
                    String.format("%.2f", afterGst)
                )
            )

            // Footer texts (like screenshot)
            canvas.drawText("Customer Sign:", margin.toFloat(), (pageHeight - margin).toFloat(), labelPaint)
            canvas.drawText("For VT", (pageWidth - margin - 60).toFloat(), (pageHeight - margin).toFloat(), labelPaint)
        }

        pdfDocument.finishPage(page)
    }

    // ---------- Save ----------
    val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "quotations")
    if (!dir.exists()) dir.mkdirs()

    val safeNo = data.quotationNo.ifBlank { "NA" }
    val file = File(dir, "Quotation_$safeNo.pdf")

    FileOutputStream(file).use { out ->
        pdfDocument.writeTo(out)
    }
    pdfDocument.close()

    // ---------- Open ----------
    openPdfFile(context, file)
}

private fun openPdfFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    }
    context.startActivity(intent)
}


/*
package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.loyalstring.rfid.data.model.quotation.QuotationPrintData
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

fun GenerateQuotationPdf(context: Context, data: QuotationPrintData) {
    val pdfDocument = PdfDocument()

    // A4 approx size in "points-like px" used by PdfDocument
    val pageWidth = 595
    val pageHeight = 842
    val margin = 30

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        color = Color.BLACK
    }

    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 1f
        style = Paint.Style.STROKE   // ✅ important
    }

    val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }

    val tableCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        color = Color.BLACK
    }

    // ---------- Table columns (similar to your screenshot) ----------
    // [Image | Particulars | GrossWt | NetWt | Qty | Rate/Gm | Making/Gm | Amount]
    val tableX = margin
    val tableW = pageWidth - (margin * 2)

    val colWidths = intArrayOf(
        (tableW * 0.10).toInt(), // Image
        (tableW * 0.28).toInt(), // Particulars
        (tableW * 0.10).toInt(), // Gross
        (tableW * 0.10).toInt(), // Net
        (tableW * 0.08).toInt(), // Qty
        (tableW * 0.11).toInt(), // Rate/Gm
        (tableW * 0.12).toInt(), // Making/Gm
        (tableW * 0.11).toInt()  // Amount
    )
    val headers = listOf("Image", "Particulars", "Gross Wt", "Net Wt", "Quantity", "Rate/Gm", "Making/Gm", "Amount")

    fun drawRowLines(canvas: Canvas, yTop: Int, rowHeight: Int) {
        var x = tableX
        canvas.drawLine(tableX.toFloat(), yTop.toFloat(), (tableX + tableW).toFloat(), yTop.toFloat(), linePaint)
        canvas.drawLine(tableX.toFloat(), (yTop + rowHeight).toFloat(), (tableX + tableW).toFloat(), (yTop + rowHeight).toFloat(), linePaint)
        canvas.drawLine(tableX.toFloat(), yTop.toFloat(), tableX.toFloat(), (yTop + rowHeight).toFloat(), linePaint)

        for (w in colWidths) {
            x += w
            canvas.drawLine(x.toFloat(), yTop.toFloat(), x.toFloat(), (yTop + rowHeight).toFloat(), linePaint)
        }
    }

    fun drawTextInCell(canvas: Canvas, text: String, x: Int, y: Int, w: Int, paint: Paint) {
        val clipped = text.take(40) // simple clip
        canvas.drawText(clipped, (x + 6).toFloat(), (y).toFloat(), paint)
    }

    // ---------- Pagination ----------
    val itemsPerPage = 14  // adjust if needed
    val pages = if (data.items.isEmpty()) 1 else ((data.items.size + itemsPerPage - 1) / itemsPerPage)

    var itemIndex = 0

    for (pageNo in 0 until pages) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo + 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = margin + 10

        // -------- Title --------
        canvas.drawText("QUOTATION", (pageWidth / 2).toFloat(), y.toFloat(), titlePaint)
        y += 25

        // -------- Top section: Left & Right --------
        val leftX = margin
        val rightX = pageWidth / 2 + 10

        fun drawPair(x: Int, label: String, value: String, yPos: Int) {
            canvas.drawText(label, x.toFloat(), yPos.toFloat(), labelPaint)
            canvas.drawText(value, (x + 120).toFloat(), yPos.toFloat(), valuePaint)
        }

       */
/* val y1 = y + 10
        drawPair(leftX, "Owner Name:", data.ownerName, y1)
        drawPair(rightX, "Quotation No:", data.quotationNo, y1)

        val y2 = y1 + 18
        drawPair(leftX, "Owner Address:", data.ownerAddress, y2)
        drawPair(rightX, "Date:", data.date, y2)

        val y3 = y2 + 18
       drawPair(leftX, "Contact:", data.customerMobile, y3)
        //drawPair(rightX, "Sales Men:", data.salesMen, y3)*//*


        val y4 = y + 18
        drawPair(leftX, "Customer Name:", data.customerName, y4)
        drawPair(rightX, "Remark:", data.remark.toString(), y4)

        val y5 = y4 + 18
        drawPair(leftX, "Mobile:", data.customerMobile, y5)

        val y6 = y5 + 18
        drawPair(leftX, "Customer Address:", data.customerAddress, y6)

        y = y6 + 20

        // separator
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), linePaint)
        y += 14

        // -------- Table header --------
        val headerHeight = 24
        drawRowLines(canvas, y, headerHeight)
        var x = tableX
        for (i in headers.indices) {
            drawTextInCell(canvas, headers[i], x, y + 16, colWidths[i], tableHeaderPaint)
            x += colWidths[i]
        }
        y += headerHeight

        // -------- Table rows --------
        val rowHeight = 36
        val endIndex = min(itemIndex + itemsPerPage, data.items.size)

        if (data.items.isEmpty()) {
            // Empty state row
            drawRowLines(canvas, y, rowHeight)
            x = tableX
            drawTextInCell(canvas, "-", x, y + 20, colWidths[0], tableCellPaint)
            x += colWidths[0]
            drawTextInCell(canvas, "-", x, y + 20, colWidths[1], tableCellPaint)
            y += rowHeight
        } else {
            for (i in itemIndex until endIndex) {
                val it = data.items[i]
                drawRowLines(canvas, y, rowHeight)

                var cx = tableX

              */
/*  // Image cell (optional)
                if (!it.imagePath.isNullOrBlank()) {
                    try {
                        val bmp = BitmapFactory.decodeFile(it.imagePath)
                        if (bmp != null) {
                            val targetW = colWidths[0] - 12
                            val targetH = rowHeight - 12
                            val scaled = Bitmap.createScaledBitmap(bmp, targetW, targetH, true)
                            canvas.drawBitmap(scaled, (cx + 6).toFloat(), (y + 6).toFloat(), null)
                        } else {
                            drawTextInCell(canvas, "-", cx, y + 22, colWidths[0], tableCellPaint)
                        }
                    } catch (_: Exception) {
                        drawTextInCell(canvas, "-", cx, y + 22, colWidths[0], tableCellPaint)
                    }
                } else {
                    drawTextInCell(canvas, "-", cx, y + 22, colWidths[0], tableCellPaint)
                }*//*

                cx += colWidths[0]

                drawTextInCell(canvas, it.particulars, cx, y + 20, colWidths[1], tableCellPaint); cx += colWidths[1]
                drawTextInCell(canvas, it.grossWt!!.ifBlank { "-" }, cx, y + 20, colWidths[2], tableCellPaint); cx += colWidths[2]
                drawTextInCell(canvas, it.netWt!!.ifBlank { "-" }, cx, y + 20, colWidths[3], tableCellPaint); cx += colWidths[3]
              //  drawTextInCell(canvas, it.quantity.ifBlank { "-" }, cx, y + 20, colWidths[4], tableCellPaint); cx += colWidths[4]
                drawTextInCell(canvas, it.ratePerGm!!.ifBlank { "-" }, cx, y + 20, colWidths[5], tableCellPaint); cx += colWidths[5]
                drawTextInCell(canvas, it.makingPerGm!!.ifBlank { "-" }, cx, y + 20, colWidths[6], tableCellPaint); cx += colWidths[6]
                drawTextInCell(canvas, it.amount!!.ifBlank { "-" }, cx, y + 20, colWidths[7], tableCellPaint)

                y += rowHeight
            }
        }

        itemIndex = endIndex

        // -------- Totals area (bottom-right, like screenshot) ----------
        // Only draw on LAST page
        if (pageNo == pages - 1) {
            y += 8

            // totals line
            canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), linePaint)
            y += 14

            // Summary row (Gross/Net/Qty/Amount)
            // We’ll print few totals aligned roughly
            val summaryY = y + 10
            canvas.drawText("Totals:", (pageWidth - margin - 250).toFloat(), summaryY.toFloat(), labelPaint)
          //  canvas.drawText("Gross: ${data.totalGrossWt}", (pageWidth - margin - 200).toFloat(), (summaryY + 16).toFloat(), valuePaint)
            //canvas.drawText("Net: ${data.totalNetWt}", (pageWidth - margin - 200).toFloat(), (summaryY + 32).toFloat(), valuePaint)
            //canvas.drawText("Qty: ${data.totalQty}", (pageWidth - margin - 200).toFloat(), (summaryY + 48).toFloat(), valuePaint)
            canvas.drawText("Amount: ${data.totalAmount}", (pageWidth - margin - 200).toFloat(), (summaryY + 64).toFloat(), valuePaint)

            // GST box (very simple)
            val boxW = 240
            val boxH = 70
            val boxX = pageWidth - margin - boxW
            val boxY = pageHeight - margin - boxH

            canvas.drawRect(
                boxX.toFloat(), boxY.toFloat(),
                (boxX + boxW).toFloat(), (boxY + boxH).toFloat(),
                linePaint
            )
            canvas.drawText("CGST:", (boxX + 10).toFloat(), (boxY + 24).toFloat(), labelPaint)
            canvas.drawText(data.cgst, (boxX + boxW - 10).toFloat(), (boxY + 24).toFloat(),
                Paint(valuePaint).apply { textAlign = Paint.Align.RIGHT })

            canvas.drawText("SGST:", (boxX + 10).toFloat(), (boxY + 46).toFloat(), labelPaint)
            canvas.drawText(data.sgst, (boxX + boxW - 10).toFloat(), (boxY + 46).toFloat(),
                Paint(valuePaint).apply { textAlign = Paint.Align.RIGHT })
        }

        pdfDocument.finishPage(page)
    }

    // ---------- Save ----------
    val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "quotations")
    if (!dir.exists()) dir.mkdirs()

    val safeNo = data.quotationNo.ifBlank { "NA" }
    val file = File(dir, "Quotation_$safeNo.pdf")

    FileOutputStream(file).use { out ->
        pdfDocument.writeTo(out)
    }
    pdfDocument.close()

    // ---------- Open ----------
    openPdfFile(context, file)
}

private fun openPdfFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    }
    context.startActivity(intent)
}
*/
