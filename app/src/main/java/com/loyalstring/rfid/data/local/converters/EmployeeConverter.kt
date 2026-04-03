package com.loyalstring.rfid.data.local.converters



import androidx.room.TypeConverter
import com.google.gson.Gson
import com.loyalstring.rfid.data.model.login.Employee

class EmployeeConverter {

    private val gson = Gson()

    @TypeConverter
    fun fromEmployee(employee: Employee?): String? {
        return if (employee == null) null else gson.toJson(employee)
    }

    @TypeConverter
    fun toEmployee(value: String?): Employee? {
        return if (value.isNullOrBlank()) null
        else gson.fromJson(value, Employee::class.java)
    }
}