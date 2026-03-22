-- ##############################################################################
-- ## Migration: Adiciona datas de início e fim aos planos financeiros
-- ## Nota: Permite que planos financeiros tenham períodos de vigência,
-- ##       possibilitando histórico de planos quando a receita muda.
-- ##       - start_date: quando o plano entra em vigor (obrigatório)
-- ##       - end_date: quando o plano é encerrado (null = plano ativo)
-- ##############################################################################

ALTER TABLE financial_plan ADD COLUMN start_date DATE;
ALTER TABLE financial_plan ADD COLUMN end_date DATE;

-- Populate start_date for existing plans
-- Archived plans get a default start_date of 1970-01-01
UPDATE financial_plan SET start_date = '1970-01-01' WHERE archived = TRUE AND start_date IS NULL;

-- Active plan gets current date as start_date
UPDATE financial_plan SET start_date = DATE('now') WHERE archived = FALSE AND start_date IS NULL;

-- Archived plans get end_date = start_date as placeholder
UPDATE financial_plan SET end_date = DATE('now') WHERE archived = TRUE AND end_date IS NULL;
