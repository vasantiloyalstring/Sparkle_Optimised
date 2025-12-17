package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.LineSeparator
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.loyalstring.rfid.data.model.sampleOut.SampleOutPrintData
import java.io.File
import java.util.Locale

fun generateSampleOutPrintPdf(context: Context, data: SampleOutPrintData) {

    val file = File(
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS),
        "SampleOut_${data.sampleOutNo.ifBlank { "Print" }}.pdf"
    )

    val writer = PdfWriter(file)
    val pdf = PdfDocument(writer)
    val doc = Document(pdf, PageSize.A4)
    doc.setMargins(20f, 20f, 20f, 20f)

    // ---- Header (Company Name) ----
    doc.add(
        Paragraph(data.companyName)
            .setTextAlignment(TextAlignment.CENTER)
            .setBold()
            .setFontSize(16f)
    )

    // horizontal line
    doc.add(LineSeparator(SolidLine(1f)).setMarginTop(8f).setMarginBottom(12f))

    // Title
    doc.add(
        Paragraph("Sample Out Print")
            .setTextAlignment(TextAlignment.CENTER)
            .setBold()
            .setFontSize(12f)
            .setMarginBottom(14f)
    )

    // ---- Info row: Left (customer) / Right (sampleOut info) ----
    val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
        .setWidth(UnitValue.createPercentValue(100f))
        .setBorder(Border.NO_BORDER)

    val leftInfo = """
        Customer Name: ${data.customerName}
        Address/City: ${data.addressCity}
        Contact No: ${data.contactNo}
    """.trimIndent()

    val rightInfo = """
        Sample Out No: ${data.sampleOutNo}
        Date: ${data.date}
        ReturnDate: ${data.returnDate}
    """.trimIndent()

    infoTable.addCell(
        Cell().add(Paragraph(leftInfo).setFontSize(10f))
            .setBorder(Border.NO_BORDER)
    )
    infoTable.addCell(
        Cell().add(Paragraph(rightInfo).setFontSize(10f).setTextAlignment(TextAlignment.RIGHT))
            .setBorder(Border.NO_BORDER)
    )

    doc.add(infoTable)
    doc.add(Paragraph("\n"))

    // ---- Main table ----
    val colWidths = floatArrayOf(
        0.6f,  // Sr.No
        2.0f,  // Item Details
        1.0f,  // Gross
        1.0f,  // Stone
        1.0f,  // Diamond
        1.0f,  // Net
        0.8f,  // Pieces
        1.0f   // Status
    )

    val table = Table(UnitValue.createPercentArray(colWidths))
        .setWidth(UnitValue.createPercentValue(100f))

    fun headerCell(text: String) =
        Cell()
            .add(Paragraph(text).setBold().setFontSize(9f).setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)

    table.addHeaderCell(headerCell("Sr.No"))
    table.addHeaderCell(headerCell("Item Details"))
    table.addHeaderCell(headerCell("Gross Wt"))
    table.addHeaderCell(headerCell("Stone Wt"))
    table.addHeaderCell(headerCell("Diamond\nWt"))
    table.addHeaderCell(headerCell("Net Wt"))
    table.addHeaderCell(headerCell("Pieces"))
    table.addHeaderCell(headerCell("Status"))

    fun n(v: String?): Double = v?.toDoubleOrNull() ?: 0.0
    fun fmt(d: Double): String = String.format(Locale.US, "%.3f", d)

    var totalGross = 0.0
    var totalStone = 0.0
    var totalDiamond = 0.0
    var totalNet = 0.0
    var totalPieces = 0.0

    data.items.forEachIndexed { idx, it ->
        val g = n(it.grossWt); totalGross += g
        val s = n(it.stoneWt); totalStone += s
        val d = n(it.diamondWt); totalDiamond += d
        val nw = n(it.netWt); totalNet += nw
        val p = n(it.pieces); totalPieces += p

        table.addCell(Cell().add(Paragraph((idx + 1).toString()).setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
        table.addCell(Cell().add(Paragraph(it.itemDetails).setFontSize(9f)).setTextAlignment(TextAlignment.LEFT))
        table.addCell(Cell().add(Paragraph(fmt(g)).setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
        table.addCell(Cell().add(Paragraph(fmt(s)).setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
        table.addCell(Cell().add(Paragraph(fmt(d)).setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
        table.addCell(Cell().add(Paragraph(fmt(nw)).setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
        table.addCell(Cell().add(Paragraph(totalPieces.toInt().toString()).setFontSize(9f)).setTextAlignment(TextAlignment.CENTER).also {
            // NOTE: pieces per-row dikhana ho to yaha it.pieces use karo
        })
        table.addCell(Cell().add(Paragraph(it.status).setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
    }

    // ---- Total row ----
    table.addCell(Cell().add(Paragraph("Total").setBold().setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
    table.addCell(Cell().add(Paragraph("").setFontSize(9f)))
    table.addCell(Cell().add(Paragraph(fmt(totalGross)).setBold().setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
    table.addCell(Cell().add(Paragraph(fmt(totalStone)).setBold().setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
    table.addCell(Cell().add(Paragraph(fmt(totalDiamond)).setBold().setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
    table.addCell(Cell().add(Paragraph(fmt(totalNet)).setBold().setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
    table.addCell(Cell().add(Paragraph(totalPieces.toInt().toString()).setBold().setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
    table.addCell(Cell().add(Paragraph("").setFontSize(9f)))

    doc.add(table)
    doc.close()

    // ---- Open PDF ----
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

    context.startActivity(Intent.createChooser(intent, "Open PDF with..."))
}
