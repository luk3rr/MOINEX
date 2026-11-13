# Tasks — import-export-data

## 2.9 Delivery plan

**Verification command (proposta, a aprovar):** `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && env -u DISPLAY -u WAYLAND_DISPLAY ./gradlew ktlintCheck spotlessCheck test` (sem testes Python: nenhum script Python é tocado).

**Baseline:** 2144 testes Kotlin + 118 Python, 0 falhas (2026-11-07). [src: quality-and-risks.md]

**Walking skeleton:** off. A fatia ponta a ponta nasce naturalmente de UNIT-1 + UNIT-2 + UNIT-3 (bolt 2).

**Branch:** main direto (M-014). **Autonomia:** gated (a confirmar em 3.x). Sem commit antes de o dono ver o diff (M-004).

### Bolts
| Bolt | Units | Definition of Done | Hipótese de confiança |
|---|---|---|---|
| B1 | UNIT-1 | Lib xlsx resolvida; CSV-zip e XLSX escrevem/leem as mesmas linhas com `CellCodec`; planner/executor funcionam com uma spec de teste; limites e neutralização de fórmula testados; verificação passa | Alta; risco = lib xlsx com JDK 25 (ADR-1) |
| B2 | UNIT-2, UNIT-3 | Export e import de carteiras ponta a ponta pela UI (layout D2) com saldos iguais ao da tela; telas conferidas nos dois temas com `make demo` (M-003, M-006); verificação passa | Média; risco = invariantes do `WalletService` (saldo inicial, status) |
| B3 | UNIT-4, UNIT-5 | Cartões e investimentos exportam/importam por serviços; parcelas, quantidade e preço médio iguais; grupos aparecem na UI sem mudar a UI; verificação passa | Média; risco = serviços de ativo/título com efeitos colaterais |
| B4 | UNIT-6, UNIT-7 | Planejamento, round-trip de todas as entidades, modelo vazio, 50 mil linhas em até 30 s; verificação passa | Média |

Dependências externas: nenhuma além da lib xlsx (Maven Central). Mockups: `mockups/refined.html`.

## Tasks
Cada tarefa: ≤1h, estado observável, FR/US/NFR servidos e teste nomeado.

### UNIT-1 Fundação
- [x] T-1.1 Adicionar fastexcel + fastexcel-reader ao `libs.versions.toml` e ao `build.gradle.kts`; provar que resolve e roda no JDK 25 (ADR-1; senão POI). FR-1. Teste: `XlsxLibSmokeTest` escreve e lê uma planilha.
- [x] T-1.2 `CellCodec` (texto, int, boolean, BigDecimal, data, data-hora, enum, nulo) com decode estrito. FR-4, NFR-2. Teste: `CellCodecTest` com valores de referência (vírgula decimal rejeitada, escala preservada).
- [x] T-1.3 Neutralização de fórmula reversível no `CellCodec`. NFR-3. Teste: `CellCodecFormulaTest` (`=1+1`, `-5`, `@x`, número negativo legítimo).
- [x] T-1.4 Contratos: `Column`, `EntitySpec`, `EntityGroup`, `Problem`, `ImportPlan`, `ImportResult`, `ProgressSink` e `EntityCatalog` (registra specs por injeção Spring, ordena por `dependsOn`). NFR-8. Teste: `EntityCatalogTest` (ordem topológica, ciclo falha).
- [x] T-1.5 `TabularWriter/Reader` CSV-zip (opencsv, UTF-8 BOM, `_meta`). FR-1, NFR-6. Teste: `CsvZipRoundTripTest` (acentos, aspas, quebra de linha).
- [x] T-1.6 `TabularWriter/Reader` XLSX (streaming, células tipadas, `_meta`). FR-1, NFR-6. Teste: `XlsxRoundTripTest`.
- [x] T-1.7 Validação de arquivo: `_meta`, versão, limites 50 MB / 200 entradas / razão de compressão, célula de fórmula. FR-6, NFR-3. Teste: `FileGuardTest` (E01–E04, E07, zip bomb sintético).
- [x] T-1.8 `ExportService` (contagens, seleção, progresso, cancelamento). FR-1, FR-3. Teste: `ExportServiceTest` com spec fake.
- [x] T-1.9 `ImportPlanner`: leitura, parse por coluna, resolução de referências por nome (arquivo + banco), detecção de duplicata por chave natural e por ocorrência. FR-5, FR-7, FR-9, FR-11. Teste: `ImportPlannerTest` (E05, E06, duplicata, referência dentro do arquivo).
- [x] T-1.10 `ImportExecutor`: transação única, ordem de dependência, revalidação, rollback total. FR-8. Teste: `ImportExecutorTest` (falha na 3ª linha desfaz tudo; segunda importação = tudo duplicado).
- [x] T-1.11 Chaves de i18n dos códigos E01–E08/W01–W02 (só acrescentar às .properties). NFR-7. Teste: `ImportMessagesTest` (toda chave existe nos dois bundles).
- [x] T-1.12 Verificação do Unit: comando aprovado passa; sem dados de rede (NFR-1). Teste: `NoNetworkTest` (classe sem dependência de cliente HTTP).

**Resumo do UNIT-1 (verificado em 2026-11-12)**
- Arquivos: `dataexchange/{codec/CellCodec, spec/{Models,EntitySpec,EntityCatalog}, tabular/{Tabular,FileGuard,CsvZipTabular,XlsxTabular,TabularFiles}, export/ExportService, imports/{ImportModels,ImportPlanner,ImportExecutor}}.kt`; testes espelhados em `src/test/kotlin/org/moinex/dataexchange/` (9 classes + `FakeSpecs`); `libs.versions.toml`, `build.gradle.kts` (fastexcel e fastexcel-reader 0.20.2); 10 chaves `dataexchange.problem.*` acrescentadas às duas .properties.
- Verificação: `ktlintCheck spotlessCheck test` exit 0, 2217 testes, 0 falhas (baseline 2144; +73 entre novos e do dia).
- Desvios: `EntitySpec` sem genérico `<T>`; `existingKeys()` devolve `Map<chave, ocorrências>` (necessário para R9); `ResolveContext`/`ApplyContext` são classes concretas; limites do `FileGuard`: 50 MB, 200 entradas, 512 MB descompactado, razão 200:1 acima de 1 MB (a razão 100:1 do contrato dava risco de falso positivo em XML); no XLSX o decimal é célula numérica e perde zeros à direita (valor igual, escala não), no CSV a escala é preservada; a versão do app vai só no `_meta` (o `Workbook` do fastexcel exige o formato "16.0300").
- Rodar: `./gradlew test --tests "org.moinex.dataexchange.*"`.


### UNIT-2 Carteiras
- [x] T-2.1 Fechar a regra de saldo inicial vs atual contra `WalletService.createWallet` (A12) e registrar em `design.md § 3.1`. FR-10. Teste: `WalletBalanceExportTest` (valor de referência calculado à parte).
- [x] T-2.2 Specs `wallet_types`, `categories`. FR-3, FR-5. Teste: `WalletTypeCategorySpecTest`.
- [x] T-2.3 Spec `wallets` (tipo, master, arquivada). FR-3, FR-5. Teste: `WalletSpecTest`.
- [x] T-2.4 Spec `wallet_transactions` com `ref` e chave por ocorrência, via `WalletService`. FR-10, US-7. Teste: `WalletTransactionSpecTest` (saldo final igual ao de operações pela tela).
- [x] T-2.5 Specs `transfers`, `recurring_transactions`. FR-10. Teste: `TransferRecurringSpecTest`.
- [x] T-2.6 Round-trip de carteiras em banco H2: exportar do banco A, importar no B vazio, comparar campo a campo. NFR-5, US-4. Teste: `WalletsRoundTripTest` (CSV e XLSX).

**Resumo do UNIT-2 (verificado em 2026-11-13)**
- Arquivos: `dataexchange/wallet/{WalletBalanceCalculator,WalletEntitySpecs}.kt` (6 specs `@Component`: wallet_types, categories, wallets, wallet_transactions, transfers, recurring_transactions), `dataexchange/spec/NaturalKeys.kt`; `EntitySpec.applyOrder`; `ImportExecutor` agora embrulha a execução em `NotificationService.withoutNotifications`. Código existente alterado (aprovado no gate 3.1): `NotificationService.withoutNotifications { }`; `WalletService.createWalletTransaction`/`createTransfer` com `enforceBalanceRules = true` (padrão).
- Testes novos: `WalletBalanceCalculatorTest` (referência calculada à mão), `WalletSpecValidationTest`, `WalletsRoundTripTest` (H2 real, serviços reais, XLSX e CSV: banco A exportado e importado em B vazio, saldos e linhas iguais, nenhuma notificação, segunda importação sem novos registros), `WalletServiceBulkImportTest`, `NotificationServiceSilentTest`.
- Verificação: `ktlintCheck spotlessCheck test` exit 0, 2242 testes, 0 falhas.
- Desvios: `transfers` não tem coluna `ref` (nada a referencia); regra W6 "mestra virtual" não implementada (o serviço também não a checa); a coluna `balance` é informativa e não é lida no import; helper de teste `WalletTestDatabase` sobe um contexto Spring mínimo com H2 (reutilizável pelos Units 4 a 6); as chaves de data são truncadas a segundos e valores a 2 casas.
- Aprendizado de teste: qualquer teste que passe por `NotificationService` real precisa stubar `FxUtils` (M-013), senão trava a suíte inteira.


### UNIT-3 Interface
- [x] T-3.1 Seção "Dados" em `settings.fxml` + `SettingsController` (3 botões, `TitledValue`). FR-12. Teste manual `make demo`; `SettingsDataSectionTest` com `FxUtils` stubado (M-013).
- [x] T-3.2 Diálogo Exportar (grupos, checkboxes, contagens, formato, FileChooser, resumo). FR-2, US-1, US-2, US-3. Teste: `ExportDialogViewModelTest`.
- [x] T-3.3 Assistente de Importar passo 1 e erros de arquivo (G). FR-6. Teste: `ImportWizardStepOneTest`.
- [x] T-3.4 Passo 2 pré-visualização D2 (entidades | abas Problemas/Novas/Duplicadas, botão bloqueado com erro). FR-7, FR-8, US-5. Teste: `ImportPreviewViewModelTest`.
- [x] T-3.5 Execução assíncrona com progresso e cancelamento antes da gravação; passo 3 resultado e log por contagens. FR-13, FR-14, US-6, US-9. Teste: `ImportRunViewModelTest`.
- [x] T-3.6 i18n pt-BR e en da UI (só acrescentar chaves) e conferência de temas escuro e claro com `scripts/dev/capture-screens.sh` (CapturePlan). NFR-7. Teste: `DataExchangeMessagesTest`; imagens em `construction/screenshots/` (M-009).

### UNIT-4 Cartões
- [x] T-4.1 Fixar layout em `design.md § 3.1 UNIT-4` (operadoras, cartões, dívidas, pagamentos, créditos, recorrências). Teste: revisão do gate 3.1.
- [x] T-4.2 Specs `credit_card_operators`, `credit_cards`. FR-3, FR-5. Teste: `CreditCardSpecTest`.
- [x] T-4.3 Spec `credit_card_debts` com parcelas e `ref`, via `CreditCardService`. FR-10. Teste: `CreditCardDebtSpecTest` (parcelas e fatura iguais às da tela).
- [x] T-4.4 Specs `credit_card_payments`, `credit_card_credits`, `recurring_credit_card_debts`. FR-10. Teste: `CreditCardPaymentSpecTest`.
- [x] T-4.5 Round-trip de cartões. NFR-5. Teste: `CreditCardRoundTripTest`.

### UNIT-5 Investimentos
- [x] T-5.1 Fixar layout em `design.md § 3.1 UNIT-5` (exchanges, tickers, compras, vendas, dividendos, títulos, operações, metas de alocação). Teste: revisão do gate 3.1.
- [x] T-5.2 Specs `crypto_exchanges`, `tickers`, `investment_targets`; ticker importado entra sem acionar o Yahoo (M-016). FR-3, FR-5. Teste: `TickerSpecTest` (verifica que `YahooGateway` não é chamado).
- [x] T-5.3 Specs `ticker_purchases`, `ticker_sales`, `dividends` via `TickerService` (quantidade e preço médio). FR-10. Teste: `TickerOperationsSpecTest` (valor de referência calculado à parte, M-017).
- [x] T-5.4 Specs `bonds`, `bond_operations` via `BondService`. FR-10. Teste: `BondSpecTest`.
- [x] T-5.5 Round-trip de investimentos. NFR-5. Teste: `InvestmentRoundTripTest`.

### UNIT-6 Planejamento
- [x] T-6.1 Fixar layout em `design.md § 3.1 UNIT-6` (planos, grupos com categorias, metas, wishlist e links). Teste: revisão do gate 3.1.
- [x] T-6.2 Specs `financial_plans`, `budget_groups`. FR-3, FR-5. Teste: `PlanningSpecTest`.
- [x] T-6.3 Specs `goals`, `wishlist_items`, `wishlist_item_links` (vínculo com transação/dívida via `ref`). FR-10. Teste: `GoalWishlistSpecTest`.
- [x] T-6.4 Round-trip de planejamento. NFR-5. Teste: `PlanningRoundTripTest`.

### UNIT-7 Fechamento
- [x] T-7.1 Modelo vazio (cabeçalhos de todas as entidades) em XLSX e CSV-zip + botão. FR-15, US-10. Teste: `TemplateExportTest`.
- [x] T-7.2 Round-trip de todas as entidades juntas em H2, importando duas vezes (segunda = zero novas). NFR-5, US-8. Teste: `FullRoundTripTest`.
- [x] T-7.3 Desempenho: 50 mil linhas em até 30 s e memória estável. NFR-4. Teste: `LargeFileBenchmarkTest` (limite folgado, sem flakiness).
- [x] T-7.4 Revisão de segurança do caminho de arquivo (zip slip, XML externo/XXE no XLSX, fórmulas, limite). NFR-3. Teste: `ArchiveSecurityTest`.
- [x] T-7.5 Atualizar `CLAUDE.md` (resumo curto do pacote `dataexchange`) e o codekb. Teste: revisão do gate 3.6.

**Resumo dos UNITs 3 a 7 (verificados em 2026-11-13)**
- UNIT-3: `dataexchange/ui/{DataExchangeText,ExportViewModel,ImportViewModel,ExportDialog,ImportDialog,DataExchangeDialogs}.kt`, seção Dados em `settings.fxml`/`SettingsController`, 95 chaves i18n por idioma. Testes: `ExportViewModelTest`, `ImportViewModelTest`. A captura de tela só cobre a tela de Configurações (dark e light em `construction/screenshots/`); os diálogos em si não foram capturados.
- UNIT-4: `dataexchange/creditcard/CreditCardEntitySpecs.kt` (6 specs); `CreditCardService.createDebt(..., enforceCreditLimit = true)`. Teste: `CreditCardsRoundTripTest` (fatura paga com rebate, parcelas, recorrência, crédito, saldos).
- UNIT-5: `dataexchange/investment/InvestmentEntitySpecs.kt` (8 specs + `TransactionLinker`). Teste: `InvestmentsRoundTripTest` (compras, venda, dividendo, câmbio de cripto, título e meta).
- UNIT-6: `dataexchange/planning/PlanningEntitySpecs.kt` (4 specs), meta como `kind = GOAL` em `wallets`, gancho `EntitySpec.prepare`. Teste: `PlanningRoundTripTest` (plano com grupos, meta sob carteira mestre, wishlist com vínculo de transação e links).
- UNIT-7: `ExportService.exportTemplate`, botão do modelo, `CatalogCompletenessTest`, `LargeFileBenchmarkTest`, `ArchiveSecurityTest`; `CLAUDE.md` e codekb atualizados.
- Desvios: os testes por spec (T-4.2..T-6.3) viraram um teste de ida e volta por grupo, que cobre apply, chaves e referências com serviços e H2 reais; `transfers` sem `ref`; o `ImportExecutor` inclui a mensagem da causa no erro de execução.

### Assumptions & open questions
- A13 (2.9): autonomia da Construction (gated vs autonomous) é decidida no início do 3.x. — confirmed: no
