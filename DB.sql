-- DROP DATABASE IF EXISTS postgres;

CREATE DATABASE payroll
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    IS_TEMPLATE = False;

COMMENT ON DATABASE postgres
    IS 'default administrative connection database';

CREATE TABLE employees (
  employee_id INT GENERATED ALWAYS AS IDENTITY,
  name VARCHAR(50) NOT NULL,
  rol VARCHAR(20) NOT NULL,
  start_date DATE NOT NULL,
  PRIMARY KEY(employee_id)
);

CREATE TABLE salary (
  salary_id INT GENERATED ALWAYS AS IDENTITY,
  employee_id INT NOT NULL,
  base_salary DECIMAL(8,2) NOT NULL,
  hours_worked DECIMAL(8,2) NOT NULL,
  deliveries_completed INT NOT NULL,
  bonus_cargo DECIMAL(8,2) NOT NULL,
  tax_isr DECIMAL(8,2) NOT NULL,
  vales_despensa DECIMAL(8,2) NOT NULL,
  payment_date DATE NOT NULL,
  PRIMARY KEY(salary_id),
  CONSTRAINT fk_employee
    FOREIGN KEY(employee_id)
        REFERENCES employees(employee_id)
);

CREATE TABLE money_loans (
  id SERIAL PRIMARY KEY,
  id_employee INT NOT NULL,
  loan_quantity DECIMAL(8,2) NOT NULL,
  loan_payment DECIMAL(8,2) NOT NULL,
  missing_balance DECIMAL(8,2) NOT NULL,
  payment_date DATE NOT NULL,
  FOREIGN KEY (id_employee) REFERENCES employees(id)
);

CREATE TABLE extra_time (
  id SERIAL PRIMARY KEY,
  id_employee INT NOT NULL,
  extra_time DECIMAL(8,2) NOT NULL,
  tasa_extra_time DECIMAL(8,2) NOT NULL,
  payment_date DATE NOT NULL,
  FOREIGN KEY (id_employee) REFERENCES employees(id)
);

CREATE TABLE departments (
  id SERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL
);

-- procedures

CREATE OR REPLACE PROCEDURE create_employee(	
	name IN employees.name%TYPE,
	rol IN employees.rol%TYPE,
	start_date IN employees.start_date%TYPE
)

LANGUAGE plpgsql
AS $$
BEGIN
  INSERT INTO employees (
    name,
    rol,
	start_date
  ) VALUES (
    name,
    rol,
	start_date
  ) ;
  COMMIT;
END;$$;

CALL create_employee('compita', 'EM', '1922-02-02');
select * from employees;

CREATE OR REPLACE PROCEDURE insert_salary(	
	employee_id IN salary.employee_id%TYPE,
	base_salary IN salary.base_salary%TYPE,
	hours_worked IN salary.hours_worked%TYPE,
	deliveries_completed IN salary.deliveries_completed%TYPE,
	bonus_cargo IN salary.bonus_cargo%TYPE,
	tax_isr IN salary.tax_isr%TYPE,
	vales_despensa IN salary.vales_despensa%TYPE,
	payment_date IN salary.payment_date%TYPE
)

LANGUAGE plpgsql
AS $$
BEGIN
  INSERT INTO salary (
    employee_id,
    base_salary,
	hours_worked,
	deliveries_completed,
	bonus_cargo,
	tax_isr,
	vales_despensa,
	payment_date  
  ) VALUES (
 employee_id,
 base_salary,
	hours_worked,
	deliveries_completed,
	bonus_cargo,
	tax_isr,
	vales_despensa,
	payment_date
  ) ;
  COMMIT;
END;$$;

CALL insert_salary(1, 20.0, 8,4,34.0,4.0,45,'1922-02-02');
select * from salary;