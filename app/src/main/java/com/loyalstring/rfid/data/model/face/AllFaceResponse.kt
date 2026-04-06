package com.loyalstring.rfid.data.model.face

data class AllFaceResponse( val Message: String,
                            val Count: Int,
                            val Data: List<FaceData>)
