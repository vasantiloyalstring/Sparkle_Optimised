package com.loyalstring.rfid.data.model.report

data class SessionListResponse(val Message: String,
                               val ClientCode: String,
                               val TotalSessions: Int,
                               val Sessions: List<SessionItem>
    )
