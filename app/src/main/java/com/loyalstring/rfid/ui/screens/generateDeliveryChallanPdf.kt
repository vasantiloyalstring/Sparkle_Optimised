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
