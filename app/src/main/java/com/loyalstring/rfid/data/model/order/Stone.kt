package com.loyalstring.rfid.data.model.order

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.loyalstring.rfid.data.local.entity.BulkItem
import kotlinx.parcelize.Parcelize


@Entity(
    tableName = "bulk_item_stones", foreignKeys = [ForeignKey(
        entity = BulkItem::class,
        parentColumns = ["id"],
        childColumns = ["bulkItemId"],
        onDelete = ForeignKey.CASCADE
    )], indices = [Index("bulkItemId")]
)
@Parcelize
data class Stone(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val bulkItemId: Int,

    val StoneName: String?,
    val StoneWeight: String?,
    val StonePieces: String?,
    val StoneRate: String?, // or Double? if numeric
    val StoneAmount: String?,
    val Description: String?,
    val ClientCode: String?,
    val LabelledStockId: Int?,
    val CompanyId: Int?,
    val CounterId: Int?,
    val BranchId: Int?,
    val EmployeeId: Int?,
    val CreatedOn: String?,
    val LastUpdated: String?,
    val StoneLessPercent: String?,
    val StoneCertificate: String?,
    val StoneSettingType: String?,
    val StoneRatePerPiece: String?,
    val StoneRateKarate: String?,
    val StoneStatusType: String?
) : Parcelable
