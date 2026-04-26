CREATE TABLE fire_calculator_settings
(
    id                   INTEGER        PRIMARY KEY,
    current_net_worth    NUMERIC(38, 2) NOT NULL,
    monthly_contribution NUMERIC(38, 2) NOT NULL,
    annual_return_rate   NUMERIC(10, 4) NOT NULL,
    monthly_expense      NUMERIC(38, 2) NOT NULL,
    withdrawal_rate      NUMERIC(10, 4) NOT NULL DEFAULT 4.0,
    current_age          INTEGER        NOT NULL
);
