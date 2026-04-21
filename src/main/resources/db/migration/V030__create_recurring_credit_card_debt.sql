-- Migration: V030__create_recurring_credit_card_debt.sql
-- Created on: April 21, 2026

CREATE TABLE IF NOT EXISTS recurring_credit_card_debt (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    crc_id             INTEGER        NOT NULL REFERENCES credit_card (id),
    category_id        INTEGER        NOT NULL REFERENCES category (id),
    amount             NUMERIC(38, 2) NOT NULL,
    description        VARCHAR(255),
    day_of_month       INTEGER        NOT NULL CHECK (day_of_month BETWEEN 1 AND 31),
    frequency          VARCHAR(50)    NOT NULL,
    status             VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    start_date         VARCHAR(255)   NOT NULL,
    end_date           VARCHAR(255)   NOT NULL,
    next_invoice_month VARCHAR(7)     NOT NULL
);
