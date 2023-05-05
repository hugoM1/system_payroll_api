package com.api.payroll.employee

import com.api.payroll.model.Employee
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.Connection
import java.sql.DriverManager

fun Application.employeeCRUD() {
    val dbConnection: Connection = connectToPostgres(embedded = false)
    val employeeManager = EmployeeDBManager(dbConnection)
    routing {
        // Create Employee
        post("/employees/"){
            val employee = call.receive<Employee>()
            val employeeCreated = employeeManager.createEmployeeWithSalary(employee)
            call.respond(HttpStatusCode.Created, employee)
        }

        post("employees/update"){
            val employee = call.receive<Employee>()
            employeeManager.updateEmployee(employee)
            call.respond(HttpStatusCode.OK, employee)
        }

        // Search Employee By ID
        get("/employees/{id}"){
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            try {
                val employee = employeeManager.searchEmployeeById(id)
                call.respond(HttpStatusCode.OK, employee)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        get("/employees/all/recent"){
            try {
                val res = employeeManager.returnRecentEmployees()
                call.respond(HttpStatusCode.OK, res)
            }catch (e: Exception){
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

/**
 * Makes a connection to a Postgres database.
 *
 * In order to connect to your running Postgres process,
 * please specify the following parameters in your configuration file:
 * - postgres.url -- Url of your running database process.
 * - postgres.user -- Username for database connection
 * - postgres.password -- Password for database connection
 *
 * If you don't have a database process running yet, you may need to [download]((https://www.postgresql.org/download/))
 * and install Postgres and follow the instructions [here](https://postgresapp.com/).
 * Then, you would be able to edit your url,  which is usually "jdbc:postgresql://host:port/database", as well as
 * user and password values.
 *
 *
 * @param embedded -- if [true] defaults to an embedded database for tests that runs locally in the same process.
 * In this case you don't have to provide any parameters in configuration file, and you don't have to run a process.
 *
 * @return [Connection] that represent connection to the database. Please, don't forget to close this connection when
 * your application shuts down by calling [Connection.close]
 * */
fun Application.connectToPostgres(embedded: Boolean): Connection {
    Class.forName("org.postgresql.Driver")
    if (embedded) {
        return DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "postgres", "admin")
    } else {
        val url = environment.config.property("postgres.url").getString()
        val user = environment.config.property("postgres.user").getString()
        val password = environment.config.property("postgres.password").getString()

        return DriverManager.getConnection(url, user, password)
    }
}