package com.loyalstring.rfid.data.remote.data
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CompanyDetails(
    @SerializedName("ClientCode")
    var clientCode: String? = null,

    @SerializedName("CompCode")
    var compCode: String? = null,

    @SerializedName("CompName")
    var compName: String? = null,

    @SerializedName("CompShortName")
    var compShortName: String? = null,

    @SerializedName("CompOwnerName")
    var compOwnerName: String? = null,

    @SerializedName("CompRegisteredAddress")
    var compRegisteredAddress: String? = null,

    @SerializedName("CompFactoryAddress")
    var compFactoryAddress: String? = null,

    @SerializedName("CompMobileNo")
    var compMobileNo: String? = null,

    @SerializedName("CompPhoneNo")
    var compPhoneNo: String? = null,

    @SerializedName("CompRegistrationNo")
    var compRegistrationNo: String? = null,

    @SerializedName("CompYearOfEstablishment")
    var compYearOfEstablishment: String? = null,

    @SerializedName("CompEmail")
    var compEmail: String? = null,

    @SerializedName("CompGSTINNo")
    var compGSTINNo: String? = null,

    @SerializedName("CompPanNo")
    var compPanNo: String? = null,

    @SerializedName("CompAddharNo")
    var compAddharNo: String? = null,

    @SerializedName("CompLogo")
    var compLogo: String? = null,

    @SerializedName("CompWebsite")
    var compWebsite: String? = null,

    @SerializedName("CompVATNo")
    var compVATNo: String? = null,

    @SerializedName("CompCGSTNo")
    var compCGSTNo: String? = null,

    @SerializedName("CompStatus")
    var compStatus: String? = null,

    @SerializedName("Town")
    var town: String? = null,

    @SerializedName("Country")
    var country: String? = null,

    @SerializedName("State")
    var state: String? = null,

    @SerializedName("City")
    var city: String? = null,

    @SerializedName("FinancialYear")
    var financialYear: String? = null,

    @SerializedName("BaseCurrency")
    var baseCurrency: String? = null,

    @SerializedName("TransactionSeries")
    var transactionSeries: String? = null,

    @SerializedName("CompLoginStatus")
    var compLoginStatus: String? = null,

    @SerializedName("Id")
    var id: Int = 0,

    @SerializedName("CreatedOn")
    var createdOn: String? = null,

    @SerializedName("LastUpdated")
    var lastUpdated: String? = null,

    @SerializedName("StatusType")
    var statusType: Boolean = false
) : Serializable
