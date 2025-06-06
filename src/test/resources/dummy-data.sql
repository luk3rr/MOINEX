/*
 * Filename: dummy-data.sql
 * Created on: September 21, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

INSERT INTO wallet (id, type_id, name, balance, archived) VALUES
(1, 2, 'Banco XYZ', 1948.31, false),
(2, 5, 'Carteira', 312.00, false),
(3, 4, 'Emergência', 3050.00, false),
(4, 4, 'Economia', 5100.00, false),
(5, 1, 'Corretora KLM', 5350.00, false),
(6, 3, 'Corretora ABC', 536.92, false);

INSERT INTO wallet_transaction (id, wallet_id, category_id, type, status, date, amount, description) VALUES
-- Banco XYZ (Checking Account)
(1, 1, 1, 'EXPENSE', 'CONFIRMED', '2024-01-15T00:00:00', 530.00, 'Compra de alimentos no supermercado'),
(2, 1, 2, 'EXPENSE', 'CONFIRMED', '2024-01-18T00:00:00', 97.00, 'Táxi para o aeroporto'),
(7, 1, 7, 'EXPENSE', 'CONFIRMED', '2024-03-01T00:00:00', 150.00, 'Pagamento de serviço de internet'),
(8, 1, 8, 'EXPENSE', 'CONFIRMED', '2024-03-05T00:00:00', 860.00, 'Veterinário'),
(13, 1, 1, 'EXPENSE', 'CONFIRMED', '2024-04-03T00:00:00', 45.00, 'Café da manhã em padaria'),
(18, 1, 1, 'EXPENSE', 'CONFIRMED', '2024-05-01T00:00:00', 130.00, 'Jantar em restaurante'),
(23, 1, 0, 'INCOME', 'CONFIRMED', '2024-06-01T00:00:00', 200.00, 'Devolução de empréstimo'),
(28, 1, 5, 'EXPENSE', 'CONFIRMED', '2024-07-01T00:00:00', 270.00, 'Compra de livro'),
(33, 1, 0, 'INCOME', 'CONFIRMED', '2024-08-01T00:00:00', 150.00, 'Devolução de fiança'),
(38, 1, 1, 'EXPENSE', 'CONFIRMED', '2024-09-01T00:00:00', 330.00, 'Jantar fora de casa'),
(43, 1, 4, 'EXPENSE', 'PENDING', '2024-10-05T00:00:00', 140.00, 'Pagamento de academia'),
(44, 1, 11, 'INCOME', 'CONFIRMED', '2023-12-05T00:00:00', 3600.00, 'Salário'),
(45, 1, 6, 'EXPENSE', 'CONFIRMED', '2023-12-10T00:00:00', 2500.00, 'Aluguel'),
(46, 1, 11, 'INCOME', 'CONFIRMED', '2023-11-05T00:00:00', 3600.00, 'Salário'),
(47, 1, 6, 'EXPENSE', 'CONFIRMED', '2023-11-10T00:00:00', 2500.00, 'Aluguel'),
(48, 1, 0, 'EXPENSE', 'CONFIRMED', '2023-11-25T00:00:00', 743.00, 'Compra de itens diversos'),
(49, 1, 11, 'INCOME', 'CONFIRMED', '2023-10-05T00:00:00', 3100.00, 'Salário'),
(50, 1, 6, 'EXPENSE', 'CONFIRMED', '2023-10-10T00:00:00', 2500.00, 'Aluguel'),
(51, 1, 0, 'EXPENSE', 'CONFIRMED', '2023-10-25T00:00:00', 959.00, 'Compra de itens diversos'),
(52, 1, 11, 'INCOME', 'CONFIRMED', '2023-09-05T00:00:00', 3234.00, 'Salário'),
(53, 1, 6, 'EXPENSE', 'CONFIRMED', '2023-09-10T00:00:00', 2300.00, 'Aluguel'),
(54, 1, 0, 'EXPENSE', 'CONFIRMED', '2023-09-25T00:00:00', 915.05, 'Compra de itens diversos'),
(59, 1, 11, 'INCOME', 'CONFIRMED', '2024-01-05T00:00:00', 3931.00, 'Salário'),
(60, 1, 6, 'EXPENSE', 'CONFIRMED', '2024-01-10T00:00:00', 2300.00, 'Aluguel'),
(61, 1, 11, 'INCOME', 'CONFIRMED', '2024-02-05T00:00:00', 3831.00, 'Salário'),
(62, 1, 6, 'EXPENSE', 'CONFIRMED', '2024-02-10T00:00:00', 2300.00, 'Aluguel'),
(65, 1, 11, 'INCOME', 'CONFIRMED', '2024-03-05T00:00:00', 3231.00, 'Salário'),
(66, 1, 6, 'EXPENSE', 'CONFIRMED', '2024-03-10T00:00:00', 2300.00, 'Aluguel'),
(67, 1, 11, 'INCOME', 'CONFIRMED', '2024-04-05T00:00:00', 3230.00, 'Salário'),
(68, 1, 6, 'EXPENSE', 'CONFIRMED', '2024-04-10T00:00:00', 2300.00, 'Aluguel'),
(69, 1, 11, 'INCOME', 'CONFIRMED', '2024-05-05T00:00:00', 3830.00, 'Salário'),
(70, 1, 6, 'EXPENSE', 'CONFIRMED', '2024-05-10T00:00:00', 2300.00, 'Aluguel'),
(71, 1, 11, 'INCOME', 'CONFIRMED', '2024-06-05T00:00:00', 3430.00, 'Salário'),
(72, 1, 6, 'EXPENSE', 'CONFIRMED', '2024-06-10T00:00:00', 2300.00, 'Aluguel'),
(73, 1, 11, 'INCOME', 'CONFIRMED', '2024-07-05T00:00:00', 4130.00, 'Salário'),
(74, 1, 6, 'EXPENSE', 'CONFIRMED', '2024-07-10T00:00:00', 2300.00, 'Aluguel'),
(75, 1, 11, 'INCOME', 'CONFIRMED', '2024-08-05T00:00:00', 3630.00, 'Salário'),
(76, 1, 6, 'EXPENSE', 'CONFIRMED', '2024-08-10T00:00:00', 2300.00, 'Aluguel'),
(77, 1, 11, 'INCOME', 'CONFIRMED', '2024-09-05T00:00:00', 3530.00, 'Salário'),
(78, 1, 6, 'EXPENSE', 'CONFIRMED', '2024-09-10T00:00:00', 2300.00, 'Aluguel'),
(79, 1, 11, 'INCOME', 'CONFIRMED', '2024-10-05T00:00:00', 3530.00, 'Salário'),
(80, 1, 6, 'EXPENSE', 'CONFIRMED', '2024-10-10T00:00:00', 2300.00, 'Aluguel'),
(84, 1, 11, 'INCOME', 'CONFIRMED', '2024-11-05T00:00:00', 3530.00, 'Salário'),
(85, 1, 6, 'EXPENSE', 'CONFIRMED', '2024-11-10T00:00:00', 2300.00, 'Aluguel'),
(86, 1, 0, 'EXPENSE', 'CONFIRMED', '2024-11-15T00:00:00', 400.00, 'Jantar de aniversário'),
(87, 1, 11, 'INCOME', 'CONFIRMED', '2024-12-05T00:00:00', 3630.00, 'Salário'),
(88, 1, 6, 'EXPENSE', 'PENDING', '2024-12-10T00:00:00', 2300.00, 'Aluguel'),
(89, 1, 0, 'EXPENSE', 'PENDING', '2024-12-20T00:00:00', 1200.00, 'Presentes e comemorações de final de ano'),

-- Carteira (Wallet)
(3, 2, 3, 'EXPENSE', 'PENDING', '2024-02-05T00:00:00', 150.00, 'Reserva de hotel para a viagem'),
(9, 2, 1, 'INCOME', 'PENDING', '2024-03-08T00:00:00', 250.00, 'Reembolso de alimentação'),
(14, 2, 11, 'INCOME', 'CONFIRMED', '2024-04-07T00:00:00', 350.00, 'Salário recebido'),
(19, 2, 5, 'EXPENSE', 'CONFIRMED', '2024-05-05T00:00:00', 540.00, 'Compra de material de estudos'),
(24, 2, 6, 'EXPENSE', 'PENDING', '2024-06-05T00:00:00', 80.00, 'Serviço de jardinagem'),
(29, 2, 11, 'INCOME', 'CONFIRMED', '2024-07-05T00:00:00', 320.00, 'Recebimento de serviços prestados'),
(34, 2, 6, 'EXPENSE', 'CONFIRMED', '2024-08-05T00:00:00', 530.00, 'Compra de material de limpeza'),
(39, 2, 4, 'EXPENSE', 'PENDING', '2024-09-05T00:00:00', 450.00, 'Compra de remédios'),
(58, 2, 4, 'EXPENSE', 'CONFIRMED', '2024-08-24T00:00:00', 70.00, 'Atualização da identidade'),
(81, 2, 4, 'EXPENSE', 'PENDING', '2024-10-05T00:00:00', 371.99, 'Compra de remédios'),

-- Emergência (Savings Account)
(12, 3, 9, 'INCOME', 'CONFIRMED', '2024-02-20T00:00:00', 100.00, 'Depósito na poupança de emergência'),
(10, 3, 4, 'EXPENSE', 'CONFIRMED', '2024-03-15T00:00:00', 622.00, 'Compra de medicamentos'),
(15, 3, 12, 'EXPENSE', 'PENDING', '2024-04-10T00:00:00', 400.00, 'Compra de roupas de frio'),
(20, 3, 4, 'EXPENSE', 'PENDING', '2024-05-08T00:00:00', 600.00, 'Consulta médica particular'),
(25, 3, 6, 'EXPENSE', 'CONFIRMED', '2024-06-10T00:00:00', 250.00, 'Manutenção banheiro'),
(30, 3, 6, 'EXPENSE', 'CONFIRMED', '2024-07-10T00:00:00', 950.00, 'Compra de móveis'),
(35, 3, 7, 'EXPENSE', 'PENDING', '2024-08-10T00:00:00', 300.00, 'Serviço de manutenção de TI'),
(40, 3, 13, 'EXPENSE', 'CONFIRMED', '2024-09-10T00:00:00', 300.00, 'Manutenção do corsa'),
(82, 3, 9, 'INCOME', 'CONFIRMED', '2024-10-01T00:00:00', 1050.00, 'Depósito na poupança de emergência'),

-- Corretora KLM (Broker)
(4, 5, 10, 'INCOME', 'CONFIRMED', '2024-05-15T00:00:00', 300.00, 'Lucro com venda de ações da empresa ABC'),
(6, 5, 10, 'INCOME', 'CONFIRMED', '2024-03-25T00:00:00', 120.00, 'Dividendos recebidos de investimento'),
(22, 5, 10, 'INCOME', 'CONFIRMED', '2024-05-15T00:00:00', 300.00, 'Rendimentos de fundo de renda fixa'),
(37, 5, 10, 'INCOME', 'CONFIRMED', '2024-08-20T00:00:00', 400.00, 'Rendimento de ações vendidas'),
(42, 5, 10, 'INCOME', 'CONFIRMED', '2024-09-20T00:00:00', 500.00, 'Lucro com venda de títulos'),
(83, 5, 10, 'INCOME', 'CONFIRMED', '2024-10-01T00:00:00', 300.00, 'Lucro com venda de títulos'),

-- Corretora ABC
(55, 3, 10, 'INCOME', 'CONFIRMED', '2024-06-15T00:00:00', 150.00, 'Rendimento de criptos vendidas'),
(57, 3, 9, 'INCOME', 'CONFIRMED', '2024-07-15T00:00:00', 200.00, 'Compra de mais criptos'),

-- Economia (Savings Account)
(5, 4, 10, 'INCOME', 'CONFIRMED', '2024-02-12T00:00:00', 500.00, 'Rendimento de poupança'),
(11, 4, 10, 'INCOME', 'CONFIRMED', '2024-03-20T00:00:00', 75.00, 'Juros acumulados na conta poupança'),
(16, 4, 9, 'INCOME', 'CONFIRMED', '2024-04-12T00:00:00', 250.00, 'Depósito de economias pessoais'),
(41, 4, 10, 'INCOME', 'CONFIRMED', '2024-09-15T00:00:00', 180.00, 'Rendimento de aplicação'),
(56, 4, 9, 'INCOME', 'CONFIRMED', '2024-06-15T00:00:00', 400.00, 'Depósito de economias pessoais');


INSERT INTO credit_card (id, operator_id, name, billing_due_day, closing_day, max_debt, last_four_digits, default_billing_wallet_id) VALUES
(1, 1, 'Visa Gold', 10, 3, 5000.00, '1234', 1),
(2, 2, 'MC Platinum', 15, 7, 7500.00, '5678', 1),
(3, 3, 'Amex Green', 20, 10, 3000.00, '9101', 1),
(4, 4, 'Discover Cashback', 25, 20, 2000.00, '1121', 1),
(5, 5, 'Diners Club Rewards', 25, 19, 4000.00, '3141', 1),
(6, 6, 'JCB', 10, 2, 1000.00, '3830', 1),
(7, 7, 'Elo', 11, 3,  2300.00, '4301', 1),
(8, 8, 'Hipercard', 5, 1, 500.00, '9031', 1),
(9, 0, 'Cartaozin', 5, 1, 1.00, '1683', 1),
(10, 0, 'Cartaozin2', 5, 1, 2.00, '3031', 1);

-- Dívidas passadas (já pagas)
INSERT INTO credit_card_debt (id, crc_id, category_id, date, total_amount, installments, description)
VALUES
(1, 1, 1, '2023-05-10T00:00:00', 200.00, 2, 'Compra de supermercado'),
(2, 1, 2, '2023-06-10T00:00:00', 150.00, 2, 'Transporte público'),
(3, 2, 3, '2023-07-15T00:00:00', 500.00, 2, 'Viagem de férias'),
(4, 3, 4, '2023-08-20T00:00:00', 300.00, 2, 'Consulta médica'),
(5, 4, 5, '2023-09-25T00:00:00', 250.00, 2, 'Material escolar');


-- Pagamentos para as dívidas passadas
INSERT INTO credit_card_payment (id, wallet_id, debt_id, date, amount, installment)
VALUES
(1, 1, 1, '2023-05-11T00:00:00', 100.00, 1),
(2, 1, 1, '2023-06-11T00:00:00', 100.00, 2),
(3, 1, 2, '2023-07-11T00:00:00', 75.00, 1),
(4, 1, 2, '2023-08-11T00:00:00', 75.00, 2),
(5, 2, 3, '2023-08-11T00:00:00', 250.00, 1),
(6, 2, 3, '2023-09-11T00:00:00', 250.00, 2),
(7, 3, 4, '2023-09-11T00:00:00', 150.00, 1),
(8, 3, 4, '2023-10-11T00:00:00', 150.00, 2),
(9, 4, 5, '2023-10-11T00:00:00', 125.00, 1),
(10, 4, 5, '2023-11-11T00:00:00', 125.00, 2);

-- Dívidas futuras (não pagas)
INSERT INTO credit_card_debt (id, crc_id, category_id, date, total_amount, installments, description)
VALUES
(6, 1, 1, '2024-09-10T00:00:00', 180.00, 2, 'Compra de supermercado'),
(7, 2, 2, '2024-09-15T00:00:00', 400.00, 2, 'Passagem de ônibus'),
(8, 3, 3, '2025-09-20T00:00:00', 600.00, 2, 'Viagem planejada'),
(9, 4, 4, '2025-09-25T00:00:00', 320.00, 2, 'Consulta médica'),
(10, 5, 5, '2025-09-30T00:00:00', 270.00, 10, 'Compra de livros');

-- Pagamentos para as dívidas futuras
INSERT INTO credit_card_payment (id, wallet_id, debt_id, date, amount, installment)
VALUES
(11, NULL, 6, '2024-09-11T00:00:00', 90.00, 1),
(12, NULL, 6, '2024-10-11T00:00:00', 90.00, 2),
(13, NULL, 7, '2024-09-11T00:00:00', 200.00, 1),
(14, NULL, 7, '2024-10-11T00:00:00', 200.00, 2),
(15, NULL, 8, '2024-09-11T00:00:00', 300.00, 1),
(16, NULL, 8, '2024-10-11T00:00:00', 300.00, 2),
(17, NULL, 9, '2024-09-11T00:00:00', 320.00, 1),
(19, NULL, 10, '2024-09-11T00:00:00', 27.00, 1),
(20, NULL, 10, '2024-10-11T00:00:00', 27.00, 2),
(21, NULL, 10, '2024-11-11T00:00:00', 27.00, 3),
(22, NULL, 10, '2024-12-11T00:00:00', 27.00, 4),
(23, NULL, 10, '2025-01-11T00:00:00', 27.00, 5),
(24, NULL, 10, '2025-02-11T00:00:00', 27.00, 6),
(25, NULL, 10, '2025-03-11T00:00:00', 27.00, 7),
(26, NULL, 10, '2025-04-11T00:00:00', 27.00, 8),
(27, NULL, 10, '2025-05-11T00:00:00', 27.00, 9),
(28, NULL, 10, '2025-06-11T00:00:00', 27.00, 10);
