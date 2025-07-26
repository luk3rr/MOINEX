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

#### 4.2.1 Gestão de Transações
##### 4.2.1.1 Operação em Carteira Virtual
###### 4.2.1.1.1 Receita
Em operações de receita:
1. o saldo da carteira virtual é incrementado
2. o saldo da carteira mestra é incrementado
3. A receita é registrada na carteira virtual

###### 4.2.1.1.2 Despesa
Em operações de despesa:
1. o saldo da carteira virtual é decrementado (se saldo suficiente)
2. o saldo da carteira mestra é atualizado
3. A despesa é registrada na carteira virtual

###### 4.2.1.1.3 Transferência
Em operações de transferência como origem:
1. o saldo da carteira virtual é decrementado (se saldo suficiente)
2. Se a transferência tiver como destino a própria carteira mestra, o saldo da carteira mestra não é alterado.
3. Se não for a própria carteira mestra, o saldo da carteira mestra é decrementado

Em operações de transferência como destino:
1. o saldo da carteira virtual é incrementado
2. Se a transferência tiver como origem a própria carteira mestra, o saldo da carteira mestra não é alterado.
3. Se não for a própria carteira mestra, o saldo da carteira mestra é incrementado

##### 4.2.1.2 Operação em Carteira Mestra
###### 4.2.1.2.1 Receita
Em operações de receita:
1. o saldo da carteira mestra é incrementado
2. o saldo de nenhuma carteira virtual é alterado

###### 4.2.1.2.2 Despesa
Em operações de despesa:
1. Se o valor da despesa for menor ou igual ao saldo não alocado da carteira mestra, o saldo da carteira
   mestra é decrementado.
2. Se o valor da despesa for maior que o saldo não alocado, a operação falha com erro de saldo insuficiente.
3. Nenhuma carteira virtual é alterada, pois a despesa não é registrada em nenhuma delas.

###### 4.2.1.2.3 Transferência
Em operações de transferência como origem:
1. Se o valor da transferência for maior que o saldo não alocado, a operação falha com erro de saldo insuficiente.
2. Se a transferência tiver como destino alguma de suas carteiras virtuais, o saldo da carteira mestra
   não é alterado.
3. Se a transferência tiver como destino uma carteira (mestra ou virtual sem vinculação com ela mesma), o saldo da carteira mestra
   é decrementado.

##### 4.2.1.3 Operação em Carteira Mestra com Saldo Não Alocado (sem vinculação a uma carteira virtual)
Nesse caso, as operações permanecem inalteradas.

#### 4.2.2 Cálculo do saldo total das carteiras
O saldo total das carteiras devem considerar somente as carteiras mestras, uma vez que as carteiras virtuais são
representações de fatias do saldo da carteira mestra. 

Nesse sentido, as funções de cálculo de saldo devem ser adaptadas para ignorar as carteiras virtuais e não distorcer o
total consolidado que é exibido nos gráficos e relatórios.

#### 4.2.3 Alteração de carteira mestra
Se uma carteira virtual for alterada para vincular a uma nova carteira mestra, o saldo da carteira virtual
deverá ser transferido para a nova carteira mestra. 

A fatia da carteira virtual na carteira mestra antiga será transferida para a nova carteira mestra,
e o saldo da carteira virtual não será alterado.

#### 4.2.4 Deleção de Carteiras 
#### 4.2.4.1 Deleção de Carteira Mestra
Se uma carteira mestra for deletada, todas as carteiras virtuais vinculadas a ela deverão ser desvinculadas.
Nesse caso, o campo masterWallet das carteiras virtuais será definido como null, tornando-as carteiras mestras independentes.

#### 4.2.4.2 Deleção de Carteira Virtual
Se uma carteira virtual for deletada, o saldo dela deve ser mantido na carteira mestra vinculada, uma vez que ela é
apenas uma representação de uma fatia do saldo da carteira mestra.

#### 4.2.5 Arquivamento de Carteiras
##### 4.2.5.1 Arquivamento de Carteira Mestra
Se uma carteira mestra for arquivada, todas as carteiras virtuais vinculadas a ela deverão ser desvinculadas.
Nesse caso, o campo masterWallet das carteiras virtuais será definido como null, tornando-as carteiras mestras independentes.

##### 4.2.5.2 Arquivamento de Carteira Virtual
Se uma carteira virtual for arquivada, o saldo dela deve ser mantido na carteira mestra vinculada, uma vez que ela é
apenas uma representação de uma fatia do saldo da carteira mestra.

No entanto, ela deve ser desvinculada da carteira mestra, definindo o campo masterWallet como null, para que a sua "fatia"
possa ser utilizada pela carteira mestra.

### 4.3. Impacto na Interface do Utilizador (UI)

#### 4.3.1 Form de Criação/Edição de Metas (Goal)
1. Deverá incluir um novo ComboBox opcional: "Vincular à Carteira Existente".
2. Este ComboBox será preenchido com as Wallets que podem ser selecionadas como Carteiras Mestras.
3. Se o utilizador não selecionar nenhuma, o sistema pode manter o comportamento antigo de criar uma carteira
   dedicada (que seria, ao mesmo tempo, mestra e virtual, com masterWallet = null).

#### 4.3.2 Home Page e Lista de Carteiras
A listagem de carteiras agora deve exibir as Carteiras Virtuais com indicação de que são "fatia" de uma Carteira Mestra.

#### 4.3.3 Wallet Page
A listagem de carteiras agora deve exibir as Carteiras Virtuais com indicação de que são "fatia" de uma Carteira Mestra.

### 4.4. Lista de Tarefas
Considerando todos os pontos acima, a lista de tarefas para implementação é a descrita abaixo.

#### 4.4.1 Alterações no Modelo de Dados
- [X] Adicionar o campo masterWallet na entidade Wallet.
- [X] Atualizar o esquema do banco de dados para incluir a coluna master_wallet_id na tabela wallet. As
  migrações devem garantir que as Wallets existentes serão master por default, isto é, terão masterWallet = null.
- [ ] Atualizar o modelo de dados para refletir a relação entre Wallet e Master Wallet.

#### 4.4.2 Lógica de Negócio
##### 4.4.2.1 Gestão de Transações
- [X] Implementar a lógica de receita para Carteiras Virtuais
- [X] Implementar a lógica de receita para Carteiras Mestras com vinculação a Carteiras Virtuais
- [X] Implementar a lógica de despesa para Carteiras Virtuais
- [X] Implementar a lógica de despesa para Carteiras Mestras com vinculação a Carteiras Virtuais
- [X] Implementar a lógica de transferência para Carteiras Virtuais
- [X] Implementar a lógica de transferência para Carteiras Mestras com vinculação a Carteiras Virtuais

##### 4.4.2.2 Cálculo de Saldo
- [X] Atualizar as funções de cálculo de balanço total para considerar apenas Carteiras Mestras nos resultados dos gráficos

#### 4.4.3 Interface
- [X] Atualizar o formulário de criação de Metas para incluir o ComboBox de vinculação a Carteiras Mestras.
- [X] Atualizar o formulário de edição de Metas para incluir o ComboBox de vinculação a Carteiras Mestras.
- [x] Atualizar a Home Page para exibir Carteiras Virtuais com indicação de "fatia" de uma Carteira Mestra.
- [x] Atualizar a Wallet Page para exibir Carteiras Virtuais com indicação de "fatia" de uma Carteira Mestra.

#### 4.4.4 Deleção de Carteiras
- [X] Implementar a lógica de deleção de Carteiras Mestras, garantindo que as Carteiras Virtuais vinculadas sejam
  desvinculadas corretamente.
- [X] Implementar a lógica de deleção de Carteiras Virtuais, garantindo que o saldo seja mantido na Carteira Mestra
  vinculada.

#### 4.4.5 Arquivamento de Carteiras
- [ ] Implementar a lógica de arquivamento de Carteiras Mestras, garantindo que as Carteiras Virtuais vinculadas sejam
  desvinculadas corretamente.
- [ ] Implementar a lógica de arquivamento de Carteiras Virtuais, garantindo que o saldo seja mantido na Carteira Mestra
  vinculada.

## 5. Riscos e Considerações

- Complexidade Lógica: A camada de serviço torna-se o ponto central e mais complexo do sistema. Todos os fluxos de
  transação devem ser encaminhados por ela e testados exaustivamente para evitar inconsistências de saldo.
- Migração de Dados: Para as Goals já existentes na base de dados, será necessário criar um script de migração. A
  abordagem mais segura é, para cada Goal antiga, manter o seu comportamento atual, onde ela mesma é a sua própria
  carteira mestra (master_wallet_id = null). Novas Goals poderão usar a nova funcionalidade.
- Performance: A lógica de cálculo do "saldo não alocado" exige uma consulta extra para somar os saldos das goals
  vinculadas. Embora seja mais performático que outras abordagens, isto deve ser monitorizado.
