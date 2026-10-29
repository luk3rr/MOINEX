# CI Pipeline — java-25-upgrade (3.7)

Sem novo pipeline: os existentes foram atualizados para Java 25.

| Arquivo | Gatilho | Mudança | Verificado |
|---|---|---|---|
| `.github/workflows/pipeline.yml` | push e PR para `main` | `Set up JDK 25`, Temurin `25`; passos: `gradlew clean build -x test`, `spotlessCheck ktlintCheck`, `test jacocoTestReport`, SonarCloud | Não executado (precisa de PR para `main`) |
| `.github/workflows/build-windows-installer.yml` | release criada ou `workflow_dispatch` | Temurin `25`, `cache: gradle`; `jpackage` do JDK 25 | Não executado |
| `scripts/build-windows-installer.bat` | chamado pelo workflow | exige Java 25+ (`LSS 25`) | Revisão de texto |
| `.gitlab-ci.yml` (legado) | n/a | imagem `eclipse-temurin:25-jdk` | Revisão de texto |

**Caches:** `pipeline.yml` usa `actions/cache@v3` para pacotes Gradle (inalterado); o workflow do instalador usa `cache: 'gradle'` do `setup-java`.

**Riscos para o primeiro run no GitHub:** o Gradle 9.8.2 baixa o wrapper no runner; o Sonar scanner e o plugin do JaCoCo não foram exercitados com Java 25; o `jpackage` do JDK 25 no Windows e o Python embutido só serão validados no workflow.

**Ações externas pendentes (T-4.5):** push da branch, PR para `main` e disparo do instalador (`workflow_dispatch`). O usuário respondeu "Nada agora" em 2026-10-29; cada uma pede um "sim" na hora [mem: M-005].
