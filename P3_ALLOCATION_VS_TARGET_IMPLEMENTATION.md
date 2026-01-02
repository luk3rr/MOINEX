# P3 - Alocação vs Meta - Plano de Implementação

## 📋 Visão Geral

Este documento contém o plano completo para implementar o painel **P3 (Alocação vs Meta)** na tela de Overview do módulo Savings. O painel mostrará a alocação atual de investimentos por tipo comparada com as metas definidas pelo usuário.

## 🎯 Objetivo

Criar um painel visual que exiba:
- Alocação atual de investimentos por tipo (Ações, Fundos, Criptomoedas)
- Meta de alocação desejada para cada tipo
- Comparação visual através de barras horizontais
- Indicação se está acima ou abaixo da meta

## 📊 Exemplo Visual

```
Ações        ████████░░  45% / 60% (Meta) ⚠️ -15%
Fundos       ███████████ 35% / 30% (Meta) ✓ +5%
Criptomoedas ██░░░░░░░░  20% / 10% (Meta) ⚠️ +10%
```

## 🗂️ Estrutura do Projeto

### Contexto Atual

**Painéis já implementados:**
- ✅ P1: Gráfico de Pizza (Distribuição de Investimentos)
- ✅ P2: Top Performers (Melhores e Piores Ativos)
- ✅ P4: Métricas de Rentabilidade

**Painel a implementar:**
- ⏳ P3: Alocação vs Meta

**Localização no FXML:**
- Arquivo: `src/main/resources/ui/main/savings.fxml`
- Campo FXML: `portfolioP3` (HBox)
- Linha aproximada: 117

## 🏗️ Arquitetura da Solução

### Opção A: Sistema Completo com Banco de Dados (Recomendado)

Implementação completa com persistência de metas no banco de dados.

#### Vantagens
- ✅ Flexível - usuário pode alterar metas
- ✅ Persistente - metas salvas no banco
- ✅ Escalável - fácil adicionar novos tipos
- ✅ Profissional - solução completa

#### Desvantagens
- ⏱️ Mais tempo de implementação (6-8 horas)
- 🔧 Requer migration de banco de dados
- 🎨 Requer tela de configuração

### Opção B: Valores Hardcoded (Rápido)

Implementação com valores fixos no código.

#### Vantagens
- ⚡ Rápido de implementar (2-3 horas)
- 🎯 Funcional imediatamente
- 🧪 Bom para protótipo/MVP

#### Desvantagens
- ❌ Não flexível - usuário não pode alterar
- ❌ Valores fixos no código
- ❌ Precisa recompilar para mudar metas

## 📝 Plano de Implementação - Opção A (Completo)

### Fase 1: Modelo de Dados

#### 1.1. Criar Entidade `InvestmentTarget`

**Arquivo:** `src/main/java/org/moinex/model/investment/InvestmentTarget.java`

```java
package org.moinex.model.investment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;
import org.moinex.util.enums.TickerType;

@Entity
@Table(name = "investment_target")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Integer id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ticker_type", nullable = false, unique = true)
    private TickerType tickerType;
    
    @Column(name = "target_percentage", nullable = false)
    private BigDecimal targetPercentage; // Ex: 60.00 para 60%
    
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
```

#### 1.2. Criar Migration SQL

**Arquivo:** `src/main/resources/db/migration/V{next_version}__create_investment_target_table.sql`

```sql
CREATE TABLE IF NOT EXISTS investment_target (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticker_type TEXT NOT NULL UNIQUE,
    target_percentage DECIMAL(5,2) NOT NULL CHECK(target_percentage >= 0 AND target_percentage <= 100),
    is_active BOOLEAN NOT NULL DEFAULT 1
);

-- Inserir valores padrão
INSERT INTO investment_target (ticker_type, target_percentage, is_active) VALUES
    ('STOCK', 60.00, 1),
    ('FUND', 30.00, 1),
    ('CRYPTOCURRENCY', 10.00, 1);
```

**Nota:** Substitua `{next_version}` pelo próximo número de versão disponível no diretório `db/migration`.

### Fase 2: Camada de Dados

#### 2.1. Criar Repository

**Arquivo:** `src/main/java/org/moinex/repository/investment/InvestmentTargetRepository.java`

```java
package org.moinex.repository.investment;

import java.util.List;
import java.util.Optional;
import org.moinex.model.investment.InvestmentTarget;
import org.moinex.util.enums.TickerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestmentTargetRepository extends JpaRepository<InvestmentTarget, Integer> {
    List<InvestmentTarget> findAllByIsActiveTrueOrderByTickerTypeAsc();
    
    Optional<InvestmentTarget> findByTickerTypeAndIsActiveTrue(TickerType tickerType);
    
    boolean existsByTickerType(TickerType tickerType);
}
```

#### 2.2. Criar Service

**Arquivo:** `src/main/java/org/moinex/service/InvestmentTargetService.java`

```java
package org.moinex.service;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import lombok.NoArgsConstructor;
import org.moinex.model.investment.InvestmentTarget;
import org.moinex.repository.investment.InvestmentTargetRepository;
import org.moinex.util.enums.TickerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@NoArgsConstructor
public class InvestmentTargetService {
    
    @Autowired
    private InvestmentTargetRepository investmentTargetRepository;
    
    /**
     * Get all active investment targets
     */
    public List<InvestmentTarget> getAllActiveTargets() {
        return investmentTargetRepository.findAllByIsActiveTrueOrderByTickerTypeAsc();
    }
    
    /**
     * Get target by ticker type
     */
    public InvestmentTarget getTargetByType(TickerType tickerType) {
        return investmentTargetRepository
                .findByTickerTypeAndIsActiveTrue(tickerType)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Investment target not found for type: " + tickerType));
    }
    
    /**
     * Set or update target for a ticker type
     */
    @Transactional
    public InvestmentTarget setTarget(TickerType tickerType, BigDecimal targetPercentage) {
        if (targetPercentage.compareTo(BigDecimal.ZERO) < 0 || 
            targetPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Target percentage must be between 0 and 100");
        }
        
        InvestmentTarget target = investmentTargetRepository
                .findByTickerTypeAndIsActiveTrue(tickerType)
                .orElse(InvestmentTarget.builder()
                        .tickerType(tickerType)
                        .isActive(true)
                        .build());
        
        target.setTargetPercentage(targetPercentage);
        return investmentTargetRepository.save(target);
    }
    
    /**
     * Delete target
     */
    @Transactional
    public void deleteTarget(Integer id) {
        InvestmentTarget target = investmentTargetRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Investment target not found"));
        
        target.setActive(false);
        investmentTargetRepository.save(target);
    }
    
    /**
     * Validate that total targets sum to 100%
     */
    public boolean validateTotalPercentage() {
        BigDecimal total = getAllActiveTargets().stream()
                .map(InvestmentTarget::getTargetPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return total.compareTo(new BigDecimal("100")) == 0;
    }
}
```

### Fase 3: DTO

#### 3.1. Criar `AllocationDTO`

**Arquivo:** `src/main/java/org/moinex/dto/AllocationDTO.java`

```java
package org.moinex.dto;

import java.math.BigDecimal;
import org.moinex.util.enums.TickerType;

/**
 * DTO for allocation vs target information
 */
public record AllocationDTO(
        TickerType tickerType,
        String typeName,
        BigDecimal currentPercentage,
        BigDecimal targetPercentage,
        BigDecimal currentValue,
        BigDecimal difference) {
    
    public boolean isAboveTarget() {
        return currentPercentage.compareTo(targetPercentage) > 0;
    }
    
    public boolean isBelowTarget() {
        return currentPercentage.compareTo(targetPercentage) < 0;
    }
    
    public boolean isOnTarget() {
        return currentPercentage.compareTo(targetPercentage) == 0;
    }
}
```

### Fase 4: Controller - Lógica de Cálculo

#### 4.1. Adicionar Injeção de Dependência

No construtor do `SavingsController`, adicionar:

```java
@Autowired
public SavingsController(
        TickerService tickerService,
        MarketService marketService,
        ConfigurableApplicationContext springContext,
        I18nService i18nService,
        WalletService walletService,
        InvestmentTargetService investmentTargetService) {  // ADICIONAR
    this.tickerService = tickerService;
    this.marketService = marketService;
    this.springContext = springContext;
    this.i18nService = i18nService;
    this.walletService = walletService;
    this.investmentTargetService = investmentTargetService;  // ADICIONAR
}
```

Adicionar campo:
```java
private InvestmentTargetService investmentTargetService;
```

#### 4.2. Adicionar Campo FXML

```java
@FXML private HBox portfolioP3;
```

#### 4.3. Implementar Método de Cálculo

```java
/**
 * Calculate allocation vs target
 */
private List<AllocationDTO> calculateAllocationVsTarget() {
    loadTickersFromDatabase();
    
    // Calcular valor total
    BigDecimal totalValue = tickers.stream()
            .map(t -> t.getCurrentQuantity().multiply(t.getCurrentUnitValue()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Incluir poupança no total
    List<Wallet> allWallets = walletService.getAllNonArchivedWalletsOrderedByName();
    List<WalletType> allWalletTypes = walletService.getAllWalletTypes();
    
    for (WalletType walletType : allWalletTypes) {
        if (walletType.getName().equalsIgnoreCase("Poupança") ||
            walletType.getName().equalsIgnoreCase("Savings Account")) {
            BigDecimal savingsBalance = allWallets.stream()
                    .filter(w -> w.getType().getId().equals(walletType.getId()))
                    .filter(Wallet::isMaster)
                    .map(Wallet::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalValue = totalValue.add(savingsBalance);
        }
    }
    
    // Calcular alocação atual por tipo
    Map<TickerType, BigDecimal> currentAllocation = new HashMap<>();
    
    for (Ticker ticker : tickers) {
        BigDecimal value = ticker.getCurrentQuantity().multiply(ticker.getCurrentUnitValue());
        currentAllocation.merge(ticker.getType(), value, BigDecimal::add);
    }
    
    // Buscar metas
    List<InvestmentTarget> targets = investmentTargetService.getAllActiveTargets();
    
    // Combinar alocação atual com metas
    List<AllocationDTO> allocations = new ArrayList<>();
    
    for (InvestmentTarget target : targets) {
        BigDecimal currentValue = currentAllocation.getOrDefault(target.getTickerType(), BigDecimal.ZERO);
        BigDecimal currentPercentage = totalValue.compareTo(BigDecimal.ZERO) > 0
                ? currentValue.divide(totalValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        
        BigDecimal difference = currentPercentage.subtract(target.getTargetPercentage());
        String typeName = UIUtils.translateTickerType(target.getTickerType(), i18nService);
        
        allocations.add(new AllocationDTO(
                target.getTickerType(),
                typeName,
                currentPercentage,
                target.getTargetPercentage(),
                currentValue,
                difference
        ));
    }
    
    return allocations;
}
```

#### 4.4. Implementar Método de Atualização do Painel

```java
/**
 * Update the allocation vs target panel (P3)
 */
private void updateAllocationVsTargetPanel() {
    portfolioP3.getChildren().clear();
    
    VBox container = new VBox(10);
    container.setAlignment(Pos.CENTER);
    container.setStyle("-fx-padding: 10;");
    
    Label titleLabel = new Label(i18nService.tr("savings.allocation.title"));
    titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    
    VBox allocationsBox = new VBox(8);
    allocationsBox.setAlignment(Pos.CENTER_LEFT);
    
    List<AllocationDTO> allocations = calculateAllocationVsTarget();
    
    for (AllocationDTO allocation : allocations) {
        allocationsBox.getChildren().add(createAllocationBar(allocation));
    }
    
    container.getChildren().addAll(titleLabel, allocationsBox);
    
    portfolioP3.getChildren().add(container);
    HBox.setHgrow(container, javafx.scene.layout.Priority.ALWAYS);
}

/**
 * Create allocation bar with current vs target
 */
private VBox createAllocationBar(AllocationDTO allocation) {
    VBox barContainer = new VBox(3);
    
    // Header: Nome do tipo
    Label typeLabel = new Label(allocation.typeName());
    typeLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
    
    // Barra de progresso
    HBox progressBar = new HBox();
    progressBar.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 5;");
    progressBar.setPrefHeight(20);
    progressBar.setMaxWidth(Double.MAX_VALUE);
    
    // Barra preenchida (alocação atual)
    double fillPercentage = allocation.currentPercentage().doubleValue();
    HBox filledBar = new HBox();
    filledBar.setStyle(
        "-fx-background-color: " + 
        (allocation.isAboveTarget() ? "#f59e0b" : 
         allocation.isBelowTarget() ? "#3b82f6" : "#22c55e") +
        "; -fx-background-radius: 5;"
    );
    filledBar.setPrefHeight(20);
    filledBar.prefWidthProperty().bind(
        progressBar.widthProperty().multiply(fillPercentage / 100.0)
    );
    
    progressBar.getChildren().add(filledBar);
    
    // Footer: Percentuais e diferença
    HBox infoBox = new HBox(10);
    infoBox.setAlignment(Pos.CENTER_LEFT);
    
    Label currentLabel = new Label(
        UIUtils.formatPercentage(allocation.currentPercentage()) + 
        " / " + 
        UIUtils.formatPercentage(allocation.targetPercentage()) + " (Meta)"
    );
    currentLabel.setStyle("-fx-font-size: 11px;");
    
    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
    
    String diffIcon = allocation.isAboveTarget() ? "⚠️" : 
                      allocation.isBelowTarget() ? "⚠️" : "✓";
    String diffSign = allocation.difference().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
    
    Label diffLabel = new Label(
        diffIcon + " " + diffSign + UIUtils.formatPercentage(allocation.difference())
    );
    diffLabel.setStyle(
        "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " +
        (allocation.isOnTarget() ? "#22c55e" : "#f59e0b") + ";"
    );
    
    infoBox.getChildren().addAll(currentLabel, spacer, diffLabel);
    
    barContainer.getChildren().addAll(typeLabel, progressBar, infoBox);
    
    return barContainer;
}
```

#### 4.5. Integrar no `initialize()` e métodos de atualização

Adicionar em `initialize()`:
```java
updateAllocationVsTargetPanel();
```

Adicionar em todos os callbacks de atualização (onde já tem `updateTopPerformersPanel()` e `updateProfitabilityMetricsPanel()`):
```java
updateAllocationVsTargetPanel();
```

### Fase 5: Internacionalização

Adicionar em `messages_pt_BR.properties`:
```properties
# Allocation vs Target
savings.allocation.title=Alocação vs Meta
savings.allocation.current=Atual
savings.allocation.target=Meta
savings.allocation.difference=Diferença
```

Adicionar em `messages_en.properties`:
```properties
# Allocation vs Target
savings.allocation.title=Allocation vs Target
savings.allocation.current=Current
savings.allocation.target=Target
savings.allocation.difference=Difference
```

### Fase 6: Tela de Configuração de Metas (Opcional)

#### 6.1. Criar Dialog FXML

**Arquivo:** `src/main/resources/ui/dialog/investment/manage_targets.fxml`

Criar uma tela com:
- Lista de tipos de investimento
- Campo para editar percentual de cada tipo
- Validação que soma deve ser 100%
- Botões Salvar/Cancelar

#### 6.2. Criar Controller do Dialog

**Arquivo:** `src/main/java/org/moinex/ui/dialog/investment/ManageTargetsController.java`

Implementar lógica para:
- Carregar metas atuais
- Editar percentuais
- Validar soma = 100%
- Salvar no banco

#### 6.3. Adicionar Botão na Tela Principal

No `savings.fxml`, adicionar botão para abrir o dialog de configuração de metas.

## 📝 Plano de Implementação - Opção B (Rápido)

### Implementação Simplificada com Valores Hardcoded

#### 1. Criar DTO Simplificado

Usar o mesmo `AllocationDTO` da Opção A.

#### 2. Implementar com Valores Fixos

```java
/**
 * Calculate allocation vs target (hardcoded targets)
 */
private List<AllocationDTO> calculateAllocationVsTarget() {
    loadTickersFromDatabase();
    
    // Metas hardcoded
    Map<TickerType, BigDecimal> targets = Map.of(
        TickerType.STOCK, new BigDecimal("60.00"),
        TickerType.FUND, new BigDecimal("30.00"),
        TickerType.CRYPTOCURRENCY, new BigDecimal("10.00")
    );
    
    // Calcular valor total
    BigDecimal totalValue = tickers.stream()
            .map(t -> t.getCurrentQuantity().multiply(t.getCurrentUnitValue()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Calcular alocação atual por tipo
    Map<TickerType, BigDecimal> currentAllocation = new HashMap<>();
    
    for (Ticker ticker : tickers) {
        BigDecimal value = ticker.getCurrentQuantity().multiply(ticker.getCurrentUnitValue());
        currentAllocation.merge(ticker.getType(), value, BigDecimal::add);
    }
    
    // Combinar alocação atual com metas
    List<AllocationDTO> allocations = new ArrayList<>();
    
    for (Map.Entry<TickerType, BigDecimal> targetEntry : targets.entrySet()) {
        TickerType type = targetEntry.getKey();
        BigDecimal targetPercentage = targetEntry.getValue();
        
        BigDecimal currentValue = currentAllocation.getOrDefault(type, BigDecimal.ZERO);
        BigDecimal currentPercentage = totalValue.compareTo(BigDecimal.ZERO) > 0
                ? currentValue.divide(totalValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        
        BigDecimal difference = currentPercentage.subtract(targetPercentage);
        String typeName = UIUtils.translateTickerType(type, i18nService);
        
        allocations.add(new AllocationDTO(
                type,
                typeName,
                currentPercentage,
                targetPercentage,
                currentValue,
                difference
        ));
    }
    
    return allocations;
}
```

Usar o mesmo método `updateAllocationVsTargetPanel()` e `createAllocationBar()` da Opção A.

## ✅ Checklist de Implementação

### Opção A (Completo)
- [ ] Criar entidade `InvestmentTarget`
- [ ] Criar migration SQL
- [ ] Criar `InvestmentTargetRepository`
- [ ] Criar `InvestmentTargetService`
- [ ] Criar `AllocationDTO`
- [ ] Adicionar campo FXML `portfolioP3`
- [ ] Injetar `InvestmentTargetService` no controller
- [ ] Implementar `calculateAllocationVsTarget()`
- [ ] Implementar `updateAllocationVsTargetPanel()`
- [ ] Implementar `createAllocationBar()`
- [ ] Adicionar chamada em `initialize()`
- [ ] Adicionar chamadas em todos os métodos de atualização
- [ ] Adicionar chaves de internacionalização
- [ ] (Opcional) Criar tela de configuração de metas
- [ ] Testar funcionalidade completa

### Opção B (Rápido)
- [ ] Criar `AllocationDTO`
- [ ] Adicionar campo FXML `portfolioP3`
- [ ] Implementar `calculateAllocationVsTarget()` com valores hardcoded
- [ ] Implementar `updateAllocationVsTargetPanel()`
- [ ] Implementar `createAllocationBar()`
- [ ] Adicionar chamada em `initialize()`
- [ ] Adicionar chamadas em todos os métodos de atualização
- [ ] Adicionar chaves de internacionalização
- [ ] Testar funcionalidade

## 🎨 Referências de Estilo

### Cores Utilizadas
- **Azul** (#3b82f6): Abaixo da meta
- **Verde** (#22c55e): Na meta
- **Laranja** (#f59e0b): Acima da meta
- **Cinza** (#e5e7eb): Fundo da barra

### Ícones
- ✓ : Na meta
- ⚠️ : Fora da meta (acima ou abaixo)

## 📚 Referências de Código Existente

### Arquivos Relacionados
- `SavingsController.java`: Controller principal
- `TickerType.java`: Enum com tipos de ativos
- `UIUtils.java`: Utilitários de formatação
- `savings.fxml`: Layout da tela
- Painéis P2 e P4: Exemplos de implementação similar

### Métodos Úteis Existentes
- `UIUtils.translateTickerType()`: Traduzir tipo de ativo
- `UIUtils.formatPercentage()`: Formatar percentual
- `UIUtils.formatCurrency()`: Formatar moeda
- `walletService.getAllNonArchivedWalletsOrderedByName()`: Buscar carteiras
- `tickerService.getAllNonArchivedTickers()`: Buscar ativos

## 🚀 Próximos Passos

1. Escolher entre Opção A (completo) ou Opção B (rápido)
2. Seguir o checklist correspondente
3. Testar a funcionalidade
4. (Opcional) Migrar de Opção B para Opção A posteriormente

## 💡 Dicas de Implementação

1. **Comece pela Opção B** se quiser ver resultados rápidos
2. **Teste incrementalmente** - implemente uma parte por vez
3. **Valide os cálculos** - certifique-se que as porcentagens somam 100%
4. **Considere edge cases** - o que acontece se não houver investimentos?
5. **Reutilize código** - veja como P2 e P4 foram implementados

## 📞 Suporte

Se tiver dúvidas durante a implementação:
- Consulte os painéis P2 e P4 já implementados
- Verifique a estrutura de outros services (TickerService, WalletService)
- Analise outros DTOs existentes no projeto

---

**Documento criado em:** 02/01/2026
**Versão:** 1.0
**Status:** Pronto para implementação
