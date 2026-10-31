# Build e testes — UNIT-1 a UNIT-14 (3.6)

Data: 2026-10-30. Comando: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./gradlew clean build jacocoTestReport`.

## Resultado

- Build: **BUILD SUCCESSFUL** (exit 0).
- Testes: **1413 executados, 0 falhas, 0 erros, 0 ignorados** (linha de base: 1374).
- Lint e formatação (`ktlintCheck`, `spotlessCheck`): sem violações.
- Cobertura (JaCoCo, relatório total): linhas 25,6% (6997/27305), instruções 25,5%, ramos 21,9%. A linha de base não registrou cobertura, então não há comparação direta.

## Cobertura dos requisitos por teste

- FR-12 (cores de série dos gráficos): `ThemeContrastTest` (série 00..39 e projeção ≥ 3:1 em ambos os temas).
- NFR-2 (contraste): `ThemeContrastTest`, `ThemeTokensTest`.
- NFR-4 (cores fora dos tokens): verificação estática em `construction/css-hex-exceptions.md` (exceções aprovadas: sombras e véu do overlay).
- FR-10 / US-15 (validação inline): `FieldValidationTest`, `DialogFormTest`.
- FR-11 / US-16 (busca global): `SearchMatcherTest`, `SearchActionCatalogTest` (23 ações de aplicativo, rótulos nos dois idiomas, ações de tela).
- FR-8 / US-5 (ícones): `IconsTest`, `IconMapperTest`.
- NFR-5 (tempo de partida): medido fora da suíte; média 8,17 s, limite 9,31 s (`construction/baseline.md`).
- Layout, clique direito, rodapé e "+" flutuante: sem testes automatizados; verificados por captura e medição de pixels (`construction/screenshots/`).

## Falhas e riscos

- Nenhuma falha de teste.
- Não há testes de interface automatizados; a verificação visual é manual, com capturas nos dois temas.
- Pendente de verificação em execução: escolher uma ação do Ctrl+S (navegação e execução), arrastar o "+" flutuante e abrir os menus de clique direito.
