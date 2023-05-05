package com.api.payroll.employee

import com.api.payroll.calculateBaseSalary
import com.api.payroll.calculateSalaryTax
import com.api.payroll.calculateVales
import com.api.payroll.model.Employee
import com.api.payroll.model.Salary
import io.ktor.util.logging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.Date
import java.time.Instant

class EmployeeDBManager(private val connection: Connection) {
    private val LOGGER = KtorSimpleLogger("PAYROLL")

    private val EMPLOYEE_DRIVER = "chofer"
    private val EMPLOYEE_CHARGER = "cargador"
    private val EMPLOYEE_AUX = "auxiliar"

    companion object {
        private const val CREATE_TABLE_EMPLOYEES =
            "CREATE TABLE IF NOT EXISTS employees (\n" +
                    "  employee_id INT GENERATED ALWAYS AS IDENTITY,\n" +
                    "  name VARCHAR(50) NOT NULL,\n" +
                    "  rol VARCHAR(20) NOT NULL,\n" +
                    "  start_date DATE NOT NULL,\n" +
                    "  PRIMARY KEY(employee_id)\n" +
                    ");"

        private const val CREATE_TABLE_SALARIES =
            "CREATE TABLE IF NOT EXISTS salary (\n" +
                    "  salary_id INT GENERATED ALWAYS AS IDENTITY,\n" +
                    "  employee_id INT NOT NULL,\n" +
                    "  base_salary DECIMAL(8,2) NOT NULL,\n" +
                    "  hours_worked DECIMAL(8,2) NOT NULL,\n" +
                    "  deliveries_completed INT NOT NULL,\n" +
                    "  bonus_cargo DECIMAL(8,2) NOT NULL,\n" +
                    "  tax_isr DECIMAL(8,2) NOT NULL,\n" +
                    "  vales_despensa DECIMAL(8,2) NOT NULL,\n" +
                    "  payment_date DATE NOT NULL,\n" +
                    "  PRIMARY KEY(salary_id),\n" +
                    "  CONSTRAINT fk_employee\n" +
                    "    FOREIGN KEY(employee_id)\n" +
                    "        REFERENCES employees(employee_id)\n" +
                    ");"

        private const val CREATE_EMPLOYEE_PROCEDURE =
            "CREATE OR REPLACE PROCEDURE create_employee(\t\n" +
                    "\tname IN employees.name%TYPE,\n" +
                    "\trol IN employees.rol%TYPE,\n" +
                    "\tstart_date IN employees.start_date%TYPE\n" +
                    ")\n" +
                    "\n" +
                    "LANGUAGE plpgsql\n" +
                    "AS \$\$\n" +
                    "BEGIN\n" +
                    "  INSERT INTO employees (\n" +
                    "    name,\n" +
                    "    rol,\n" +
                    "\tstart_date\n" +
                    "  ) VALUES (\n" +
                    "    name,\n" +
                    "    rol,\n" +
                    "\tstart_date\n" +
                    "  ) ;\n" +
                    "  COMMIT;\n" +
                    "END;\$\$;"

        private const val CREATE_INSERT_SALARY_PROCEDURE =
            "CREATE OR REPLACE PROCEDURE insert_salary(\t\n" +
                    "\temployee_id IN salary.employee_id%TYPE,\n" +
                    "\tbase_salary IN salary.base_salary%TYPE,\n" +
                    "\thours_worked IN salary.hours_worked%TYPE,\n" +
                    "\tdeliveries_completed IN salary.deliveries_completed%TYPE,\n" +
                    "\tbonus_cargo IN salary.bonus_cargo%TYPE,\n" +
                    "\ttax_isr IN salary.tax_isr%TYPE,\n" +
                    "\tvales_despensa IN salary.vales_despensa%TYPE,\n" +
                    "\tpayment_date IN salary.payment_date%TYPE\n" +
                    ")\n" +
                    "\n" +
                    "LANGUAGE plpgsql\n" +
                    "AS \$\$\n" +
                    "BEGIN\n" +
                    "  INSERT INTO salary (\n" +
                    "    employee_id,\n" +
                    "    base_salary,\n" +
                    "\thours_worked,\n" +
                    "\tdeliveries_completed,\n" +
                    "\tbonus_cargo,\n" +
                    "\ttax_isr,\n" +
                    "\tvales_despensa,\n" +
                    "\tpayment_date  \n" +
                    "  ) VALUES (\n" +
                    " employee_id,\n" +
                    " base_salary,\n" +
                    "\thours_worked,\n" +
                    "\tdeliveries_completed,\n" +
                    "\tbonus_cargo,\n" +
                    "\ttax_isr,\n" +
                    "\tvales_despensa,\n" +
                    "\tpayment_date\n" +
                    "  ) ;\n" +
                    "  COMMIT;\n" +
                    "END;\$\$;"

        private const val INSERT_EMPLOYEE = "CALL create_employee(?, ?, ?);"
        private const val INSERT_SALARY = "CALL insert_salary(?, ?, ?, ?, ?, ?, ?, ?);"
        private const val SELECT_EMPLOYEE_BY_ID = "SELECT * FROM employees e INNER JOIN salary s on e.employee_id = s.employee_id WHERE e.employee_id = ?"
        private const val CREATE_EMPLOYEE_WITH_SALARY = "with rows as (\n" +
                "INSERT INTO employees (name, rol, start_date) VALUES (?, ?, ?) returning employee_id\n" +
                ")\n" +
                "INSERT INTO salary(employee_id,\n" +
                " base_salary,\n" +
                "\thours_worked,\n" +
                "\tdeliveries_completed,\n" +
                "\tbonus_cargo,\n" +
                "\ttax_isr,\n" +
                "\tvales_despensa,\n" +
                "\tpayment_date,\n" +
                "\ttotal_salary)"+
                "select employee_id , ?, ?, ?, ?, ?, ?, ?, ? from rows;"

        private const val SELECT_RECENT_EMPLOYEES = "SELECT * FROM employees e " +
                "INNER JOIN salary s on e.employee_id = s.employee_id " +
                "ORDER BY e.employee_id DESC;"

        private const val UPDATE_EMPLOYEE = "WITH rows as (\n" +
                "\tUPDATE employees \n" +
                "\t\tSET name = ?, rol =?, start_date = ?  \n" +
                "\tWHERE employee_id = ? \n" +
                "\treturning employee_id\n" +
                ")\n" +
                "UPDATE salary\n" +
                "SET employee_id = employee_id,\n" +
                "hours_worked = ?, \n" +
                "deliveries_completed = ?, \n" +
                "bonus_cargo = ?, \n" +
                "tax_isr = ?, \n" +
                "vales_despensa = ?, \n" +
                "payment_date = ?, \n" +
                "total_salary = ? WHERE employee_id = ?"

        private const val INSERT_DELIVERIES_BY_EMPLOYEE_ID = "INSERT INTO employee_deliveries (employee_id, deliveries_quantity, delivery_date)\n" +
                "\tVALUES(?, ?, ?);"
    }

    init {
        val statement = connection.createStatement()
        statement.executeUpdate(CREATE_TABLE_EMPLOYEES)
        statement.executeUpdate(CREATE_TABLE_SALARIES)
    }

    // create new employee
    suspend fun createEmployee(employee: Employee): Employee = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(INSERT_EMPLOYEE)
        statement.setString(1, employee.name)
        statement.setString(2, employee.rol)
        statement.setString(3, employee.startDate)
        statement.executeUpdate()

        return@withContext employee
    }

    suspend fun insertSalary(salary: Salary): Salary = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(INSERT_SALARY)
        statement.setInt(1, salary.employeeId)
        statement.setFloat(2, salary.baseSalary)
        statement.setFloat(3, salary.hoursWorked)
        statement.setInt(4, salary.deliveriesCompleted)
        statement.setFloat(5, salary.taxISR)
        statement.setFloat(6, salary.bonusCargo)
        statement.setFloat(7, salary.valesDespensa)
        statement.setString(8, salary.paymentDate)
        statement.executeUpdate()

        return@withContext salary
    }

    /**
     * Creates new employees and sets its current salary and dates
     * @param @Employee
     * @return the new created @Employee
     */
    suspend fun createEmployeeWithSalary(employee: Employee): Employee = withContext(Dispatchers.IO){
        val statement = connection.prepareStatement(CREATE_EMPLOYEE_WITH_SALARY)
        statement.setString(1, employee.name)
        statement.setString(2, employee.rol)
        statement.setDate(3, Date.valueOf(employee.startDate))
        statement.setFloat(4, calculateBaseSalary())
        statement.setFloat(5, employee.salary.hoursWorked)
        statement.setInt(6, employee.salary.deliveriesCompleted)
        statement.setFloat(7, employee.salary.bonusCargo)
        statement.setFloat(8, calculateSalaryTax())
        statement.setFloat(9, calculateVales())
        statement.setDate(10, Date.valueOf(employee.salary.paymentDate))
        statement.setFloat(11, employee.salary.totalSalary)
        statement.executeUpdate()
        return@withContext employee
    }

    suspend fun insertEmployee(employee: Employee): Employee = withContext(Dispatchers.IO){

        employee.salary = setDefaultSalaryForEmployee(employee)

        createEmployeeWithSalary(employee)
        return@withContext employee
    }

    suspend fun searchEmployeeById(employeeId: Int): Employee = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(SELECT_EMPLOYEE_BY_ID)
        statement.setInt(1, employeeId)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            LOGGER.trace(resultSet.getString("name"))
            val employee = Employee(
                resultSet.getInt("employee_ID"),
                resultSet.getString("name"),
                resultSet.getString("rol"),
                resultSet.getString("start_date"),
                salary = Salary(
                    resultSet.getInt("employee_id"),
                    resultSet.getFloat("base_salary"),
                    resultSet.getFloat("hours_worked"),
                    resultSet.getInt("deliveries_completed"),
                    resultSet.getFloat("bonus_cargo"),
                    resultSet.getFloat("tax_isr"),
                    resultSet.getFloat("vales_despensa"),
                    resultSet.getString("payment_date"))
            )
            LOGGER.trace(employee.toString())
            return@withContext employee

        } else {
            throw Exception("Record not found")
        }
    }

    suspend fun returnRecentEmployees() = withContext(Dispatchers.IO){
        val statement = connection.prepareStatement(SELECT_RECENT_EMPLOYEES)
        val resultSet = statement.executeQuery()
        val employees = arrayListOf<Employee>()
        while (resultSet.next()){
            val employee = Employee(
                resultSet.getInt("employee_ID"),
                resultSet.getString("name"),
                resultSet.getString("rol"),
                resultSet.getString("start_date"),
                salary = Salary(
                    resultSet.getInt("employee_id"),
                    resultSet.getFloat("base_salary"),
                    resultSet.getFloat("hours_worked"),
                    resultSet.getInt("deliveries_completed"),
                    resultSet.getFloat("bonus_cargo"),
                    resultSet.getFloat("tax_isr"),
                    resultSet.getFloat("vales_despensa"),
                    resultSet.getString("payment_date"),
                    resultSet.getFloat("total_salary"))
            )
            employees.add(employee)
        }
        return@withContext employees
    }

    suspend fun updateEmployee(employee: Employee) = withContext(Dispatchers.IO) {
        val statementUpdate = connection.prepareStatement(UPDATE_EMPLOYEE)
        statementUpdate.setString(1, employee.name)
        statementUpdate.setString(2, employee.rol)
        statementUpdate.setDate(3, Date.valueOf(employee.startDate))
//
        statementUpdate.setInt(4, employee.id)

        statementUpdate.setFloat(5, employee.salary.hoursWorked)
        statementUpdate.setInt(6, employee.salary.deliveriesCompleted)
        statementUpdate.setFloat(7, employee.salary.bonusCargo)
        statementUpdate.setFloat(8, employee.salary.taxISR)
        statementUpdate.setFloat(9, employee.salary.valesDespensa)
        statementUpdate.setDate(10, Date.valueOf(employee.salary.paymentDate))
        statementUpdate.setFloat(11, employee.salary.totalSalary)
        statementUpdate.setInt(12, employee.id)
        statementUpdate.executeUpdate()

        val statementInsertDelivery = connection.prepareStatement(INSERT_DELIVERIES_BY_EMPLOYEE_ID)
        statementInsertDelivery.setInt(1, employee.id)
        statementInsertDelivery.setInt(2, employee.deliveries[0].deliveryQuantity)
        statementInsertDelivery.setDate(3, Date.valueOf(employee.deliveries[0].deliveryDate))
        statementInsertDelivery.executeUpdate()

        return@withContext employee
    }

    fun setDefaultSalaryForEmployee(employee: Employee): Salary{
        val baseSalary = 30.00F
        val hoursWorked = 0F
        val deliveriesCompleted = 0
        val bonusCargo = 0F
        val taxISR = 9F
        val valesDespensa = 4F
        val paymentDate = Date.from(Instant.now()).toString()
        return Salary(
            employee.id,
            baseSalary,
            hoursWorked,
            deliveriesCompleted,
            bonusCargo,
            taxISR,
            valesDespensa,
            paymentDate)

    }
}