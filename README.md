# PayrollFX — Payroll Management System
## Updated for `payrolldbs` Schema

### Schema Changes (Old → New)
| Old Table | New Table(s) |
|---|---|
| `employee` | `employees` (renamed + columns renamed) |
| `department` | `departments` |
| `designation` | `designations` |
| `employee_contact` (tel_home, tel_mobile cols) | `employee_contact_numbers` (typed rows: home/mobile/work) |
| `employee.address`, `employee.city` | `employee_addresses` (address_line1, address_line2, city, postal_code) |
| `leave_balance` (annual/casual/optional cols) | `employee_leave_balances` + `leave_types` (normalised rows) |
| `employee_salary` | `employee_salaries` |
| `salary_structure` (travel_allow/food_allow) | `salary_structures` (travel_allowance/food_allowance) |
| `users` (with `admin` boolean) | `system_users` + `user_roles` + `user_role_assignments` |

### Java Changes Per File
**model/DbConnection.java** — DB name changed to `payrolldbs`, port default 3306.

**model/Person.java** — `address`/`city` split into `addressLine1`, `addressLine2`, `postalCode`, `city`. Setter names updated (`setTelHome`, `setTelMobile`).

**model/Employee.java** — All SQL queries updated to new table/column names. INSERT writes to `employees` + `employee_addresses` + `employee_contact_numbers`. DELETE uses CASCADE (single delete on `employees`). SELECT uses GROUP BY + MAX(CASE) to pivot contact rows back to home/mobile. Leave seeded via `employee_leave_balances` JOIN `leave_types`.

**model/User.java** — Auth query joins `system_users` → `user_role_assignments` → `user_roles`. Admin determined by `role_name='Admin'` (not a boolean column). Supports plain-text password for dev; add jBCrypt for production.

**model/Leave.java** — Replaced 3 fixed double fields (annual/casual/optional) with a `Map<String,Integer>` keyed by `leave_type_name`. Dynamically loads all leave types from DB. `applyLeave()` now takes a deductions map.

**model/Payroll.java** — Column names: `travel_allowance`, `food_allowance` (not travel_allow/food_allow). Table: `employee_salaries` (not employee_salary), `salary_structures` (not salary_structure).

**model/PaySlip.java** — Same column name fixes as Payroll.

**view/EmployeeView.java** — Form fields added: `tfAddrLine1`, `tfAddrLine2`, `tfPostalCode`. Table column for City added.

**view/LeaveView.java** — Leave balance/apply cards are now dynamically built after searching an employee (not hard-coded to Annual/Casual/Optional). Works for any leave types defined in DB.

**view/LoginView.java** — Dev hint updated with correct seed usernames.

**view/HomeView.java** — Dashboard shows schema name + table list. Role badge shows "Admin"/"Employee".

### Setup
```bash
# 1. Import new schema
mysql -u root -p < payrolldbs.sql

# 2. Configure connection
edit config/db.properties  # set db.user, db.password, db.port

# 3. For dev login (plain-text passwords), run:
UPDATE system_users SET password_hash = 'yourpassword' WHERE username = 'aarav.hr';

# 4. Build & run
mvn javafx:run
```

### Default Seed Credentials
All 10 employees have usernames in format `firstname.role` (e.g. `aarav.hr`, `sneha.se`).
Passwords are bcrypt-hashed in seed data — update to plain-text for dev testing.
Only `aarav.hr` has Admin role. All others have Employee role.
