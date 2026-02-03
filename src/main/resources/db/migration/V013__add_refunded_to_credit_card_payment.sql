-- Add refunded flag to credit_card_payment table
ALTER TABLE credit_card_payment ADD COLUMN refunded BOOLEAN NOT NULL DEFAULT false;
