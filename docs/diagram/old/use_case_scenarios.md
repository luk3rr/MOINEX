### 1. Criar Carteira
- **Ator:** Usuário
- **Pré-condição:** Nenhuma
- **Fluxo normal:**
  1. O usuário seleciona a opção "Criar Carteira".
  2. O sistema solicita as informações da nova carteira (nome, tipo, saldo inicial, etc.).
  3. O usuário fornece as informações necessárias e confirma.
  4. O sistema salva a nova carteira no banco de dados.
- **Fluxos alternativos:**
  - **2.1:** Se o usuário não fornecer as informações obrigatórias, o sistema exibe uma mensagem de erro e solicita que ele preencha os dados novamente.
  - **2.2:** Se o saldo inicial for inválido (ex.: negativo), o sistema exibe uma mensagem de erro e solicita que o usuário forneça um valor válido.
  - **2.3:** Se o nome da carteira já existir, o sistema exibe uma mensagem de erro e solicita que o usuário forneça outro nome.
- **Pós-condição:**
  - Uma nova carteira é registrada no sistema e fica disponível para uso pelo usuário.

---

### 2. Cadastrar Cartão de Crédito
- **Ator:** Usuário
- **Pré-condição:** Nenhuma
- **Fluxo normal:**
  1. O usuário seleciona a opção "Cadastrar Cartão de Crédito".
  2. O sistema solicita os dados do cartão (nome, limite de crédito, dia de fechamento da fatura, dia de cobrança, etc)
  3. O usuário fornece os dados e confirma.
  4. O sistema salva o cartão de crédito no banco de dados.
- **Fluxos alternativos:**
  - **2.1:** Se o usuário não fornecer os dados obrigatórios, o sistema exibe uma mensagem de erro e solicita que ele preencha os dados novamente.
  - **2.2:** Se o nome do cartão já existir, o sistema exibe uma mensagem de erro e solicita que o usuário forneça outro nome.
  - **2.3:** Se o limite de crédito for inválido (ex.: negativo), o sistema exibe uma mensagem de erro e solicita que o usuário forneça um valor válido.
  - **2.4:** Se o dia de fechamento ou de cobrança for inválido (ex.: dia menor ou igual a 0 ou maior que 28), o sistema exibe uma mensagem de erro e solicita que o usuário forneça um valor válido.
- **Pós-condição:**
  - O cartão de crédito é registrado no sistema e fica disponível para registro de transações.

---

### 3. Criar Categoria de Transações
- **Ator:** Usuário
- **Pré-condição:** Nenhuma
- **Fluxo normal:**
  1. O usuário seleciona a opção "Criar Categoria de Transações".
  2. O sistema solicita o nome da categoria.
  3. O usuário fornece os dados e confirma.
  4. O sistema salva a nova categoria no banco de dados.
- **Fluxos alternativos:**
  - **3.1:** Caso o nome da categoria já exista, o sistema exibe uma mensagem de erro e solicita que o usuário forneça outro nome.
- **Pós-condição:**
  - A nova categoria é registrada no sistema e fica disponível para associação a transações.

---

### 4. Cadastrar Receita
- **Ator:** Usuário
- **Pré-condição:** O usuário deve possuir ao menos uma carteira cadastrada.
- **Fluxo normal:**
  1. O usuário seleciona a opção "Cadastrar Receita".
  2. O sistema solicita os dados da receita (valor, categoria, descrição, etc.).
  3. O usuário preenche os dados e confirma.
  4. O sistema registra a receita na carteira selecionada e atualiza o saldo.
- **Fluxos alternativos:**
  - **2.1:** Caso o valor da receita seja inválido (ex.: valor negativo), o sistema exibe uma mensagem de erro.
  - **2.2:** Caso o usuário não forneça os dados obrigatórios, o sistema exibe uma mensagem de erro e solicita que ele preencha os dados novamente.
- **Pós-condição:**
  - A receita é registrada na carteira e o saldo da mesma é atualizado.

---

### 5. Definir Receita como Recorrente (Extensão de "Cadastrar Receita")
- **Ator:** Usuário
- **Pré-condição:** O usuário deve cadastrar uma receita.
- **Fluxo normal:**
  1. Após cadastrar a receita, o usuário seleciona a opção "Definir Receita como Recorrente".
  2. O sistema solicita a periodicidade da recorrência (mensal, semanal, etc.).
  3. O usuário confirma.
  4. O sistema cria um registro de transação recorrente.
- **Pós-condição:**
  - A receita é marcada como recorrente, e o sistema agendará a criação automática dessa receita.

---

### 6. Cadastrar Despesa
- **Ator:** Usuário
- **Pré-condição:** O usuário deve possuir ao menos uma carteira cadastrada.
- **Fluxo normal:**
  1. O usuário seleciona a opção "Cadastrar Despesa".
  2. O sistema solicita os dados da despesa (valor, categoria, descrição, etc.).
  3. O usuário preenche os dados e confirma.
  4. O sistema registra a despesa na carteira selecionada e atualiza o saldo.
- **Fluxos alternativos:**
  - **2.1:** Caso o usuário não forneça os dados obrigatórios, o sistema exibe uma mensagem de erro e solicita que ele preencha os dados novamente.
  - **2.2:** Caso o valor da despesa seja inválido (ex.: valor negativo), o sistema exibe uma mensagem de erro.
- **Pós-condição:**
  - A despesa é registrada e o saldo da carteira é atualizado.

---

### 7. Definir Despesa como Recorrente (Extensão de "Cadastrar Despesa")
- **Ator:** Usuário
- **Pré-condição:** O usuário deve cadastrar uma despesa.
- **Fluxo normal:**
  1. Após cadastrar a despesa, o usuário seleciona a opção "Definir Despesa como Recorrente".
  2. O sistema solicita a periodicidade da recorrência (mensal, semanal, etc.).
  3. O usuário confirma.
  4. O sistema cria um registro de transação recorrente.
- **Pós-condição:**
  - A despesa é marcada como recorrente, e o sistema agendará a criação automática dessa despesa.

---

### 8. Cadastrar Transferência entre Carteiras
- **Ator:** Usuário
- **Pré-condição:** O usuário deve possuir saldo suficiente na carteira de origem.
- **Fluxo normal:**
  1. O usuário seleciona a opção "Cadastrar Transferência entre Carteiras".
  2. O sistema solicita os dados da transferência (carteira de origem, carteira de destino, valor, descrição, etc.).
  3. O usuário preenche os dados e confirma.
  4. O sistema debita o valor da carteira de origem e credita na carteira de destino.
- **Fluxos alternativos:**
  - **4.1:** Caso o saldo na carteira de origem seja insuficiente, o sistema exibe uma mensagem de erro.
- **Pós-condição:**
  - A transferência é registrada entre as carteiras, com os respectivos débitos e créditos realizados.

---

### 9. Visualizar Saldo e Histórico de Transações
- **Ator:** Usuário
- **Pré-condição:** O usuário deve possuir ao menos uma carteira cadastrada.
- **Fluxo normal:**
  1. O usuário seleciona a opção "Visualizar Saldo e Histórico de Transações".
  2. O sistema exibe o saldo atual da carteira e uma lista de transações realizadas.
  3. O usuário pode filtrar as transações por período ou categoria, caso deseje.
- **Pós-condição:**
  - O usuário visualiza o saldo atualizado e o histórico de transações.

---

### 10. Registrar Dívida no Cartão de Crédito
- **Ator:** Usuário
- **Pré-condição:** O usuário deve possuir ao menos um cartão de crédito cadastrado.
- **Fluxo normal:**
  1. O usuário seleciona a opção "Registrar Dívida no Cartão de Crédito".
  2. O sistema solicita os dados da dívida (valor, descrição, categoria, e data de vencimento).
  3. O usuário preenche os dados e confirma.
  4. O sistema registra a dívida associada ao cartão e atualiza o saldo devedor do cartão.
- **Fluxos alternativos:**
  - **2.1:** Caso o usuário não forneça os dados obrigatórios, o sistema exibe uma mensagem de erro e solicita que ele preencha novamente.
  - **2.2:** Caso o valor informado seja inválido (ex.: negativo), o sistema exibe uma mensagem de erro.
- **Pós-condição:**
  - A dívida é registrada no cartão de crédito e o saldo devedor do cartão é atualizado.

---

### 11. Registrar Pagamento da Fatura do Cartão de Crédito
- **Ator:** Usuário
- **Pré-condição:** O usuário deve possuir ao menos um cartão de crédito com saldo devedor registrado.
- **Fluxo normal:**
  1. O usuário seleciona a opção "Registrar Pagamento da Fatura do Cartão de Crédito".
  2. O sistema solicita a carteira de onde será debitado.
  3. O usuário preenche os dados e confirma.
  4. O sistema debita o valor da carteira selecionada e reduz o saldo devedor do cartão.
- **Pós-condição:**
  - O pagamento é registrado, o saldo da carteira é atualizado e o saldo devedor do cartão é reduzido.

---

### 12. Arquivar Carteira
- **Ator:** Usuário
- **Pré-condição:** Nenhuma
- **Fluxo normal:**
  1. O usuário seleciona a opção "Arquivar Carteira".
- **Pós-condição:**
  - A carteira é arquivada e não fica mais disponível para novas transações.

---

### 13. Arquivar Categoria
- **Ator:** Usuário
- **Pré-condição:** Nenhuma
- **Fluxo normal:**
  1. O usuário seleciona a opção "Arquivar Categoria".
- **Pós-condição:**
  - A categoria é arquivada e não pode ser associada a novas transações.

---

### 14. Arquivar Cartão de Crédito
- **Ator:** Usuário
- **Pré-condição:** O saldo devedor do cartão deve ser zero e não devem haver faturas pendentes.
- **Fluxo normal:**
  1. O usuário seleciona a opção "Arquivar Cartão de Crédito".
  2. O sistema verifica se o saldo devedor é zero e se não há faturas pendentes.
  3. Caso todas as condições sejam atendidas, o sistema arquiva o cartão de crédito.
- **Fluxos alternativos:**
  - **6.2:** Caso existam faturas pendentes, o sistema exibe uma mensagem de erro e impede o arquivamento.
- **Pós-condição:**
  - O cartão de crédito é arquivado e não pode ser utilizado para novas transações.

---
