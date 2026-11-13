# Area: import e exportação de dados

Status: draft (intent 261112-import-export-data, ainda não revisado pelo dono) · Confidence: alta no código, média nos casos de borda de dados reais

## Pacote `org.moinex.dataexchange`
- `spec`: `EntitySpec` (uma por tabela exportável), `EntityCatalog` (ordem por dependência, grupos), `NaturalKeys`, modelos (`Column`, `ParsedRow`, `Problem`, códigos E01..E08/W01..W02), `ResolveContext` (planejamento) e `ApplyContext` (gravação, com `memo`).
- `codec/CellCodec`: formato fixo (ISO, ponto decimal, enum pelo nome); neutraliza fórmula só no CSV (prefixo `'` reversível).
- `tabular`: `TabularWriter/Reader` para XLSX (fastexcel + fastexcel-reader 0.20.2) e zip de CSV (opencsv), `FileGuard` (50 MB, 200 entradas, 512 MB descompactado, razão 200:1, zip slip), `_meta` com `layout_version`.
- `export/ExportService`, `imports/{ImportPlanner,ImportExecutor,ImportModels}`: o planejador lê e valida sem gravar; o executor aplica em uma transação (`TransactionTemplate`) com notificações silenciadas.
- `wallet`, `creditcard`, `investment`, `planning`: as 24 specs. `ui`: `ExportDialog`, `ImportDialog`, `DataExchangeDialogs` e os view models.

## Regras que não são óbvias
- Referência entre linhas é por nome/chave natural; transação de carteira, dívida de cartão e itens de wishlist são referenciados pela identidade (carteira, data, valor, descrição).
- O import restaura estado em vez de refazer operações. `Wallet.balance` é o saldo atual e fatura paga/estorno mexem direto nele; por isso `wallets` exporta `initial_balance` (saldo atual menos o efeito líquido de transações CONFIRMED e transferências, e das virtuais na mestra) e as specs de cartão e investimento não alteram saldo.
- Metas (`Goal`) são carteiras (`kind = GOAL` na aba `wallets`). Plano financeiro só existe com os grupos somando 100%, então `FinancialPlanSpec` cria o plano e os grupos juntos (`EntitySpec.prepare`).
- Mudanças em código existente: `NotificationService.withoutNotifications`, `WalletService.createWalletTransaction/createTransfer(enforceBalanceRules)`, `CreditCardService.createDebt(enforceCreditLimit)`.
- Não exportado: caches, histórico de preços, indicadores, análises, snapshots, notificações, conversas e credenciais de IA, juros calculados de títulos (`bond_interest_calculation`, recalculados pelo app), `ticker.metadata`, FIRE.

## Riscos conhecidos
- Linhas de mesma chave natural dentro do arquivo são tratadas por ocorrência; vínculos com transações idênticas apontam para a primeira ainda não usada.
- Ajustes manuais de juros de títulos se perdem (são recalculados).
- Nomes de categoria com `;` não cabem na coluna `categories` dos grupos de orçamento.
