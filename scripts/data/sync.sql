/*
 * Filename: sync.sql
 * Created on: October 27, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Script para obter os dados da base de dados antiga e inserir no Moinex.
 * Deve ser executado a partir da raiz do projeto para que os caminhos relativos funcionem corretamente.
 * Pode ser executado com:
 * $ sqlite3 $HOME/.moinex/data/moinex.db < scripts/data/sync.sql
 *
 * Garanta que old.db esteja na raiz do projeto ou altere o caminho do arquivo abaixo.
 */

ATTACH DATABASE 'old.db' AS mf;

-- RECEITAS
CREATE TEMP TABLE temp_receitas_mf AS
WITH RECEITAS_MF AS (
    SELECT
         mf.cts.descricao AS cts_nome,
         CASE -- Se houver uma data registrada, então a carteira foi arquivada
             WHEN mf.cts.data_exclusao IS NULL
             THEN FALSE
             ELSE TRUE
         END AS cts_arquivada,
         mf.rct.descricao AS rct_desc,
         mf.rct.valor,
         CASE
            -- Receitas fixas têm o campo data_efetivacao nulo
            WHEN mf.rct.data_efetivacao IS NOT NULL
            -- Formata a data para o formato utilizado no Moinex
            THEN strftime('%Y-%m-%dT%H:%M:%S', mf.rct.data_efetivacao)
            ELSE strftime('%Y-%m-%dT%H:%M:%S', mf.rct_fx.data_efetivacao)
         END AS rct_data,
         mf.rct.recorrencia AS recorrente,
         CASE -- Verifica se a receita foi efetivada
             WHEN mf.rct.efetivado = 'S' OR mf.rct_fx.efetivado = 'S'
             THEN 'CONFIRMED'
             ELSE 'PENDING'
         END AS status,
         mf.rct_cat.descricao AS cat_nome,
         CASE -- Se houver uma data registrada, então a categoria foi arquivada
             WHEN mf.rct_cat.data_exclusao IS NULL
             THEN FALSE
             ELSE TRUE
         END AS cat_arquivada
    FROM mf.contas AS cts
    FULL JOIN mf.receitas AS rct ON rct.id_conta = cts.id
    FULL JOIN mf.receitas_cat AS rct_cat ON rct.id_categoria = rct_cat.id
    FULL JOIN mf.receitas_fixas AS rct_fx ON rct.id = rct_fx.id_receita
    WHERE
        mf.cts.descricao IS NOT NULL
        AND mf.rct_cat.descricao IS NOT NULL
        -- Toda transação deve ter ao menos uma data
        AND (mf.rct.data_efetivacao IS NOT NULL
            OR mf.rct_fx.data_efetivacao IS NOT NULL
        )
        -- Considera apenas as receitas fixas que não foram excluídas no mês
        -- e as receitas que não são fixas (campo null)
        AND (mf.rct_fx.excluido_mes = 'N' OR mf.rct_fx.excluido_mes IS NULL)
)

SELECT * FROM RECEITAS_MF;

-- 1. Inserir as wallets
INSERT INTO wallet (name, archived, balance, type_id)
-- type_id (wallet type) por padrão é 0, uma vez que não da pra mapear diretamente
SELECT DISTINCT cts_nome, cts_arquivada, 0, 0
FROM temp_receitas_mf
-- Insere somente os dados que já não foram inseridos
WHERE NOT EXISTS (
    SELECT 1 FROM wallet WHERE name = temp_receitas_mf.cts_nome
);

-- 2. Inserir as categorias
INSERT INTO category (name, archived)
SELECT DISTINCT cat_nome, cat_arquivada
FROM temp_receitas_mf
-- Insere somente os dados que já não foram inseridos
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = temp_receitas_mf.cat_nome
);

-- 3. Inserir as receitas
INSERT INTO wallet_transaction (amount, date, description, status, type, category_id, wallet_id)
SELECT valor, rct_data, rct_desc, status, 'INCOME', ct.id, wt.id
FROM temp_receitas_mf
JOIN wallet wt ON wt.name = temp_receitas_mf.cts_nome
JOIN category ct ON ct.name = temp_receitas_mf.cat_nome
-- Insere somente os dados que já não foram inseridos
WHERE NOT EXISTS (
    SELECT 1
    FROM wallet_transaction wt2
    WHERE wt2.description = temp_receitas_mf.rct_desc
      AND wt2.date = temp_receitas_mf.rct_data
      AND wt2.amount = temp_receitas_mf.valor
      AND wt2.status = temp_receitas_mf.status
);

DROP TABLE IF EXISTS temp_receitas_mf;

-- DESPESAS
CREATE TEMP TABLE temp_despesas_mf AS
WITH DESPESAS_MF AS (
    SELECT DISTINCT -- Por algum motivo o desgraçado duplicou um tanto de despesa no banco
         mf.cts.descricao AS cts_nome,
         CASE -- Se houver uma data registrada, então a carteira foi arquivada
             WHEN mf.cts.data_exclusao IS NULL
             THEN FALSE
             ELSE TRUE
         END AS cts_arquivada,
         mf.dsp.descricao AS dsp_desc,
         mf.dsp.valor,
         CASE
            -- Despesas fixas têm o campo data_efetivacao nulo
            WHEN mf.dsp.data_efetivacao IS NOT NULL
            -- Formata a data para o formato utilizado no Moinex
            THEN strftime('%Y-%m-%dT%H:%M:%S', mf.dsp.data_efetivacao)
            ELSE strftime('%Y-%m-%dT%H:%M:%S', mf.dsp_fx.data_efetivacao)
         END AS dsp_data,
         mf.dsp.recorrencia AS recorrente,
         CASE -- Verifica se a despesa foi efetivada
             WHEN mf.dsp.status = 'pg' OR mf.dsp_fx.status = 'pg'
             THEN 'CONFIRMED'
             ELSE 'PENDING'
         END AS status,
         mf.dsp.num_parcela,
         mf.dsp.num_parcelas,
         mf.dsp_cat.nome AS cat_nome,
         CASE -- Se houver uma data registrada, então a categoria foi arquivada
             WHEN mf.dsp_cat.data_exclusao IS NULL
             THEN FALSE
             ELSE TRUE
         END AS cat_arquivada
    FROM contas cts
    FULL JOIN despesas dsp ON dsp.id_conta = cts.id
    FULL JOIN despesas_cat_valor dsp_cat_v ON dsp.id = dsp_cat_v.id_despesa
    FULL JOIN despesas_cat dsp_cat ON dsp_cat_v.id_categoria = dsp_cat.id
    FULL JOIN cartoes_credito crc ON dsp.id_cartao_credito = crc.id
    FULL JOIN despesas_fixas dsp_fx ON dsp.id = dsp_fx.id_despesa
    WHERE
        -- Apenas transações que não são do cartão de crédito
        mf.crc.nome IS NULL
        -- Toda transação deve ter ao menos uma data
        AND (mf.dsp.data_efetivacao IS NOT NULL
            OR mf.dsp_fx.data_efetivacao IS NOT NULL)
        -- Considera apenas as despesas fixas que não foram excluídas no mês
        -- e as receitas que não são fixas (campo null)
        AND (mf.dsp_fx.excluido_mes = 'N' OR mf.dsp_fx.excluido_mes IS NULL)
)

SELECT * FROM DESPESAS_MF;

-- 1. Inserir as wallets
INSERT INTO wallet (name, archived, balance, type_id)
-- type_id (wallet type) por padrão é 0, uma vez que não da pra mapear diretamente
SELECT DISTINCT cts_nome, cts_arquivada, 0, 0
FROM temp_despesas_mf
-- Insere somente os dados que já não foram inseridos
WHERE NOT EXISTS (
    SELECT 1 FROM wallet WHERE name = temp_despesas_mf.cts_nome
);

-- 2. Inserir as categorias
INSERT INTO category (name, archived)
SELECT DISTINCT cat_nome, cat_arquivada
FROM temp_despesas_mf
-- Insere somente os dados que já não foram inseridos
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = temp_despesas_mf.cat_nome
);

-- 3. Inserir as despesas
INSERT INTO wallet_transaction (amount, date, description, status, type, category_id, wallet_id)
SELECT valor, dsp_data, dsp_desc, status, 'EXPENSE', ct.id, wt.id
FROM temp_despesas_mf
JOIN wallet wt ON wt.name = temp_despesas_mf.cts_nome
JOIN category ct ON ct.name = temp_despesas_mf.cat_nome
-- Insere somente os dados que já não foram inseridos
WHERE NOT EXISTS (
    SELECT 1
    FROM wallet_transaction wt2
    WHERE wt2.description = temp_despesas_mf.dsp_desc
      AND wt2.date = temp_despesas_mf.dsp_data
      AND wt2.amount = temp_despesas_mf.valor
      AND wt2.status = temp_despesas_mf.status
);

DROP TABLE IF EXISTS temp_despesas_mf;

-- DESPESAS CARTÃO DE CRÉDITO
CREATE TEMP TABLE temp_despesas_crc_mf AS
WITH DESPESAS_CRC_MF AS (
    SELECT DISTINCT -- Por algum motivo o desgraçado duplicou um tanto de despesa no banco
         mf.dsp.id,
         mf.cts.descricao AS cts_nome,
         mf.crc.nome AS crc_nome,
         CASE -- Se houver uma data registrada, então a carteira foi arquivada
             WHEN mf.cts.data_exclusao IS NULL
             THEN FALSE
             ELSE TRUE
         END AS cts_arquivada,
         mf.dsp.descricao AS dsp_desc,
         mf.dsp.valor,
         CASE
            -- Despesas fixas têm o campo data_efetivacao nulo
            WHEN mf.dsp.data_efetivacao IS NOT NULL
            -- Formata a data para o formato utilizado no Moinex
            THEN strftime('%Y-%m-%dT%H:%M:%S', mf.dsp.data_efetivacao)
            ELSE strftime('%Y-%m-%dT%H:%M:%S', mf.dsp_fx.data_efetivacao)
         END AS dsp_data,
         mf.dsp.recorrencia AS recorrente,
         CASE -- Verifica se a despesa foi efetivada
             WHEN mf.dsp.status = 'pg' OR mf.dsp_fx.status = 'pg'
             THEN 'CONFIRMED'
             ELSE 'PENDING'
         END AS status,
         mf.dsp.num_parcela,
         mf.dsp.num_parcelas,
         mf.dsp_cat.nome AS cat_nome,
         CASE -- Se houver uma data registrada, então a categoria foi arquivada
             WHEN mf.dsp_cat.data_exclusao IS NULL
             THEN FALSE
             ELSE TRUE
         END AS cat_arquivada,
         mf.crc.dia_venc AS crc_dia_vencimento,
         mf.crc.dia_fecha AS crc_dia_fechamento,
         mf.crc.limite AS crc_limite
    FROM contas cts
    FULL JOIN despesas dsp ON dsp.id_conta = cts.id
    FULL JOIN despesas_cat_valor dsp_cat_v ON dsp.id = dsp_cat_v.id_despesa
    FULL JOIN despesas_cat dsp_cat ON dsp_cat_v.id_categoria = dsp_cat.id
    FULL JOIN cartoes_credito crc ON dsp.id_cartao_credito = crc.id
    FULL JOIN despesas_fixas dsp_fx ON dsp.id = dsp_fx.id_despesa
    WHERE
        -- Apenas transações que são do cartão de crédito
        crc.nome IS NOT NULL
        -- Considera apenas as despesas fixas que não foram excluídas no mês
        -- e as receitas que não são fixas (campo null)
        AND (mf.dsp_fx.excluido_mes = 'N' OR mf.dsp_fx.excluido_mes IS NULL)
        AND mf.dsp.valor > 0 -- Reembolsos do cartão não são considerados (são armazenados com sinal negativo)
)

SELECT * FROM DESPESAS_CRC_MF;

-- Atualiza as datas nulas com base em datas anteriores da mesma fatura (parcela) e descrição
-- ou define para o próximo mês usando 'dia_fechamento' caso seja uma única fatura sem data
-- OBS.: Não garanto que esse update vai funcionar caso haja duas faturas correndo com a mesma descrição
UPDATE temp_despesas_crc_mf
SET
    dsp_data = (
        SELECT
            CASE
                -- Caso existam datas anteriores da mesma descrição, acrescenta 1 mês na data máxima
                -- Garanta que o dia não seja maior que 28. Caso contrário, haverá problemas com 
                -- a fatura de fevereiro de um ano não bissexto
                WHEN EXISTS (
                    SELECT 1
                    FROM temp_despesas_crc_mf AS prev_desp
                    WHERE prev_desp.dsp_desc = temp_despesas_crc_mf.dsp_desc
                    AND prev_desp.dsp_data IS NOT NULL
                )
                THEN strftime('%Y-%m-%dT%H:%M:%S', DATE(MAX(prev_desp.dsp_data), '+' || 1 || ' MONTH'))
                -- Caso seja a única fatura sem data, define o próximo mês com base em 'dia_fechamento'
                ELSE strftime('%Y-%m-' || temp_despesas_crc_mf.crc_dia_fechamento || 'T00:00:00', 'now', '+1 MONTH')
            END
        FROM
            temp_despesas_crc_mf AS prev_desp
        WHERE
            prev_desp.dsp_desc = temp_despesas_crc_mf.dsp_desc
            AND prev_desp.dsp_data IS NOT NULL
    ),
    cts_nome = CASE WHEN dsp_data IS NULL THEN NULL ELSE cts_nome END
WHERE dsp_data IS NULL;

-- 1. Inserir as wallets
INSERT INTO wallet (name, archived, balance, type_id)
-- type_id (wallet type) por padrão é 0, uma vez que não da pra mapear diretamente
SELECT DISTINCT cts_nome, cts_arquivada, 0, 0
FROM temp_despesas_crc_mf
-- Insere somente os dados que já não foram inseridos
WHERE NOT EXISTS (
    SELECT 1 FROM wallet WHERE name = temp_despesas_crc_mf.cts_nome
)
AND temp_despesas_crc_mf.cts_nome IS NOT NULL;

-- 2. Inserir as categorias
INSERT INTO category (name, archived)
SELECT DISTINCT cat_nome, cat_arquivada
FROM temp_despesas_crc_mf
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = temp_despesas_crc_mf.cat_nome
);

-- 3. Inserir os cartões de crédito
INSERT INTO credit_card (billing_due_day, closing_day, last_four_digits, max_debt, name, operator_id)
-- operator_id por padrão é 0, uma vez que não da pra mapear diretamente
SELECT DISTINCT crc_dia_vencimento, crc_dia_fechamento, '0000', crc_limite, crc_nome, 0
FROM temp_despesas_crc_mf
-- Insere somente os dados que já não foram inseridos
WHERE NOT EXISTS (
    SELECT 1 FROM credit_card WHERE name = temp_despesas_crc_mf.crc_nome
)
AND temp_despesas_crc_mf.crc_nome IS NOT NULL;

-- 4. Inserir as despesas do cartão com num_parcelas > 1
-- OBS.: Se você tiver faturas diferentes, mas com o mesmo título e com quantidade de parcelas diferentes,
--       o código abaixo ainda conseguirá identificar que são coisas diferentes, caso contrário, não garanto.
INSERT INTO credit_card_debt (date, description, installments, total_amount, category_id, crc_id)
SELECT
    -- Data da primeira fatura
    first_dates.first_date,
    temp.dsp_desc AS description,
    temp.num_parcelas AS installments,
    -- Soma de todas as faturas com a mesma descrição
    SUM(temp.valor) AS total_amount,
    ct.id AS category_id,
    crc.id AS crc_id
FROM temp_despesas_crc_mf AS temp
FULL JOIN wallet wt ON wt.name = temp.cts_nome
FULL JOIN category ct ON ct.name = temp.cat_nome
FULL JOIN credit_card crc ON crc.name = temp.crc_nome
FULL JOIN (
    -- Subconsulta para obter a data da primeira fatura para cada descrição
    SELECT
        dsp_desc,
        MIN(dsp_data) AS first_date
    FROM temp_despesas_crc_mf
    WHERE num_parcelas > 1
    GROUP BY dsp_desc, num_parcelas
) AS first_dates ON first_dates.dsp_desc = temp.dsp_desc
WHERE
    temp.num_parcelas > 1
    AND EXISTS (
        -- Verifica se existem faturas sequenciais mensais
        SELECT 1
        FROM temp_despesas_crc_mf AS sequenciais
        WHERE
            sequenciais.dsp_desc = temp.dsp_desc
            AND sequenciais.num_parcelas = temp.num_parcelas
            AND sequenciais.num_parcelas > 1
            AND strftime('%Y-%m', sequenciais.dsp_data) IN (
                SELECT
                    strftime('%Y-%m', DATE(first_dates.first_date, '+' || (n.n - 1) || ' MONTH'))
                -- Gera uma sequência de 12 números para os 12 meses do ano
                FROM (
                    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
                    UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
                ) AS n
                -- Verifica se a fatura do mês n existe
                WHERE
                    first_dates.first_date IS NOT NULL
                    AND EXISTS (
                        SELECT 1
                        FROM temp_despesas_crc_mf AS prev
                        WHERE prev.dsp_desc = temp.dsp_desc
                        AND strftime('%Y-%m', prev.dsp_data) = strftime('%Y-%m', DATE(first_dates.first_date, '+' || (n.n - 1) || ' MONTH'))
                    )
            )
    )
GROUP BY temp.dsp_desc, first_dates.first_date, ct.id, crc.id;

-- 5. Inserir as despesas do cartão com num_parcelas = 1
INSERT INTO credit_card_debt (date, description, installments, total_amount, category_id, crc_id)
SELECT
    temp.dsp_data AS date,
    temp.dsp_desc AS description,
    temp.num_parcelas AS installments,
    temp.valor AS total_amount,
    ct.id AS category_id,
    crc.id AS crc_id
FROM temp_despesas_crc_mf AS temp
FULL JOIN wallet wt ON wt.name = temp.cts_nome
FULL JOIN category ct ON ct.name = temp.cat_nome
FULL JOIN credit_card crc ON crc.name = temp.crc_nome
WHERE
    temp.num_parcelas = 1
    -- Verifica se não existem faturas duplicadas com a mesma data e descrição
    AND NOT EXISTS (
        SELECT 1
        FROM credit_card_debt ccd
        WHERE ccd.description = temp.dsp_desc
          AND ccd.date = temp.dsp_data
          AND ccd.total_amount = temp.valor
    );

-- 6. Inserir parcelas das faturas
INSERT INTO credit_card_payment (amount, date, installment, debt_id, wallet_id)
SELECT DISTINCT
    temp.valor AS amount,
    temp.dsp_data AS date,
    temp.num_parcela AS installment,
    crc_debt.id AS debt_id,
    wt.id AS wallet_id
FROM temp_despesas_crc_mf AS temp
FULL JOIN wallet wt ON wt.name = temp.cts_nome
JOIN credit_card_debt crc_debt
    -- Relaciona a fatura com a parcela
    -- Pega a data da primeira fatura e o valor total para obter o id do debt
    ON crc_debt.description = temp.dsp_desc
    AND (
            (crc_debt.installments <> 1 
            AND crc_debt.total_amount = (
                SELECT SUM(sub_temp.valor)
                FROM temp_despesas_crc_mf AS sub_temp
                WHERE sub_temp.dsp_desc = temp.dsp_desc
                AND sub_temp.num_parcelas = temp.num_parcelas
            )
            AND crc_debt.date = (
                SELECT MIN(dsp_data)
                FROM temp_despesas_crc_mf AS sub_temp
                WHERE sub_temp.dsp_desc = temp.dsp_desc
                AND sub_temp.num_parcelas = temp.num_parcelas
            )
        )
        OR -- Usa o ID da base de dados antiga para diferenciar compras não parceladas
        (
            (crc_debt.installments = 1 
            AND crc_debt.total_amount = (
                SELECT sub_temp.valor
                FROM temp_despesas_crc_mf AS sub_temp
                WHERE sub_temp.dsp_desc = temp.dsp_desc
                AND sub_temp.num_parcelas = temp.num_parcelas
                AND sub_temp.id = temp.id
            )           
            AND crc_debt.date = (
                SELECT dsp_data
                FROM temp_despesas_crc_mf AS sub_temp
                WHERE sub_temp.dsp_desc = temp.dsp_desc
                AND sub_temp.num_parcelas = temp.num_parcelas
                AND sub_temp.id = temp.id
                )
            )
        )
    )
-- Insere somente os dados que já não foram inseridos
WHERE NOT EXISTS (
    SELECT 1
    FROM credit_card_payment
    WHERE amount = temp.valor
      AND date = temp.dsp_data
      AND installment = temp.num_parcela
);

DROP TABLE IF EXISTS temp_despesas_crc_mf;

DETACH DATABASE mf;
