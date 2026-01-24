-- Add include_in_net_worth flag to recurring_transaction table
ALTER TABLE recurring_transaction ADD COLUMN include_in_net_worth BOOLEAN NOT NULL DEFAULT false;
