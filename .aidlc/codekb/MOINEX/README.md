# Code KB — MOINEX
- Repo path: raiz do repositório   Remote: github.com/luk3rr/MOINEX
- Analysed commit: 95b19ba7 (2026-11-02, v2.21.0; inclui a feature f426f361)   Branch: main
- Depth: Comprehensive (todas as áreas)   Scope: src/main, src/test, resources, CI; exclui build/, .gradle/, .aidlc/
- Last full pass: 2026-10-29   Last delta: 2026-11-07 (áreas ai-chat-history e ai-agent, data-model)   Reviewed by: user (2026-11-02)

| File | Status | Confidence | Notes |
|---|---|---|---|
| overview.md | human-reviewed (2026-11-01) | medium | |
| architecture.md | human-reviewed (2026-11-01) | medium-high | |
| code-structure.md | human-reviewed (2026-11-02) | high | |
| technology-stack.md | human-reviewed (2026-11-01) | high | JFoenix removido; versões não reconferidas além de AtlantaFX e Ikonli |
| data-model.md | human-reviewed (2026-11-02); atualização 2026-11-07 em draft | medium | diagrama ER real em docs/database-erd.md (gerado das migrations) |
| api.md | human-reviewed (2026-11-02) | medium | |
| interactions.md | human-reviewed (2026-11-02) | medium-high | |
| conventions.md | human-reviewed (2026-11-01) | medium-high | |
| operations.md | human-reviewed (2026-11-02) | high | |
| quality-and-risks.md | human-reviewed (2026-11-02) | medium-high | baseline: 1553 testes, 0 falhas (2026-11-01) |
| areas/market-data.md | human-reviewed (2026-11-06) | alta no código, média nas estimativas | adicionado no intent 261106-yfinance-resilience; commit cb27418a |
| areas/ai-agent.md | human-reviewed (2026-11-04); atualização 2026-11-07 em draft | alta no código, média no comportamento do LLM | adicionado no intent 261104-ai-agent-evolution; commit edee9038 |
| areas/ai-chat-history.md | draft (2026-11-07) | alta no código, média no comportamento do modelo | adicionado no intent 261106-ai-chat-history |
| areas/financial-calculations.md | human-reviewed (2026-11-07) | alta no código, média nas suspeitas | adicionado no intent 261107-financial-calculations-review; HEAD 6c94ec03 |
