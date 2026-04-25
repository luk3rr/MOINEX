ALTER TABLE credit_card_payment
    ADD COLUMN paid_amount NUMERIC(38, 2) NOT NULL DEFAULT 0.00;

-- Installments already paid must have paid_amount = amount to preserve semantic consistency.
-- Pending installments (wallet_id IS NULL) keep 0, which is correct.
UPDATE credit_card_payment
    SET paid_amount = amount
    WHERE wallet_id IS NOT NULL;
