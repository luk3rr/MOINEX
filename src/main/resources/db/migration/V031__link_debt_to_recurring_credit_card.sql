-- Migration: V031__link_debt_to_recurring_credit_card.sql
-- Created on: April 21, 2026

ALTER TABLE credit_card_debt
    ADD COLUMN recurring_id INTEGER REFERENCES recurring_credit_card_debt (id);