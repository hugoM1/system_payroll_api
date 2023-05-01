package com.api.payroll.model

import kotlinx.serialization.Serializable

@Serializable
data class Employee(
    val id: Int = -1,
    val name: String = " ",
    val rol: String = " ",
    val startDate: String = " "
)
