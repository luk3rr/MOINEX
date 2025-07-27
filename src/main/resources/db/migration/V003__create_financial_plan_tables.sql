-- ##############################################################################
-- ## Migration: Cria as tabelas para a funcionalidade de Planeamento Financeiro
-- ## Nota: Esta migração cria a estrutura de base de dados necessária para
-- ##       suportar a nova funcionalidade de Planeamento Financeiro. Ela
-- ##       introduz as tabelas para armazenar os planos, os grupos de orçamento
-- ##       e a associação destes grupos com as categorias de despesa existentes.
-- ##############################################################################

CREATE TABLE financial_plan
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        VARCHAR(50)    NOT NULL,
    base_income NUMERIC(38, 2) NOT NULL
);

CREATE TABLE budget_group
(
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    name              VARCHAR(50)    NOT NULL,
    target_percentage NUMERIC(5, 2) NOT NULL,
    plan_id           INTEGER NOT NULL,
    FOREIGN KEY (plan_id) REFERENCES financial_plan (id) ON DELETE CASCADE
);

CREATE TABLE budget_group_categories
(
    budget_group_id INTEGER NOT NULL,
    category_id     INTEGER NOT NULL,
    PRIMARY KEY (budget_group_id, category_id),
    FOREIGN KEY (budget_group_id) REFERENCES budget_group (id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE CASCADE
);
