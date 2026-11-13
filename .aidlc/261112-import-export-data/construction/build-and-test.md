# Build and Test: import-export-data (2026-11-13)

## Comando e resultado
`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && env -u DISPLAY -u WAYLAND_DISPLAY ./gradlew ktlintCheck spotlessCheck test`: **exit 0, BUILD SUCCESSFUL, 2270 testes, 0 falhas** (baseline 2144; +126). A app também sobe com os novos beans (captura da tela de Configurações em dark e light, sem tocar em `~/.moinex`).

## Matriz requisito → teste
| Requisito | Teste |
|---|---|
| FR-1, FR-3, FR-4 | CellCodecTest, CsvZipRoundTripTest, XlsxRoundTripTest, ExportServiceTest, os quatro *RoundTripTest |
| FR-2, FR-12 | ExportViewModelTest; seção Dados conferida na captura de tela |
| FR-5, FR-11 | ImportPlannerTest, WalletSpecValidationTest, round-trips (referências por nome e por identidade) |
| FR-6 | FileGuardTest, ImportPlannerTest (E01 a E04, E07) |
| FR-7, FR-8 | ImportPlannerTest, ImportExecutorTest (rollback total), ImportViewModelTest |
| FR-9 | ImportExecutorTest e a segunda importação de cada round-trip (zero novas) |
| FR-10 | WalletBalanceCalculatorTest, WalletsRoundTripTest, CreditCardsRoundTripTest (fatura paga com rebate), InvestmentsRoundTripTest, PlanningRoundTripTest, WalletServiceBulkImportTest |
| FR-13, FR-14 | ImportViewModelTest (resultado, falha, estados); a UI assíncrona não tem teste automatizado |
| FR-15 | CatalogCompletenessTest (modelo vazio) |
| NFR-1 | NoNetworkTest; nenhuma notificação criada (WalletsRoundTripTest) |
| NFR-2 | CellCodecTest (valores de referência), WalletBalanceCalculatorTest (cálculo à mão), round-trips |
| NFR-3 | CellCodecFormulaTest, FileGuardTest, ArchiveSecurityTest |
| NFR-4 | LargeFileBenchmarkTest (50 mil linhas, XLSX e CSV, abaixo de 30 s; cerca de 7 s no primeiro formato) |
| NFR-5 | os quatro *RoundTripTest em XLSX e CSV |
| NFR-6 | CsvZipRoundTripTest (BOM, UTF-8), XlsxRoundTripTest |
| NFR-7 | ImportMessagesTest; temas conferidos só na tela de Configurações |
| NFR-8 | EntityCatalogTest, CatalogCompletenessTest |
| NFR-9 | todos os testes usam H2 em memória ou specs falsas |

## Lacunas registradas
- Os diálogos Exportar e Importar (JavaFX) não foram capturados nem exercitados por teste de tela; só a lógica (view models) e a seção nas Configurações foram vistas. Precisam do checklist manual abaixo.
- Não há teste com o SQLite real (os de integração usam H2); o schema real nunca recebeu o import.
- Sem verificação de dependências (CVE) com ferramenta; fastexcel e fastexcel-reader 0.20.2 vêm do Maven Central.
- Juros calculados de títulos não são exportados (recalculados pelo app).

## Checklist manual sugerido (M-003, com `make demo` e não com o banco real, M-006)
1. Configurações > Dados: os três botões, nos dois temas e nos dois idiomas.
2. Exportar: grupos e contagens, XLSX e CSV, cancelar durante a exportação, abrir o arquivo no Excel/LibreOffice.
3. Importar o arquivo exportado em um banco novo: pré-visualização (abas Problemas/Novas/Duplicadas), erro bloqueando o botão, resultado, segunda importação sem novos registros.
4. Importar um arquivo com erro de propósito (carteira inexistente) e um arquivo que não é do MOINEX.
5. Baixar modelo vazio.
