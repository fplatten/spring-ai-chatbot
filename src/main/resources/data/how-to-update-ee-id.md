# How to Update an Employee's ID

This document outlines the multi-step process for a technical support specialist to update an employee's ID in the backend system.

### Step 1: Obtain Approval

Before proceeding, ensure you have written approval from a team lead or manager. Attach the approval email to your ticket.

### Step 2: Update the `employees` Table

Run the following SQL script to update the `employees` table. **WARNING:** This is a critical operation.

```sql
UPDATE employees
SET employee_id = '{{NEW_EE_ID}}'
WHERE company_id = '{{COMPANY_ID}}'
    AND employee_id = '{{OLD_EE_ID}}'
    AND work_agreement_id = '{{WORK_AGREEMENT_ID}}';
```

### Step 3: Update Related Tables

```sql
SET @company_code = "{{COMPANY_ID}}";
SET @old_employee_id = "{{OLD_EE_ID}}";
SET @new_employee_id = "{{NEW_EE_ID}}";
SET @work_agreement_id = "{{WORK_AGREEMENT_ID}}";

-- Update payroll records
UPDATE payroll
SET employee_id = @new_employee_id
WHERE company_id = @company_code
  AND employee_id = @old_employee_id
  AND work_agreement_id = @work_agreement_id;

-- Update benefits records
UPDATE benefits
SET employee_id = @new_employee_id
WHERE company_id = @company_code
  AND employee_id = @old_employee_id
  AND work_agreement_id = @work_agreement_id;
```

### Step 4: System Re-sync
After updating the database, run the daily re-synchronization script.

```
/usr/local/bin/resync-hcm.sh --id {{NEW_EE_ID}}
```

### Step 5: Final Verification
Verify the change in the front-end application and close the ticket.


