# Requirements — 261102-linux-package

## Original request
Pergunta do usuário: "Quero facilitar a instalação para linux. Consigo fazer algo semelhante ao que foi feito no windows, empacotando tudo?"
Decisão do usuário, depois da análise das opções A/B/C: "Sim, vamos seguir com a opção B com ai-dlc"

Contexto da opção B (proposta do assistente e aceita): pacote Linux com JRE embutido (jpackage) e Python embutido com as dependências (yfinance, requests, pandas, numpy), sem depender do Python da distro; gerado pelo CI e anexado à GitHub Release, ao lado do `.exe`. Formatos a definir (`.deb` e/ou `tar.gz` portátil).

## 1.1 Intent & stakeholders
Facilitar a instalação do MOINEX no Linux com um pacote que traga tudo (JRE e Python com as dependências), no mesmo espírito do instalador do Windows, gerado pelo CI e anexado à GitHub Release. Stakeholder: o dono/mantenedor e usuários Linux da app. [src: pedido original]

## 1.4 Scope
- **Dentro:** pacote `.deb` e `tar.gz` portátil para Linux x86_64, com JRE (jpackage) e Python 3.12 embutidos com as dependências de `requirements.txt`; app usa o Python embutido quando roda do pacote; workflow do GitHub Actions que gera e anexa os dois à Release; documentação curta de instalação e de atualização.
- **Fora (backlog):** `.rpm`, AppImage, arm64, repositório apt/PPA, auto-atualização, assinatura do pacote, mudança no instalador do Windows.
- **Mantido:** `make install`/`install.sh` como instalação a partir do código. [src: Q formatos, Q arquitetura, Q install.sh]

## 2.3 Requirements
### Functional

| ID | Requirement | Rationale | Acceptance criteria | Priority | Source |
|---|---|---|---|---|---|
| FR-1 | O CI gera um pacote `.deb` x86_64 do MOINEX com JRE embutido. | Instalação em um comando em Debian/Ubuntu e derivadas. | Given um runner Linux, When o workflow roda, Then existe `moinex_<versão>_amd64.deb` (nome gerado pelo jpackage) que `dpkg-deb --info` lê. | Must | [src: Q formatos] |
| FR-2 | O CI gera um `tar.gz` portátil (app-image) x86_64 com JRE embutido. | Funciona em qualquer distro, sem root. | Given o workflow, Then existe `moinex-<versão>-linux-x64.tar.gz` que, descompactado, tem um launcher executável. | Must | [src: Q formatos] |
| FR-3 | Os dois pacotes embutem Python 3.12 com `requests`, `yfinance`, `pandas` e `numpy` nas versões de `requirements.txt`, sem depender do Python do sistema. | Instalação sem `pip` nem `--break-system-packages`. | Given o pacote instalado em um ambiente sem `python3`, When o Python embutido importa as quatro bibliotecas, Then não há erro. | Must | [src: opção B] |
| FR-4 | Quando a app roda a partir do pacote (jpackage), no Linux ela usa o Python embutido; fora do pacote (`make install`, `bootRun`) continua usando `/usr/bin/python3`. | Não quebrar a instalação por código nem o desenvolvimento. | Given `jpackage.app-path` apontando para um pacote com Python embutido, Then `Constants.PYTHON_INTERPRETER` é o do pacote; Given sem a propriedade, Then é `/usr/bin/python3`. | Must | [src: opção B] |
| FR-5 | O `.deb` instala em `/opt`, cria entrada no menu de aplicativos com ícone e o launcher abre a app. | Experiência de app instalada. | Given o `.deb` instalado, Then existe o `.desktop` e o ícone, e o launcher inicia a janela principal. | Must | [inferred] |
| FR-6 | Instalar um `.deb` de versão maior sobre o anterior atualiza a app sem tocar em `~/.moinex` (banco e preferências). | Atualização segura, alinhada ao aviso de nova versão. | Given 2.22.0 instalada e um banco de teste, When instalo a 2.23.0, Then o banco continua intacto e a versão é a nova. | Must | [inferred] |
| FR-7 | Remover o `.deb` apaga o que foi instalado em `/opt` e o `.desktop`, e preserva `~/.moinex`. | Dados financeiros do usuário não podem ser apagados. | Given o `.deb` instalado, When removo, Then `/opt/moinex` some e `~/.moinex` permanece. | Must | [inferred] |
| FR-8 | Um workflow do GitHub Actions gera os dois pacotes em runner Linux no evento `release: created` (e `workflow_dispatch`) e anexa à Release; em `workflow_dispatch` publica só como artifact. | Mesmo gatilho do Windows. | Given uma Release criada, Then os dois arquivos aparecem nos assets. | Must | [src: build-windows-installer.yml] |
| FR-9 | O workflow valida o pacote antes de anexar: o Python embutido importa as bibliotecas e o `.deb` lista os arquivos esperados. | Evitar publicar pacote quebrado. | Given um pacote sem `pandas`, Then o job falha e nada é anexado. | Must | [inferred] |
| FR-10 | O README ou docs ganha instruções de instalação, atualização e remoção para `.deb`, `tar.gz` e `install.sh`, avisando para não misturar `install.sh` com o `.deb`. | Evitar duas instalações conflitantes. | Given a doc, Then os três caminhos estão descritos. | Should | [src: Q install.sh] |

### Non-functional

| ID | Category | Measurable target | Verification | Source |
|---|---|---|---|---|
| NFR-1 | Compatibilidade | O pacote roda no `ubuntu-latest` do CI e em Ubuntu LTS mais antiga suportada pelo runner (glibc do runner define o piso); o piso é documentado. | Smoke no runner + registro da glibc. | [inferred] |
| NFR-2 | Tamanho | Tamanho de cada pacote medido e registrado em `build-and-test.md`; sem meta rígida, alerta se passar de 600 MB. | Medição no build. | [inferred] |
| NFR-3 | Reprodutibilidade | Python e dependências vêm de fonte fixa (versão do Python e `requirements.txt` com versões pinadas; checksum do tarball do Python verificado). | Revisão do script e falha do build em checksum diferente. | [inferred] |
| NFR-4 | Segurança | Nada de segredo no pacote; Python obtido de origem oficial por HTTPS com checksum. | Revisão. | [inferred] |
| NFR-5 | Retrocompatibilidade | Testes do Windows e do desenvolvimento seguem passando; mudança em `Constants` coberta por teste. | `./gradlew ktlintCheck spotlessCheck test` sem display. | [inferred] |
| NFR-6 | Manutenibilidade | Um script de build (`scripts/build-linux-package.sh`) roda localmente e no CI com o mesmo comando. | Execução local e no CI. | [inferred] |

### Constraints · Out of scope · Glossary
- Constraints: JDK 25 para `jpackage`; JAR Linux compilado em Linux (nativos JavaFX); headers de arquivo novos (M-012); sem comentários `//` explicativos em Kotlin.
- Out of scope: ver 1.4.
- Glossary: *app-image* = diretório autocontido gerado pelo `jpackage --type app-image`; *Python embutido* = interpretador standalone empacotado com as bibliotecas.

### Assumptions & open questions
- A1 (design.md): `jpackage.app-path` e `--app-content` no Linux; a confirmar na Construction.
- A2: o `jpackage --type deb` funciona no runner com `fakeroot` e `dpkg`; a confirmar no CI.
- A3 confirmada no spike (2026-11-02): `pip install -r requirements.txt` no Python 3.12.16 standalone instalou `numpy 2.4.0`, `pandas 2.2.3`, `yfinance 1.7.0` e `requests 2.32.3` e os imports passaram.
- A4: o gatilho `release: created` basta; o artefato Linux e o `.exe` ficam na mesma Release.
