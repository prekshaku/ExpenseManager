-- Step 1: Drop and recreate clean
DROP DATABASE IF EXISTS expense_manager;
-- Step 1: Create DB safely
CREATE DATABASE IF NOT EXISTS expense_manager;
USE expense_manager;

-- Step 2: Create table
CREATE TABLE IF NOT EXISTS expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50),
    amount DOUBLE,
    description VARCHAR(255)
);

-- Step 3: Alter table
ALTER TABLE expenses
  DROP COLUMN description,
  ADD COLUMN expense_date DATE NOT NULL DEFAULT '2024-01-01';
  
  SET SQL_SAFE_UPDATES = 0;

-- Step 4: Set all existing rows to today
UPDATE expenses SET expense_date = CURDATE();

-- Step 5: Spread dates for graph testing
UPDATE expenses 
SET expense_date = DATE_SUB(CURDATE(), INTERVAL (id * 2) DAY);

-- Step 6: Verify
SELECT expense_date, SUM(amount) AS total
FROM expenses
GROUP BY expense_date
ORDER BY expense_date ASC;