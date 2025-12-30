package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanPrintData
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import kotlin.math.min

fun generateDeliveryChallanPdf(
    context: Context,
    data: DeliveryChallanPrintData
): Uri? {
    return try {
        val pdf = PdfDocument()

        // A4
        val pageWidth = 595
        val pageHeight = 842

        val margin = 28f
        val rightX = pageWidth - margin
        val contentWidth = pageWidth - (2 * margin)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val boldPaint = Paint(textPaint).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val headerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F2F2F2")
            style = Paint.Style.FILL
        }

        val dfWt = DecimalFormat("0.###") // 2.66 / 1.064 / 0.776
        val dfAmt = DecimalFormat("0.0")  // 0.0 like image

        fun safeDouble(v: String?): Double {
            if (v.isNullOrBlank()) return 0.0
            // keep digits, dot, minus
            val clean = v.trim().replace(Regex("[^0-9.\\-]"), "")
            return clean.toDoubleOrNull() ?: 0.0
        }

        fun dateOnly(s: String): String {
            val t = s.trim()
            if (t.isEmpty()) return ""
            // common formats: "13-12-2025 10:20", "2025-12-13T10:20:00", etc.
            return when {
                t.contains("T") -> t.substringBefore("T")
                t.contains(" ") -> t.substringBefore(" ")
                else -> t
            }
        }

        fun drawTextLeft(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
            canvas.drawText(text, x, y, paint)
        }

        fun drawTextRight(canvas: Canvas, text: String, xRight: Float, y: Float, paint: Paint) {
            val w = paint.measureText(text)
            canvas.drawText(text, xRight - w, y, paint)
        }

        fun drawCell(
            canvas: Canvas,
            rect: RectF,
            text: String,
            align: Paint.Align,
            paint: Paint,
            fill: Paint? = null
        ) {
            fill?.let { canvas.drawRect(rect, it) }
            canvas.drawRect(rect, linePaint)

            val p = Paint(paint)
            p.textAlign = align

            val textY = rect.top + (rect.height() / 2f) + (p.textSize / 3f)
            val x = when (align) {
                Paint.Align.LEFT -> rect.left + 6f
                Paint.Align.CENTER -> rect.centerX()
                Paint.Align.RIGHT -> rect.right - 6f
            }
            canvas.drawText(text, x, textY, p)
        }

        // ---------- MAIN TABLE (same as image) ----------
        val colRatios = floatArrayOf(
            0.08f, // S.No
            0.30f, // Item Name
            0.08f, // Pcs
            0.14f, // Gross Wt
            0.14f, // Stone Wt
            0.14f, // Net Wt
            0.12f  // Amount
        )

        fun buildColXs(): FloatArray {
            val xs = FloatArray(colRatios.size + 1)
            xs[0] = margin
            var acc = margin
            for (i in colRatios.indices) {
                acc += contentWidth * colRatios[i]
                xs[i + 1] = acc
            }
            return xs
        }

        fun startNewPage(pageNo: Int): Pair<PdfDocument.Page, Canvas> {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create()
            val page = pdf.startPage(pageInfo)
            return page to page.canvas
        }

        fun drawTopHeader(canvas: Canvas): Float {
            var y = margin + 14f

            // Left (like image): name + email line
            drawTextLeft(canvas, data.customerName.ifBlank { data.branchName }, margin, y, textPaint)
            y += 16f
            // you don't have Email field; using Phone here (you can change label)
            drawTextLeft(canvas, "Phone : ${data.phone}", margin, y, textPaint)

            // Right
            val d = dateOnly(data.createdDateTime)
            drawTextRight(canvas, "Date: $d", rightX, margin + 14f, textPaint)
            drawTextRight(canvas, "Status: Order Summary", rightX, margin + 30f, textPaint)

            return margin + 46f
        }

        fun drawMainHeaderRow(canvas: Canvas, yTop: Float): Float {
            val xs = buildColXs()
            val h = 22f
            val rects = Array(colRatios.size) { i ->
                RectF(xs[i], yTop, xs[i + 1], yTop + h)
            }

            val headers = listOf("S.No", "Item Name", "Pcs", "Gross Wt", "Stone Wt", "Net Wt", "Amount")
            for (i in headers.indices) {
                drawCell(
                    canvas = canvas,
                    rect = rects[i],
                    text = headers[i],
                    align = if (i == 1) Paint.Align.LEFT else Paint.Align.CENTER,
                    paint = boldPaint,
                    fill = headerFillPaint
                )
            }
            return yTop + h
        }

        fun drawMainRow(canvas: Canvas, yTop: Float, index: Int, itemName: String, pcs: Int, gross: Double, stone: Double, net: Double, amount: Double): Float {
            val xs = buildColXs()
            val h = 20f
            val rects = Array(colRatios.size) { i ->
                RectF(xs[i], yTop, xs[i + 1], yTop + h)
            }

            val values = listOf(
                (index + 1).toString(),
                itemName,
                pcs.toString(),
                dfWt.format(gross),
                dfWt.format(stone),
                dfWt.format(net),
                dfAmt.format(amount),
            )

            for (i in values.indices) {
                drawCell(
                    canvas = canvas,
                    rect = rects[i],
                    text = values[i],
                    align = if (i == 1) Paint.Align.LEFT else Paint.Align.CENTER,
                    paint = textPaint
                )
            }
            return yTop + h
        }

        fun drawSummaryTable(canvas: Canvas, yTop: Float): Float {
            // group totals by Item Name
            val grouped = data.items.groupBy { it.itemName }.map { (name, list) ->
                val totalPcs = list.sumOf { it.pcs }
                val totalGross = list.sumOf { safeDouble(it.grossWt) }
                val totalNet = list.sumOf { safeDouble(it.netWt) }
                Triple(name, totalPcs, Pair(totalGross, totalNet))
            }

            var y = yTop + 18f

            // Summary table columns like image
            val xs = floatArrayOf(
                margin,
                margin + contentWidth * 0.35f, // Item Name
                margin + contentWidth * 0.55f, // Total Pcs
                margin + contentWidth * 0.78f, // Total Gross
                margin + contentWidth          // Total Net
            )

            val headerH = 22f
            val headerRects = arrayOf(
                RectF(xs[0], y, xs[1], y + headerH),
                RectF(xs[1], y, xs[2], y + headerH),
                RectF(xs[2], y, xs[3], y + headerH),
                RectF(xs[3], y, xs[4], y + headerH),
            )
            val headers = listOf("Item Name", "Total Pcs", "Total Gross Wt", "Total Net Wt")
            for (i in headers.indices) {
                drawCell(
                    canvas,
                    headerRects[i],
                    headers[i],
                    align = if (i == 0) Paint.Align.LEFT else Paint.Align.CENTER,
                    paint = boldPaint,
                    fill = headerFillPaint
                )
            }
            y += headerH

            val rowH = 20f
            grouped.forEach { g ->
                val name = g.first
                val pcs = g.second
                val gross = g.third.first
                val net = g.third.second

                val rects = arrayOf(
                    RectF(xs[0], y, xs[1], y + rowH),
                    RectF(xs[1], y, xs[2], y + rowH),
                    RectF(xs[2], y, xs[3], y + rowH),
                    RectF(xs[3], y, xs[4], y + rowH),
                )

                drawCell(canvas, rects[0], name, Paint.Align.LEFT, textPaint)
                drawCell(canvas, rects[1], pcs.toString(), Paint.Align.CENTER, textPaint)
                drawCell(canvas, rects[2], dfWt.format(gross), Paint.Align.CENTER, textPaint)
                drawCell(canvas, rects[3], dfWt.format(net), Paint.Align.CENTER, textPaint)

                y += rowH
            }

            return y
        }

        // ---------- Pagination ----------
        val bottomMargin = 28f
        val pageBottom = pageHeight - bottomMargin
        val rowHeight = 20f
        val needSpaceForSummary = 120f // reserve space on last page for summary table

        var pageNo = 1
        var (page, canvas) = startNewPage(pageNo)

        var y = drawTopHeader(canvas)
        y = drawMainHeaderRow(canvas, y)

        data.items.forEachIndexed { i, it ->
            val isLast = (i == data.items.lastIndex)

            val gross = safeDouble(it.grossWt)
            val stone = safeDouble(it.stoneWt)
            val net = safeDouble(it.netWt)
            val amt = safeDouble(it.itemAmount)

            val reserve = if (isLast) needSpaceForSummary else 0f
            if (y + rowHeight + reserve > pageBottom) {
                pdf.finishPage(page)
                pageNo++
                val pair = startNewPage(pageNo)
                page = pair.first
                canvas = pair.second

                y = drawTopHeader(canvas)
                y = drawMainHeaderRow(canvas, y)
            }

            y = drawMainRow(canvas, y, i, it.itemName, it.pcs, gross, stone, net, amt)

            if (isLast) {
                // summary table at end (like image)
                y = drawSummaryTable(canvas, y)
            }
        }

        pdf.finishPage(page)

        // ---------- Save ----------
        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(dir, "Delivery_Challan_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()

        FileProvider.getUriForFile(context, context.packageName + ".provider", file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


/*
package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanPrintData
import java.io.File
import java.io.FileOutputStream

fun generateDeliveryChallanPdf(
    context: Context,
    data: DeliveryChallanPrintData
): Uri? {
    return try {
        val pdfDocument = PdfDocument()

        // A4 size at ~72 dpi
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // ---------- PAINTS ----------
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f        // small, like thermal print
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }

        val boldPaint = Paint(textPaint).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val leftMargin = 30f
        val rightMargin = pageInfo.pageWidth - 30f
        var y = 40f

        fun drawTextLeft(text: String, yPos: Float, bold: Boolean = false) {
            canvas.drawText(text, leftMargin, yPos, if (bold) boldPaint else textPaint)
        }

        fun drawTextRight(text: String, yPos: Float, bold: Boolean = false) {
            val paintObj = if (bold) boldPaint else textPaint
            val w = paintObj.measureText(text)
            canvas.drawText(text, rightMargin - w, yPos, paintObj)
        }

        fun drawLine(yPos: Float) {
            canvas.drawLine(leftMargin, yPos, rightMargin, yPos, textPaint)
        }

        // ---------- HEADER ----------
        drawTextLeft(data.branchName, y, bold = true); y += 14
        drawTextLeft(data.city, y, bold = true); y += 20

        drawTextLeft("Created: ${data.createdDateTime}", y); y += 18

        drawTextLeft("Customer: ${data.customerName}", y); y += 14
        drawTextLeft("Quotation #: ${data.quotationNo}", y); y += 14
        drawTextLeft("Phone: ${data.phone}", y); y += 18

        drawLine(y); y += 10

        // ---------- ITEM DETAILS TITLE ----------
        drawTextLeft("ITEM DETAILS", y, bold = true); y += 10
        drawLine(y); y += 14

        // Table header – like your thermal sample
        drawTextLeft("Item", y, bold = true)
        drawTextLeft("Purity", y, bold = true)
        drawTextRight("Pcs", y, bold = true)
        y += 12

        drawTextLeft("Gr.Wt   St.Wt   Net Wt", y)
        drawTextRight("Rate/gm  Wastage  St. Amt", y)
        y += 14

        // ---------- ITEMS ----------
        data.items.forEach { item ->
            // Row 1
            drawTextLeft(item.itemName, y)
            drawTextLeft(item.purity, y)
            drawTextRight(item.pcs.toString(), y)
            y += 12

            // Row 2
            val leftLine =
                "${item.grossWt}  ${item.stoneWt}  ${item.netWt}"
            val rightLine =
                "${item.ratePerGram}   ${item.wastage}   ${item.itemAmount}"

            drawTextLeft(leftLine, y)
            drawTextRight(rightLine, y)
            y += 14
        }

        drawLine(y); y += 14

        // ---------- SUMMARY ----------
        drawTextLeft("Taxable Amount", y)
        drawTextRight(data.taxableAmount, y)
        y += 14

        drawTextLeft("CGST ${"%.2f".format(data.cgstPercent)}%", y)
        drawTextRight(data.cgstAmount, y)
        y += 14

        drawTextLeft("SGST ${"%.2f".format(data.sgstPercent)}%", y)
        drawTextRight(data.sgstAmount, y)
        y += 18

        drawLine(y); y += 14

        drawTextLeft("Total Net Amount", y, bold = true)
        drawTextRight(data.totalNetAmount, y, bold = true)
        y += 20

        // ---------- FINISH PAGE ----------
        pdfDocument.finishPage(page)

        // Save to cache
        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val fileName = "delivery_challan_${System.currentTimeMillis()}.pdf"
        val file = File(dir, fileName)
        FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
        pdfDocument.close()

        // Uri for sharing / preview
        FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
*/
