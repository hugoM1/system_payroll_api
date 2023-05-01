package com.api.payroll

import com.api.payroll.employee.employeeCRUD
import io.ktor.server.application.*
import com.api.payroll.plugins.*
import io.ktor.network.tls.certificates.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory
import java.io.File

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}
fun Application.module() {
    configureSockets()
    configureSerialization()
    configureDatabases()
    configureMonitoring()
    configureHTTP()
    configureSecurity()
    configureRouting()
    employeeCRUD()
}
