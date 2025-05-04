-- bond definition

CREATE TABLE bond (id integer, average_unit_value numeric(38,2) not null, average_unit_value_count numeric(38,2) not null, current_quantity numeric(38,2) not null, current_unit_value numeric(38,2) not null, name varchar(255) not null, symbol varchar(255) not null unique, maturity_date varchar(255), archived boolean default false not null, interest_index tinyint not null check (interest_index between 0 and 5), interest_rate numeric(38,2) not null, interest_type tinyint not null check (interest_type between 0 and 2), type varchar(255) not null check (type in ('CDB','LCI','LCA','TREASURY_PREFIXED','TREASURY_POSTFIXED','INTERNATIONAL','OTHER')), primary key (id));


-- bond_purchase definition

CREATE TABLE bond_purchase (id integer, quantity numeric(20,8) not null, unit_price numeric(38,2) not null, wallet_transaction_id bigint not null, bond_id bigint, primary key (id));


-- bond_sale definition

CREATE TABLE bond_sale (id integer, quantity numeric(20,8) not null, unit_price numeric(38,2) not null, wallet_transaction_id bigint not null, bond_id bigint, primary key (id));


-- brazilian_market_indicators definition

CREATE TABLE brazilian_market_indicators (id integer, ipca_12_months numeric(38,2), ipca_last_month_rate numeric(38,2), ipca_last_month_reference varchar(255), last_update varchar(255), selic_target numeric(38,2), primary key (id));


-- calendar_event definition

CREATE TABLE calendar_event (id integer, date varchar(255) not null, description varchar(255), event_type varchar(255) not null check (event_type in ('CREDIT_CARD_STATEMENT_CLOSING','CREDIT_CARD_DUE_DATE','DEBT_PAYMENT_DUE_DATE','INCOME_RECEIPT_DATE')), title varchar(255) not null, primary key (id));


-- category definition

CREATE TABLE category (id integer, archived boolean not null, name varchar(50) not null, primary key (id));


-- credit_card definition

CREATE TABLE credit_card (id integer, archived boolean default false not null, billing_due_day integer not null, closing_day integer not null, last_four_digits varchar(4), max_debt numeric(38,2) not null, name varchar(50) not null unique, default_billing_wallet_id bigint, operator_id bigint, primary key (id));


-- credit_card_debt definition

CREATE TABLE credit_card_debt (id integer, date varchar(255) not null, description varchar(255), installments integer not null, total_amount numeric(38,2) not null, category_id bigint not null, crc_id bigint not null, primary key (id));


-- credit_card_operator definition

CREATE TABLE credit_card_operator (id integer, icon varchar(30), name varchar(50) not null unique, primary key (id));


-- credit_card_payment definition

CREATE TABLE credit_card_payment (id integer, amount numeric(38,2) not null, date varchar(255) not null, installment integer not null, debt_id bigint not null, wallet_id bigint, primary key (id));


-- crypto_exchange definition

CREATE TABLE crypto_exchange (id integer, date varchar(255) not null, description varchar(255), received_quantity numeric(38,2) not null, sold_quantity numeric(38,2) not null, received_crypto_id bigint not null, sold_crypto_id bigint not null, primary key (id));


-- dividend definition

CREATE TABLE dividend (id integer, ticker_id bigint not null, wallet_transaction_id bigint not null, primary key (id));


-- goal definition

CREATE TABLE goal (completion_date varchar(255), initial_balance numeric(38,2) not null, motivation varchar(500), target_balance numeric(38,2) not null, target_date varchar(255) not null, wallet_id bigint not null, primary key (wallet_id));


-- HT_wallet definition

CREATE TABLE HT_wallet(id bigint not null, hib_sess_id varchar(36) not null, primary key (id, hib_sess_id));

-- market_quotes_and_commodities definition

CREATE TABLE market_quotes_and_commodities (id integer, bitcoin numeric(38,2), coffee numeric(38,2), dollar numeric(38,2), ethereum numeric(38,2), euro numeric(38,2), gold numeric(38,2), ibovespa numeric(38,2), last_update timestamp, oil_brent numeric(38,2), soybean numeric(38,2), wheat numeric(38,2), primary key (id));


-- purchase definition

CREATE TABLE purchase (id integer, quantity numeric(20,8) not null, unit_price numeric(38,2) not null, ticker_id bigint, wallet_transaction_id bigint not null, primary key (id));


-- recurring_transaction definition

CREATE TABLE recurring_transaction (id integer, amount numeric(38,2) not null, description varchar(255), type varchar(255) not null check (type in ('INCOME','EXPENSE')), end_date varchar(255) not null, frequency varchar(255) not null check (frequency in ('DAILY','WEEKLY','MONTHLY','YEARLY')), next_due_date varchar(255) not null, start_date varchar(255) not null, status varchar(255) default 'ACTIVE' not null check (status in ('ACTIVE','INACTIVE')), category_id bigint not null, wallet_id bigint not null, primary key (id));


-- sale definition

CREATE TABLE sale (id integer, quantity numeric(20,8) not null, unit_price numeric(38,2) not null, average_cost numeric(38,2) not null, ticker_id bigint, wallet_transaction_id bigint not null, primary key (id));


-- ticker definition

CREATE TABLE ticker (id integer, average_unit_value numeric(38,2) not null, average_unit_value_count numeric(38,2) not null, current_quantity numeric(38,2) not null, current_unit_value numeric(38,2) not null, name varchar(255) not null, symbol varchar(255) not null unique, archived boolean default false not null, last_update varchar(255) not null, type varchar(255) not null check (type in ('STOCK','FUND','CRYPTOCURRENCY')), primary key (id));


-- ticker_purchase definition

CREATE TABLE ticker_purchase (id integer, quantity numeric(20,8) not null, unit_price numeric(38,2) not null, wallet_transaction_id bigint not null, ticker_id bigint, primary key (id));


-- ticker_sale definition

CREATE TABLE ticker_sale (id integer, quantity numeric(20,8) not null, unit_price numeric(38,2) not null, average_cost numeric(38,2) not null, wallet_transaction_id bigint not null, ticker_id bigint, primary key (id));


-- transfer definition

CREATE TABLE transfer (id integer, amount numeric(38,2) not null, date varchar(255) not null, description varchar(255), receiver_wallet_id bigint not null, sender_wallet_id bigint not null, primary key (id));


-- wallet definition

CREATE TABLE wallet (id integer, archived boolean default false not null, balance numeric(38,2) not null, name varchar(50) not null unique, type_id bigint, primary key (id));


-- wallet_transaction definition

CREATE TABLE wallet_transaction (id integer, amount numeric(38,2) not null, description varchar(255), type varchar(255) not null check (type in ('INCOME','EXPENSE')), date varchar(255) not null, status varchar(255) not null check (status in ('PENDING','CONFIRMED')), category_id bigint not null, wallet_id bigint not null, primary key (id));


-- wallet_type definition

CREATE TABLE wallet_type (id integer, icon varchar(30), name varchar(50) not null unique, primary key (id));
