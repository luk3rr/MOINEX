# Especificação da Funcionalidade: Metas com Saldo Compartilhado
## 1. Resumo e Motivação

### 1.1. Resumo

Esta funcionalidade introduz um novo paradigma para a gestão de Metas (Goal) e Carteiras (Wallet) no Moinex. O objetivo
é dissociar a existência de uma meta da criação de uma nova carteira dedicada, permitindo que múltiplas metas partilhem
o saldo de uma única "Carteira Mestra". Isto permite que uma Goal atue como uma "Carteira Virtual", representando uma
fatia de um fundo maior, ao mesmo tempo que se comporta como uma carteira independente em operações como transferências.

### 1.2. Motivação

Atualmente, o sistema impõe uma relação 1-para-1 entre uma Goal e uma Wallet, o que não reflete a realidade de muitos
utilizadores. Frequentemente, uma pessoa utiliza um único instrumento financeiro (um CDB, uma conta poupança, um fundo
de investimento) para guardar dinheiro para múltiplos objetivos.

A motivação principal é aumentar a flexibilidade e o alinhamento do Moinex com o comportamento do mundo real, permitindo
que os utilizadores:

- Otimizem os seus investimentos: Concentrando o dinheiro para várias metas num único investimento que pode ter melhor
  rentabilidade.
- Simplifiquem a gestão: Evitando a necessidade de criar dezenas de contas ou investimentos reais para cada pequeno
  objetivo.
- Organizem-se melhor: Mantendo a separação lógica e o acompanhamento individual para cada meta, mesmo que o dinheiro
  esteja fisicamente consolidado.

## 2. Conceitos Fundamentais

Para implementar esta funcionalidade, introduzimos três conceitos interligados:

- Carteira Mestra (Master Wallet): É a representação de um "fundo" de dinheiro real e consolidado (ex: "Conta da
  Corretora XP", "Tesouro Selic 2029"). Armazena o saldo total e não aponta para nenhuma outra carteira. No modelo de
  dados, o seu campo masterWallet será null.
- Carteira Virtual (Virtual Wallet): É um rótulo organizacional que representa uma "fatia" do saldo de uma Carteira
  Mestra. Não existe como uma entidade financeira separada no mundo real. No sistema, uma Goal é o principal exemplo de
  uma Carteira Virtual. No modelo de dados, o seu campo masterWallet apontará para a Carteira Mestra correspondente.
- Saldo Partilhado (Shared Balance): É o princípio de que o saldo da Carteira Mestra é a soma de todas as suas fatias (
  os saldos das Carteiras Virtuais vinculadas) mais qualquer valor que ainda não tenha sido alocado.

## 3. Casos de Uso

1. Múltiplas Metas de Poupança: Como utilizador, quero poupar para uma "Viagem à Europa" e para um "Macbook Novo" ao
   mesmo tempo. Quero que o dinheiro de ambas as metas fique no meu único investimento "CDB de Liquidez Diária" para
   simplificar, mas quero ver o progresso de cada meta separadamente no Moinex.
2. Orçamento por Envelopes: Como utilizador, quero alocar o meu salário que cai na "Conta à Ordem" em diferentes
   categorias de gastos (ex: "Alimentação", "Lazer", "Contas Fixas") sem ter de mover o dinheiro para contas diferentes.
   Quero que cada categoria seja uma carteira virtual para poder registar despesas e saber quanto ainda tenho disponível
   em cada "envelope".
3. Provisionamento de Despesas Anuais: Como utilizador, quero guardar 150,00 € todos os meses para pagar o "IUC" no
   final do ano. Quero que esse dinheiro se acumule na minha "Conta Poupança", mas que seja acompanhado separadamente
   para que não o gaste por engano.

## 4. Especificação Técnica e de Implementação

A implementação será baseada no modelo de herança existente (Goal extends Wallet), com a introdução de uma relação de "
carteira mestra".

### 4.1. Alterações no Modelo de Dados (Schema)

Na entidade Wallet, será adicionado um novo campo autorreferencial:

```java
// Em Wallet.java
@Entity
public class Wallet {
// ... campos existentes ...

    /**
     * Link opcional para uma carteira "mestra".
     * Se nulo, esta é uma Carteira Mestra.
     * Se preenchido, esta é uma Carteira Virtual.
     */
    @ManyToOne
    @JoinColumn(name = "master_wallet_id", nullable = true)
    private Wallet masterWallet;

    // ...

}
```

Isto resultará na adição de uma coluna master_wallet_id na tabela wallet, que será uma chave estrangeira para wallet.id.

### 4.2. Lógica de Negócio na Camada de Serviço

Toda a complexidade será abstraída na camada de serviço para garantir consistência.
Significado do Campo balance:

- Se masterWallet == null, balance é o saldo total e real da carteira.
- Se masterWallet != null, balance é a "fatia" pertencente àquela carteira virtual.

Gestão de Transações (Abordagem Restrita):

1. Operação em Carteira Virtual (Goal):
    1. Aporte (Entrada): O balance da Goal e o balance da sua masterWallet são ambos incrementados pelo valor do aporte.
    2. Resgate (Saída): Após validar que a Goal tem saldo suficiente, o balance da Goal e o balance da sua masterWallet
       são ambos decrementados.
    3. Transferência: Uma Goal pode ser remetente ou destinatária. A lógica segue a de resgate (para remetente) ou
       aporte (para destinatária).

2. Operação em Carteira Mestra:
    1. Aporte (Entrada): Apenas o balance da masterWallet é incrementado. Este valor aumenta o "saldo não alocado".
    2. Despesa/Transferência (Saída):
    3. Cálculo Crítico: O serviço deve primeiro calcular o saldo não alocado da carteira mestra: saldoNaoAlocado =
       masterWallet.balance - (soma de todos os balances das Goals vinculadas)
    4. Validação: A despesa ou transferência (valorSaida) é comparada com o saldoNaoAlocado.
    5. Se valorSaida <= saldoNaoAlocado: A operação é permitida. Apenas o balance da masterWallet é decrementado.
    6. Se valorSaida > saldoNaoAlocado: A operação é bloqueada e uma mensagem clara é exibida ao utilizador, explicando
       que o saldo livre é insuficiente e que ele precisa primeiro de fazer um resgate de uma das suas metas.

### 4.3. Impacto na Interface do Utilizador (UI)

1. Ecrã de Criação/Edição de Metas (Goal):
    1. Deverá incluir um novo ComboBox opcional: "Vincular à Carteira Existente".
    2. Este ComboBox será preenchido com as Wallets do tipo "Savings" (ou outros tipos permitidos).
    3. Se o utilizador não selecionar nenhuma, o sistema pode manter o comportamento antigo de criar uma carteira
       dedicada (que seria, ao mesmo tempo, mestra e virtual, com masterWallet = null).

2. Ecrã de Visualização de Carteiras:
    1. A lista de carteiras deve diferenciar visualmente as Carteiras Mestra das Virtuais (Goals). Uma sugestão é usar
       indentação, mostrando as Goals como "filhas" da sua masterWallet.
    2. O saldo exibido para cada item deve respeitar a lógica (fatia vs. total).

3. Ecrã de Detalhes da Carteira Mestra:
    1. Além de mostrar o seu saldo total, este ecrã deve listar as Carteiras Virtuais (Goals) vinculadas a ela e o valor
       das suas respetivas "fatias".

## 5. Riscos e Considerações

- Complexidade Lógica: A camada de serviço torna-se o ponto central e mais complexo do sistema. Todos os fluxos de
  transação devem ser encaminhados por ela e testados exaustivamente para evitar inconsistências de saldo.
- Migração de Dados: Para as Goals já existentes na base de dados, será necessário criar um script de migração. A
  abordagem mais segura é, para cada Goal antiga, manter o seu comportamento atual, onde ela mesma é a sua própria
  carteira mestra (master_wallet_id = null). Novas Goals poderão usar a nova funcionalidade.
- Performance: A lógica de cálculo do "saldo não alocado" exige uma consulta extra para somar os saldos das goals
  vinculadas. Embora seja mais performático que outras abordagens, isto deve ser monitorizado.
