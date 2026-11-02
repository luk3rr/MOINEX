# Verificação — Construction (261102-linux-package)

## 1. Inventário

| Artefato | Presente |
|---|---|
| scripts/setup-python-linux.sh, build-linux-package.sh, validate-linux-package.sh | ✓ |
| common/util/EmbeddedPython.kt + EmbeddedPythonTest.kt, Constants.kt | ✓ |
| .github/workflows/build-linux-package.yml | ✓ |
| README.md (instalação e desinstalação) | ✓ |
| construction/build-and-test.md e ci-pipeline.md | ✓ |
| tasks.md com 15 de 15 tarefas marcadas | ✓ |

## 2. Rastreabilidade
Unit → código → verificação: ver a matriz em `construction/build-and-test.md`.

## 3. Consistência
Caminho `lib/python/bin/python3`, nome dos pacotes e `ubuntu-22.04` coincidem entre scripts, workflow, `EmbeddedPython`, testes e docs. FR-1 foi ajustado ao nome real do `.deb`.

## 4. Veredito: PASS com riscos aceitos
Lacunas registradas: workflow não executado no GitHub, FR-6/FR-7 só estáticos, compatibilidade de glibc não medida. Nada commitado; branch `feat/linux-package`.
