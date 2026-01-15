package com.loyalstring.rfid.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.loyalstring.rfid.data.model.order.Diamond
import com.loyalstring.rfid.data.model.order.Stone

class StoneDiamondConverter {

    private val gson = Gson()

    // ---------- Stone ----------
    @TypeConverter
    fun stoneListToJson(list: List<Stone>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun jsonToStoneList(json: String?): List<Stone>? {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<Stone>>() {}.type
        return gson.fromJson(json, type)
    }

    // ---------- Diamond ----------
    @TypeConverter
    fun diamondListToJson(list: List<Diamond>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun jsonToDiamondList(json: String?): List<Diamond>? {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<Diamond>>() {}.type
        return gson.fromJson(json, type)
    }
}