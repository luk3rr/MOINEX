# Build & Test — ticker-refresh-action

- Comando: `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && ./gradlew ktlintCheck spotlessCheck test`
- Resultado (2026-11-04): BUILD SUCCESSFUL, 1584 testes, 0 falhas.
- Novo teste: `TickerPriceHistoryServiceRebuildTest` (reset apaga antes de salvar; falha da API restaura o histórico anterior e relança o erro → FR-6).
- Não coberto por teste automático (sem testes de UI): FR-1..5 na tela. Checklist manual (M-003, com `make demo`, M-006):
  1. Aba Ações/Fundos: clique direito numa linha mostra "Atualizar dados" e "Resetar e buscar novamente".
  2. "Atualizar dados": botão do topo gira e desabilita; ao fim notificação de sucesso com o símbolo; tabela recarrega.
  3. "Resetar...": aparece confirmação; "Não" não altera nada; "Sim" refaz e notifica.
  4. Sem rede: ambas notificam erro com o símbolo; o histórico do ativo continua no detalhe.
  5. Tema claro e escuro; idiomas pt-BR e en.
