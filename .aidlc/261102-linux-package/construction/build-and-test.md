# Build and Test — 261102-linux-package

## Comando aprovado

`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && env -u DISPLAY -u WAYLAND_DISPLAY ./gradlew ktlintCheck spotlessCheck test` → exit 0, 1582 testes, 0 falhas (2026-11-02). Sem display, como o runner do CI.

## Verificações além do Gradle

| O quê | Resultado |
|---|---|
| `scripts/setup-python-linux.sh` do zero | OK: 266 MB, 17 s; segunda execução reaproveita; arquivo adulterado → exit 1 e arquivo apagado |
| Seis scripts Python do projeto compilados com o Python embutido | OK |
| `scripts/build-linux-package.sh all` | exit 0 em 65 s: `moinex-2.22.0-linux-x64.tar.gz` 257 MB, `moinex_2.22.0_amd64.deb` 221 MB (617 MB instalado) |
| `scripts/validate-linux-package.sh` | exit 0: conteúdo do `.deb`, nada fora de `/opt`, imports do Python nos dois pacotes, app sobe sob Xvfb por 30 s |
| App extraída do `.deb` usa o Python do pacote | OK: `APIUtils` executa `<pacote>/opt/moinex/lib/python/bin/python3` |
| Scripts de manutenção do `.deb` | só `xdg-desktop-menu` sobre o `.desktop` em `/opt`; nada toca `~/.moinex` |

## Testes novos

`EmbeddedPythonTest` (6 casos). Os scripts de shell não têm testes automatizados próprios; são exercitados pela execução real e pela validação.

## Matriz requisito → verificação

| Requisito | Verificação |
|---|---|
| FR-1 | `build-linux-package.sh`, `dpkg-deb -I`, validate |
| FR-2 | `build-linux-package.sh`, validate (launcher e Python do tar.gz) |
| FR-3 | setup (imports) e validate |
| FR-4 | EmbeddedPythonTest + launch real (log do `APIUtils`) |
| FR-5 | `.desktop` e ícone no `.deb`; app sobe sob Xvfb; entrada de menu real não testada |
| FR-6 | **não exercitado** com `dpkg -i`; payload só em `/opt` e scripts sem `~/.moinex` (estático). Roteiro manual em `tasks.md` |
| FR-7 | idem FR-6 |
| FR-8 | workflow escrito e com YAML válido; **não executado** no GitHub |
| FR-9 | `validate-linux-package.sh` (no workflow); sem teste negativo com pacote quebrado |
| FR-10 | README revisado |
| NFR-1 | piso glibc 2.35 pelo runner `ubuntu-22.04`; **não medido** (build local usa glibc 2.43) |
| NFR-2 | medido: 221 MB e 257 MB (abaixo de 600 MB) |
| NFR-3 | versão e SHA256 fixados no script |
| NFR-4 | HTTPS e checksum; nenhum segredo |
| NFR-5 | comando aprovado verde sem display |
| NFR-6 | mesmo script local e no CI |

## Riscos abertos

- O workflow nunca rodou no GitHub: apt, `jpackage --type deb` e a glibc do runner são premissas.
- Um pacote gerado localmente (glibc 2.43) pode não abrir em Ubuntu 22.04; só o build do CI (22.04) responde isso.
- FR-6/FR-7 precisam do roteiro manual com sudo.
- `.deb` depende de GTK3 (`libgtk-3-0 | libgtk-3-0t64`) declarado à mão; novas libs nativas do JavaFX podem exigir ajuste.
- O Yahoo limitou o IP de teste (429); não prova o caminho de rede ponta a ponta em condições normais.
