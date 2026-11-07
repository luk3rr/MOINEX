# Notas de auditoria (alimentam o relatório do UNIT-7)

## RecurringCreditCardDebtService (FR-21, leitura do T-5.1)

Veredito: **correto**, sem alteração de código.

- A projeção de ocorrências começa no maior entre o próximo mês de fatura do recorrente e o mês corrente, avança pela frequência e para na data final. [src: RecurringCreditCardDebtService.kt, getProjectedOccurrences]
- A dívida projetada até um mês soma só ocorrências de meses depois do corrente e até o mês alvo, o que evita contar o que já virou dívida de verdade. [src: getTotalProjectedAmountUntilMonth]
- O dia do mês é limitado a 1..28 pelo modelo, então `atDay` nunca falha em fevereiro. [src: RecurringCreditCardDebt.kt, Constants.MAX_BILLING_DUE_DAY]
- Materializar para um mês só aceita o próximo mês agendado e pula meses já materializados. [src: materializeForMonth, isAlreadyMaterialized]
- Resíduo, fora do escopo: a materialização antecipada cria a dívida com a data de agora, não a data da ocorrência.

## Patrimônio: o que ficou depois do FR-18

- Parcelas de recorrência com data final entram como ativo ou passivo só em meses depois do corrente e só as ainda não geradas (a partir de `nextDueDate`). Nos meses futuros, o saldo das carteiras também recebe as recorrências projetadas, então a parcela do mês pode pesar duas vezes (no saldo projetado e como passivo ou ativo recorrente). O requisito aprovado não cobre esse caso; decidir com o dono se a recorrência marcada para o patrimônio deve sair da projeção de saldo.

## Venda de renda fixa fora do par

- Ver o registro `DECISION` do UNIT-4 no `audit.md`: a diferença de preço entra duas vezes no valor creditado na carteira quando o preço de venda difere do custo.

## Cache de patrimônio

- Os valores do gráfico de patrimônio são guardados e só mudam com o botão "Recalcular" da tela inicial. Depois desta atualização é preciso apertá-lo para que o histórico use o preço de fechamento de cada mês e as regras novas.
