-- Add calculated_until_date column to track until which date the interest was calculated
ALTER TABLE bond_interest_calculation 
ADD COLUMN calculated_until_date VARCHAR(10);

-- Initialize with calculation_date for existing records
UPDATE bond_interest_calculation 
SET calculated_until_date = calculation_date 
WHERE calculated_until_date IS NULL;
