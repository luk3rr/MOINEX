# Tasks — ticker-refresh-action

Verification command: `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && ./gradlew ktlintCheck spotlessCheck test`

- [x] T-1 Repositório: `findAllByTickerId`, `deleteAllByTickerId` (FR-6)
- [x] T-2 Serviço: `TickerPriceHistoryService.rebuildPriceHistory` com restauração em falha (FR-2, FR-6)
- [x] T-3 UI: ações de menu "Atualizar dados" / "Resetar e buscar novamente", confirmação, bloqueio de concorrência, notificações (FR-1, FR-3, FR-4, FR-5)
- [x] T-4 i18n en/pt-BR (só chaves acrescentadas) e `TranslationKeys` (NFR-2)
- [x] T-5 Teste de serviço do reset (sucesso e falha)
- [x] T-6 Validação visual do dono (aprovada 2026-11-04)
