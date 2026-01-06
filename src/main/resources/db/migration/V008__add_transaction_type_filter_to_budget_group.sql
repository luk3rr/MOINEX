-- ##############################################################################
-- ## Migration: Adiciona filtro de tipo de transação aos grupos de orçamento
-- ## Nota: Esta migração adiciona uma coluna para permitir que cada grupo de
-- ##       orçamento possa filtrar se considera apenas receitas, apenas despesas
-- ##       ou ambos os tipos de transação na soma total.
-- ##############################################################################

ALTER TABLE budget_group 
ADD COLUMN transaction_type_filter VARCHAR(255) NOT NULL DEFAULT 'EXPENSE'
CHECK (transaction_type_filter IN ('INCOME', 'EXPENSE', 'BOTH'));