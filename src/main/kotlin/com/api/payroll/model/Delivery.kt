package com.api.payroll.model

import kotlinx.serialization.Serializable

@Serializable
data class Delivery(
    val employeeId: Int = 0,
    val deliveryQuantity : Int = -1,
    val deliveryDate : String = ""
)
