package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.local.dao.BulkItemDao
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.EpcDto
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.remote.data.ClearStockDataModelReq
import com.loyalstring.rfid.data.remote.response.AlllabelResponse
import com.loyalstring.rfid.data.remote.response.ClearStockDataModelResponse
import com.loyalstring.rfid.ui.utils.toBulkItem
import kotlinx.coroutines.flow.Flow

interface BulkRepository {
     val bulkItemDao: BulkItemDao
    suspend fun insertBulkItems(items: List<BulkItem>)
    suspend fun insertRFIDTags(items: List<EpcDto>)
    fun getAllBulkItems(): Flow<List<BulkItem>>
    fun getMinimalItemsFlow(): Flow<List<BulkItem>>
    suspend fun getMinimalItemFlow(): List<BulkItem>
    fun getAllItemsFlow(): Flow<List<BulkItem>>
    fun getAllRFIDTags(): Flow<List<EpcDto>>
    suspend fun clearAllItems()
    suspend fun clearAllRFID()
    suspend fun syncBulkItemsFromServer(request: ClientCodeRequest): List<AlllabelResponse.LabelItem>
    /*suspend fun syncBulkItemsFromServer(
        request: ClientCodeRequest,
        tagType: String,
        mapItem: suspend (AlllabelResponse.LabelItem) -> BulkItem?,
        onProgress: suspend (processed: Int, synced: Int) -> Unit = { _, _ -> }
    )*/
    suspend fun syncBulkItemsFromServer(
        request: ClientCodeRequest,
        tagType: String,
        mapItem: suspend (AlllabelResponse.LabelItem) -> BulkItem?,
        onProgress: suspend (processed: Int, synced: Int, totalCount: Int) -> Unit = { _, _,_ -> }
    )
    suspend fun insertSingleItem(item: BulkItem)
    suspend fun syncRFIDItemsFromServer(request: ClientCodeRequest): List<EpcDto>

    suspend fun getDistinctBranchNames(): List<String>
    suspend fun getDistinctCounterNames(): List<String>
    suspend fun getDistinctBoxNames(): List<String>

    suspend fun getBranchIdFromName(name: String): Int?
    suspend fun getCounterIdFromName(name: String): Int?
    suspend fun getBoxIdFromName(name: String): Int?
    suspend fun getPacketIdFromName(name: String): Int?

    suspend fun updateBulkItem(item: BulkItem) {
        val existing = bulkItemDao.getById(item.bulkItemId)
        if (existing == null) {
           // bulkItemDao.insertBulkItem(List<BulkItem>)
        } else {
            bulkItemDao.updateBulkItem(item)
        }
    }

    suspend fun deleteBulkItemById(id: Int): Int {
        return bulkItemDao.deleteById(id)
    }

    suspend fun clearStockData(req: ClearStockDataModelReq): ClearStockDataModelResponse


    suspend fun syncAndSaveBulkItems(clientCode: String) {
        val response = syncBulkItemsFromServer(ClientCodeRequest(clientCode))

        val bulkItems = response.map { it.toBulkItem() }

        bulkItemDao.clearAllItems()
        bulkItemDao.insertBulkItem(bulkItems)
    }
/*added this one for single use tag*/
    suspend fun getItemCodeByEpc(epc: String): String {
        return bulkItemDao.getItemCodeByEpc(epc) ?: ""
    }

    suspend fun updateScanStatus(items: List<BulkItem>) {
        bulkItemDao.updateBulkItems(items)
    }




}
