package com.loyalstring.rfid.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.stream.JsonWriter
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table


import org.apache.poi.ss.usermodel.CellStyle
import com.itextpdf.layout.properties.TextAlignment
import com.loyalstring.rfid.data.local.dao.BulkItemDao
import com.loyalstring.rfid.data.local.dao.CustomerEmailDao
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.CustomerEmailEntity
import com.loyalstring.rfid.data.model.DetailedItem
import com.loyalstring.rfid.data.model.SummaryItem
import com.loyalstring.rfid.data.model.stockVerification.Item
import com.loyalstring.rfid.data.model.stockVerification.ScanSessionResponse
import com.loyalstring.rfid.data.model.stockVerification.StockVerificationRequestData
import com.loyalstring.rfid.repository.CommonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

import java.util.Properties
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.activation.FileDataSource
import javax.inject.Inject
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import com.itextpdf.layout.Document as PdfLayoutDocument


import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import java.io.OutputStreamWriter


@HiltViewModel
class ScanDisplayViewModel @Inject constructor(
    private val customerEmailDao: CustomerEmailDao,
    private val repository: CommonRepository,
    private val bulkItemDao: BulkItemDao,
) : ViewModel() {

    private val _emailStatus = MutableStateFlow<String?>(null)
    val emailStatus: StateFlow<String?> = _emailStatus

    // ✅ last response store karega (last batch)
    private val _scanSession = MutableStateFlow<ScanSessionResponse?>(null)
    val scanSession: StateFlow<ScanSessionResponse?> = _scanSession.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ✅ final success message (Compose me show dialog/snackbar)
    private val _successMsg = MutableStateFlow<String?>(null)
    val successMsg: StateFlow<String?> = _successMsg.asStateFlow()

    fun saveEmail(email: String) {
        viewModelScope.launch(Dispatchers.IO) {

            customerEmailDao.insertEmail(CustomerEmailEntity(email = email))
        }
    }

    suspend fun getAllEmails(): List<String> {
        return customerEmailDao.getAllEmails().map { it.email }
    }

    private fun toDetailedItemSafe(it: BulkItem): DetailedItem {
        return DetailedItem(
            counterName = it.counterName,
            category = it.category,
            product = it.productName,
            purity = it.purity,
            barcodeNumber = it.rfid,
            itemCode = it.itemCode,
            pieces = it.pcs ?: 1,
            grossWeight = it.grossWeight,
            stoneWeight = it.stoneWeight,
            netWeight = it.netWeight,
            mrp = it.mrp.toString()
        )
    }

    suspend fun loadUnmatchedChunk(limit: Int, offset: Int): List<DetailedItem> {
        return bulkItemDao.getUnmatchedPaged(limit, offset).map { toDetailedItemSafe(it) }
    }

    suspend fun loadMatchedChunk(limit: Int, offset: Int): List<DetailedItem> {
        return bulkItemDao.getMatchedPaged(limit, offset).map { toDetailedItemSafe(it) }
    }



    suspend fun generateScanReportPdf(
        context: Context,
        allItemsSummary: List<SummaryItem>,
        matchedItems:  List<DetailedItem>
    ): File = withContext(Dispatchers.IO) {

        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "report_${System.currentTimeMillis()}.pdf"
        )

        val writer = PdfWriter(file)
        val pdf = PdfDocument(writer)
        val doc = PdfLayoutDocument(pdf, PageSize.A4)
        doc.setMargins(20f, 20f, 20f, 20f)

        fun addHeader(title: String) {
            doc.add(Paragraph(title).setBold().setFontSize(12f))
        }

        fun textCell(text: String?) =
            Cell().add(Paragraph(text ?: "-").setFontSize(8f))
                .setTextAlignment(TextAlignment.LEFT)

        fun centerCell(text: String?) =
            Cell().add(Paragraph(text ?: "-").setFontSize(8f))
                .setTextAlignment(TextAlignment.CENTER)

        fun createDetailTableHeader(): Table {
            val table = Table(
                floatArrayOf(55f,55f,60f,50f,70f,60f,40f,55f,55f,55f,55f,70f)
            ).useAllAvailableWidth()

            listOf(
                "Counter Name","Category","Product","Purity","Barcode No","Item Code",
                "Pieces","Gross Wt","Stone Wt","Net Wt","MRP","Status"
            ).forEach {
                table.addHeaderCell(
                    Cell().add(Paragraph(it).setBold().setFontSize(8f))
                        .setTextAlignment(TextAlignment.CENTER)
                )
            }
            return table
        }

        // ------------------ SUMMARY ------------------

        addHeader("All Items Summary")

        val summaryTable = Table(
            floatArrayOf(60f,60f,60f,50f,55f,65f,60f,60f,60f)
        ).useAllAvailableWidth()

        listOf(
            "Counter","Category","Product",
            "Total","Match","Unmatch",
            "Tot Gwt","Match Gwt","Unmatch Gwt"
        ).forEach {
            summaryTable.addHeaderCell(
                Cell().add(Paragraph(it).setBold().setFontSize(8f))
                    .setTextAlignment(TextAlignment.CENTER)
            )
        }

        allItemsSummary.forEach {
            summaryTable.addCell(textCell(it.counterName))
            summaryTable.addCell(textCell(it.category))
            summaryTable.addCell(textCell(it.product))
            summaryTable.addCell(centerCell(it.totalQty.toString()))
            summaryTable.addCell(centerCell(it.matchQty.toString()))
            summaryTable.addCell(centerCell(it.unmatchQty.toString()))
            summaryTable.addCell(centerCell(it.totalGrossWt))
            summaryTable.addCell(centerCell(it.matchGrossWt))
            summaryTable.addCell(centerCell(it.unmatchGrossWt))
        }

        doc.add(summaryTable)
        doc.add(Paragraph("\n"))

        // ------------------ UNMATCHED ------------------

        addHeader("Unmatched Items")

        val chunkSize = 500
        var offset = 0

        while (true) {
            val chunk = loadUnmatchedChunk(chunkSize, offset)
            if (chunk.isEmpty()) break

            val table = createDetailTableHeader()

            chunk.forEach { d ->
                table.addCell(textCell(d.counterName))
                table.addCell(textCell(d.category))
                table.addCell(textCell(d.product))
                table.addCell(centerCell(d.purity))
                table.addCell(centerCell(d.barcodeNumber))
                table.addCell(centerCell(d.itemCode))
                table.addCell(centerCell(d.pieces.toString()))
                table.addCell(centerCell(d.grossWeight))
                table.addCell(centerCell(d.stoneWeight))
                table.addCell(centerCell(d.netWeight))
                table.addCell(centerCell(d.mrp))
                table.addCell(centerCell("Not Found"))
            }

            doc.add(table)
            pdf.addNewPage()
            offset += chunkSize
        }

        // ------------------ MATCHED ------------------

        addHeader("Matched Items")

        offset = 0

        while (true) {
            //val chunk = loadMatchedChunk(chunkSize, offset)
            val chunk = matchedItems.drop(offset).take(chunkSize)
            if (chunk.isEmpty()) break

            val table = createDetailTableHeader()

            chunk.forEach { d ->
                table.addCell(textCell(d.counterName))
                table.addCell(textCell(d.category))
                table.addCell(textCell(d.product))
                table.addCell(centerCell(d.purity))
                table.addCell(centerCell(d.barcodeNumber))
                table.addCell(centerCell(d.itemCode))
                table.addCell(centerCell(d.pieces.toString()))
                table.addCell(centerCell(d.grossWeight))
                table.addCell(centerCell(d.stoneWeight))
                table.addCell(centerCell(d.netWeight))
                table.addCell(centerCell(d.mrp))
                table.addCell(centerCell("Found"))
            }

            doc.add(table)
            pdf.addNewPage()
            offset += chunkSize
        }

        doc.close()
        file
    }

    /*suspend fun generateScanReportExcel(
        context: Context,
        allItemsSummary: List<SummaryItem>,
        matchedItems: List<DetailedItem>,
        unmatchedItems: List<DetailedItem>
    ): File {

        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "scan_report.xlsx"
        )

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Scan Report")

        *//* ---------------- Styles ---------------- *//*

        val headerStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 10
            })
            alignment = HorizontalAlignment.CENTER
        }

        val cellStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply {
                fontHeightInPoints = 9
            })
            alignment = HorizontalAlignment.LEFT
        }

        val centerStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply {
                fontHeightInPoints = 9
            })
            alignment = HorizontalAlignment.CENTER
        }

        val boldStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 9
            })
        }

        var rowIndex = 0

        fun createRow(values: List<Any?>, style: XSSFCellStyle) {
            val row = sheet.createRow(rowIndex++)
            values.forEachIndexed { index, value ->
                val cell = row.createCell(index)
                cell.setCellValue(value?.toString() ?: "-")
                cell.cellStyle = style
            }
        }

        fun addTitle(title: String) {
            val row = sheet.createRow(rowIndex++)
            val cell = row.createCell(0)
            cell.setCellValue(title)
            cell.cellStyle = boldStyle
            rowIndex++
        }

        *//* ============================
           1️⃣ ALL ITEMS SUMMARY
           ============================ *//*

        addTitle("All Items Summary")

        createRow(
            listOf(
                "Counter Name", "Category", "Product",
                "Total Qty", "Match Qty", "Unmatch Qty",
                "Total G.Wt", "Match G.Wt", "Unmatch G.Wt"
            ),
            headerStyle
        )

        var totalQty = 0
        var totalMatch = 0
        var totalUnmatch = 0
        var totalGwt = 0.0
        var totalMatchGwt = 0.0
        var totalUnmatchGwt = 0.0

        allItemsSummary.forEach {
            createRow(
                listOf(
                    it.counterName,
                    it.category,
                    it.product,
                    it.totalQty,
                    it.matchQty,
                    it.unmatchQty,
                    it.totalGrossWt,
                    it.matchGrossWt,
                    it.unmatchGrossWt
                ),
                cellStyle
            )

            totalQty += it.totalQty
            totalMatch += it.matchQty
            totalUnmatch += it.unmatchQty
            totalGwt += it.totalGrossWt?.toDoubleOrNull() ?: 0.0
            totalMatchGwt += it.matchGrossWt?.toDoubleOrNull() ?: 0.0
            totalUnmatchGwt += it.unmatchGrossWt?.toDoubleOrNull() ?: 0.0
        }

        createRow(
            listOf(
                "TOTAL", "", "",
                totalQty,
                totalMatch,
                totalUnmatch,
                "%.3f".format(totalGwt),
                "%.3f".format(totalMatchGwt),
                "%.3f".format(totalUnmatchGwt)
            ),
            boldStyle
        )

        rowIndex += 2

        *//* ============================
           2️⃣ UNMATCHED ITEMS
           ============================ *//*

        *//*addTitle("Unmatched Items")

        createRow(
            listOf(
                "Counter Name", "Category", "Product", "Purity",
                "Barcode No", "Item Code", "Pieces",
                "Gross Wt", "Stone Wt", "Net Wt", "MRP", "Status"
            ),
            headerStyle
        )

        var uPieces = 0
        var uGross = 0.0
        var uStone = 0.0
        var uNet = 0.0

        unmatchedItems.forEach {
            createRow(
                listOf(
                    it.counterName,
                    it.category,
                    it.product,
                    it.purity,
                    it.barcodeNumber,
                    it.itemCode,
                    it.pieces,
                    it.grossWeight,
                    it.stoneWeight,
                    it.netWeight,
                    it.mrp,
                    "Not Found"
                ),
                cellStyle
            )

            uPieces += it.pieces
            uGross += it.grossWeight?.toDoubleOrNull() ?: 0.0
            uStone += it.stoneWeight?.toDoubleOrNull() ?: 0.0
            uNet += it.netWeight?.toDoubleOrNull() ?: 0.0
        }

        createRow(
            listOf(
                "TOTAL", "", "", "", "", "",
                uPieces,
                "%.3f".format(uGross),
                "%.3f".format(uStone),
                "%.3f".format(uNet),
                "-", "-"
            ),
            boldStyle
        )

        rowIndex += 2*//*

        *//* ============================
           3️⃣ MATCHED ITEMS
           ============================ *//*

        addTitle("Matched Items")

        createRow(
            listOf(
                "Counter Name", "Category", "Product", "Purity",
                "Barcode No", "Item Code", "Pieces",
                "Gross Wt", "Stone Wt", "Net Wt", "MRP", "Status"
            ),
            headerStyle
        )

        var mPieces = 0
        var mGross = 0.0
        var mStone = 0.0
        var mNet = 0.0

        matchedItems.forEach {
            createRow(
                listOf(
                    it.counterName,
                    it.category,
                    it.product,
                    it.purity,
                    it.barcodeNumber,
                    it.itemCode,
                    it.pieces,
                    it.grossWeight,
                    it.stoneWeight,
                    it.netWeight,
                    it.mrp,
                    "Found"
                ),
                cellStyle
            )

            mPieces += it.pieces
            mGross += it.grossWeight?.toDoubleOrNull() ?: 0.0
            mStone += it.stoneWeight?.toDoubleOrNull() ?: 0.0
            mNet += it.netWeight?.toDoubleOrNull() ?: 0.0
        }

        createRow(
            listOf(
                "TOTAL", "", "", "", "", "",
                mPieces,
                "%.3f".format(mGross),
                "%.3f".format(mStone),
                "%.3f".format(mNet),
                "-", "-"
            ),
            boldStyle
        )

        *//* ---------------- Autosize ---------------- *//*

        *//*   for (i in 0..11) {
               sheet.autoSizeColumn(i)
           }*//*
        sheet.setColumnWidth(0, 20 * 256) // Counter Name
        sheet.setColumnWidth(1, 18 * 256) // Category
        sheet.setColumnWidth(2, 18 * 256) // Product
        sheet.setColumnWidth(3, 12 * 256) // Purity
        sheet.setColumnWidth(4, 20 * 256) // Barcode
        sheet.setColumnWidth(5, 16 * 256) // Item Code
        sheet.setColumnWidth(6, 10 * 256) // Pieces
        sheet.setColumnWidth(7, 12 * 256) // Gross Wt
        sheet.setColumnWidth(8, 12 * 256) // Stone Wt
        sheet.setColumnWidth(9, 12 * 256) // Net Wt
        sheet.setColumnWidth(10, 12 * 256) // MRP
        sheet.setColumnWidth(11, 14 * 256) // Status

        workbook.write(FileOutputStream(file))
        workbook.close()

        return file
    }*/


    suspend fun generateScanReportCsv(
        context: Context,
        allItemsSummary: List<SummaryItem>,
        matchedItems: List<DetailedItem>,
        unmatchedItems: List<DetailedItem>
    ): File = withContext(Dispatchers.IO) {

        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "scan_report.csv"
        )

        BufferedWriter(FileWriter(file)).use { writer ->

            fun writeRow(vararg values: Any?) {
                val line = values.joinToString(",") {
                    val v = it?.toString() ?: "-"
                    "\"${v.replace("\"", "\"\"")}\""
                }
                writer.write(line)
                writer.newLine()
            }

            /* ============================
               1️⃣ ALL ITEMS SUMMARY
               ============================ */

            writeRow("All Items Summary")
            writeRow(
                "Counter Name", "Category", "Product",
                "Total Qty", "Match Qty", "Unmatch Qty",
                "Total G.Wt", "Match G.Wt", "Unmatch G.Wt"
            )

            var totalQty = 0
            var totalMatch = 0
            var totalUnmatch = 0
            var totalGwt = 0.0
            var totalMatchGwt = 0.0
            var totalUnmatchGwt = 0.0

            allItemsSummary.forEach {
                writeRow(
                    it.counterName, it.category, it.product,
                    it.totalQty, it.matchQty, it.unmatchQty,
                    it.totalGrossWt, it.matchGrossWt, it.unmatchGrossWt
                )

                totalQty += it.totalQty
                totalMatch += it.matchQty
                totalUnmatch += it.unmatchQty
                totalGwt += it.totalGrossWt?.toDoubleOrNull() ?: 0.0
                totalMatchGwt += it.matchGrossWt?.toDoubleOrNull() ?: 0.0
                totalUnmatchGwt += it.unmatchGrossWt?.toDoubleOrNull() ?: 0.0
            }

            writeRow(
                "TOTAL", "", "",
                totalQty, totalMatch, totalUnmatch,
                "%.3f".format(totalGwt),
                "%.3f".format(totalMatchGwt),
                "%.3f".format(totalUnmatchGwt)
            )

            writer.newLine()

            /* ============================
               2️⃣ UNMATCHED ITEMS
               ============================ */

            writeRow("Unmatched Items")
            writeRow(
                "Counter Name", "Category", "Product", "Purity",
                "Barcode No", "Item Code", "Pieces",
                "Gross Wt", "Stone Wt", "Net Wt", "MRP", "Status"
            )

            unmatchedItems.forEach {
                writeRow(
                    it.counterName, it.category, it.product, it.purity,
                    it.barcodeNumber, it.itemCode, it.pieces,
                    it.grossWeight, it.stoneWeight, it.netWeight, it.mrp,
                    "Not Found"
                )
            }

            writer.newLine()

            /* ============================
               3️⃣ MATCHED ITEMS
               ============================ */

            writeRow("Matched Items")
            writeRow(
                "Counter Name", "Category", "Product", "Purity",
                "Barcode No", "Item Code", "Pieces",
                "Gross Wt", "Stone Wt", "Net Wt", "MRP", "Status"
            )

            matchedItems.forEach {
                writeRow(
                    it.counterName, it.category, it.product, it.purity,
                    it.barcodeNumber, it.itemCode, it.pieces,
                    it.grossWeight, it.stoneWeight, it.netWeight, it.mrp,
                    "Found"
                )
            }
        }

        file
    }




    /*suspend fun generateScanReportPdf(
        context: Context,
        allItemsSummary: List<SummaryItem>,
        matchedItems: List<DetailedItem>,
        unmatchedItems: List<DetailedItem>
    ): File {
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "report.pdf"
        )

        val writer = PdfWriter(file)
        val pdf = PdfDocument(writer)
        val doc = PdfLayoutDocument(pdf, PageSize.A4)

        // Header helper
        fun addHeader(title: String) {
            doc.add(Paragraph(title).setBold().setFontSize(12f))
        }

        // ----------------------------
        // 1) All Items Summary
        // ----------------------------
        addHeader("All Items Summary")
        val summaryTable = Table(
            floatArrayOf(60f, 60f, 60f, 50f, 55f, 65f, 60f, 60f, 60f)
        ).useAllAvailableWidth()

        listOf(
            "Counter Name", "Category", "Product",
            "Total Qty", "Match Qty", "Unmatch Qty",
            "Total G.Wt", "Match G.Wt", "Unmatch G.Wt"
        ).forEach { col ->
            summaryTable.addHeaderCell(
                Cell().add(
                    Paragraph(col).setBold().setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
                )
            )
        }

        var totalQty = 0
        var totalMatch = 0
        var totalUnmatch = 0
        var totalGwt = 0.0
        var totalMatchGwt = 0.0
        var totalUnmatchGwt = 0.0

        allItemsSummary.forEach {
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.counterName ?: "-").setFontSize(8f)
                        .setTextAlignment(TextAlignment.LEFT)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.category ?: "-").setFontSize(8f)
                        .setTextAlignment(TextAlignment.LEFT)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.product ?: "-").setFontSize(8f)
                        .setTextAlignment(TextAlignment.LEFT)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.totalQty.toString()).setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.matchQty.toString()).setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.unmatchQty.toString()).setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.totalGrossWt ?: "-").setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.matchGrossWt ?: "-").setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.unmatchGrossWt ?: "-").setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )

            totalQty += it.totalQty
            totalMatch += it.matchQty
            totalUnmatch += it.unmatchQty
            totalGwt += it.totalGrossWt?.toDoubleOrNull() ?: 0.0
            totalMatchGwt += it.matchGrossWt?.toDoubleOrNull() ?: 0.0
            totalUnmatchGwt += it.unmatchGrossWt?.toDoubleOrNull() ?: 0.0
        }

        // Totals row
        summaryTable.addCell(Cell(1, 3).add(Paragraph("TOTAL").setBold().setFontSize(8f)))
        summaryTable.addCell(
            Paragraph(totalQty.toString()).setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
        )
        summaryTable.addCell(
            Paragraph(totalMatch.toString()).setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
        )
        summaryTable.addCell(
            Paragraph(totalUnmatch.toString()).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        summaryTable.addCell(
            Paragraph("%.3f".format(totalGwt)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        summaryTable.addCell(
            Paragraph("%.3f".format(totalMatchGwt)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        summaryTable.addCell(
            Paragraph("%.3f".format(totalUnmatchGwt)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )

        doc.add(summaryTable)
        doc.add(Paragraph("\n"))

        // ----------------------------
        // 2) Unmatched Items
        // ----------------------------
        addHeader("Unmatched Items")
        val unmatchTable = Table(
            floatArrayOf(55f, 55f, 60f, 50f, 70f, 60f, 40f, 55f, 55f, 55f, 55f, 70f)
        ).useAllAvailableWidth()

        listOf(
            "Counter Name", "Category", "Product", "Purity", "Barcode No", "Item Code",
            "Pieces", "Gross Wt", "Stone Wt", "Net Wt", "MRP", "Status"
        ).forEach { col ->
            unmatchTable.addHeaderCell(
                Cell().add(
                    Paragraph(col).setBold().setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
                )
            )
        }

        var unmatchedTotalPieces = 0
        var unmatchedTotalGross = 0.0
        var unmatchedTotalStone = 0.0
        var unmatchedTotalNet = 0.0

        unmatchedItems.forEach { d ->
            unmatchTable.addCell(
                Paragraph(d.counterName ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.LEFT)
            )
            unmatchTable.addCell(
                Paragraph(d.category ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.LEFT)
            )
            unmatchTable.addCell(
                Paragraph(d.product ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.LEFT)
            )
            unmatchTable.addCell(
                Paragraph(d.purity ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph(d.barcodeNumber ?: "-").setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph(d.itemCode ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph(d.pieces.toString()).setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph(d.grossWeight ?: "-").setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph(d.stoneWeight ?: "-").setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph(d.netWeight ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph(d.mrp ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph("Not Found").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )

            unmatchedTotalPieces += d.pieces
            unmatchedTotalGross += d.grossWeight?.toDoubleOrNull() ?: 0.0
            unmatchedTotalStone += d.stoneWeight?.toDoubleOrNull() ?: 0.0
            unmatchedTotalNet += d.netWeight?.toDoubleOrNull() ?: 0.0
        }

        unmatchTable.addCell(Cell(1, 6).add(Paragraph("TOTAL").setBold().setFontSize(8f)))
        unmatchTable.addCell(
            Paragraph(unmatchedTotalPieces.toString()).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        unmatchTable.addCell(
            Paragraph("%.3f".format(unmatchedTotalGross)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        unmatchTable.addCell(
            Paragraph("%.3f".format(unmatchedTotalStone)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        unmatchTable.addCell(
            Paragraph("%.3f".format(unmatchedTotalNet)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        unmatchTable.addCell(Paragraph("-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))
        unmatchTable.addCell(Paragraph("-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))

        doc.add(unmatchTable)
        doc.add(Paragraph("\n"))

        // ----------------------------
        // 3) Matched Items
        // ----------------------------
        addHeader("Matched Items")
        val matchTable = Table(
            floatArrayOf(55f, 55f, 60f, 50f, 70f, 60f, 40f, 55f, 55f, 55f, 55f, 70f)
        ).useAllAvailableWidth()


        listOf(
            "Counter Name", "Category", "Product", "Purity", "Barcode No", "Item Code",
            "Pieces", "Gross Wt", "Stone Wt", "Net Wt", "MRP", "Status"
        ).forEach { col ->
            matchTable.addHeaderCell(
                Cell().add(
                    Paragraph(col).setBold().setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
                )
            )
        }

        var matchedTotalPieces = 0
        var matchedTotalGross = 0.0
        var matchedTotalStone = 0.0
        var matchedTotalNet = 0.0

        matchedItems.forEach { d ->
            matchTable.addCell(
                Paragraph(d.counterName ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.LEFT)
            )
            matchTable.addCell(
                Paragraph(d.category ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.LEFT)
            )
            matchTable.addCell(
                Paragraph(d.product ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.LEFT)
            )
            matchTable.addCell(
                Paragraph(d.purity ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph(d.barcodeNumber ?: "-").setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph(d.itemCode ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph(d.pieces.toString()).setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph(d.grossWeight ?: "-").setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph(d.stoneWeight ?: "-").setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph(d.netWeight ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph(d.mrp ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph("Found").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )

            matchedTotalPieces += d.pieces
            matchedTotalGross += d.grossWeight?.toDoubleOrNull() ?: 0.0
            matchedTotalStone += d.stoneWeight?.toDoubleOrNull() ?: 0.0
            matchedTotalNet += d.netWeight?.toDoubleOrNull() ?: 0.0
        }

        matchTable.addCell(Cell(1, 6).add(Paragraph("TOTAL").setBold().setFontSize(8f)))
        matchTable.addCell(
            Paragraph(matchedTotalPieces.toString()).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        matchTable.addCell(
            Paragraph("%.3f".format(matchedTotalGross)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        matchTable.addCell(
            Paragraph("%.3f".format(matchedTotalStone)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        matchTable.addCell(
            Paragraph("%.3f".format(matchedTotalNet)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        matchTable.addCell(Paragraph("-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))
        matchTable.addCell(Paragraph("-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))

        doc.add(matchTable)

        doc.close()
        return file
    }*/

    suspend fun generateScanReportExcel(
        context: Context,
        allItemsSummary: List<SummaryItem>,
        matchedItems: List<DetailedItem>,
        unmatchedItems: List<DetailedItem>
    ): File {

        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "scan_report.xlsx"
        )

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Scan Report")

        /* ---------------- Styles ---------------- */

        val headerStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 10
            })
            alignment = HorizontalAlignment.CENTER
        }

        val cellStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply {
                fontHeightInPoints = 9
            })
            alignment = HorizontalAlignment.LEFT
        }

        val centerStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply {
                fontHeightInPoints = 9
            })
            alignment = HorizontalAlignment.CENTER
        }

        val boldStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 9
            })
        }

        var rowIndex = 0

        fun createRow(values: List<Any?>, style: XSSFCellStyle) {
            val row = sheet.createRow(rowIndex++)
            values.forEachIndexed { index, value ->
                val cell = row.createCell(index)
                cell.setCellValue(value?.toString() ?: "-")
                cell.cellStyle = style
            }
        }

        fun addTitle(title: String) {
            val row = sheet.createRow(rowIndex++)
            val cell = row.createCell(0)
            cell.setCellValue(title)
            cell.cellStyle = boldStyle
            rowIndex++
        }

        /* ============================
           1️⃣ ALL ITEMS SUMMARY
           ============================ */

        addTitle("All Items Summary")

        createRow(
            listOf(
                "Counter Name", "Category", "Product",
                "Total Qty", "Match Qty", "Unmatch Qty",
                "Total G.Wt", "Match G.Wt", "Unmatch G.Wt"
            ),
            headerStyle
        )

        var totalQty = 0
        var totalMatch = 0
        var totalUnmatch = 0
        var totalGwt = 0.0
        var totalMatchGwt = 0.0
        var totalUnmatchGwt = 0.0

        allItemsSummary.forEach {
            createRow(
                listOf(
                    it.counterName,
                    it.category,
                    it.product,
                    it.totalQty,
                    it.matchQty,
                    it.unmatchQty,
                    it.totalGrossWt,
                    it.matchGrossWt,
                    it.unmatchGrossWt
                ),
                cellStyle
            )

            totalQty += it.totalQty
            totalMatch += it.matchQty
            totalUnmatch += it.unmatchQty
            totalGwt += it.totalGrossWt?.toDoubleOrNull() ?: 0.0
            totalMatchGwt += it.matchGrossWt?.toDoubleOrNull() ?: 0.0
            totalUnmatchGwt += it.unmatchGrossWt?.toDoubleOrNull() ?: 0.0
        }

        createRow(
            listOf(
                "TOTAL", "", "",
                totalQty,
                totalMatch,
                totalUnmatch,
                "%.3f".format(totalGwt),
                "%.3f".format(totalMatchGwt),
                "%.3f".format(totalUnmatchGwt)
            ),
            boldStyle
        )

        rowIndex += 2

        /* ============================
           2️⃣ UNMATCHED ITEMS
           ============================ */

        addTitle("Unmatched Items")

        createRow(
            listOf(
                "Counter Name", "Category", "Product", "Purity",
                "Barcode No", "Item Code", "Pieces",
                "Gross Wt", "Stone Wt", "Net Wt", "MRP", "Status"
            ),
            headerStyle
        )

        var uPieces = 0
        var uGross = 0.0
        var uStone = 0.0
        var uNet = 0.0

        unmatchedItems.forEach {
            createRow(
                listOf(
                    it.counterName,
                    it.category,
                    it.product,
                    it.purity,
                    it.barcodeNumber,
                    it.itemCode,
                    it.pieces,
                    it.grossWeight,
                    it.stoneWeight,
                    it.netWeight,
                    it.mrp,
                    "Not Found"
                ),
                cellStyle
            )

            uPieces += it.pieces
            uGross += it.grossWeight?.toDoubleOrNull() ?: 0.0
            uStone += it.stoneWeight?.toDoubleOrNull() ?: 0.0
            uNet += it.netWeight?.toDoubleOrNull() ?: 0.0
        }

        createRow(
            listOf(
                "TOTAL", "", "", "", "", "",
                uPieces,
                "%.3f".format(uGross),
                "%.3f".format(uStone),
                "%.3f".format(uNet),
                "-", "-"
            ),
            boldStyle
        )

        rowIndex += 2

        /* ============================
           3️⃣ MATCHED ITEMS
           ============================ */

        addTitle("Matched Items")

        createRow(
            listOf(
                "Counter Name", "Category", "Product", "Purity",
                "Barcode No", "Item Code", "Pieces",
                "Gross Wt", "Stone Wt", "Net Wt", "MRP", "Status"
            ),
            headerStyle
        )

        var mPieces = 0
        var mGross = 0.0
        var mStone = 0.0
        var mNet = 0.0

        matchedItems.forEach {
            createRow(
                listOf(
                    it.counterName,
                    it.category,
                    it.product,
                    it.purity,
                    it.barcodeNumber,
                    it.itemCode,
                    it.pieces,
                    it.grossWeight,
                    it.stoneWeight,
                    it.netWeight,
                    it.mrp,
                    "Found"
                ),
                cellStyle
            )

            mPieces += it.pieces
            mGross += it.grossWeight?.toDoubleOrNull() ?: 0.0
            mStone += it.stoneWeight?.toDoubleOrNull() ?: 0.0
            mNet += it.netWeight?.toDoubleOrNull() ?: 0.0
        }

        createRow(
            listOf(
                "TOTAL", "", "", "", "", "",
                mPieces,
                "%.3f".format(mGross),
                "%.3f".format(mStone),
                "%.3f".format(mNet),
                "-", "-"
            ),
            boldStyle
        )

        /* ---------------- Autosize ---------------- */

     /*   for (i in 0..11) {
            sheet.autoSizeColumn(i)
        }*/
        sheet.setColumnWidth(0, 20 * 256) // Counter Name
        sheet.setColumnWidth(1, 18 * 256) // Category
        sheet.setColumnWidth(2, 18 * 256) // Product
        sheet.setColumnWidth(3, 12 * 256) // Purity
        sheet.setColumnWidth(4, 20 * 256) // Barcode
        sheet.setColumnWidth(5, 16 * 256) // Item Code
        sheet.setColumnWidth(6, 10 * 256) // Pieces
        sheet.setColumnWidth(7, 12 * 256) // Gross Wt
        sheet.setColumnWidth(8, 12 * 256) // Stone Wt
        sheet.setColumnWidth(9, 12 * 256) // Net Wt
        sheet.setColumnWidth(10, 12 * 256) // MRP
        sheet.setColumnWidth(11, 14 * 256) // Status

        workbook.write(FileOutputStream(file))
        workbook.close()

        return file
    }



    // Helper for matched/unmatched
    private fun buildDetailedTable(items: List<DetailedItem>, status: String): Table {
        val headers = listOf(
            "Counter Name", "Category", "Product", "Purity", "Barcode No", "Item Code",
            "Pieces", "Gross Wt", "Stone Wt", "Net Wt", "MRP", "Status"
        )
        val colWidths = floatArrayOf(70f, 70f, 70f, 50f, 80f, 70f, 40f, 60f, 60f, 60f, 60f, 50f)
        val table = Table(colWidths)
        headers.forEach { table.addHeaderCell(it) }

        var totalPieces = 0
        var totalGross = 0.0
        var totalStone = 0.0
        var totalNet = 0.0
        var totalMrp = 0.0

        items.forEach { d ->
            table.addCell(d.counterName ?: "-")
            table.addCell(d.category ?: "-")
            table.addCell(d.product ?: "-")
            table.addCell(d.purity ?: "-")
            table.addCell(d.barcodeNumber ?: "-")
            table.addCell(d.itemCode ?: "-")
            table.addCell(d.pieces.toString())
            table.addCell(d.grossWeight ?: "-")
            table.addCell(d.stoneWeight ?: "-")
            table.addCell(d.netWeight ?: "-")
            table.addCell(d.mrp ?: "-")
            table.addCell(status)

            totalPieces += d.pieces
            totalGross += d.grossWeight?.toDoubleOrNull() ?: 0.0
            totalStone += d.stoneWeight?.toDoubleOrNull() ?: 0.0
            totalNet += d.netWeight?.toDoubleOrNull() ?: 0.0
            totalMrp += d.mrp?.toDoubleOrNull() ?: 0.0
        }

        // totals row
        table.addCell(Cell(1, 6).add(Paragraph("TOTAL").setBold()))
        table.addCell(totalPieces.toString())
        table.addCell(String.format("%.3f", totalGross))
        table.addCell(String.format("%.3f", totalStone))
        table.addCell(String.format("%.3f", totalNet))
        table.addCell(String.format("%.2f", totalMrp))
        table.addCell("")

        return table
    }


    fun sendEmailWithReport(context: Context, file: File, email: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, "Scan Report")
            putExtra(Intent.EXTRA_TEXT, "Please find attached the scan report.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Send email..."))
    }

    fun sendEmailDirectly(context: Context, file: File, toEmail: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication("android@loyalstring.com", "Loyal@123")
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress("android@loyalstring.com"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                    subject = "Inventory Scan Report"
                    setText("Please find attached the inventory report.")

                    val multipart = MimeMultipart()
                    val attachmentPart = MimeBodyPart()
                    attachmentPart.attachFile(file)
                    multipart.addBodyPart(attachmentPart)
                    setContent(multipart)
                }

                Transport.send(message) // ✅ runs in background thread


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendEmailHostinger(
        sendEmail: String,
        sendPass: String,
        recipients: List<String>,
        subject: String,
        body: String,
        type: String = "text/html", // default HTML
        attachments: Map<String, String> = emptyMap() // filename -> filepath
    ) {
        Thread {
            try {
                val props = Properties().apply {
                    put("mail.smtp.host", "smtp.hostinger.com")
                    put("mail.smtp.port", "465")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.ssl.enable", "true")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(
                            "android@loyalstring.com",  // full email
                            "Android@456#"       // exact Hostinger mailbox password
                        )
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(sendEmail))
                    setRecipients(
                        Message.RecipientType.TO,
                        recipients.joinToString(",") { it }
                    )
                    setSubject(subject)

                    val multipart = MimeMultipart()

                    // Email body
                    val textPart = MimeBodyPart().apply {
                        if (type.equals("text/html", ignoreCase = true)) {
                            setContent(body, "text/html; charset=utf-8")
                        } else {
                            setText(body)
                        }
                    }
                    multipart.addBodyPart(textPart)

                    // Attachments
                    attachments.forEach { (filename, filepath) ->
                        val attachmentPart = MimeBodyPart()
                        val source: DataSource = FileDataSource(filepath)
                        attachmentPart.dataHandler = DataHandler(source)
                        attachmentPart.fileName = filename
                        multipart.addBodyPart(attachmentPart)
                    }

                    setContent(multipart)
                }

                Transport.send(message)
                _emailStatus.value = "success"
                Log.d("Email", "✅ Email sent successfully to ${recipients.joinToString()}")
            } catch (e: Exception) {
                Log.d("Email", "❌ Failed: ${e.message}", e)
                _emailStatus.value = "error: ${e.message}"

            }
        }.start()
    }


    /*fun buildSummary(displayItems: List<BulkItem>): List<SummaryItem> {
        return displayItems.groupBy { Triple(it.counterName, it.category, it.productName) }
            .map { (key, items) ->
                val totalQty = items.size
                val matchQty = items.count {
                    it.scannedStatus.equals(
                        "Matched",
                        ignoreCase = true
                    ) || it.scannedStatus.equals("Found", ignoreCase = true)
                }
                val unmatchQty = totalQty - matchQty
                val totalGross = items.sumOf { it.grossWeight?.toDoubleOrNull() ?: 0.0 }
                val matchGross = items.filter {
                    it.scannedStatus.equals(
                        "Matched",
                        true
                    ) || it.scannedStatus.equals("Found", true)
                }
                    .sumOf { it.grossWeight?.toDoubleOrNull() ?: 0.0 }
                val unmatchGross = totalGross - matchGross

                SummaryItem(
                    counterName = key.first,
                    category = key.second,
                    product = key.third,
                    totalQty = totalQty,
                    matchQty = matchQty,
                    unmatchQty = unmatchQty,
                    totalGrossWt = "%.3f".format(totalGross),
                    matchGrossWt = "%.3f".format(matchGross),
                    unmatchGrossWt = "%.3f".format(unmatchGross)
                )
            }
    }*/

    fun buildSummary(displayItems: List<BulkItem>): List<SummaryItem> {

        data class Acc(
            var totalQty: Int = 0,
            var matchQty: Int = 0,
            var totalGross: Double = 0.0,
            var matchGross: Double = 0.0
        )

        val map = HashMap<Triple<String?, String?, String?>, Acc>()

        for (it in displayItems) {

            val key = Triple(it.counterName, it.category, it.productName)
            val acc = map.getOrPut(key) { Acc() }

            acc.totalQty++

            val gross = it.grossWeight?.toDoubleOrNull() ?: 0.0
            acc.totalGross += gross

            val isMatched =
                it.scannedStatus.equals("Matched", true) ||
                        it.scannedStatus.equals("Found", true)

            if (isMatched) {
                acc.matchQty++
                acc.matchGross += gross
            }
        }

        return map.map { (key, acc) ->
            SummaryItem(
                counterName = key.first,
                category = key.second,
                product = key.third,
                totalQty = acc.totalQty,
                matchQty = acc.matchQty,
                unmatchQty = acc.totalQty - acc.matchQty,
                totalGrossWt = "%.3f".format(acc.totalGross),
                matchGrossWt = "%.3f".format(acc.matchGross),
                unmatchGrossWt = "%.3f".format(acc.totalGross - acc.matchGross)
            )
        }
    }


    /*fun buildDetailedLists(displayItems: List<BulkItem>): Pair<List<DetailedItem>, List<DetailedItem>> {
        val matched = displayItems.filter {
            it.scannedStatus.equals(
                "Matched",
                true
            ) || it.scannedStatus.equals("Found", true)
        }
            .map { toDetailedItem(it) }

        val unmatched = displayItems.filter {
            it.scannedStatus.equals(
                "Unmatched",
                true
            ) || it.scannedStatus.equals("Not Found", true)
        }
            .map { toDetailedItem(it) }

        return Pair(matched, unmatched)
    }*/

    fun buildDetailedLists(
        displayItems: List<BulkItem>
    ): Pair<List<DetailedItem>, List<DetailedItem>> {

        val matched = ArrayList<DetailedItem>()
        val unmatched = ArrayList<DetailedItem>()

        for (it in displayItems) {
            when {
                it.scannedStatus.equals("Matched", true) ||
                        it.scannedStatus.equals("Found", true) -> {
                    matched.add(toDetailedItem(it))
                }

                it.scannedStatus.equals("Unmatched", true) ||
                        it.scannedStatus.equals("Not Found", true) -> {
                    unmatched.add(toDetailedItem(it))
                }
            }
        }

        return matched to unmatched
    }


    fun toDetailedItem(item: BulkItem): DetailedItem {
        return DetailedItem(
            counterName = item.counterName,
            category = item.category,
            product = item.productName ?: item.productName,
            purity = item.purity,
            barcodeNumber = item.rfid ?: item.rfid, // adapt if different
            itemCode = item.itemCode,
            pieces = item.pcs ?: 1,
            grossWeight = item.grossWeight,
            stoneWeight = item.stoneWeight,
            netWeight = item.netWeight,
            mrp = item.mrp.toString()
        )
    }

    /**
         * ✅ 500 items at a time upload
         * ✅ end me single success msg
         */
        /*fun uploadStockVerification(
            clientCode: String,
            items: List<Item>,
            batchSize: Int = 500
        ) {
            viewModelScope.launch {
                _loading.value = true
                _error.value = null
                _successMsg.value = null
                _scanSession.value = null

                try {
                    if (clientCode.isBlank()) {
                        _error.value = "ClientCode is missing"
                        return@launch
                    }
                    if (items.isEmpty()) {
                        _error.value = "Item list is empty"
                        return@launch
                    }

                    Log.d("SAVE_BUTTON", "ItemSize "+items.size)
                    val batches = items.chunked(batchSize)
                    Log.d("SAVE_BUTTON", "BatchesSize "+batches.size)
                    var lastResponse: ScanSessionResponse? = null

                    for (batch in batches) {
                        val req = StockVerificationRequestData(
                            clientCode = clientCode,
                            items = batch
                        )
                        Log.d("SAVE_BUTTON", "BatchSize "+batch.size)
                        val result = repository.stockVarificationNew(req)

                        var failed = false

                        result.onSuccess { res ->
                            lastResponse = res
                        }.onFailure { e ->
                            failed = true
                            _error.value = e.message ?: "Stock upload failed"
                            Log.d("SAVE_BUTTON", "Stock upload failed ")
                        }

                        if (failed) return@launch
                    }

                    // ✅ all batches success
                    _scanSession.value = lastResponse
                    _successMsg.value = "Stock status has been updated to the server"
                    Log.d("SAVE_BUTTON", "Stock status has been updated to the server")
                } catch (e: Exception) {
                    _error.value = e.message ?: "Unexpected error"
                } finally {
                    _loading.value = false
                }
            }
        }*/


    /*fun uploadStockVerification(
        clientCode: String,
        items: List<Item>,
        batchSize: Int = 5000,
        parallelUploads: Int = 3
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            _loading.value = true
            _error.value = null
            _successMsg.value = null
            _scanSession.value = null

            try {
                if (clientCode.isBlank()) {
                    _error.value = "ClientCode is missing"
                    return@launch
                }

                if (items.isEmpty()) {
                    _error.value = "Item list is empty"
                    return@launch
                }

                Log.d("SAVE_BUTTON", "Total Items = ${items.size}")

                val batches = items.chunked(batchSize)
                Log.d("SAVE_BUTTON", "Total Batches = ${batches.size}")

                var lastResponse: ScanSessionResponse? = null

                // limit parallelism
                val dispatcher = Dispatchers.IO.limitedParallelism(parallelUploads)

                batches
                    .asSequence()
                    .chunked(parallelUploads)   // group for parallel execution
                    .forEachIndexed { groupIndex, group ->

                        coroutineScope {

                            val jobs = group.mapIndexed { index, batch ->
                                async(dispatcher) {

                                    val req = StockVerificationRequestData(
                                        clientCode = clientCode,
                                        items = batch
                                    )

                                    Log.d(
                                        "SAVE_BUTTON",
                                        "Uploading batch ${(groupIndex * parallelUploads) + index + 1} size=${batch.size}"
                                    )

                                    val result = repository.stockVarificationNew(req)

                                    result.onSuccess { res ->
                                        lastResponse = res
                                    }.onFailure { e ->
                                        throw Exception(e.message ?: "Batch upload failed")
                                    }
                                }
                            }

                            jobs.awaitAll()
                        }
                    }

                _scanSession.value = lastResponse
                _successMsg.value = "Stock status updated successfully"

            } catch (e: Exception) {
                Log.e("SAVE_BUTTON", "Upload failed", e)
                _error.value = e.message ?: "Stock upload failed"
            } finally {
                _loading.value = false
            }
        }
    }*/


    /*fun uploadStockVerification(
        clientCode: String,
        items: List<Item>,
        batchSize: Int = 3000,
        parallelUploads: Int = 3
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            _loading.value = true
            _error.value = null
            _successMsg.value = null
            _scanSession.value = null

            try {
                if (clientCode.isBlank()) {
                    _error.value = "ClientCode is missing"
                    return@launch
                }

                if (items.isEmpty()) {
                    _error.value = "Item list is empty"
                    return@launch
                }

                Log.d("SAVE_BUTTON", "Total Items = ${items.size}")

                val batches = items.chunked(batchSize)
                Log.d("SAVE_BUTTON", "Total Batches = ${batches.size}")

                var lastResponse: ScanSessionResponse? = null

                val dispatcher = Dispatchers.IO.limitedParallelism(parallelUploads)

                batches
                    .chunked(parallelUploads)
                    .forEachIndexed { groupIndex, group ->

                        coroutineScope {

                            val jobs = group.mapIndexed { index, batch ->
                                async(dispatcher) {

                                    val batchNo =
                                        (groupIndex * parallelUploads) + index + 1

                                    Log.d(
                                        "SAVE_BUTTON",
                                        "Uploading batch $batchNo size=${batch.size}"
                                    )

                                    val req = StockVerificationRequestData(
                                        clientCode = clientCode,
                                        items = batch
                                    )

                                    val result = repository.stockVarificationNew(req)

                                    result.onSuccess { res ->
                                        lastResponse = res
                                    }.onFailure { e ->
                                        Log.e("SAVE_BUTTON", "Batch $batchNo failed", e)
                                        throw Exception(e.message ?: "Batch upload failed")
                                    }
                                }
                            }

                            jobs.awaitAll()
                        }
                    }

                _scanSession.value = lastResponse
                _successMsg.value = "Stock status updated successfully"

            } catch (e: Exception) {
                Log.e("SAVE_BUTTON", "Upload failed", e)
                _error.value = e.message ?: "Stock upload failed"
            } finally {
                _loading.value = false
            }
        }
    }*/


    private fun stockVerificationBody(
        clientCode: String,
        items: List<Item>
    ): RequestBody {

        return object : RequestBody() {

            override fun contentType() =
                "application/json; charset=utf-8".toMediaType()

            override fun writeTo(sink: BufferedSink) {

                val writer = JsonWriter(OutputStreamWriter(sink.outputStream(), Charsets.UTF_8))
                writer.isLenient = true

                writer.beginObject()
                writer.name("ClientCode").value(clientCode)

                writer.name("Items")
                writer.beginArray()

                for (item in items) {

                    writer.beginObject()

                    // -------- Strings --------
                    writer.name("ItemCode").value(item.itemCode)
                    writer.name("Status").value(item.status)
                    writer.name("CounterName").value(item.counterName)
                    writer.name("CategoryName").value(item.categoryName)
                    writer.name("ProductName").value(item.productName)
                    writer.name("DesignName").value(item.designName)
                    writer.name("PurityName").value(item.purityName)
                    writer.name("CompanyName").value(item.companyName)
                    writer.name("BranchName").value(item.branchName)

                    // -------- Numbers --------
                    writer.name("GrossWeight").value(item.grossWeight)
                    writer.name("NetWeight").value(item.netWeight)
                    writer.name("Quantity").value(item.quantity)

                    // -------- IDs --------
                    writer.name("CounterId").value(item.counterId)
                    writer.name("CategoryId").value(item.categoryId)
                    writer.name("ProductId").value(item.productId)
                    writer.name("DesignId").value(item.designId)
                    writer.name("PurityId").value(item.purityId)
                    writer.name("CompanyId").value(item.companyId)
                    writer.name("BranchId").value(item.branchId)

                    writer.endObject()
                }

                writer.endArray()
                writer.endObject()

                writer.flush()
            }
        }
    }




    fun uploadStockVerification(
        clientCode: String,
        items: List<Item>,
        batchSize: Int = 3000,
        parallelUploads: Int = 3
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            _loading.value = true
            _error.value = null
            _successMsg.value = null
            _scanSession.value = null

            try {
                if (clientCode.isBlank()) {
                    _error.value = "ClientCode is missing"
                    return@launch
                }

                if (items.isEmpty()) {
                    _error.value = "Item list is empty"
                    return@launch
                }

                Log.d("SAVE_BUTTON", "Total Items = ${items.size}")

                val batches = items.chunked(batchSize)
                Log.d("SAVE_BUTTON", "Total Batches = ${batches.size}")

                var lastResponse: ScanSessionResponse? = null

                val dispatcher = Dispatchers.IO.limitedParallelism(parallelUploads)

                batches
                    .chunked(parallelUploads)
                    .forEachIndexed { groupIndex, group ->

                        coroutineScope {

                            val jobs = group.mapIndexed { index, batch ->

                                async(dispatcher) {

                                    val batchNo =
                                        (groupIndex * parallelUploads) + index + 1

                                    Log.d(
                                        "SAVE_BUTTON",
                                        "Uploading batch $batchNo size=${batch.size}"
                                    )

                                    // ✅ STREAMING BODY (NO HUGE JSON)
                                    val body = stockVerificationBody(clientCode, batch)

                                    val result = repository.uploadStock(body)

                                    result.onSuccess { res ->
                                        lastResponse = res
                                    }.onFailure { e ->
                                        Log.e("SAVE_BUTTON", "Batch $batchNo failed", e)
                                        throw Exception(e.message ?: "Batch upload failed")
                                    }
                                }
                            }

                            jobs.awaitAll()
                        }
                    }

                _scanSession.value = lastResponse
                _successMsg.value = "Stock status updated successfully"

            } catch (e: Exception) {
                Log.e("SAVE_BUTTON", "Upload failed", e)
                _error.value = e.message ?: "Stock upload failed"
            } finally {
                _loading.value  = false
            }
        }
    }



    fun clearMessages() {
            _successMsg.value = null
            _error.value = null
        }
    }


