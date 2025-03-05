ALTER TABLE credit_card_debt RENAME COLUMN total_amount TO amount;
ALTER TABLE credit_card ADD COLUMN available_rebate NUMERIC(38,2) NOT NULL DEFAULT 0.00;
