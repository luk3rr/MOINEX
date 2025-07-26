# Especificação da Funcionalidade: Planeamento Financeiro (Orçamento por Grupos)
## Visão Geral e Motivação
### Resumo
Esta funcionalidade permite aos utilizadores criar um Plano Financeiro mensal baseado na sua renda. O utilizador pode definir "Grupos de Orçamento" (ex: "Despesas Essenciais", "Lazer", "Investimentos"), atribuir uma percentagem da sua renda a cada grupo e, o mais importante, associar as suas categorias de despesa existentes a estes grupos.

A aplicação irá então comparar, em tempo real, os gastos reais de cada grupo com a meta definida, mostrando o progresso e ajudando o utilizador a manter-se dentro do seu orçamento.

![](img/monthly_planning_interface.png)
*Rascunho da interface*

### Motivação
A gestão financeira eficaz vai para além do simples registo de transações; ela exige planeamento. Inspirado em métodos populares como o 50/30/20, este módulo dá aos utilizadores o poder de:

- Definir Intenções: Decidir para onde o seu dinheiro deve ir antes de o gastar.
- Obter Clareza: Entender se os seus padrões de gastos estão alinhados com os seus objetivos financeiros.
- Tomar Ações Corretivas: Identificar rapidamente áreas onde estão a gastar a mais e fazer ajustes.

## Conceitos Fundamentais
Plano Financeiro (FinancialPlan): A entidade principal. Um utilizador pode ter um ou mais planos. Cada plano contém:
- Nome: Ex: "Orçamento Mensal 2025".
- Renda Base Mensal: O valor total de rendimentos que serve de base para todos os cálculos percentuais.

Grupo de Orçamento (BudgetGroup): A espinha dorsal do plano. Cada grupo representa uma "fatia" do orçamento.
- Nome: Ex: "Despesas Essenciais", "Estilo de Vida", "Metas Financeiras".
- Percentagem Alvo: A percentagem da Renda Base que o utilizador pretende transacionar neste grupo (ex: 50%).
- Categorias Associadas: Uma lista de categorias de despesa (Category) que pertencem a este grupo. Esta é a ligação entre o planeamento e as transações reais.

## Fluxo do Utilizador
### Criação de um Novo Plano (Wizard)
- Passo 1: Iniciar: O utilizador clica em "Criar Novo Plano Financeiro".
- Passo 2: Renda: Insere a sua Renda Base Mensal (ex: $ 2.750,00).
- Passo 3: Modelo (Template): O sistema oferece modelos pré-definidos para facilitar:
  - Estratégia 50/30/20: (50% Essenciais, 30% Variáveis, 20% Investimentos)
  - Estratégia 30/30/40: (30% Essenciais, 30% Variáveis, 40% Investimentos)
  - Personalizado: O utilizador cria os seus próprios grupos e percentagens.
- Passo 4: Associar Categorias: Para cada Grupo de Orçamento criado (ex: "Despesas Essenciais"), o utilizador vê uma lista de todas as suas categorias de transações e seleciona as que pertencem a esse grupo (ex: "Aluguel", "Contas de Casa", "Supermercado"). Esta é a etapa mais crucial.

### Visualização e Acompanhamento (Dashboard)
Uma nova secção na UI irá mostrar o plano ativo para o mês corrente. Para cada Grupo de Orçamento, o utilizador verá:

- Nome do Grupo: "Despesas Essenciais"
- Meta: 50% ($ 1.375,00)
- Gasto Atual: A soma de todas as transações do mês cujas categorias estão associadas a este grupo (ex: $950,00).
- Progresso: Uma barra de progresso visual que mostra (Gasto Atual / Meta).
  - Se for um grupo relacionado a receita ou investimentos: A cor da barra muda consoante o progresso (vermelho -> amarelo -> verde).
  - Se for um grupo relacionado a despesas: A cor da barra muda consoante o progresso (verde -> amarelo -> vermelho).
- Disponível/Excedido: A diferença entre a meta e o gasto atual (ex: "Disponível: \$425,00", "Ainda falta: $425,00).

## Especificação Técnica e Adaptações no App
### Alterações no Modelo de Dados
Serão necessárias três novas tabelas na base de dados:

financial_plan
- id (PK)
- name (VARCHAR)
- base_income (NUMERIC)

budget_group
- id (PK)
- name (VARCHAR)
- target_percentage (INTEGER)
- plan_id (FK para financial_plan.id)

budget_group_categories (Tabela de Junção para a relação Many-to-Many)
- budget_group_id (FK para budget_group.id)
- category_id (FK para a sua tabela category.id existente)

### Lógica na Camada de Serviço (Service Layer)
FinancialPlanningService: Um novo serviço será criado para gerir toda a lógica.
- createPlan(name, income, groups): Método para criar o plano e os seus grupos.
- assignCategoryToGroup(groupId, categoryId): Método para criar a associação na tabela de junção.
- getPlanStatus(planId, month, year): Este será o método principal. Ele irá:
  - Obter o plano e os seus grupos.
  - Para cada grupo, obter a lista de IDs de categorias associadas.
  - Chamar o WalletTransactionRepository com uma consulta como: SELECT SUM(amount) FROM wallet_transaction WHERE category_id IN (...) AND strftime('%Y-%m', date) = 'YYYY-MM'.
  - Calcular o progresso e retornar um objeto com todos os dados para a UI.

### Melhorias e Ideias para Evolução
Esta funcionalidade tem um enorme potencial de crescimento. Como possível evolução, consideramos as seguintes melhorias:
- Análise de "Sobra/Falta": No final do mês, se um grupo não atingiu a meta (ex: sobrou dinheiro em "Lazer"), permitir que o utilizador "transfira" essa sobra para outro grupo (como "Investimentos") ou a "transporte" para o próximo mês.
- Alertas e Notificações: Notificar o utilizador quando ele atingir 80% ou 100% da meta de um grupo.
- Planeamento para Despesas Não-Mensais: Criar um tipo de grupo especial, como "Fundo de Provisionamento", onde o utilizador pode alocar uma percentagem mensal para despesas anuais (ex: IUC, seguros), e o saldo acumula de mês para mês.
- Sugestões Inteligentes: Após alguns meses de utilização, a aplicação poderia analisar os dados e sugerir: "Notamos que a sua categoria 'Restaurantes' está consistentemente a exceder o orçamento. Gostaria de lhe atribuir uma meta própria?"
- Gráficos Históricos: Mostrar a evolução dos gastos por grupo ao longo do tempo.
