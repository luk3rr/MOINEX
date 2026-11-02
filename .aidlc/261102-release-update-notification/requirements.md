# Requirements — 261102-release-update-notification

## Original request
Gostaria de garantir que o usuário receberá uma notificação na app sempre que uma nova release for lançada no github. A notificação deve ter o link da versão mais recente para permitir que ele vá a página e faça o download do instalar para atualizar a app. A segunda opção seria alguma maneira de a app se atualizar mediante permissão do usuário.

## 1.1 Intent & stakeholders
Garantir que o usuário da MOINEX seja avisado dentro da app sempre que sair uma nova release no GitHub, com o link da página da versão mais recente para baixar o instalador. Stakeholder único: o dono/mantenedor, que também é usuário (Windows e Linux). [src: pedido original]

## 1.4 Scope
- **Dentro**: checagem da última release estável no GitHub; comparação com a versão instalada; notificação (toast + central) com botão para abrir a página da release; opção em Configurações para ligar/desligar; aviso uma vez por versão.
- **Fora (backlog)**: auto-atualização (baixar e executar o instalador). Registrada como segunda opção do pedido, adiada por decisão do dono. [src: Q escopo]
- **Fronteira do MVP**: o aviso com link completo, em pt-BR e en.

## 2.3 Requirements
### Functional

| ID | Requirement | Rationale | Acceptance criteria | Priority | Source |
|---|---|---|---|---|---|
| FR-1 | A app consulta a última release estável publicada de `luk3rr/MOINEX` no GitHub. | Descobrir que há versão nova. | Given a API responde 200 com `tag_name` `v2.22.0`, When a checagem roda, Then a versão remota lida é 2.22.0. | Must | [src: pedido] |
| FR-2 | A checagem roda na inicialização e a cada 24 h enquanto a app estiver aberta. | Avisar também quem deixa a app aberta por dias. | Given a app aberta, When passam 24 h desde a última checagem, Then uma nova checagem ocorre. | Must | [src: Q quando] |
| FR-3 | A versão remota é comparada à instalada por versão semântica (`vX.Y.Z`); só versão estritamente maior é "nova". | Evitar falso aviso em versão igual ou de desenvolvimento maior. | Given instalada 2.21.0, When remota é 2.21.0 ou 2.20.9, Then não há aviso; When remota é 2.22.0, Then há aviso. | Must | [src: pedido] |
| FR-4 | Ao detectar versão nova, a app envia uma notificação do tipo INFO (toast e central) com título e mensagem informando a nova versão. | Cumpre o pedido de aviso na app. | Given versão nova, When detectada, Then aparece toast e entrada não lida na central. | Must | [src: pedido] |
| FR-5 | A notificação traz o link da página da release e um botão "Abrir página" que abre o link no navegador padrão; o link é persistido junto com a notificação. | Permite ir à página e baixar o instalador. | Given a notificação de atualização, When o usuário clica em "Abrir página", Then o navegador abre a URL da release. | Must | [src: pedido, Q aviso] |
| FR-6 | Cada versão é notificada uma única vez; a última versão notificada é guardada. Versão ainda mais nova gera novo aviso. | Evitar insistência. | Given 2.22.0 já notificada, When nova checagem vê 2.22.0, Then nenhum aviso novo; When vê 2.23.0, Then avisa. | Must | [src: Q repetição] |
| FR-7 | Configurações tem uma opção "Verificar atualizações automaticamente", ligada por padrão; desligada, nenhuma chamada ao GitHub ocorre. | Privacidade e controle. | Given a opção desligada, When a app inicia, Then nenhuma requisição é feita. | Must | [src: Q opt-out] |
| FR-8 | Falhas (sem rede, limite de taxa, JSON inválido, versão ilegível) não interrompem a app nem geram notificação de erro ao usuário; são registradas em log DEBUG. | Checagem é conveniência, não pode atrapalhar. | Given sem rede, When a checagem roda, Then a app segue normal e nenhuma notificação aparece. | Must | [inferred] |
| FR-9 | Textos da notificação e da opção existem em pt-BR e en. | i18n do projeto. | Given idioma en ou pt-BR, Then textos nesse idioma. | Must | [src: CLAUDE.md] |

### Non-functional

| ID | Category | Measurable target | Verification | Source |
|---|---|---|---|---|
| NFR-1 | Performance/UX | A checagem é assíncrona; não bloqueia a thread FX nem atrasa a abertura da janela principal. | Teste do serviço com cliente HTTP falso + revisão de que a chamada roda fora da FX thread. | [inferred] |
| NFR-2 | Resiliência | Timeout de conexão e de requisição ≤ 10 s por chamada. | Teste unitário/inspeção da configuração do cliente. | [inferred] |
| NFR-3 | Privacidade/Segurança | Só `GET` à API pública sem token, sem enviar dados do usuário; a URL aberta deve começar com `https://github.com/luk3rr/MOINEX/`. | Teste com resposta contendo URL de outro domínio (não é aberta/notificada). | [inferred] |
| NFR-4 | Compatibilidade | Funciona em Windows e Linux; a migration Flyway nova roda em banco existente sem perda de dados. | Teste de migration em H2/SQLite e validação Hibernate `validate`. | [src: CLAUDE.md] |
| NFR-5 | Testabilidade | Cliente HTTP e relógio injetáveis; sem rede real nos testes. | Testes Kotest/MockK. | [inferred] |

### Constraints · Out of scope · Glossary
- Constraints: Spring Boot + JavaFX; `ddl-auto: validate` (schema por Flyway, próxima migration V036); sem comentários `//`; header padrão nos arquivos novos. [mem: M-012]
- Out of scope: auto-atualização; pré-lançamentos; checagem de assinatura do instalador; atualização em Linux por `make install`.
- Glossary: *release estável* = release publicada não rascunho e não pré-lançamento (retorno de `/releases/latest`).

### Assumptions & open questions
- A1 (design.md): repositório público, API responde sem token (verificado).
- A2: o limite de 60 requisições/h sem token da API do GitHub é suficiente (≈1 por inicialização + 1 por dia).

## 2.4 User stories
### US-1 Ser avisado de nova versão  (FR-1, FR-2, FR-3, FR-4, FR-6, FR-8, FR-9)  Persona: usuário da MOINEX
As a usuário I want ser avisado dentro da app quando sai uma release nova so that eu saiba que posso atualizar sem acompanhar o GitHub.
- AC1: Given instalada 2.21.0 e remota 2.22.0, When a app inicia, Then recebo um toast e uma notificação não lida.
- AC2: Given o mesmo aviso já enviado, When a app reinicia, Then não recebo outro para 2.22.0.
- AC3: Given sem internet, When a checagem roda, Then nada muda para mim.

### US-2 Ir à página da release  (FR-5)  Persona: usuário da MOINEX
As a usuário I want abrir a página da release a partir da notificação so that eu baixe o instalador.
- AC1: Given a notificação de atualização na central, When clico em "Abrir página", Then o navegador abre a página da release.
- AC2: Given uma notificação comum (sem link), Then não há botão.

### US-3 Controlar a checagem  (FR-7)  Persona: usuário da MOINEX
As a usuário I want desligar a verificação automática so that a app não acesse a internet sem eu querer.
- AC1: Given a opção desligada, When a app inicia, Then nenhuma requisição ao GitHub é feita.
- AC2: Given instalação nova, Then a opção está ligada.
