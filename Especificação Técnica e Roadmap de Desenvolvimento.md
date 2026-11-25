# Especificação Técnica e Roadmap de Desenvolvimento — TriHub

**Universidade Federal do Ceará (UFC) — Campus Russas**  
**Disciplina:** RUS0291 — Desenvolvimento de Software para Dispositivos Móveis  
**Professor:** Pedro Hericson Machado Araújo  
**Pacote Base:** `com.example.trihb`

---

## 1. Contas de Teste Pré-Cadastradas

O banco de dados local **Room (SQLite)** inicia pré-populado (`SeedCallback` em `AppDatabase.kt`) com duas contas independentes para testes:

| Conta | E-mail (Login) | Senha | Perfil / Foco Configurado | Dados Pré-Carregados |
| :--- | :--- | :--- | :--- | :--- |
| **Alan** | `alan@trihub.com` | `123456` | **Emagrecimento**<br>Modalidades: *Musculação, Corrida*<br>Peso: `74,2 kg` → Meta: `72,0 kg`<br>Altura: `176 cm` (IMC `24,0`) | • 4 refeições (`07:30`, `12:30`, `16:30`, `20:00`)<br>• Hidratação: `1.500 / 2.500 ml`<br>• 3 pesagens históricas (`76,0 kg` → `75,1 kg` → `74,2 kg`)<br>• Histórico inicial no chat |
| **Marina S.** | `marina@trihub.com` | `123456` | **Performance Esportiva**<br>Modalidades: *Corrida, Ciclismo, Natação*<br>Restrição: *Sem Lactose*<br>Peso: `59,4 kg` → Meta: `58,5 kg`<br>Altura: `166 cm` (IMC `21,6`) | • 4 refeições adaptadas sem lactose<br>• Hidratação: `2.250 / 2.800 ml`<br>• 3 pesagens históricas (`60,5 kg` → `59,9 kg` → `59,4 kg`)<br>• Histórico de chat próprio |

---

## 2. Stack Tecnológica

| Camada | Tecnologia | Uso no Projeto |
| :--- | :--- | :--- |
| **Linguagem & Concorrência** | **Kotlin 2.2** + **Coroutines 1.10** (`Flow`, `StateFlow`, `combine`, `flatMapLatest`) | Execução de consultas ao banco (`suspend`) e leitura de sensores fora da *Main Thread*. |
| **Interface Gráfica (UI)** | **Jetpack Compose** + **Material Design 3** | Interface declarativa em Kotlin sem uso de layouts XML (`res/layout/`) ou `ViewBinding`. |
| **Gráficos 2D** | **Compose Canvas** (`androidx.compose.foundation.Canvas`) | Renderização nativa do gráfico de evolução de peso, área de tendência e linha de meta. |
| **Navegação** | **Navigation Compose 2.10** (`NavHost`, `NavController`, `navArgument`) | Estrutura em Activity única (`MainActivity`) com 9 rotas e passagem tipada de argumentos (`NavType.IntType`). |
| **Arquitetura** | **Lifecycle ViewModel Compose 2.11** (`collectAsStateWithLifecycle`) | Preservação de estado em mudanças de configuração e coleta vinculada ao ciclo de vida da tela. |
| **Persistência Local** | **Room Database 2.8** + **KSP** (SQLite em modo **WAL**) | Modelagem relacional com chaves estrangeiras (`CASCADE`), índices compostos (`@Index`) e consultas reativas via `Flow`. |
| **Sensores Físicos** | **Android `SensorManager`** (`Sensor.TYPE_ACCELEROMETER`) | Detecção de movimento físico para ajuste dinâmico das metas diárias de calorias e hidratação. |
| **Mídia Local** | **Android Photo Picker** (`ActivityResultContracts.PickVisualMedia`) | Seleção de foto da galeria com persistência no diretório interno do aplicativo (`filesDir/avatars/`). |
| **Assistente Nutricional** | **`NutriAiService`** (Motor Local + Integração opcional Gemini REST) | Respostas baseadas nos dados reais da conta no Room em modo offline, com suporte opcional à API do Gemini quando online. |

---

## 3. Detalhamento da Arquitetura e Módulos

### 3.1. Interface Declarativa em Jetpack Compose
- O `app/build.gradle.kts` utiliza `compose = true` e `viewBinding = false`.
- A abertura do aplicativo (`IntroSplashScreen.kt`) executa a animação da logo com `Animatable(0f)` e `LaunchedEffect(Unit)` antes de verificar a sessão ativa no banco.

### 3.2. State Hoisting e Fluxo Unidirecional de Dados (UDF)
- As telas seguem a separação entre dois composables:
  1. **Rota *Stateful* (`*Screen`):** obtém a instância do `ViewModel` via `AppViewModelProvider.Factory` e observa o `StateFlow` com `collectAsStateWithLifecycle()`.
  2. **Conteúdo *Stateless* (`*Content`):** recebe apenas o estado imutável (`uiState`) e funções *lambda* de evento, facilitando a manutenção e o uso de `@Preview`.

### 3.3. Banco de Dados Relacional (`Room v2`)
- Foram estruturadas **7 tabelas SQLite** em `data/local/entity/`:
  1. `usuarios` (`UsuarioEntity`): credenciais (`email` com índice `UNIQUE` e `senhaHash` em SHA-256), foto local (`fotoUri`), bio, peso, altura, metas, foco principal, modalidades, nível de atividade, restrições alimentares e `preferenciasJson`.
  2. `sessao_ativa` (`SessaoEntity`): controle da conta autenticada (`usuarioAtivoId`) e configurações locais.
  3. `refeicoes` (`RefeicaoEntity`): vinculada ao usuário por `ForeignKey.CASCADE` e índice composto `(usuarioId, horario)`.
  4. `hidratacao` (`HidratacaoEntity`): relação `1:1` por `usuarioId` (`ON DELETE CASCADE`).
  5. `progresso_corporal` (`ProgressoEntity`): histórico de peso, cintura e quadril por `usuarioId` (`ON DELETE CASCADE`).
  6. `chat_mensagens` (`MensagemEntity`): histórico de conversa por `usuarioId` (`ON DELETE CASCADE`).
  7. `feed_posts` (`PostFeedEntity`): mural comunitário compartilhado no dispositivo, utilizando `autorUsuarioId` para controle de autoria e exclusão.
- No `TriHubRepository.kt`, o operador `flatMapLatest` observa o `usuarioAtivoIdFlow` e alterna automaticamente os fluxos de refeições, hidratação, progresso e chat sempre que o usuário troca de conta.

### 3.4. Sensor Acelerômetro (`ActivitySensorManager.kt`)
- A partir dos eixos `(x, y, z)` do `Sensor.TYPE_ACCELEROMETER`, calcula-se a magnitude vetorial `sqrt(x² + y² + z²)` e subtrai-se a gravidade padrão (`9,81 m/s²`).
- Variações acima de `2,2 m/s²` com intervalo mínimo de `350 ms` (*debounce*) incrementam o contador reativo `passosDetectados`.
- A cada **20 passos**, a classe `BonusAtividade` adiciona `+15 kcal` à meta calórica e `+25 ml` à meta de hidratação do dia.
- O registro e a liberação do sensor são gerenciados via `DisposableEffect` (`ON_RESUME` / `ON_PAUSE`) em `TriHubNavGraph.kt`.

### 3.5. Assistente Nutricional (`NutriAiService.kt`)
- Antes de processar uma pergunta no chat, o `ChatViewModel` monta o `NutriUserContext` a partir da leitura transacional (`UsuarioResumoRelation`) e do contador de passos, reunindo perfil, IMC, restrições, refeições pendentes/concluídas, saldo calórico e consumo de água.
- Em modo offline, o serviço local analisa a pergunta e responde utilizando os números reais da conta ativa. Quando há conexão e uma chave configurada no perfil, realiza a chamada HTTPS ao modelo `gemini-2.5-flash` enviando o mesmo contexto do banco local.

### 3.6. Tutorial de Introdução (`TutorialOverlay.kt`)
- Guia em 6 passos que navega pelas 5 abas principais exibindo a função de cada botão da interface.
- É apresentado automaticamente para novas contas, possui opção de pular em todos os passos (persistindo o estado em `preferenciasJson`) e pode ser reaberto pelo botão `?` no topo da aba Dieta.
