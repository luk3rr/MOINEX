# Verificação de fim de Construção — adopt-atlantafx

Data: 2026-10-30. Gate: unidade → código → testes → resultados, e cobertura de NFR.

| Critério | Resultado |
|---|---|
| Unidades UNIT-1 a UNIT-14 com código commitado | PASS (14/14; UNIT-12 e 13 com layout das telas revisado) |
| Verificação aprovada (`ktlintCheck spotlessCheck test`, e `clean build jacocoTestReport`) | PASS (1413 testes, 0 falhas) |
| Build e testes da solução (3.6) | PASS (`construction/build-and-test.md`) |
| NFR-5 (tempo de partida ≤ +20%) | PASS (8,17 s; limite 9,31 s) |
| NFR-4 (cores fora dos tokens) | PASS com exceções aprovadas (sombras e véu do overlay) |
| NFR-2 (contraste WCAG AA dos tokens) | PASS (`ThemeContrastTest`) |
| NFR-7 (i18n só por acréscimo) | PASS (diff das `.properties` só com linhas adicionadas) |
| FR-8 (ícones Ikonli) | PASS, com desvio: os logos de bandeira e a splash continuam PNG |
| Aprovação visual das telas | PENDENTE (decisão do usuário; capturas em `construction/screenshots/`) |
| Aprovação final de capturas e smoke manual (T-14.4, T-14.8) | PENDENTE |
| Verificação em execução de: Ctrl+S navegando até a ação, arraste do "+", menus de clique direito | PENDENTE (não automatizado) |

## Desvios registrados (todos em `design.md`, `tasks.md` e `audit.md`)

- Validação de formulário própria, sem `atlantafx-validation` (reserva do ADR-5).
- Ações por linha por clique direito e ações de tela no "+" (ADR-7), com o Ctrl+S listando as ações de todas as telas.
- Créditos de cartão: Editar e Excluir continuam vazios; vão para um novo intent.
- Início e Resumo anual: sem ações; mudança visual adiada.

## Conclusão

Construção concluída do ponto de vista de código e testes. Restam apenas as aprovações visuais e o smoke manual com você, além da verificação em execução das três interações listadas acima.
