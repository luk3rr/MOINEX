# CI — ai-agent-evolution

Nenhuma mudança. O `pipeline.yml` do GitHub Actions já roda `ktlintCheck`/`spotlessCheck` e `test` com Jacoco em push e PR para `main`; os testes novos entram nesse comando sem configuração extra. Nenhum teste novo exige rede, display ou banco real (usam mocks e fábricas com dados fictícios). Nada foi executado no CI: nada foi enviado ao remoto.
