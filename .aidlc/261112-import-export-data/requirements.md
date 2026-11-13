# Requirements — import-export-data

## Original request
Quero poder importar dados e exportar dados do programa em formato csv/xlsx

## 2.3 Requirements Analysis

### Resumo
O usuário exporta seus dados do MOINEX para planilhas (CSV e XLSX) para analisar, guardar ou migrar, e reimporta arquivos no mesmo layout. O objetivo é backup/migração/carga em lote do layout próprio do app; ler layouts externos (CSV de banco) fica fora, pois a IA já lê extratos. [src: Q-objetivo] [src: ai/ingest/CsvStatementParser.kt]

### Requisitos funcionais
| ID | Requisito | Critério de aceite | Prioridade | Fonte |
|---|---|---|---|---|
| FR-1 | Exportar dados escolhidos pelo usuário em CSV ou XLSX | Dado o conjunto de entidades marcado e o formato, quando o usuário exporta, então é gerado o arquivo (XLSX: um arquivo com uma aba por entidade; CSV: um arquivo por entidade, reunidos num único .zip) no local escolhido | Must | [src: Q-formato] |
| FR-2 | Escolher quais dados exportar, agrupados por domínio | A tela lista os grupos Carteiras, Cartões, Investimentos e Planejamento (com suas entidades); só o marcado entra no arquivo; nenhum marcado desabilita o botão | Must | [src: Q-dados] |
| FR-3 | O arquivo exportado contém todas as linhas das entidades escolhidas e só dados do usuário | Contagem de linhas por entidade no arquivo = contagem no banco (exceto arquivadas, se houver regra); caches, mercado, snapshots, notificações, conversas e credenciais de IA nunca aparecem | Must | [src: Q-exclusões] |
| FR-4 | Formato de valores fixo e independente do idioma | Datas ISO, ponto decimal sem separador de milhar, enums pelo nome interno, cabeçalhos em inglês estáveis; no XLSX, datas e números são células tipadas; o mesmo arquivo exportado em pt-BR e em en reimporta igual | Must | [src: Q-valores] |
| FR-5 | Referências entre entidades por chave natural | Transação refere carteira e categoria por nome, compra refere ativo por símbolo, etc.; nenhum id interno aparece como referência; o arquivo reimporta num banco diferente | Must | [src: Q-referências] |
| FR-6 | Importar arquivo CSV (.csv/.zip) ou XLSX no layout do app | O usuário escolhe o arquivo; o app reconhece o formato pela extensão e pelo conteúdo e rejeita arquivo que não seja do layout com mensagem clara | Must | [src: Q-objetivo] |
| FR-7 | Pré-visualização antes de gravar | Após ler o arquivo o app mostra, por entidade, quantas linhas serão adicionadas, quantas são duplicadas (ignoradas) e os erros/avisos com entidade, linha e coluna; nada é gravado até o usuário confirmar | Must | [src: Q-validação] |
| FR-8 | Gravação atômica | Se qualquer linha tiver erro, o botão de confirmar fica bloqueado; ao confirmar, ou todas as linhas novas são gravadas ou nenhuma (falha na gravação desfaz tudo) | Must | [src: Q-validação] |
| FR-9 | Só adicionar; duplicatas são ignoradas | Linha cuja chave natural já existe no banco é pulada e contada como duplicada; importar o mesmo arquivo duas vezes não cria registros novos na segunda | Must | [src: Q-conflitos] |
| FR-10 | Importar preserva saldos e invariantes | Após importar transações, transferências e operações, o saldo das carteiras, as parcelas de cartão e a quantidade/preço médio dos ativos são iguais aos que as mesmas operações teriam se criadas pela tela | Must | [src: service/wallet/WalletService.kt:234] |
| FR-11 | Ordem de dependência e referência faltante | Importar numa só rodada carteiras, categorias e suas transações funciona; referência a nome que não existe nem no arquivo nem no banco é erro de linha na pré-visualização | Must | [src: Q-referências] |
| FR-12 | Ponto de acesso central nas Configurações | Seção "Dados" com Exportar e Importar, textos nos dois idiomas | Must | [src: Q-interface] |
| FR-13 | Relatório do resultado | Ao terminar export/import o app mostra o resumo (contagens por entidade, local do arquivo) e registra no log; falhas mostram a causa | Should | [inferred] |
| FR-14 | Operações longas não travam a interface | Export e leitura/gravação de import rodam fora da thread de UI, com indicador de progresso e botão de cancelar antes da gravação | Should | [inferred] [src: AppDispatchers] |
| FR-15 | Modelo/arquivo de exemplo | O app oferece exportar um arquivo vazio com os cabeçalhos de cada entidade para o usuário preencher | Could | [inferred] |

### Requisitos não funcionais
| ID | Requisito | Critério de aceite | Prioridade |
|---|---|---|---|
| NFR-1 | Privacidade: tudo local | Nenhuma chamada de rede em export/import; o arquivo só vai para o caminho que o usuário escolher; nenhum dado vai para o log além de contagens e nomes de entidade/coluna | Must |
| NFR-2 | Precisão monetária | Valores em BigDecimal ponta a ponta; export seguido de import devolve os mesmos valores, sem arredondar nem passar por Double; testes com valores de referência calculados à parte [mem: M-017] | Must |
| NFR-3 | Segurança de planilha | Texto que comece com `=`, `+`, `-`, `@` é neutralizado no export (injeção de fórmula) sem perder o valor ao reimportar; nenhuma fórmula é avaliada no import; XLSX/zip malformado, zip bomb e arquivo grande são rejeitados com limite de tamanho | Must |
| NFR-4 | Desempenho | Export e pré-visualização de 50 mil linhas no total em até 30 s numa máquina comum, sem estourar a memória padrão da JVM | Should |
| NFR-5 | Round-trip | Para cada entidade: exportar de um banco A e importar num banco vazio B reproduz as mesmas linhas (comparação campo a campo, chaves naturais incluídas) | Must |
| NFR-6 | Encoding e compatibilidade | CSV em UTF-8 com BOM e separador vírgula, abre no Excel com acentos corretos; XLSX abre no Excel e no LibreOffice | Must |
| NFR-7 | i18n e acessibilidade da UI | Textos em pt-BR e en pelas chaves de i18n (só acrescentar chaves); telas nos temas claro e escuro | Must |
| NFR-8 | Manutenção | Cada entidade se registra num catálogo único (nome, colunas, chave natural, leitura, escrita); acrescentar uma entidade não mexe no leitor/escritor de CSV/XLSX | Should |
| NFR-9 | Sem corromper o banco de uso real | Testes e validação visual rodam só com banco H2 de teste ou o home temporário do `make demo` [mem: M-006, M-011] | Must |

### Restrições
- App desktop Spring Boot + JavaFX, SQLite com 1 conexão; escrita via serviços de domínio. [src: CLAUDE.md]
- CSV usa a opencsv 5.9 existente; XLSX exige dependência nova (escolha em 2.6). [src: build.gradle.kts]
- Estilo: header de arquivo, sem comentários `//`, i18n só acrescentando chaves, mockups em HTML. [mem: M-007, M-012]

### Fora de escopo
- Ler layouts externos (CSV de banco/corretora) e mapeamento de colunas; a IA continua cuidando de extratos.
- Atualizar ou sobrescrever registros existentes (upsert) e restaurar com apagamento prévio.
- Caches e dados regeneráveis: histórico de preços, indicadores, análise fundamentalista, snapshots, notificações; conversas e credenciais de IA.
- Exportar a partir dos botões das telas de lista; relatórios formatados/gráficos; agendamento e nuvem.
- Migração de dados vindos de versões antigas do layout (só o layout atual v1).

### Glossário
- **Chave natural**: campo(s) que identificam a linha sem id interno (ex.: nome da carteira; símbolo do ativo; carteira + data + valor + descrição para transação).
- **Layout v1**: conjunto de entidades, colunas e tipos do arquivo exportado; versionado no arquivo.
- **Duplicata**: linha cuja chave natural já existe no banco.

### Assumptions & open questions
- A2 (2.3): a chave natural de entidades sem nome único (transação, compra, dívida de cartão, dividendo) é o conjunto de campos que as identifica (a definir em 2.8); linhas idênticas legítimas dentro do mesmo arquivo contam como distintas. — confirmed: no
- A3 (2.3): o layout leva um campo de versão (`layout_version=1`) para evoluir depois. — confirmed: no
- A4 (2.3): exportar inclui entidades arquivadas/inativas, e importar as recria como arquivadas. — confirmed: no
- A5 (2.3): FR-15 (modelo vazio) fica como Could e só entra se o plano couber. — confirmed: no

## 2.4 User Stories

Persona: **Dono** (único usuário do app; analisa as próprias finanças e migra entre máquinas). [src: memory M-004]

| ID | História | FRs | Aceite (Given/When/Then) |
|---|---|---|---|
| US-1 | Como dono, quero exportar meus dados para XLSX para abrir no Excel e analisar. | FR-1, FR-3, FR-4 | Given que tenho carteiras e transações, When exporto o grupo Carteiras em XLSX, Then recebo um .xlsx com uma aba por entidade, datas e valores tipados, e a contagem de linhas bate com o app |
| US-2 | Como dono, quero exportar em CSV para usar em outras ferramentas. | FR-1, FR-4, NFR-6 | Given a mesma seleção, When escolho CSV, Then recebo um .zip com um .csv por entidade em UTF-8 que abre no Excel com acentos corretos |
| US-3 | Como dono, quero escolher quais dados exportar. | FR-2, FR-12 | Given a seção Dados, When desmarco Investimentos, Then o arquivo não tem nada de investimentos; Given nada marcado, Then o botão Exportar fica desabilitado |
| US-4 | Como dono, quero importar um arquivo exportado pelo app num banco novo. | FR-5, FR-6, FR-10, FR-11, NFR-5 | Given um banco vazio e um arquivo exportado, When importo, Then carteiras, categorias e transações aparecem com os mesmos saldos do banco de origem |
| US-5 | Como dono, quero ver uma pré-visualização antes de gravar. | FR-7, FR-8 | Given um arquivo com 2 linhas inválidas, When ele é lido, Then vejo entidade, linha e coluna de cada erro e o botão Importar está bloqueado; Given um arquivo válido, Then vejo contagens a adicionar e duplicadas e nada foi gravado até confirmar |
| US-6 | Como dono, quero reimportar o mesmo arquivo sem duplicar dados. | FR-9 | Given que já importei o arquivo, When importo de novo, Then todas as linhas aparecem como duplicadas e nada novo é gravado |
| US-7 | Como dono, quero que o import mantenha meus saldos corretos. | FR-10 | Given transações de entrada e saída importadas, Then o saldo de cada carteira é o mesmo que o das mesmas operações criadas pela tela |
| US-8 | Como dono, quero levar também cartões, investimentos e planejamento. | FR-1, FR-3, FR-5, FR-10 | Given dívidas parceladas, compras/vendas/dividendos, títulos e metas, When exporto e importo num banco vazio, Then parcelas, quantidades, preços médios e metas ficam iguais |
| US-9 | Como dono, quero saber o resultado e acompanhar operações longas. | FR-13, FR-14 | Given um export grande, When rodo, Then vejo progresso sem a tela congelar e, ao final, o resumo com contagens e o local do arquivo |
| US-10 | Como dono, quero um arquivo modelo vazio para preencher à mão. | FR-15 | Given a seção Dados, When peço o modelo, Then recebo as abas/CSVs só com cabeçalhos |

Cobertura FR→US: FR-1 US-1/2/8 · FR-2 US-3 · FR-3 US-1/8 · FR-4 US-1/2 · FR-5 US-4/8 · FR-6 US-4 · FR-7 US-5 · FR-8 US-5 · FR-9 US-6 · FR-10 US-4/7/8 · FR-11 US-4 · FR-12 US-3 · FR-13 US-9 · FR-14 US-9 · FR-15 US-10. NFR-1/2/3 são verificadas em testes de Unit, não em história.

Estados de UX a cobrir (notas de design): vazio (sem dados no grupo), carregando, erro de arquivo (não é do layout / versão desconhecida / grande demais), sucesso, cancelado.

### Assumptions & open questions
- A6 (2.4): uma única persona; sem papéis. — confirmed: no
