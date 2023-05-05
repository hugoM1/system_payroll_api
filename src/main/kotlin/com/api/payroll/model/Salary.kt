package com.api.payroll.model

import kotlinx.serialization.Serializable

@Serializable
data class Salary(
    val employeeId: Int = 0,
    val baseSalary: Float = 0.0F,
    val hoursWorked: Float = 0.0F,
    val deliveriesCompleted: Int = 0,
    val bonusCargo: Float = 0.0F,
    val taxISR: Float = 0.0F,
    val valesDespensa: Float = 0.0F,
    val paymentDate: String = " ",
    val totalSalary: Float = 0.0F
)
