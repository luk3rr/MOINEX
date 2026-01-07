-- =================================================================
-- Add include_in_analysis flag to wallet_transaction table
-- =================================================================
-- This flag allows users to exclude specific transactions from
-- income/expense/savings analysis while keeping them in the system
-- for balance tracking (e.g., asset purchases, loan repayments)
-- =================================================================

ALTER TABLE wallet_transaction 
ADD COLUMN include_in_analysis BOOLEAN NOT NULL DEFAULT TRUE;

-- =================================================================
-- Add include_in_analysis flag to recurring_transaction table
-- =================================================================

ALTER TABLE recurring_transaction 
ADD COLUMN include_in_analysis BOOLEAN NOT NULL DEFAULT TRUE;
