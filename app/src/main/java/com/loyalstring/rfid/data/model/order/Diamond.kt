package com.loyalstring.rfid.data.model.order
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.loyalstring.rfid.data.local.entity.BulkItem
import kotlinx.parcelize.Parcelize
@Entity(
    tableName = "bulk_item_diamonds",
    foreignKeys = [
        ForeignKey(
            entity = BulkItem::class,
            parentColumns = ["id"],
            childColumns = ["bulkItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bulkItemId")]
)
@Parcelize
data class Diamond(   @PrimaryKey(autoGenerate = true)
                      val id: Int = 0,

                      val bulkItemId: Int,
                    val diamondName: String?,
                    val diamondProductName: String?,
                    val diamondWeight: String?,
                    val diamondSellRate: String?,
                    val diamondPieces: String?,
                    val diamondClarity: String?,
                    val diamondClarityName: String?,
                    val diamondColour: String?,
                    val diamondColourName: String?,
                    val diamondCut: String?,
                    val diamondShape: String?,
                    val diamondShapeName: String?,
                    val diamondSize: String?,
                    val certificate: String?,
                    val settingType: String?,
                    val diamondSellAmount: String?,
                    val diamondPurchaseAmount: String?,
                    val description: String?,
                    val clientCode: String?,
                    val labelledStockId: Int,
                    val companyId: Int,
                    val counterId: Int,
                    val branchId: Int,
                    val employeeId: Int,
                    val createdOn: String,
                    val lastUpdated: String,
                    val diamondMargin: String?,
                    val totalDiamondWeight: String?,
                    val diamondSleve: String?,
                    val diamondRate: String?,
                    val diamondAmount: String?,
                    val diamondPacket: String?,
                    val diamondBox: String?,
                    val diamondDescription: String?,
                    val diamondSettingType: String?,
                    val diamondDeduct: String?) : Parcelable
