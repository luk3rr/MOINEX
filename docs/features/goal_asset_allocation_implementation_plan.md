# Plano de Implementação: Vinculação de Goals a Ativos de Investimento

## 📋 Visão Geral

**Objetivo:** Permitir que usuários criem goals vinculadas a ativos de investimento (Bonds e Tickers) com sistema flexível de distribuição de vendas.

**Abordagem:** Sistema híbrido que mantém compatibilidade total com o sistema atual de wallets e adiciona nova funcionalidade de alocação de ativos.

**Estimativa Total:** 40-50 horas de desenvolvimento + 20-30 horas de testes

---

## 🎯 FASE 1: Database Schema e Entidades Core (4-6h) ✅

### 1.1. Migration Script ✅
- [x] Criar `V007__add_goal_asset_allocation.sql`
- [x] Adicionar coluna `tracking_mode` em `goal` (default 'WALLET')
- [x] Criar tabela `goal_asset_allocation`
- [x] Adicionar coluna `current_unit_value` em `bond`
- [x] Criar índices para performance
- [x] Testar migration (ajustado pelo usuário para padrão VARCHAR(255))

### 1.2. Enums ✅
- [x] `GoalTrackingMode` (WALLET, ASSET_ALLOCATION)
- [x] `GoalAssetType` (BOND, TICKER) - criado como GoalAssetType pois AssetType já existia
- [x] `AllocationType` (PERCENTAGE, QUANTITY, VALUE)
- [x] `SaleDistributionStrategy` (PROPORTIONAL, SINGLE_GOAL, MANUAL, KEEP_ALLOCATIONS)

### 1.3. Entidades ✅
- [x] Criar `GoalAssetAllocation` entity
- [x] Atualizar `Goal` com `trackingMode` e `assetAllocations`
- [x] Atualizar `Bond` com `currentUnitValue`

### 1.4. DTOs ✅
- [x] `AssetSaleDistribution`
- [x] `AffectedGoalInfo`
- [x] `GoalAssetAllocationDTO`

---

## 🗄️ FASE 2: Repository Layer (2-3h) ✅

### 2.1. GoalAssetAllocationRepository ✅
- [x] Criar interface com métodos:
  - [x] `findByGoal(Goal)`
  - [x] `findByAssetTypeAndAssetId(GoalAssetType, Integer)`
  - [x] `findByGoalAndAsset(Integer, GoalAssetType, Integer)`
  - [x] `countGoalsByAsset(GoalAssetType, Integer)`
  - [x] `deleteByGoal(Goal)`
  - [x] `findByGoalId(Integer)` - adicional

---

## ⚙️ FASE 3: Service Layer - Core Logic (8-10h) ✅

### 3.1. GoalAssetAllocationService - CRUD ✅
- [x] `addAllocation(GoalAssetAllocationDTO)`
- [x] `removeAllocation(Integer)`
- [x] `updateAllocation(Integer, GoalAssetAllocationDTO)`
- [x] `getAllocationsByGoal(Goal)`
- [x] `getAllocationsByGoalId(Integer)` - adicional

### 3.2. Cálculo de Valores ✅
- [x] `calculateAllocationValue(GoalAssetAllocation)`
- [x] `calculateBondAllocationValue(GoalAssetAllocation)`
- [x] `calculateTickerAllocationValue(GoalAssetAllocation)`
- [x] `calculateGoalTotalValue(Goal)`
- [x] `calculateValueByAllocationType()` - método auxiliar

### 3.3. Validações ✅
- [x] `validateAllocation(GoalAssetAllocationDTO)`
- [x] `validateAssetExists(GoalAssetType, Integer)`
- [x] `validateTotalPercentage(GoalAssetAllocationDTO)`
- [x] `validateAvailableQuantity(GoalAssetAllocationDTO)`
- [x] `convertToDTO()` e `getAssetName()` - métodos auxiliares

---

## 📊 FASE 4: Service Layer - Sale Distribution (6-8h) ✅

### 4.1. AssetSaleDistributionService ✅
- [x] `getAffectedGoals(GoalAssetType, Integer, BigDecimal)`
- [x] `processAssetSale(GoalAssetType, Integer, BigDecimal, AssetSaleDistribution)`
- [x] `applyProportionalDistribution(List, BigDecimal)`
- [x] `applySingleGoalDistribution(List, BigDecimal, Integer)`
- [x] `applyManualDistribution(List, Map)`
- [x] `adjustAllocation()` - método auxiliar para ajustar alocações
- [x] `calculateSuggestedReduction()` - cálculo de redução sugerida
- [x] `hasLinkedGoals()` e `countLinkedGoals()` - métodos de verificação

---

## 🔗 FASE 5: Integração com Services Existentes (4-6h)

### 5.1. GoalService ✅
- [x] Adicionar parâmetro `trackingMode` em `addGoal`
- [x] Implementar `getGoalCurrentValue(Goal)`
- [x] Validação: goals ASSET_ALLOCATION não podem ter master wallet
- [x] Manter compatibilidade com código existente (métodos antigos chamam novos com WALLET mode)

### 5.2. BondService ✅
- [x] Adicionar parâmetro `saleDistribution` em `addOperation`
- [x] Implementar `getGoalsAffectedBySale(Integer, BigDecimal)`
- [x] Implementar `updateBondCurrentValue(Integer, BigDecimal)`
- [x] Atualizar `updateOperation` com sale distribution
- [x] Implementar `hasLinkedGoals(Integer)` - método auxiliar

### 5.3. TickerService ✅
- [x] Adicionar parâmetro `saleDistribution` em `addSale`
- [x] Implementar `getGoalsAffectedBySale(Integer, BigDecimal)`
- [x] Atualizar `updateSale` com sale distribution
- [x] Implementar `hasLinkedGoals(Integer)` - método auxiliar

---

## 🎨 FASE 6: UI Components (12-15h) ✅

### 6.1. Dialog de Criação de Goal ✅
- [x] Adicionar radio buttons para selecionar modo (WALLET/ASSET_ALLOCATION)
- [x] Mostrar/ocultar painéis conforme seleção
- [x] Atualizar `AddGoalController.handleSave()` para usar tracking mode
- [x] Abrir dialog de alocações após criar goal em modo ASSET
- [x] Adicionar traduções (trackingMode, walletBased, assetBased)
- [x] Injetar SpringContext para abrir dialogs

### 6.2. Dialog de Gerenciamento de Alocações ✅
- [x] Criar `manage_goal_allocations.fxml`
- [x] Implementar `ManageGoalAllocationsController`
- [x] TableView com alocações existentes
- [x] Botões: Adicionar, Editar, Remover (seguindo padrão do projeto)
- [x] Mostrar valor total da goal
- [x] Listener de seleção para mostrar/ocultar botões de ação

### 6.3. Dialog de Adição/Edição de Alocação ✅
- [x] Criar `add_goal_asset_allocation.fxml`
- [x] Implementar `AddGoalAssetAllocationController`
- [x] RadioButtons para selecionar tipo de ativo (Bond/Ticker)
- [x] ComboBox para selecionar ativo específico
- [x] RadioButtons para tipo de alocação (Percentage/Quantity/Value)
- [x] TextField para valor da alocação
- [x] Validações completas
- [x] Painel de informações do ativo (valor atual, quantidade disponível)
- [x] Suporte para edição de alocações existentes
- [x] 36 traduções (EN + PT-BR) + 42 constantes

### 6.4. Dialog de Distribuição de Venda
- [ ] Criar `sale_distribution.fxml`
- [ ] Implementar `SaleDistributionController`
- [ ] TableView mostrando goals afetadas
- [ ] Radio buttons para estratégias:
  - Proporcional (default)
  - Goal única
  - Manual
  - Não afetar
- [ ] Campos dinâmicos para distribuição manual
- [ ] Validações de soma

### 6.5. Atualizar Controllers de Operações
- [ ] `AddBondOperationController`: verificar goals afetadas antes de venda
- [ ] `EditBondOperationController`: similar
- [ ] `AddTickerSaleController`: verificar goals afetadas
- [ ] `EditTickerSaleController`: similar

### 6.6. Visualização de Goals
- [ ] Atualizar `GoalsController` para mostrar modo de rastreamento
- [ ] Mostrar valor atual calculado para goals baseadas em ativos
- [ ] Adicionar botão "Gerenciar Alocações" para goals ASSET_ALLOCATION

---

## 🧪 FASE 7: Testing (15-20h)

### 7.1. Unit Tests - Entities
- [ ] `GoalAssetAllocationTest`
- [ ] `GoalTest` (novos campos)

### 7.2. Unit Tests - Services
- [ ] `GoalAssetAllocationServiceTest`
  - CRUD operations
  - Cálculos de valor
  - Validações
  - Distribuição de vendas (todas as estratégias)
- [ ] `GoalServiceTest` (atualizar)
- [ ] `BondServiceTest` (atualizar)
- [ ] `TickerServiceTest` (atualizar)

### 7.3. Integration Tests
- [ ] Criar goal em modo ASSET_ALLOCATION
- [ ] Adicionar múltiplas alocações
- [ ] Vender ativo com distribuição proporcional
- [ ] Vender ativo com goal única
- [ ] Vender ativo com distribuição manual
- [ ] Deletar goal com alocações
- [ ] Arquivar goal com alocações

### 7.4. UI Tests
- [ ] Testar criação de goal em ambos os modos
- [ ] Testar adição/edição/remoção de alocações
- [ ] Testar dialog de distribuição de venda
- [ ] Testar validações de UI

### 7.5. Cenários de Teste Específicos
- [ ] Goal com 100% de um bond
- [ ] Goal com múltiplos ativos (50% bond + 30 ações)
- [ ] Múltiplas goals no mesmo ativo
- [ ] Venda parcial afetando múltiplas goals
- [ ] Conversão de goal WALLET para ASSET_ALLOCATION
- [ ] Performance com muitas alocações

---

## 📚 FASE 8: Documentação e Deployment (3-4h)

### 8.1. Documentação Técnica
- [ ] Atualizar README com nova funcionalidade
- [ ] Documentar modelo de dados
- [ ] Documentar fluxos de negócio
- [ ] Criar diagramas (ER, fluxo de venda)

### 8.2. Documentação de Usuário
- [ ] Guia de uso: criar goal vinculada a ativos
- [ ] Guia de uso: gerenciar alocações
- [ ] Guia de uso: distribuição de vendas
- [ ] FAQ

### 8.3. Migration e Deployment
- [ ] Testar migration em ambiente de staging
- [ ] Backup de banco de dados
- [ ] Executar migration em produção
- [ ] Verificar que goals existentes funcionam normalmente
- [ ] Monitorar logs por 24h

---

## 📝 Notas de Implementação

### Decisões Arquiteturais
1. **Múltiplas goals no mesmo ativo:** SIM (com validação de sobreposição)
2. **Comportamento ao vender:** Perguntar ao usuário (híbrido)
3. **Soma de percentuais > 100%:** NÃO (validar)
4. **Goal com WALLET + ASSET simultaneamente:** NÃO (v1 - ou um ou outro)

### Prioridades
- **P0 (Must Have):** Fases 1-5 + UI básica (6.1-6.3)
- **P1 (Should Have):** Dialog de distribuição (6.4-6.5) + Testes core (7.1-7.2)
- **P2 (Nice to Have):** Testes completos (7.3-7.5) + Documentação (8)

### Riscos e Mitigações
- **Risco:** Complexidade de cálculo de valores flutuantes
  - **Mitigação:** Adicionar `currentUnitValue` em Bond, atualizar periodicamente
- **Risco:** Performance com muitas alocações
  - **Mitigação:** Índices adequados, lazy loading, cache
- **Risco:** Inconsistência em vendas
  - **Mitigação:** Transações, validações rigorosas, testes extensivos

---

## 🚀 Ordem de Execução Recomendada

1. **Sprint 1 (1 semana):** Fases 1-2 + Fase 3.1-3.2
2. **Sprint 2 (1 semana):** Fase 3.3 + Fase 4
3. **Sprint 3 (1 semana):** Fase 5 + Testes unitários básicos
4. **Sprint 4 (1.5 semanas):** Fase 6.1-6.3 + Testes de integração
5. **Sprint 5 (1 semana):** Fase 6.4-6.6 + Testes de UI
6. **Sprint 6 (0.5 semana):** Fase 8 + Deployment

**Total:** ~6 semanas de desenvolvimento

---

## ✅ Checklist Final Antes do Deploy

- [ ] Todas as migrations testadas
- [ ] Todos os testes passando (>90% coverage)
- [ ] Code review completo
- [ ] Documentação atualizada
- [ ] Backup de produção realizado
- [ ] Plano de rollback preparado
- [ ] Monitoramento configurado
- [ ] Usuários beta testaram funcionalidade
