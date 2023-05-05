package com.api.payroll

const val BASE_SALARY_HOUR: Int = 30
const val BASE_LABOR_HOURS: Int = 8
const val BASE_LABOR_DAYS: Int = 6
const val BASE_LABOR_WEEKS: Int = 4
const val BASE_VALES: Int = 4
const val BASE_TAX_ISR: Int = 9

fun calculateBaseSalary(): Float {
    return (BASE_SALARY_HOUR * BASE_LABOR_HOURS * BASE_LABOR_DAYS * BASE_LABOR_WEEKS).toFloat()
}

fun calculateVales(): Float {
    return ((calculateBaseSalary() * BASE_VALES) / 100)
}

fun calculateSalaryTax(): Float {
    val salaryTax =  (calculateBaseSalary() * BASE_TAX_ISR)/ 100
    return calculateBaseSalary() - salaryTax
}