# Requirements — adopt-atlantafx

## Original request
Quero substituir JavaFX por AtlantaFX

## 1.1 Intent & stakeholders
## 1.4 Scope        (in / out / MVP boundary)
## 2.3 Requirements
### Functional
### Non-functional
### Constraints · Out of scope · Glossary
## 2.4 User stories

## 2.3 Requirements (draft — pausado, decisões do usuário até aqui)
Estas decisões foram dadas em 2026-10-29 e devem ser transformadas em FR/NFR quando o intent for retomado. [user]

- **Biblioteca de tema:** AtlantaFX **3.0.0** (a mais recente; exige Java 25+ e JavaFX 27+). A versão 2.1.0 (Java 21, JavaFX 17+) foi descartada. [src: repo1.maven.org atlantafx-base 3.0.0 / 2.1.0, class versions 69 e 65]
- **Pré-requisito:** atualização de plataforma (Java 21→25, JavaFX 23.0.1→27.0.1, Kotlin 2.1.0→2.3+, MockK, ktlint, jlink/instalador Windows, workflows de CI) como **intent separado, concluído antes**. [user]
- **Temas:** escuro = `Dracula` nativo do AtlantaFX com sobreposição de roxo `#BD93F9` e verde da sidebar; claro = `PrimerLight`. Identidade preservada: Dracula, botões em pílula, verde da sidebar (M-001). [user] [mem: M-001]
- **JFoenix:** removido; `JFXButton` → `Button` (81 FXML, 14 arquivos Kotlin). [src: .aidlc/codekb/MOINEX/technology-stack.md]
- **Cobertura da troca de tema:** a troca de claro/escuro atualiza também janelas já abertas, e os `Alert`/diálogos nativos passam a usar o tema. [user]
- **Ícones:** migrar os ícones de interface (common 48, sidebar 12, wallet_type 9 = 69 PNGs) para Ikonli; **ficam em PNG**: os 4 ícones do app, as 9 bandeiras de cartão e os 4.009 emojis do chat de IA. [user]
- **Redesenho:** todas as telas principais; o usuário marcou "Todas" (Home, Resumo anual, Carteiras, Cartões, Transações, Metas/Planos/FIRE, Wishlist, Investimentos e, por "Todas", também Chat de IA, Configurações e Notificações — confirmar no retorno). Objetivos: hierarquia visual e mais respiro; formulários e diálogos melhores; navegação e busca. Fora do foco: reduzir cliques nas tarefas frequentes. [user]
- **Não respondido ainda:** quais telas além das 8 listadas; definição mensurável de "melhor" (NFRs); acessibilidade/contraste; comportamento no tema claro da sidebar (A2).
