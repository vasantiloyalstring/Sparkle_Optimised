package com.loyalstring.rfid.data.model.sampleOut

data class SampleOutListResponse(
    val Id: Int,
    val SampleStatus: String,
    val SampleOutNo: String,
    val StatusType: Boolean,
    val CreatedOn: String,
    val LastUpdated: String,
    val CustomerId: Int,
    val Quantity: Int,
    val TotalWt: String,
    val PackingWeight: String,
    val Status: String,
    val TotalGrossWt: String,
    val TotalNetWt: String,
    val VendorId: Int,
    val FineWastagePercent: String,
    val FineWastageWt: String,
    val TotalStoneWeight: String,
    val TotalDiamondWeight: String,
    val ReturnDate: String?,      // null in JSON
    val Description: String?,     // can be ""
    val ClientCode: String,
    val SampleInDate: String?,    // null in JSON
    val BranchId: Int?,           // null in JSON
    val IssueItems: List<IssueItemDto>,
    val Customer: SampleCustomerDto?
)
