# Build and Test — java-25-upgrade

Data: 2026-10-29 · Branch: `chore/java-25` · JDK: OpenJDK 25.0.4.1 · Gradle 9.8.2

## Comando de verificação aprovado
`JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./gradlew ktlintCheck spotlessCheck test` — exit 0 em todas as Units (UNIT-1 com JDK 21, as demais com JDK 25).

## Build completo (3.6)
`./gradlew clean build jacocoTestReport` em JDK 25: **BUILD SUCCESSFUL em 50s**, 16 tarefas executadas.

| Medida | Resultado |
|---|---|
| Testes | 1374, 0 falhas, 0 erros, 0 ignorados (baseline antes da mudança: 1374, 0 falhas) |
| Cobertura JaCoCo 0.8.16 | instruções 25,7% (35396/137918), linhas 25,5% (6673/26143), ramos 20,8% (1745/8406); sem baseline anterior para comparar |
| Avisos de compilação Kotlin | 20 (conversões redundantes, `!!` desnecessário, `CONSTRAINED_RESIZE_POLICY` deprecado); nenhum erro; código de produção não alterado por causa deles |
| Bytecode | major version 69 (Java 25) em `AppKt` do `moinex.jar` |

Estratégia de testes **Standard**: a base de testes unitários existente (service, model, ai) cobre a lógica; não há testes de UI (limitação já registrada na codekb). Não foram adicionados testes novos porque a mudança não altera comportamento; o risco de runtime foi coberto por smoke (abaixo).

## Smoke de runtime (automatizado por mim)
| Cenário | Resultado |
|---|---|
| Banco novo (`user.home` descartável), Boot 3.5 + JDK 25 | 34 migrations, janela "Moinex" aberta |
| Banco novo, Boot 4.1.2 | falhou 2 vezes antes dos ajustes (`no such table: category`; `missing table [bond]`); depois: 34 migrations, Hibernate 7.4.13 validou o schema, 15 categorias semeadas, janela aberta |
| **Cópia** do banco real, Boot 4.1.2 | Flyway validou 34 migrations ("up to date"), Hibernate validou, pipeline de inicialização completo, janela aberta, contagens iguais nas 43 tabelas; banco real intocado; cópia apagada |
| Repetição: 3 execuções seguidas | janela aberta e processo vivo nas 3 |

Observação aberta: em 2 de ~9 execuções iniciadas pelo shell da ferramenta o app encerrou sozinho em 5–16s sem erro no log (uma antes do Boot 4). Causa não identificada; não reproduzida nas 5 execuções seguintes.

## Matriz requisito → verificação
| Requisito | Evidência | Estado |
|---|---|---|
| FR-1 JDK 25 | `javap` major 69; build em JDK 25 | ✓ |
| FR-2 Gradle 9.x | `./gradlew --version` → 9.8.2 em JVM 25 | ✓ |
| FR-3 Kotlin 2.4.x | 4 plugins em 2.4.21; compila main e test | ✓ |
| FR-4 JavaFX 27.0.1 | catálogo + janela abre | ✓ |
| FR-5 Spring Boot 4.x | 4.1.2; contexto sobe; telas carregam (janela) | ✓ |
| FR-6 JPA/SQLite/Hibernate 7 | Hibernate 7.4.13, `SQLiteDialect`, validação num banco real copiado | ✓ |
| FR-7 Ferramentas de qualidade | ktlint (engine 1.4.1 + compilador pinado), Spotless, JaCoCo 0.8.16, MockK 1.14.11, Kotest 6.1.4 | ✓ |
| FR-8 Dependências sem uso | 5 libs e plugin jlink removidos; build e testes verdes | ✓ |
| FR-9 CI Temurin 25 | workflows editados e revisados; **não executados** (T-4.5 adiada por decisão do usuário) | parcial |
| FR-10 Docs e scripts | busca por Java 21 sem resultados fora de `gradlew`; `bash -n` OK | ✓ |
| NFR-1 Regressão | 1374 testes, 0 falhas | ✓ |
| NFR-2 Compat. de dados | contagens iguais em 43 tabelas; **saldos nas telas ainda a conferir pelo usuário** | parcial |
| NFR-3 Smoke manual | janela abre; **checklist manual do usuário (T-2.8/T-5.2) pendente** | pendente |
| NFR-4 Reversibilidade | branch `chore/java-25`; commits só com aprovação do usuário | ✓ |

## Notas de segurança
- Dependências atualizadas: Spring Boot 4.1.2, Hibernate 7.4.13, HikariCP 7.0.2, JavaFX 27.0.1, Kotlin 2.4.21 (todos versões estáveis atuais no Maven Central). Nenhuma varredura de vulnerabilidades foi executada.
- Removidas 5 dependências sem uso (menos superfície).
- Nenhum segredo lido ou escrito. A cópia do banco do usuário ficou só em pasta temporária e foi apagada.

## Falhas durante a construção
1. Gradle não encontrou o compilador do JDK 25 (só o JRE estava instalado); o usuário instalou `openjdk-25-jdk`.
2. ktlint 1.4.1 e 1.8.0 falhavam no JDK 25 com `Extensions storage is not registered` (compilador embutido forçado a 2.4.21); corrigido pinando o compilador por configuração. ktlint 1.8.0 descartado (regras novas violadas por código existente).
3. Boot 4: `data.sql` antes do Flyway e validação do Hibernate antes do Flyway; corrigido em `FlywayConfig`.
4. Hook de pré-commit falhou uma vez porque o `Makefile` apontava JDK 25 enquanto o estado da UNIT-1 ainda usava Gradle 8.14; commit refeito com `JAVA_HOME` do JDK 21.
