# Plano de Migração: Maven → Gradle → Kotlin

## Visão Geral

Migração completa do projeto MOINEX de Java 21 + Maven para Kotlin + Gradle.

**Estatísticas do Projeto:**
- Total de arquivos Java: 224
- Framework: Spring Boot 3.4.5
- UI: JavaFX 23
- Persistência: JPA + Hibernate
- Database: SQLite
- Build tool atual: Maven

---

## Fase 0: Migração Maven → Gradle 🔧

**Objetivo**: Migrar sistema de build de Maven para Gradle antes de introduzir Kotlin

### Por que Gradle primeiro?
- Gradle tem melhor suporte para projetos híbridos Java/Kotlin
- Kotlin DSL nativo (`build.gradle.kts`)
- Build incremental mais eficiente
- Melhor integração com ferramentas Kotlin

### Ações

#### 1. Criar `build.gradle.kts`
- Converter todas as dependências do `pom.xml`
- Configurar plugins (Spring Boot, JavaFX, JaCoCo, etc)
- Manter compatibilidade com Java 21

#### 2. Criar `settings.gradle.kts`
- Configurar nome do projeto
- Configurar repositórios de plugins

#### 3. Criar `gradle.properties`
- Versões de dependências
- Configurações de build

#### 4. Adicionar Gradle Wrapper
- `gradlew` e `gradlew.bat`
- Garantir build reproduzível

#### 5. Testar build
- `./gradlew clean build`
- `./gradlew test`
- `./gradlew bootRun`

#### 6. Atualizar CI/CD
- Ajustar scripts de build (se existirem)
- Atualizar documentação

### Dependências a Migrar

**Spring Boot:**
- spring-boot-starter
- spring-boot-starter-data-jpa
- spring-boot-starter-test

**Database:**
- sqlite-jdbc
- hibernate-community-dialects
- h2database (testes)

**JavaFX:**
- javafx-controls
- javafx-fxml
- jfoenix

**Utilities:**
- lombok (será removido depois)
- opencsv
- reflections
- exp4j
- json
- flyway

**Testing:**
- junit-jupiter
- mockito-core
- mockito-junit-jupiter

**Logging:**
- slf4j-api
- logback-classic

**Code Quality:**
- spotless (substituir por ktlint depois)
- jacoco
- google-java-format

### Duração Estimada
**1-2 dias**

### Critérios de Sucesso
- [ ] Build completa com sucesso
- [ ] Todos os testes passam
- [ ] Aplicação inicia corretamente
- [ ] Mesma funcionalidade do Maven

---

## Fase 1: Configuração Kotlin no Gradle ⚙️

**Objetivo**: Adicionar suporte Kotlin ao projeto mantendo Java funcionando

### Ações

#### 1. Atualizar `build.gradle.kts`
- Adicionar `kotlin("jvm")` plugin
- Adicionar `kotlin("plugin.spring")` para Spring
- Adicionar `kotlin("plugin.jpa")` para JPA
- Adicionar dependências Kotlin stdlib

#### 2. Criar estrutura de diretórios
```
src/
├── main/
│   ├── java/          # Código Java existente
│   ├── kotlin/        # Novo código Kotlin
│   └── resources/
└── test/
    ├── java/
    ├── kotlin/
    └── resources/
```

#### 3. Configurar compilação híbrida
- Java e Kotlin compilam juntos
- Kotlin pode chamar Java
- Java pode chamar Kotlin

#### 4. Configurar ktlint
- Substituir Spotless Java por ktlint
- Manter formatação consistente

#### 5. Teste de integração
- Criar uma classe Kotlin de teste
- Verificar interoperabilidade Java ↔ Kotlin

### Duração Estimada
**1 dia**

### Critérios de Sucesso
- [ ] Projeto compila com Java + Kotlin
- [ ] Testes passam
- [ ] Ktlint configurado
- [ ] Classe Kotlin de exemplo funciona

---

## Fase 2: Migrar Models e Enums 📦

**Objetivo**: Migrar classes de dados (maior impacto, menor risco)

### Arquivos Alvo (~40 arquivos)
- `model/enums/*` - Todos os enums
- `model/*` - Entities JPA

### Estratégia

#### 1. Começar por Enums
**Ordem sugerida:**
1. Enums simples sem métodos
2. Enums com métodos auxiliares
3. Enums com lógica complexa

**Exemplo de conversão:**
```java
// Java
public enum AssetType {
    STOCK, BOND, CRYPTO, REAL_ESTATE
}
```

```kotlin
// Kotlin
enum class AssetType {
    STOCK, BOND, CRYPTO, REAL_ESTATE
}
```

#### 2. Migrar Entities JPA
**Benefícios:**
- Elimina Lombok (`@Data`, `@NoArgsConstructor`, etc)
- Data classes nativas
- Null safety
- Default values

**Exemplo de conversão:**
```java
// Java
@Entity
@Table(name = "ticker")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ticker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false)
    private String symbol;
    
    private BigDecimal currentPrice;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

```kotlin
// Kotlin
@Entity
@Table(name = "ticker")
data class Ticker(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    
    @Column(nullable = false)
    var symbol: String,
    
    var currentPrice: BigDecimal? = null,
    
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now()
)
```

#### 3. Atenção Especial
- **JPA requer `var` (não `val`)** - entidades são mutáveis
- **Construtores sem argumentos** - usar plugin `kotlin-jpa`
- **Lazy loading** - cuidado com `lateinit` vs nullable

### Duração Estimada
**2-3 dias**

### Critérios de Sucesso
- [ ] Todos os enums migrados
- [ ] Todas as entities migradas
- [ ] Testes de persistência passam
- [ ] Sem warnings de compilação

---

## Fase 3: Migrar DTOs e Configurações 🔧

**Objetivo**: Migrar classes de transferência e configuração Spring

### Arquivos Alvo (~20 arquivos)
- `config/*` - Classes de configuração Spring
- DTOs (se existirem em packages separados)

### Benefícios
- Named parameters
- Default values
- Extension functions para conversões

### Exemplo: Configuration Class
```kotlin
@Configuration
class DatabaseConfig {
    
    @Bean
    fun dataSource(): DataSource = DataSourceBuilder
        .create()
        .driverClassName("org.sqlite.JDBC")
        .url("jdbc:sqlite:moinex.db")
        .build()
}
```

### Duração Estimada
**1-2 dias**

---

## Fase 4: Migrar Repositories 🗄️

**Objetivo**: Migrar interfaces JPA para Kotlin

### Arquivos Alvo (~30 arquivos)
- `repository/**/*Repository.java`

### Estratégia
Migração trivial - são apenas interfaces

**Exemplo:**
```kotlin
interface TickerRepository : JpaRepository<Ticker, Int> {
    fun findBySymbol(symbol: String): Ticker?
    
    @Query("SELECT t FROM Ticker t WHERE t.currentPrice > :price")
    fun findExpensiveStocks(@Param("price") price: BigDecimal): List<Ticker>
}
```

### Benefícios
- Nullable types explícitos (`Ticker?`)
- Suporte futuro a coroutines
- Queries mais limpas

### Duração Estimada
**1 dia**

---

## Fase 5: Migrar Services 💼

**Objetivo**: Migrar lógica de negócio (parte mais crítica)

### Arquivos Alvo (~60 arquivos)
- `service/*Service.java`

### Estratégia

#### 1. Ordem de Migração
1. Services sem dependências (leaf services)
2. Services com poucas dependências
3. Services complexos (BondInterestCalculationService, etc)

#### 2. Abordagem por Service
- Migrar um service por vez
- Executar testes após cada migração
- Refatorar usando idioms Kotlin

### Benefícios Kotlin em Services

#### Scope Functions
```kotlin
// Antes (Java)
Ticker ticker = tickerRepository.findById(id).orElse(null);
if (ticker != null) {
    ticker.setCurrentPrice(newPrice);
    tickerRepository.save(ticker);
}

// Depois (Kotlin)
tickerRepository.findById(id).ifPresent { ticker ->
    ticker.currentPrice = newPrice
    tickerRepository.save(ticker)
}

// Ou melhor ainda
tickerRepository.findById(id)?.apply {
    currentPrice = newPrice
    tickerRepository.save(this)
}
```

#### When Expressions
```kotlin
// Antes (Java)
switch (assetType) {
    case STOCK:
        return calculateStockValue();
    case BOND:
        return calculateBondValue();
    default:
        throw new IllegalArgumentException();
}

// Depois (Kotlin)
when (assetType) {
    AssetType.STOCK -> calculateStockValue()
    AssetType.BOND -> calculateBondValue()
    else -> throw IllegalArgumentException()
}
```

#### Extension Functions
```kotlin
// Criar extensions para conversões comuns
fun Ticker.toDTO() = TickerDTO(
    id = this.id,
    symbol = this.symbol,
    price = this.currentPrice
)

// Uso
val dto = ticker.toDTO()
```

#### Smart Casts
```kotlin
// Kotlin elimina casts redundantes
val asset: Asset = getAsset()
if (asset is Stock) {
    // asset é automaticamente Stock aqui
    println(asset.dividendYield)
}
```

### Duração Estimada
**1-2 semanas**

### Critérios de Sucesso
- [ ] Todos os services migrados
- [ ] Testes unitários passam
- [ ] Testes de integração passam
- [ ] Cobertura de código mantida

---

## Fase 6: Migrar Controllers e UI 🖥️

**Objetivo**: Migrar camada de apresentação (JavaFX)

### Arquivos Alvo (~50 arquivos)
- `ui/**/*Controller.java`

### Desafios
- JavaFX tem menos exemplos em Kotlin
- FXML bindings podem precisar ajustes
- Event handlers precisam de atenção

### Exemplo: Controller
```kotlin
@Component
class HomeController : Initializable {
    
    @FXML
    private lateinit var tableView: TableView<TickerDTO>
    
    @Autowired
    private lateinit var tickerService: TickerService
    
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        loadData()
    }
    
    @FXML
    private fun handleRefresh() {
        loadData()
    }
    
    private fun loadData() {
        val tickers = tickerService.findAll()
        tableView.items = FXCollections.observableArrayList(tickers)
    }
}
```

### Benefícios
- Event handlers mais concisos
- Lambdas limpas
- Null safety em bindings

### Duração Estimada
**1 semana**

---

## Fase 7: Migrar Utilities e Auxiliares 🛠️

**Objetivo**: Migrar classes utilitárias

### Arquivos Alvo (~20 arquivos)
- `util/*`
- `chart/*`
- `error/*`

### Estratégia

#### Extension Functions vs Static Utils
```kotlin
// Antes (Java)
public class StringUtils {
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }
}

// Uso: StringUtils.isNullOrEmpty(myString)

// Depois (Kotlin)
fun String?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()

// Uso: myString.isNullOrEmpty()
```

#### Sealed Classes para Erros
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// Uso type-safe
when (val result = fetchData()) {
    is Result.Success -> println(result.data)
    is Result.Error -> println(result.exception)
    is Result.Loading -> showSpinner()
}
```

### Duração Estimada
**2-3 dias**

---

## Fase 8: Limpeza Final 🧹

**Objetivo**: Remover dependências Java-only e finalizar

### Ações

#### 1. Remover Lombok
```kotlin
// build.gradle.kts
dependencies {
    // REMOVER:
    // compileOnly("org.projectlombok:lombok:1.18.36")
    // annotationProcessor("org.projectlombok:lombok:1.18.36")
}
```

#### 2. Configurar ktlint
```kotlin
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "12.0.3"
}

ktlint {
    version.set("1.0.1")
    android.set(false)
    outputToConsole.set(true)
}
```

#### 3. Atualizar Code Quality
- Remover google-java-format
- Remover spotless para Java
- Manter JaCoCo para coverage

#### 4. Revisar Testes
- Converter testes para Kotlin
- Usar Kotest (opcional, mais idiomático)
- Manter JUnit 5 se preferir

#### 5. Atualizar Documentação
- README.md
- Instruções de build
- Guia de contribuição

#### 6. Remover Diretórios Java
```bash
# Quando 100% migrado
rm -rf src/main/java
rm -rf src/test/java
```

### Duração Estimada
**1-2 dias**

### Critérios de Sucesso
- [ ] Sem código Java restante
- [ ] Lombok removido
- [ ] ktlint configurado e passando
- [ ] Documentação atualizada
- [ ] Build limpo sem warnings

---

## Cronograma Total

| Fase | Descrição | Duração | Acumulado |
|------|-----------|---------|-----------|
| 0 | Maven → Gradle | 1-2 dias | 2 dias |
| 1 | Configurar Kotlin | 1 dia | 3 dias |
| 2 | Models/Enums | 2-3 dias | 6 dias |
| 3 | DTOs/Config | 1-2 dias | 8 dias |
| 4 | Repositories | 1 dia | 9 dias |
| 5 | Services | 1-2 semanas | 19 dias |
| 6 | Controllers/UI | 1 semana | 26 dias |
| 7 | Utilities | 2-3 dias | 29 dias |
| 8 | Limpeza Final | 1-2 dias | 31 dias |

**Total estimado: ~1 mês** (trabalhando em tempo integral)

---

## Ferramentas Úteis

### IntelliJ IDEA
- **Code → Convert Java File to Kotlin File** - Conversão automática
- **Analyze → Inspect Code** - Detectar problemas
- **Refactor → Migrate** - Assistente de migração

### Gradle
```bash
# Build
./gradlew build

# Testes
./gradlew test

# Rodar aplicação
./gradlew bootRun

# Verificar ktlint
./gradlew ktlintCheck

# Formatar código
./gradlew ktlintFormat

# Coverage
./gradlew jacocoTestReport
```

### Git Strategy
- Criar branch `migration/gradle`
- Criar branch `migration/kotlin`
- Commits pequenos e frequentes
- Um commit por classe/package migrado

---

## Riscos e Mitigações

### Risco 1: Quebra de Funcionalidade
**Mitigação:**
- Testes automatizados em cada fase
- Migração gradual
- Rollback fácil (Git)

### Risco 2: JavaFX Incompatibilidades
**Mitigação:**
- Migrar UI por último
- Testar cada controller individualmente
- Manter documentação de problemas

### Risco 3: Performance
**Mitigação:**
- Benchmarks antes/depois
- Profiling de partes críticas
- Otimizações Kotlin quando necessário

### Risco 4: Curva de Aprendizado
**Mitigação:**
- Estudar Kotlin idioms
- Code reviews
- Refatoração incremental

---

## Checklist de Validação

### Após Cada Fase
- [ ] Código compila sem erros
- [ ] Testes passam (mesma cobertura ou melhor)
- [ ] Aplicação inicia corretamente
- [ ] Funcionalidades testadas manualmente
- [ ] Sem warnings de compilação
- [ ] Code review (se em equipe)
- [ ] Commit e push

### Validação Final
- [ ] 100% do código em Kotlin
- [ ] Todos os testes passam
- [ ] Coverage >= baseline anterior
- [ ] Aplicação funciona identicamente
- [ ] Performance mantida ou melhorada
- [ ] Documentação atualizada
- [ ] CI/CD funcionando
- [ ] Build reproduzível

---

## Recursos de Aprendizado

### Documentação Oficial
- [Kotlin Docs](https://kotlinlang.org/docs/home.html)
- [Kotlin for Java Developers](https://kotlinlang.org/docs/java-to-kotlin-interop.html)
- [Spring Boot + Kotlin](https://spring.io/guides/tutorials/spring-boot-kotlin/)

### Guias Específicos
- [JPA with Kotlin](https://kotlinlang.org/docs/jpa.html)
- [JavaFX with Kotlin](https://github.com/edvin/tornadofx)
- [Gradle Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html)

### Idioms Kotlin
- Scope functions (`let`, `apply`, `run`, `also`, `with`)
- Extension functions
- Data classes
- Sealed classes
- When expressions
- Null safety

---

## Próximos Passos Imediatos

1. ✅ Ler e entender este plano
2. 🔄 Criar backup do projeto
3. 🔄 Criar branch `migration/gradle`
4. 🔄 Começar Fase 0: Maven → Gradle

**Pronto para começar?**
