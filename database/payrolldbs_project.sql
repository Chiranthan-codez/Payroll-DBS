-- =============================================================================
--  PAYROLL MANAGEMENT SYSTEM — COMPLETE DATABASE SCRIPT
--  Course  : CSS 2212 — Database Systems Lab, MIT Manipal (2025-2026)
--  Database: payrolldbs  |  Engine: MySQL 8.x
--
--  Lab Manual Coverage Map
--  ───────────────────────
--  Lab 1  : DDL (CREATE, ALTER, DROP, RENAME, TRUNCATE), DML (INSERT/SELECT/UPDATE/DELETE)
--  Lab 2  : ALL integrity constraints (PK, FK, UNIQUE, NOT NULL, DEFAULT, CHECK, ENUM, named)
--  Lab 3  : VIEWs, SET operations (UNION/INTERSECT emulation), nested sub-queries
--  Lab 4  : GROUP BY, HAVING, ORDER BY, WITH clause, derived relations, COMMIT/ROLLBACK/SAVEPOINT
--  Lab 5  : ER → Relational schema (1:1, 1:N, M:N, multi-valued attributes)
--  Lab 11 : JDBC-compatible schema (used directly by the JavaFX project)
--  Extras : DELIMITER-safe stored procedures, functions, triggers (Labs 9-10 concepts in MySQL)
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- SECTION 0 :  DATABASE CREATION  (Lab 1 – DDL)
-- ─────────────────────────────────────────────────────────────────────────────
DROP DATABASE IF EXISTS payrolldbs;
CREATE DATABASE payrolldbs
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE payrolldbs;


-- =============================================================================
-- SECTION 1 :  TABLE DEFINITIONS  (Lab 1 DDL + Lab 2 Constraints + Lab 5 ER→Schema)
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- Table 1: departments
--  ER  : Strong entity  |  Cardinality: 1 dept → N employees
--  Lab 2 constraints used: PRIMARY KEY (named), NOT NULL, UNIQUE, CHECK
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE departments (
    dept_id    INT          NOT NULL AUTO_INCREMENT,
    dept_name  VARCHAR(250) NOT NULL,

    -- Named constraints (Lab 2)
    CONSTRAINT pk_departments PRIMARY KEY (dept_id),
    CONSTRAINT uq_dept_name   UNIQUE      (dept_name),
    -- CHECK: dept_name must not be blank (Lab 2 – CHECK constraint)
    CONSTRAINT chk_dept_name  CHECK       (TRIM(dept_name) <> '')
);


-- ─────────────────────────────────────────────────────────────────────────────
-- Table 2: designations
--  ER  : Strong entity  |  Cardinality: 1 designation → N employees
--  Lab 2: PRIMARY KEY, NOT NULL
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE designations (
    designation_id   INT          NOT NULL AUTO_INCREMENT,
    designation_name VARCHAR(250) NOT NULL,

    CONSTRAINT pk_designations PRIMARY KEY (designation_id)
);


-- ─────────────────────────────────────────────────────────────────────────────
-- Table 3: employees
--  ER  : Core strong entity
--  Lab 2: PK, NOT NULL, UNIQUE (nic), FOREIGN KEY with ON DELETE SET NULL,
--          CHECK (gender), DEFAULT (gender)
--  Lab 5: 1:N from departments; 1:N from designations
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE employees (
    empid                    INT         NOT NULL AUTO_INCREMENT,
    nic                      VARCHAR(35) NOT NULL,
    fname                    VARCHAR(30) NOT NULL,
    lname                    VARCHAR(30) NOT NULL,
    dob                      DATE,
    gender                   VARCHAR(10) NOT NULL DEFAULT 'Male',
    date_of_joining          DATE        NOT NULL,
    current_dept_id          INT,
    current_designation_id   INT,

    CONSTRAINT pk_employees              PRIMARY KEY (empid),
    CONSTRAINT uq_nic                    UNIQUE      (nic),
    -- CHECK: gender must be 'Male' or 'Female' or 'Other'  (Lab 2)
    CONSTRAINT chk_gender                CHECK       (gender IN ('Male','Female','Other')),
    -- FK with ON DELETE SET NULL  (Lab 2 – FK behaviour)
    CONSTRAINT fk_emp_dept               FOREIGN KEY (current_dept_id)
        REFERENCES departments(dept_id)  ON DELETE SET NULL,
    CONSTRAINT fk_emp_desig              FOREIGN KEY (current_designation_id)
        REFERENCES designations(designation_id) ON DELETE SET NULL
);


-- ─────────────────────────────────────────────────────────────────────────────
-- Table 4: employee_addresses
--  ER  : Weak entity dependent on employees  (1:1)
--  Lab 2: NOT NULL, FK with ON DELETE CASCADE, DEFAULT
--  Lab 5: Multi-valued address decomposed into atomic columns (1NF)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE employee_addresses (
    address_id    INT          NOT NULL AUTO_INCREMENT,
    empid         INT          NOT NULL,
    address_line1 TEXT         NOT NULL,
    address_line2 TEXT,
    city          VARCHAR(30)  NOT NULL DEFAULT 'Unknown',
    postal_code   VARCHAR(20),

    CONSTRAINT pk_emp_addr   PRIMARY KEY (address_id),
    CONSTRAINT fk_addr_emp   FOREIGN KEY (empid)
        REFERENCES employees(empid) ON DELETE CASCADE
);


-- ─────────────────────────────────────────────────────────────────────────────
-- Table 5: employee_contact_numbers
--  ER  : Multi-valued attribute → own table  (Lab 5 pattern)
--        1 employee → N contact numbers
--  Lab 2: ENUM constraint on contact_type, NOT NULL, FK ON DELETE CASCADE
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE employee_contact_numbers (
    contact_id     INT         NOT NULL AUTO_INCREMENT,
    empid          INT         NOT NULL,
    contact_type   ENUM('home','mobile','work') NOT NULL,
    contact_number VARCHAR(15) NOT NULL,

    CONSTRAINT pk_contacts     PRIMARY KEY (contact_id),
    CONSTRAINT fk_contact_emp  FOREIGN KEY (empid)
        REFERENCES employees(empid) ON DELETE CASCADE
);


-- ─────────────────────────────────────────────────────────────────────────────
-- Table 6: salary_structures
--  ER  : Independent lookup entity  |  1 structure → N employee salary records
--  Lab 2: PK, NOT NULL, DEFAULT, CHECK (allowances must be >= 0)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE salary_structures (
    structure_id      INT    NOT NULL AUTO_INCREMENT,
    travel_allowance  DOUBLE NOT NULL DEFAULT 0.00,
    food_allowance    DOUBLE NOT NULL DEFAULT 0.00,
    bonus             DOUBLE NOT NULL DEFAULT 0.00,

    CONSTRAINT pk_salary_structures     PRIMARY KEY (structure_id),
    CONSTRAINT chk_travel_allowance     CHECK (travel_allowance >= 0),
    CONSTRAINT chk_food_allowance       CHECK (food_allowance   >= 0),
    CONSTRAINT chk_bonus                CHECK (bonus            >= 0)
);


-- ─────────────────────────────────────────────────────────────────────────────
-- Table 7: employee_salaries
--  ER  : Relationship entity linking employees (1:1) and salary_structures (N:1)
--  Lab 2: PK, NOT NULL, FK ON DELETE CASCADE & RESTRICT, CHECK (positive values)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE employee_salaries (
    salary_record_id INT    NOT NULL AUTO_INCREMENT,
    empid            INT    NOT NULL,
    structure_id     INT    NOT NULL,
    base_salary      INT    NOT NULL,
    epf              DOUBLE NOT NULL DEFAULT 0.00,
    tax              DOUBLE NOT NULL DEFAULT 0.00,
    paye             DOUBLE NOT NULL DEFAULT 0.00,

    CONSTRAINT pk_emp_salaries      PRIMARY KEY (salary_record_id),
    CONSTRAINT chk_base_salary      CHECK (base_salary > 0),
    CONSTRAINT chk_epf              CHECK (epf  >= 0),
    CONSTRAINT chk_tax              CHECK (tax  >= 0),
    CONSTRAINT chk_paye             CHECK (paye >= 0),
    CONSTRAINT fk_sal_emp           FOREIGN KEY (empid)
        REFERENCES employees(empid)          ON DELETE CASCADE,
    CONSTRAINT fk_sal_structure     FOREIGN KEY (structure_id)
        REFERENCES salary_structures(structure_id) ON DELETE RESTRICT
);


-- ─────────────────────────────────────────────────────────────────────────────
-- Table 8: leave_types
--  ER  : Lookup / master entity
--  Lab 2: PK, UNIQUE, NOT NULL
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE leave_types (
    leave_type_id    INT         NOT NULL AUTO_INCREMENT,
    leave_type_name  VARCHAR(50) NOT NULL,
    leave_description TEXT,

    CONSTRAINT pk_leave_types    PRIMARY KEY (leave_type_id),
    CONSTRAINT uq_leave_type_nm  UNIQUE      (leave_type_name)
);


-- ─────────────────────────────────────────────────────────────────────────────
-- Table 9: employee_leave_balances
--  ER  : Relationship entity  (M:N between employees and leave_types resolved)
--  Lab 2: composite UNIQUE KEY, FK ON DELETE CASCADE & RESTRICT, DEFAULT, CHECK
--  Lab 5: normalised row-based pattern replacing three hard-coded columns (1NF fix)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE employee_leave_balances (
    balance_id       INT NOT NULL AUTO_INCREMENT,
    empid            INT NOT NULL,
    leave_type_id    INT NOT NULL,
    available_leaves INT NOT NULL DEFAULT 0,

    CONSTRAINT pk_leave_balances      PRIMARY KEY (balance_id),
    -- Composite UNIQUE: one row per employee per leave type  (Lab 2)
    CONSTRAINT uq_emp_leave           UNIQUE      (empid, leave_type_id),
    CONSTRAINT chk_avail_leaves       CHECK       (available_leaves >= 0),
    CONSTRAINT fk_lb_emp              FOREIGN KEY (empid)
        REFERENCES employees(empid)          ON DELETE CASCADE,
    CONSTRAINT fk_lb_leave_type       FOREIGN KEY (leave_type_id)
        REFERENCES leave_types(leave_type_id) ON DELETE RESTRICT
);


-- ─────────────────────────────────────────────────────────────────────────────
-- Table 10: system_users
--  ER  : 1:1 with employees
--  Lab 2: PK, UNIQUE (empid = 1:1 enforcement, username), NOT NULL, FK CASCADE
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE system_users (
    user_id       INT          NOT NULL AUTO_INCREMENT,
    empid         INT          NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    CONSTRAINT pk_system_users   PRIMARY KEY (user_id),
    CONSTRAINT uq_su_empid       UNIQUE      (empid),        -- enforces 1:1
    CONSTRAINT uq_username       UNIQUE      (username),
    CONSTRAINT fk_su_emp         FOREIGN KEY (empid)
        REFERENCES employees(empid) ON DELETE CASCADE
);


-- ─────────────────────────────────────────────────────────────────────────────
-- Table 11: user_roles
--  ER  : Role master table  (M:N with system_users via junction table)
--  Lab 2: PK, UNIQUE, NOT NULL
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE user_roles (
    role_id   INT         NOT NULL AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL,

    CONSTRAINT pk_user_roles  PRIMARY KEY (role_id),
    CONSTRAINT uq_role_name   UNIQUE      (role_name)
);


-- ─────────────────────────────────────────────────────────────────────────────
-- Table 12: user_role_assignments
--  ER  : Junction / bridge table resolving M:N (system_users ↔ user_roles)
--  Lab 5: N:N relationship pattern
--  Lab 2: Composite UNIQUE, FK ON DELETE CASCADE & RESTRICT
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE user_role_assignments (
    assignment_id INT NOT NULL AUTO_INCREMENT,
    user_id       INT NOT NULL,
    role_id       INT NOT NULL,

    CONSTRAINT pk_role_assignments  PRIMARY KEY (assignment_id),
    CONSTRAINT uq_user_role         UNIQUE      (user_id, role_id),
    CONSTRAINT fk_ura_user          FOREIGN KEY (user_id)
        REFERENCES system_users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_ura_role          FOREIGN KEY (role_id)
        REFERENCES user_roles(role_id)   ON DELETE RESTRICT
);


-- ─────────────────────────────────────────────────────────────────────────────
-- Table 13: payroll_audit_log
--  Used by Trigger (Lab 10) to record every salary change
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE payroll_audit_log (
    log_id          INT          NOT NULL AUTO_INCREMENT,
    empid           INT          NOT NULL,
    action_type     VARCHAR(10)  NOT NULL,   -- 'INSERT', 'UPDATE', 'DELETE'
    old_base_salary INT,
    new_base_salary INT,
    changed_by      VARCHAR(50)  NOT NULL DEFAULT 'system',
    changed_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_audit_log  PRIMARY KEY (log_id),
    CONSTRAINT chk_action    CHECK (action_type IN ('INSERT','UPDATE','DELETE'))
);


-- =============================================================================
-- SECTION 2 :  DDL ALTERATIONS  (Lab 1 – ALTER TABLE)
-- =============================================================================

-- Add column: employee performance rating (added after initial design)
ALTER TABLE employees
    ADD COLUMN performance_rating TINYINT NOT NULL DEFAULT 3
        COMMENT 'Rating 1-5; 1=Poor, 5=Excellent';

-- Add named CHECK constraint on performance_rating (Lab 2 – modifying constraints)
ALTER TABLE employees
    ADD CONSTRAINT chk_perf_rating CHECK (performance_rating BETWEEN 1 AND 5);

-- Add column to employee_salaries: last_revised_date
ALTER TABLE employee_salaries
    ADD COLUMN last_revised_date DATE NOT NULL DEFAULT (CURRENT_DATE);

-- Modify column: widen designation_name to support long titles (Lab 1 – MODIFY)
ALTER TABLE designations
    MODIFY designation_name VARCHAR(300) NOT NULL;


-- =============================================================================
-- SECTION 3 :  STATIC / SEED DATA  (Lab 1 – DML INSERT)
-- =============================================================================

-- ── Leave Types ──────────────────────────────────────────────────────────────
INSERT INTO leave_types (leave_type_name, leave_description) VALUES
    ('Annual',   'Paid annual leave accrued per year'),
    ('Casual',   'Short-notice casual leave'),
    ('Optional', 'Optional/festive leave chosen by employee');

-- ── User Roles ────────────────────────────────────────────────────────────────
INSERT INTO user_roles (role_name) VALUES
    ('Admin'),
    ('Employee'),
    ('HR Manager');

-- ── Departments ───────────────────────────────────────────────────────────────
INSERT INTO departments (dept_name) VALUES
    ('Human Resources'),
    ('Software Engineering'),
    ('Finance'),
    ('Marketing');

-- ── Designations ──────────────────────────────────────────────────────────────
INSERT INTO designations (designation_name) VALUES
    ('HR Manager'),                  -- 1
    ('Senior Software Engineer'),    -- 2
    ('Junior Software Engineer'),    -- 3
    ('Finance Analyst'),             -- 4
    ('Marketing Specialist'),        -- 5
    ('HR Assistant'),                -- 6
    ('Finance Manager'),             -- 7
    ('Senior Marketing Lead'),       -- 8
    ('DevOps Engineer'),             -- 9
    ('QA Tester');                   -- 10

-- ── Salary Structures ─────────────────────────────────────────────────────────
INSERT INTO salary_structures (travel_allowance, food_allowance, bonus) VALUES
    (5000.00, 3000.00, 15000.00),   -- structure_id 1: Senior
    (3000.00, 2000.00,  8000.00),   -- structure_id 2: Junior
    (4500.00, 2500.00, 12000.00);   -- structure_id 3: Mid-level


-- ─────────────────────────────────────────────────────────────────────────────
-- 10 Sample Employees  (INSERT + multi-table population)
-- ─────────────────────────────────────────────────────────────────────────────

-- Employee 1: HR Manager (Admin)
INSERT INTO employees (nic,fname,lname,dob,gender,date_of_joining,current_dept_id,current_designation_id)
VALUES ('199056781234','Aarav','Mehta','1985-04-15','Male','2018-06-01',1,1);
INSERT INTO employee_addresses (empid,address_line1,address_line2,city,postal_code)
VALUES (1,'45, Park Street','Apartment 3B','Mumbai','400001');
INSERT INTO employee_contact_numbers (empid,contact_type,contact_number)
VALUES (1,'home','02267890123'),(1,'mobile','9876543210');
INSERT INTO employee_salaries (empid,structure_id,base_salary,epf,tax,paye)
VALUES (1,1,85000,7650.00,17000.00,10200.00);
INSERT INTO employee_leave_balances (empid,leave_type_id,available_leaves)
VALUES (1,1,22),(1,2,10),(1,3,5);
INSERT INTO system_users (empid,username,password_hash)
VALUES (1,'aarav.hr','admin123');
INSERT INTO user_role_assignments (user_id,role_id) VALUES (1,1);

-- Employee 2: Senior Software Engineer
INSERT INTO employees (nic,fname,lname,dob,gender,date_of_joining,current_dept_id,current_designation_id)
VALUES ('199212345678','Sneha','Patel','1990-09-22','Female','2019-03-10',2,2);
INSERT INTO employee_addresses (empid,address_line1,address_line2,city,postal_code)
VALUES (2,'12, Marine Drive','Tower 2','Bangalore','560001');
INSERT INTO employee_contact_numbers (empid,contact_type,contact_number)
VALUES (2,'home','08023456789'),(2,'mobile','9812345670');
INSERT INTO employee_salaries (empid,structure_id,base_salary,epf,tax,paye)
VALUES (2,1,95000,8550.00,19000.00,11400.00);
INSERT INTO employee_leave_balances (empid,leave_type_id,available_leaves)
VALUES (2,1,22),(2,2,10),(2,3,5);
INSERT INTO system_users (empid,username,password_hash)
VALUES (2,'sneha.se','sneha123');
INSERT INTO user_role_assignments (user_id,role_id) VALUES (2,2);

-- Employee 3: Junior Software Engineer
INSERT INTO employees (nic,fname,lname,dob,gender,date_of_joining,current_dept_id,current_designation_id)
VALUES ('199578901234','Rahul','Verma','1993-11-05','Male','2021-01-15',2,3);
INSERT INTO employee_addresses (empid,address_line1,address_line2,city,postal_code)
VALUES (3,'78, MG Road','PG Hostel','Pune','411001');
INSERT INTO employee_contact_numbers (empid,contact_type,contact_number)
VALUES (3,'mobile','9765432109');
INSERT INTO employee_salaries (empid,structure_id,base_salary,epf,tax,paye)
VALUES (3,2,45000,4050.00,6750.00,4050.00);
INSERT INTO employee_leave_balances (empid,leave_type_id,available_leaves)
VALUES (3,1,15),(3,2,7),(3,3,3);
INSERT INTO system_users (empid,username,password_hash)
VALUES (3,'rahul.jse','rahul123');
INSERT INTO user_role_assignments (user_id,role_id) VALUES (3,2);

-- Employee 4: Finance Analyst
INSERT INTO employees (nic,fname,lname,dob,gender,date_of_joining,current_dept_id,current_designation_id)
VALUES ('198834567890','Priya','Sharma','1987-02-18','Female','2020-08-01',3,4);
INSERT INTO employee_addresses (empid,address_line1,address_line2,city,postal_code)
VALUES (4,'23, Rajpath','Flat 4C','Delhi','110001');
INSERT INTO employee_contact_numbers (empid,contact_type,contact_number)
VALUES (4,'home','01145678901'),(4,'mobile','9890123456');
INSERT INTO employee_salaries (empid,structure_id,base_salary,epf,tax,paye)
VALUES (4,3,60000,5400.00,10000.00,6000.00);
INSERT INTO employee_leave_balances (empid,leave_type_id,available_leaves)
VALUES (4,1,20),(4,2,8),(4,3,4);
INSERT INTO system_users (empid,username,password_hash)
VALUES (4,'priya.finance','priya123');
INSERT INTO user_role_assignments (user_id,role_id) VALUES (4,2);

-- Employee 5: Marketing Specialist
INSERT INTO employees (nic,fname,lname,dob,gender,date_of_joining,current_dept_id,current_designation_id)
VALUES ('199123456789','Arjun','Singh','1992-07-30','Male','2021-05-01',4,5);
INSERT INTO employee_addresses (empid,address_line1,address_line2,city,postal_code)
VALUES (5,'19, Brigade Road','Studio 2','Hyderabad','500001');
INSERT INTO employee_contact_numbers (empid,contact_type,contact_number)
VALUES (5,'mobile','9823456781');
INSERT INTO employee_salaries (empid,structure_id,base_salary,epf,tax,paye)
VALUES (5,2,42000,3780.00,6300.00,3780.00);
INSERT INTO employee_leave_balances (empid,leave_type_id,available_leaves)
VALUES (5,1,15),(5,2,7),(5,3,3);
INSERT INTO system_users (empid,username,password_hash)
VALUES (5,'arjun.marketing','arjun123');
INSERT INTO user_role_assignments (user_id,role_id) VALUES (5,2);

-- Employee 6: HR Assistant
INSERT INTO employees (nic,fname,lname,dob,gender,date_of_joining,current_dept_id,current_designation_id)
VALUES ('199645678901','Anjali','Rao','1994-12-09','Female','2022-02-01',1,6);
INSERT INTO employee_addresses (empid,address_line1,address_line2,city,postal_code)
VALUES (6,'56, Church Street','Shared Apartment','Chennai','600001');
INSERT INTO employee_contact_numbers (empid,contact_type,contact_number)
VALUES (6,'mobile','9789012345');
INSERT INTO employee_salaries (empid,structure_id,base_salary,epf,tax,paye)
VALUES (6,2,32000,2880.00,4800.00,2880.00);
INSERT INTO employee_leave_balances (empid,leave_type_id,available_leaves)
VALUES (6,1,12),(6,2,5),(6,3,2);
INSERT INTO system_users (empid,username,password_hash)
VALUES (6,'anjali.hr','anjali123');
INSERT INTO user_role_assignments (user_id,role_id) VALUES (6,2);

-- Employee 7: Finance Manager
INSERT INTO employees (nic,fname,lname,dob,gender,date_of_joining,current_dept_id,current_designation_id)
VALUES ('198367890123','Vikram','Kumar','1981-06-25','Male','2017-04-01',3,7);
INSERT INTO employee_addresses (empid,address_line1,address_line2,city,postal_code)
VALUES (7,'89, Saket','Penthouse','Delhi','110017');
INSERT INTO employee_contact_numbers (empid,contact_type,contact_number)
VALUES (7,'home','0119876543'),(7,'mobile','9810987654');
INSERT INTO employee_salaries (empid,structure_id,base_salary,epf,tax,paye)
VALUES (7,1,90000,8100.00,18000.00,10800.00);
INSERT INTO employee_leave_balances (empid,leave_type_id,available_leaves)
VALUES (7,1,22),(7,2,10),(7,3,5);
INSERT INTO system_users (empid,username,password_hash)
VALUES (7,'vikram.finance','vikram123');
INSERT INTO user_role_assignments (user_id,role_id) VALUES (7,2);

-- Employee 8: Senior Marketing Lead
INSERT INTO employees (nic,fname,lname,dob,gender,date_of_joining,current_dept_id,current_designation_id)
VALUES ('198590123456','Deepa','Nair','1983-10-11','Female','2019-09-01',4,8);
INSERT INTO employee_addresses (empid,address_line1,address_line2,city,postal_code)
VALUES (8,'34, Jubilee Hills','Flat 12','Hyderabad','500033');
INSERT INTO employee_contact_numbers (empid,contact_type,contact_number)
VALUES (8,'home','0402345678'),(8,'mobile','9845678901');
INSERT INTO employee_salaries (empid,structure_id,base_salary,epf,tax,paye)
VALUES (8,1,88000,7920.00,17600.00,10560.00);
INSERT INTO employee_leave_balances (empid,leave_type_id,available_leaves)
VALUES (8,1,20),(8,2,9),(8,3,4);
INSERT INTO system_users (empid,username,password_hash)
VALUES (8,'deepa.marketing','deepa123');
INSERT INTO user_role_assignments (user_id,role_id) VALUES (8,2);

-- Employee 9: DevOps Engineer
INSERT INTO employees (nic,fname,lname,dob,gender,date_of_joining,current_dept_id,current_designation_id)
VALUES ('199023456781','Karan','Jain','1988-08-19','Male','2020-01-10',2,9);
INSERT INTO employee_addresses (empid,address_line1,address_line2,city,postal_code)
VALUES (9,'67, Whitefield','Tower 5','Bangalore','560037');
INSERT INTO employee_contact_numbers (empid,contact_type,contact_number)
VALUES (9,'mobile','9834567890');
INSERT INTO employee_salaries (empid,structure_id,base_salary,epf,tax,paye)
VALUES (9,1,92000,8280.00,18400.00,11040.00);
INSERT INTO employee_leave_balances (empid,leave_type_id,available_leaves)
VALUES (9,1,22),(9,2,10),(9,3,5);
INSERT INTO system_users (empid,username,password_hash)
VALUES (9,'karan.devops','karan123');
INSERT INTO user_role_assignments (user_id,role_id) VALUES (9,2);

-- Employee 10: QA Tester
INSERT INTO employees (nic,fname,lname,dob,gender,date_of_joining,current_dept_id,current_designation_id)
VALUES ('199356789012','Swati','Desai','1991-05-21','Female','2021-06-01',2,10);
INSERT INTO employee_addresses (empid,address_line1,address_line2,city,postal_code)
VALUES (10,'21, Koramangala','PG Flat','Bangalore','560034');
INSERT INTO employee_contact_numbers (empid,contact_type,contact_number)
VALUES (10,'mobile','9745678902');
INSERT INTO employee_salaries (empid,structure_id,base_salary,epf,tax,paye)
VALUES (10,2,48000,4320.00,7200.00,4320.00);
INSERT INTO employee_leave_balances (empid,leave_type_id,available_leaves)
VALUES (10,1,18),(10,2,8),(10,3,3);
INSERT INTO system_users (empid,username,password_hash)
VALUES (10,'swati.qa','swati123');
INSERT INTO user_role_assignments (user_id,role_id) VALUES (10,2);

-- Assign HR Manager role to employee 6 (Anjali) in addition to Employee role
INSERT INTO user_role_assignments (user_id,role_id) VALUES (6,3);


-- =============================================================================
-- SECTION 4 :  VIEWS  (Lab 3 – CREATE VIEW)
-- =============================================================================

-- ── View 1: Full employee profile (used by EmployeeView.java)
CREATE VIEW vw_employee_full AS
SELECT
    e.empid,
    e.nic,
    e.fname,
    e.lname,
    CONCAT(e.fname,' ',e.lname)          AS full_name,
    e.dob,
    TIMESTAMPDIFF(YEAR, e.dob, CURDATE()) AS age,
    e.gender,
    e.date_of_joining,
    e.performance_rating,
    d.dept_name,
    ds.designation_name,
    a.address_line1,
    a.address_line2,
    a.city,
    a.postal_code,
    -- Conditional aggregation to pivot contact rows (Lab 4 concept)
    MAX(CASE WHEN cn.contact_type = 'home'   THEN cn.contact_number END) AS tel_home,
    MAX(CASE WHEN cn.contact_type = 'mobile' THEN cn.contact_number END) AS tel_mobile,
    MAX(CASE WHEN cn.contact_type = 'work'   THEN cn.contact_number END) AS tel_work
FROM employees e
LEFT JOIN departments             d  ON e.current_dept_id          = d.dept_id
LEFT JOIN designations            ds ON e.current_designation_id   = ds.designation_id
LEFT JOIN employee_addresses       a  ON e.empid                   = a.empid
LEFT JOIN employee_contact_numbers cn ON e.empid                   = cn.empid
GROUP BY
    e.empid, e.nic, e.fname, e.lname, e.dob, e.gender,
    e.date_of_joining, e.performance_rating,
    d.dept_name, ds.designation_name,
    a.address_line1, a.address_line2, a.city, a.postal_code;


-- ── View 2: Payslip summary (used by PaySlipView.java & Payroll.java)
CREATE VIEW vw_payslip AS
SELECT
    e.empid,
    CONCAT(e.fname,' ',e.lname)                        AS full_name,
    d.dept_name,
    ds.designation_name,
    es.base_salary,
    ss.travel_allowance,
    ss.food_allowance,
    ss.bonus,
    es.epf,
    es.tax,
    es.paye,
    -- Calculated columns (gross, deductions, net)
    (es.base_salary + ss.travel_allowance + ss.food_allowance + ss.bonus) AS gross_pay,
    (es.epf + es.tax + es.paye)                                           AS total_deductions,
    (es.base_salary + ss.travel_allowance + ss.food_allowance + ss.bonus
     - es.epf - es.tax - es.paye)                                         AS net_pay
FROM employees e
LEFT JOIN departments        d  ON e.current_dept_id        = d.dept_id
LEFT JOIN designations       ds ON e.current_designation_id = ds.designation_id
JOIN  employee_salaries      es ON e.empid                  = es.empid
JOIN  salary_structures      ss ON es.structure_id          = ss.structure_id;


-- ── View 3: Leave summary per employee (used by LeaveView.java)
CREATE VIEW vw_leave_summary AS
SELECT
    e.empid,
    CONCAT(e.fname,' ',e.lname) AS full_name,
    d.dept_name,
    ds.designation_name,
    lt.leave_type_name,
    lb.available_leaves
FROM employees e
LEFT JOIN departments          d  ON e.current_dept_id        = d.dept_id
LEFT JOIN designations         ds ON e.current_designation_id = ds.designation_id
JOIN  employee_leave_balances  lb ON e.empid                  = lb.empid
JOIN  leave_types              lt ON lb.leave_type_id         = lt.leave_type_id
ORDER BY e.empid, lt.leave_type_id;


-- ── View 4: User authentication view (used by User.java)
CREATE VIEW vw_user_auth AS
SELECT
    su.user_id,
    su.empid,
    su.username,
    su.password_hash,
    MAX(CASE WHEN ur.role_name = 'Admin' THEN 1 ELSE 0 END) AS is_admin,
    GROUP_CONCAT(ur.role_name ORDER BY ur.role_name SEPARATOR ', ') AS roles
FROM system_users          su
JOIN user_role_assignments ura ON su.user_id  = ura.user_id
JOIN user_roles            ur  ON ura.role_id = ur.role_id
GROUP BY su.user_id, su.empid, su.username, su.password_hash;


-- ── View 5: Department headcount and average salary  (Lab 3 + Lab 4 – GROUP BY in view)
CREATE VIEW vw_dept_salary_stats AS
SELECT
    d.dept_name,
    COUNT(es.empid)        AS headcount,
    MIN(es.base_salary)    AS min_salary,
    MAX(es.base_salary)    AS max_salary,
    ROUND(AVG(es.base_salary),2) AS avg_salary,
    SUM(es.base_salary)    AS total_salary
FROM departments    d
LEFT JOIN employees e  ON d.dept_id = e.current_dept_id
LEFT JOIN employee_salaries es ON e.empid = es.empid
GROUP BY d.dept_id, d.dept_name;


-- =============================================================================
-- SECTION 5 :  REPRESENTATIVE SQL QUERIES  (Labs 1-5)
--  (Prefixed with comments so they can be executed individually or as a batch)
-- =============================================================================

-- ── Lab 1 / Lab 2 : Basic DML ─────────────────────────────────────────────────

-- Q1: List all employee full names and their department (Lab 1 – SELECT)
-- SELECT full_name, dept_name FROM vw_employee_full ORDER BY full_name;

-- Q2: List all female employees (Lab 1 – WHERE filter)
-- SELECT fname, lname, gender, dept_name FROM vw_employee_full WHERE gender = 'Female';

-- Q3: Display employee's name and their age calculated from DOB (Lab 1 – derived column)
-- SELECT fname, lname, dob,
--        TIMESTAMPDIFF(YEAR, dob, CURDATE()) AS age
-- FROM employees ORDER BY age DESC;

-- ── Lab 2 : Constraint demonstration ─────────────────────────────────────────

-- Q4: Show all named constraints on employees (MySQL information_schema equivalent)
-- SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE
-- FROM information_schema.TABLE_CONSTRAINTS
-- WHERE TABLE_SCHEMA = 'payrolldbs' AND TABLE_NAME = 'employees';

-- ── Lab 3 : Nested Sub-queries & SET operations ────────────────────────────────

-- Q5: Find employees who HAVE a salary record (IN sub-query)
-- SELECT fname, lname FROM employees
-- WHERE empid IN (SELECT empid FROM employee_salaries);

-- Q6: Find employees who do NOT yet have a salary record (NOT IN)
-- SELECT fname, lname FROM employees
-- WHERE empid NOT IN (SELECT empid FROM employee_salaries);

-- Q7: UNION — combine employees from Software Engineering and Finance
-- SELECT fname, lname, 'Software Engineering' AS dept FROM employees
-- WHERE current_dept_id = 2
-- UNION
-- SELECT fname, lname, 'Finance' FROM employees
-- WHERE current_dept_id = 3;

-- Q8: Employees in both HR AND who are also system users (INTERSECT emulation)
-- SELECT fname, lname FROM employees WHERE current_dept_id = 1
-- AND empid IN (SELECT empid FROM system_users);

-- Q9: Employees in Software Engineering but NOT admins (MINUS / EXCEPT emulation)
-- SELECT fname, lname FROM employees WHERE current_dept_id = 2
-- AND empid NOT IN (
--     SELECT su.empid FROM system_users su
--     JOIN user_role_assignments ura ON su.user_id = ura.user_id
--     WHERE ura.role_id = 1
-- );

-- Q10: Correlated sub-query — employees earning more than their dept average
-- SELECT e.fname, e.lname, es.base_salary, d.dept_name
-- FROM employees e
-- JOIN employee_salaries es ON e.empid = es.empid
-- JOIN departments d ON e.current_dept_id = d.dept_id
-- WHERE es.base_salary > (
--     SELECT AVG(es2.base_salary)
--     FROM employee_salaries es2
--     JOIN employees e2 ON es2.empid = e2.empid
--     WHERE e2.current_dept_id = e.current_dept_id
-- );

-- ── Lab 4 : GROUP BY, HAVING, ORDER BY, WITH (CTE) ──────────────────────────

-- Q11: Count employees per department (GROUP BY)
-- SELECT d.dept_name, COUNT(e.empid) AS headcount
-- FROM departments d LEFT JOIN employees e ON d.dept_id = e.current_dept_id
-- GROUP BY d.dept_id, d.dept_name
-- ORDER BY headcount DESC;

-- Q12: Departments where average base_salary > 60000 (HAVING)
-- SELECT d.dept_name, ROUND(AVG(es.base_salary),2) AS avg_sal
-- FROM departments d
-- JOIN employees e ON d.dept_id = e.current_dept_id
-- JOIN employee_salaries es ON e.empid = es.empid
-- GROUP BY d.dept_id, d.dept_name
-- HAVING avg_sal > 60000
-- ORDER BY avg_sal DESC;

-- Q13: WITH clause (CTE) — find department with the highest total salary
-- WITH dept_totals AS (
--     SELECT d.dept_name, SUM(es.base_salary) AS total_sal
--     FROM departments d
--     JOIN employees e ON d.dept_id = e.current_dept_id
--     JOIN employee_salaries es ON e.empid = es.empid
--     GROUP BY d.dept_id, d.dept_name
-- )
-- SELECT dept_name, total_sal
-- FROM dept_totals
-- WHERE total_sal = (SELECT MAX(total_sal) FROM dept_totals);

-- Q14: Derived relation — employees earning above company-wide average
-- SELECT e.fname, e.lname, es.base_salary
-- FROM employees e
-- JOIN employee_salaries es ON e.empid = es.empid
-- JOIN (SELECT AVG(base_salary) AS co_avg FROM employee_salaries) AS co ON 1=1
-- WHERE es.base_salary > co.co_avg
-- ORDER BY es.base_salary DESC;

-- Q15: Multi-join query used directly by Employee.java / buildSelectSQL()
-- SELECT e.empid, e.nic, e.fname, e.lname, e.dob, e.gender, e.date_of_joining,
--        e.current_dept_id, e.current_designation_id,
--        d.dept_name, ds.designation_name,
--        a.address_line1, a.address_line2, a.city, a.postal_code,
--        MAX(CASE WHEN cn.contact_type='home'   THEN cn.contact_number END) AS tel_home,
--        MAX(CASE WHEN cn.contact_type='mobile' THEN cn.contact_number END) AS tel_mobile
-- FROM employees e
-- LEFT JOIN departments             d  ON e.current_dept_id        = d.dept_id
-- LEFT JOIN designations            ds ON e.current_designation_id = ds.designation_id
-- LEFT JOIN employee_addresses       a  ON e.empid = a.empid
-- LEFT JOIN employee_contact_numbers cn ON e.empid = cn.empid
-- GROUP BY e.empid, e.nic, e.fname, e.lname, e.dob, e.gender, e.date_of_joining,
--          e.current_dept_id, e.current_designation_id,
--          d.dept_name, ds.designation_name,
--          a.address_line1, a.address_line2, a.city, a.postal_code;

-- Q16: Search query used by searchEmployees() in Employee.java (LIKE + CAST)
-- SELECT * FROM vw_employee_full
-- WHERE CAST(empid AS CHAR) LIKE '%2%'
--    OR fname   LIKE '%ra%'
--    OR lname   LIKE '%ra%'
--    OR dept_name        LIKE '%eng%'
--    OR designation_name LIKE '%eng%';

-- Q17: INSERT ... SELECT — seed leave balances for a new employee  (Lab 4 concept)
-- INSERT IGNORE INTO employee_leave_balances (empid, leave_type_id, available_leaves)
-- SELECT 1, leave_type_id,
--        CASE leave_type_name
--            WHEN 'Annual'   THEN 15
--            WHEN 'Casual'   THEN 10
--            ELSE 5
--        END
-- FROM leave_types;

-- Q18: Multi-table UPDATE with JOIN  (used by Leave.java — updateLeaveBalance)
-- UPDATE employee_leave_balances lb
-- JOIN   leave_types lt ON lb.leave_type_id = lt.leave_type_id
-- SET    lb.available_leaves = 20
-- WHERE  lb.empid = 1 AND lt.leave_type_name = 'Annual';

-- Q19: Authentication query  (used by User.java — authenticate())
-- SELECT su.user_id,
--        MAX(CASE WHEN ur.role_name='Admin' THEN 1 ELSE 0 END) AS is_admin
-- FROM system_users su
-- JOIN user_role_assignments ura ON su.user_id  = ura.user_id
-- JOIN user_roles            ur  ON ura.role_id = ur.role_id
-- WHERE su.username = 'aarav.hr'
--   AND (su.password_hash = 'admin123' OR su.password_hash = 'admin123')
-- GROUP BY su.user_id;

-- Q20: Built-in string functions (Lab 2 – built-in functions)
-- SELECT empid,
--        UPPER(CONCAT(fname,' ',lname))       AS name_upper,
--        LOWER(fname)                         AS fname_lower,
--        LENGTH(fname)                        AS fname_length,
--        SUBSTRING(lname,1,3)                 AS lname_prefix,
--        DATE_FORMAT(dob,'%d-%b-%Y')          AS dob_formatted,
--        DATE_FORMAT(date_of_joining,'%d-%m-%Y') AS joining_formatted
-- FROM employees ORDER BY empid;

-- Q21: Date arithmetic — years of service
-- SELECT fname, lname,
--        TIMESTAMPDIFF(YEAR, date_of_joining, CURDATE()) AS years_of_service
-- FROM employees ORDER BY years_of_service DESC;

-- Q22: ROUND used on salary calculations (Lab 2 – ROUND function)
-- SELECT full_name, gross_pay, total_deductions,
--        ROUND(net_pay, 2) AS net_pay
-- FROM vw_payslip ORDER BY net_pay DESC;


-- =============================================================================
-- SECTION 6 :  TRANSACTION CONTROL  (Lab 4 – COMMIT / ROLLBACK / SAVEPOINT)
-- =============================================================================

-- Demonstrate a safe multi-step salary update with SAVEPOINT and ROLLBACK
-- (Equivalent to setAutoCommit(false) / commit() / rollback() in Java)

START TRANSACTION;

    SAVEPOINT before_salary_update;

    -- Increase senior employees' base salary by 5 %
    UPDATE employee_salaries
    SET    base_salary = ROUND(base_salary * 1.05)
    WHERE  structure_id = 1;   -- Senior structure

    SAVEPOINT after_senior_raise;

    -- Also increase EPF proportionally
    UPDATE employee_salaries
    SET    epf = ROUND(base_salary * 0.09, 2)
    WHERE  structure_id = 1;

    -- Verify (would be a SELECT in practice); commit both updates
    COMMIT;

-- Second transaction: demonstrate ROLLBACK to savepoint
START TRANSACTION;

    SAVEPOINT before_test_insert;

    -- Test insert that we want to undo
    INSERT INTO employee_leave_balances (empid, leave_type_id, available_leaves)
    VALUES (1, 1, 999);   -- invalid high value — roll it back

    ROLLBACK TO before_test_insert;

    -- Restore correct value
    UPDATE employee_leave_balances
    SET    available_leaves = 22
    WHERE  empid = 1 AND leave_type_id = 1;

COMMIT;


-- =============================================================================
-- SECTION 7 :  STORED PROCEDURES  (Lab 9 – Procedures, IN/OUT/INOUT params)
-- =============================================================================

DELIMITER $$

-- ── Procedure 1: Get full employee profile  (IN parameter)
CREATE PROCEDURE sp_get_employee(IN p_empid INT)
BEGIN
    SELECT * FROM vw_employee_full WHERE empid = p_empid;
END$$


-- ── Procedure 2: Insert a new employee atomically  (IN params + transaction)
--   Mirrors the logic of Employee.java insertEmployee()
CREATE PROCEDURE sp_add_employee(
    IN p_nic          VARCHAR(35),
    IN p_fname        VARCHAR(30),
    IN p_lname        VARCHAR(30),
    IN p_dob          DATE,
    IN p_gender       VARCHAR(10),
    IN p_joining      DATE,
    IN p_dept_id      INT,
    IN p_desig_id     INT,
    IN p_addr1        TEXT,
    IN p_city         VARCHAR(30),
    IN p_mobile       VARCHAR(15),
    IN p_base_salary  INT,
    IN p_structure_id INT,
    OUT p_new_empid   INT,
    OUT p_message     VARCHAR(255)
)
BEGIN
    DECLARE v_epf  DOUBLE DEFAULT 0;
    DECLARE v_tax  DOUBLE DEFAULT 0;
    DECLARE v_paye DOUBLE DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_message = 'Error: transaction rolled back.';
        SET p_new_empid = -1;
    END;

    SET v_epf  = ROUND(p_base_salary * 0.09, 2);
    SET v_tax  = ROUND(p_base_salary * 0.20, 2);
    SET v_paye = ROUND(p_base_salary * 0.12, 2);

    START TRANSACTION;

        INSERT INTO employees (nic,fname,lname,dob,gender,date_of_joining,current_dept_id,current_designation_id)
        VALUES (p_nic,p_fname,p_lname,p_dob,p_gender,p_joining,p_dept_id,p_desig_id);

        SET p_new_empid = LAST_INSERT_ID();

        INSERT INTO employee_addresses (empid,address_line1,city)
        VALUES (p_new_empid, p_addr1, p_city);

        INSERT INTO employee_contact_numbers (empid,contact_type,contact_number)
        VALUES (p_new_empid,'mobile',p_mobile);

        INSERT INTO employee_salaries (empid,structure_id,base_salary,epf,tax,paye)
        VALUES (p_new_empid,p_structure_id,p_base_salary,v_epf,v_tax,v_paye);

        -- Seed leave balances (INSERT ... SELECT pattern — Lab 3/4)
        INSERT IGNORE INTO employee_leave_balances (empid,leave_type_id,available_leaves)
        SELECT p_new_empid, leave_type_id,
               CASE leave_type_name WHEN 'Annual' THEN 15 WHEN 'Casual' THEN 10 ELSE 5 END
        FROM leave_types;

    COMMIT;

    SET p_message = CONCAT('Employee added successfully with ID: ', p_new_empid);
END$$


-- ── Procedure 3: Get department salary statistics  (IN + OUT parameters)
CREATE PROCEDURE sp_dept_salary_stats(
    IN  p_dept_name VARCHAR(250),
    OUT p_headcount INT,
    OUT p_avg_sal   DOUBLE,
    OUT p_max_sal   INT,
    OUT p_min_sal   INT
)
BEGIN
    SELECT COUNT(es.empid),
           ROUND(AVG(es.base_salary),2),
           MAX(es.base_salary),
           MIN(es.base_salary)
    INTO   p_headcount, p_avg_sal, p_max_sal, p_min_sal
    FROM   departments d
    JOIN   employees e   ON d.dept_id = e.current_dept_id
    JOIN   employee_salaries es ON e.empid = es.empid
    WHERE  d.dept_name = p_dept_name;
END$$


-- ── Procedure 4: Apply salary hike (IN + INOUT)
--   INOUT demonstrates the parameter mode table from Lab 9
CREATE PROCEDURE sp_apply_salary_hike(
    IN    p_empid      INT,
    IN    p_hike_pct   DOUBLE,         -- e.g. 10 for 10%
    INOUT p_old_salary INT             -- passed in as current; returned as new
)
BEGIN
    DECLARE v_new_salary INT;
    SET v_new_salary = ROUND(p_old_salary * (1 + p_hike_pct / 100));

    UPDATE employee_salaries
    SET    base_salary       = v_new_salary,
           last_revised_date = CURDATE()
    WHERE  empid = p_empid;

    SET p_old_salary = v_new_salary;   -- INOUT: return updated value
END$$


-- ── Procedure 5: List employees of a department (used in Lab 9 demo)
CREATE PROCEDURE sp_list_dept_employees(IN p_dept_name VARCHAR(250))
BEGIN
    SELECT e.empid,
           CONCAT(e.fname,' ',e.lname) AS full_name,
           ds.designation_name,
           es.base_salary
    FROM   employees e
    JOIN   departments d       ON e.current_dept_id        = d.dept_id
    JOIN   designations ds     ON e.current_designation_id = ds.designation_id
    JOIN   employee_salaries es ON e.empid = es.empid
    WHERE  d.dept_name = p_dept_name
    ORDER  BY es.base_salary DESC;
END$$


-- =============================================================================
-- SECTION 8 :  STORED FUNCTIONS  (Lab 9 – Functions)
-- =============================================================================

-- ── Function 1: Calculate net pay for a given employee
CREATE FUNCTION fn_get_net_pay(p_empid INT)
RETURNS DOUBLE
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE v_net DOUBLE DEFAULT 0;
    SELECT (es.base_salary + ss.travel_allowance + ss.food_allowance + ss.bonus
            - es.epf - es.tax - es.paye)
    INTO   v_net
    FROM   employee_salaries es
    JOIN   salary_structures ss ON es.structure_id = ss.structure_id
    WHERE  es.empid = p_empid;
    RETURN IFNULL(v_net, 0);
END$$


-- ── Function 2: Count employees in a department  (mirrors Lab 9 dept_count example)
CREATE FUNCTION fn_dept_emp_count(p_dept_name VARCHAR(250))
RETURNS INT
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE v_count INT DEFAULT 0;
    SELECT COUNT(*)
    INTO   v_count
    FROM   employees e
    JOIN   departments d ON e.current_dept_id = d.dept_id
    WHERE  d.dept_name = p_dept_name;
    RETURN v_count;
END$$


-- ── Function 3: Calculate years of service
CREATE FUNCTION fn_years_of_service(p_empid INT)
RETURNS INT
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE v_years INT DEFAULT 0;
    SELECT TIMESTAMPDIFF(YEAR, date_of_joining, CURDATE())
    INTO   v_years
    FROM   employees WHERE empid = p_empid;
    RETURN IFNULL(v_years, 0);
END$$


-- ── Function 4: Get available leave for a given type (used conceptually in LeaveView)
CREATE FUNCTION fn_get_leave_balance(p_empid INT, p_leave_type VARCHAR(50))
RETURNS INT
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE v_bal INT DEFAULT 0;
    SELECT lb.available_leaves
    INTO   v_bal
    FROM   employee_leave_balances lb
    JOIN   leave_types lt ON lb.leave_type_id = lt.leave_type_id
    WHERE  lb.empid = p_empid AND lt.leave_type_name = p_leave_type;
    RETURN IFNULL(v_bal, 0);
END$$


-- =============================================================================
-- SECTION 9 :  TRIGGERS  (Lab 10 – Row triggers, audit system)
-- =============================================================================

-- ── Trigger 1: BEFORE INSERT on employees — enforce NIC format (row trigger)
CREATE TRIGGER trg_emp_before_insert
BEFORE INSERT ON employees
FOR EACH ROW
BEGIN
    -- Enforce: NIC must be at least 10 characters
    IF LENGTH(TRIM(NEW.nic)) < 10 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'NIC must be at least 10 characters long.';
    END IF;
    -- Enforce: fname and lname must contain only alphabets and spaces
    IF NEW.fname REGEXP '[^a-zA-Z ]' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'First name must contain only alphabetic characters.';
    END IF;
END$$


-- ── Trigger 2: AFTER INSERT on employee_salaries — write audit log
CREATE TRIGGER trg_salary_after_insert
AFTER INSERT ON employee_salaries
FOR EACH ROW
BEGIN
    INSERT INTO payroll_audit_log (empid, action_type, old_base_salary, new_base_salary)
    VALUES (NEW.empid, 'INSERT', NULL, NEW.base_salary);
END$$


-- ── Trigger 3: AFTER UPDATE on employee_salaries — record salary change in audit log
--   Mirrors Lab 10: "record into Old_Data table when salary is updated"
CREATE TRIGGER trg_salary_after_update
AFTER UPDATE ON employee_salaries
FOR EACH ROW
BEGIN
    IF OLD.base_salary <> NEW.base_salary THEN
        INSERT INTO payroll_audit_log (empid, action_type, old_base_salary, new_base_salary)
        VALUES (NEW.empid, 'UPDATE', OLD.base_salary, NEW.base_salary);
    END IF;
END$$


-- ── Trigger 4: BEFORE DELETE on employees — prevent deletion of Admin users
CREATE TRIGGER trg_emp_before_delete
BEFORE DELETE ON employees
FOR EACH ROW
BEGIN
    DECLARE v_is_admin INT DEFAULT 0;
    SELECT COUNT(*)
    INTO   v_is_admin
    FROM   system_users su
    JOIN   user_role_assignments ura ON su.user_id  = ura.user_id
    JOIN   user_roles            ur  ON ura.role_id = ur.role_id
    WHERE  su.empid = OLD.empid AND ur.role_name = 'Admin';

    IF v_is_admin > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot delete an Admin user employee.';
    END IF;
END$$


-- ── Trigger 5: AFTER INSERT on system_users — automatically assign 'Employee' role
CREATE TRIGGER trg_user_after_insert
AFTER INSERT ON system_users
FOR EACH ROW
BEGIN
    DECLARE v_emp_role_id INT;
    SELECT role_id INTO v_emp_role_id FROM user_roles WHERE role_name = 'Employee';
    -- Only insert if not already assigned
    INSERT IGNORE INTO user_role_assignments (user_id, role_id)
    VALUES (NEW.user_id, v_emp_role_id);
END$$

DELIMITER ;


-- =============================================================================
-- SECTION 10 :  DDL DROP / RENAME (Lab 1 – DROP, RENAME examples)
-- =============================================================================

-- These are illustrative DDL statements commented out to preserve the schema:
-- To rename a table  (Lab 1):
--   RENAME TABLE employee_contact_numbers TO emp_contacts;
--
-- To drop a column  (Lab 1 – ALTER DROP):
--   ALTER TABLE employees DROP COLUMN performance_rating;
--
-- To truncate lookup table before re-seeding  (Lab 1 – TRUNCATE):
--   SET FOREIGN_KEY_CHECKS = 0;
--   TRUNCATE TABLE leave_types;
--   SET FOREIGN_KEY_CHECKS = 1;
--
-- To drop a named constraint  (Lab 2 – DROP CONSTRAINT):
--   ALTER TABLE employees DROP CHECK chk_gender;
--
-- To add a named constraint after table creation  (Lab 2 – ADD CONSTRAINT):
--   ALTER TABLE employees
--       ADD CONSTRAINT chk_nic_len CHECK (LENGTH(nic) >= 10);


-- =============================================================================
-- SECTION 11 :  VERIFICATION QUERIES  (run after import to confirm everything works)
-- =============================================================================

-- 1. Show all tables
-- SHOW TABLES;

-- 2. Confirm row counts
-- SELECT 'employees'              AS tbl, COUNT(*) AS cnt FROM employees
-- UNION ALL SELECT 'system_users',              COUNT(*) FROM system_users
-- UNION ALL SELECT 'employee_salaries',         COUNT(*) FROM employee_salaries
-- UNION ALL SELECT 'employee_leave_balances',   COUNT(*) FROM employee_leave_balances
-- UNION ALL SELECT 'user_role_assignments',     COUNT(*) FROM user_role_assignments;

-- 3. Test the full employee view
-- SELECT * FROM vw_employee_full;

-- 4. Test payslip view
-- SELECT full_name, gross_pay, total_deductions, net_pay FROM vw_payslip;

-- 5. Test fn_get_net_pay function
-- SELECT empid, fname, fn_get_net_pay(empid) AS net_pay FROM employees;

-- 6. Test sp_dept_salary_stats procedure
-- CALL sp_dept_salary_stats('Software Engineering', @hc, @avg, @mx, @mn);
-- SELECT @hc AS headcount, @avg AS avg_salary, @mx AS max_salary, @mn AS min_salary;

-- 7. Show audit log (populated by triggers)
-- SELECT * FROM payroll_audit_log ORDER BY changed_at DESC;

-- =============================================================================
-- END OF SCRIPT
-- =============================================================================
