# TriHub

Aplicativo Android desenvolvido como projeto final (AP2) da disciplina **Desenvolvimento de Software para Dispositivos Móveis (RUS0291)** na **UFC — Campus Russas**, ministrada pelo Prof. Pedro Hericson Machado Araújo.

A ideia do **TriHub** (que começou como *NutriHub*) é juntar em um lugar só o controle das refeições do dia, o acompanhamento de peso e medidas usando o sensor de movimento do celular, um mural de conquistas parecido com o Strava e um chat rápido para tirar dúvidas de substituição alimentar. O app foi feito para rodar 100% offline usando banco local SQLite (Room).

Para detalhes sobre a modelagem das tabelas, sensores e arquitetura dos módulos, consulte o arquivo [Especificação Técnica e Roadmap de Desenvolvimento.md](./Especifica%C3%A7%C3%A3o%20T%C3%A9cnica%20e%20Roadmap%20de%20Desenvolvimento.md).

---

## Contas de teste

O banco já inicia com duas contas preenchidas para facilitar os testes sem precisar criar um cadastro do zero:

| Usuário | E-mail | Senha | Perfil configurado |
| :--- | :--- | :--- | :--- |
| **Alan** | `alan@trihub.com` | `123456` | Foco em emagrecimento (musculação e corrida), 4 refeições cadastradas, 1500/2500 ml de água e histórico de peso (`76,0 kg` a `74,2 kg`). |
| **Marina S.** | `marina@trihub.com` | `123456` | Foco em performance esportiva (corrida, ciclismo e natação), restrição a lactose, 4 refeições adaptadas e histórico próprio. |

Também é possível criar uma conta nova pela tela de cadastro. Nesse caso, o app monta automaticamente 4 refeições iniciais de acordo com o objetivo escolhido e abre o tutorial de boas-vindas.

---

## Funcionalidades

- **Login e cadastro local:** autenticação com e-mail e senha (salva com hash SHA-256) e separação dos dados por usuário no banco local.
- **Tutorial inicial:** guia passo a passo que passa pelas 5 abas mostrando para que serve cada botão. Pode ser pulado a qualquer momento ou reaberto pelo ícone `?` no topo da aba Dieta.
- **Dieta:** checklist de refeições do dia, controle de água (`+250 ml` / `-250 ml`), barra de calorias e cadastro/edição/exclusão de refeições.
- **Progresso e acelerômetro:** leitura do sensor de movimento do aparelho (`TYPE_ACCELEROMETER`). A cada 20 passos detectados, a meta diária ganha `+15 kcal` e `+25 ml` de água (há também um botão para simular `+20 passos` durante testes em bancada). Inclui histórico de peso, cintura e quadril com gráfico desenhado em `Canvas`.
- **Feed:** mural para postar conquistas do dia com tags rápidas (água, refeição ou treino), botão de incentivo (aplausos) e opção de apagar as próprias postagens.
- **Chat (Dra. Camila):** chat de dúvidas sobre alimentação. Em modo offline, o assistente consulta os dados reais da conta no Room (refeições que faltam no dia, água consumida, peso, IMC e restrições) para montar a resposta. Se houver chave da API do Gemini configurada no perfil e internet ativa, usa o modelo `gemini-2.5-flash`.
- **Perfil:** escolha de foto da galeria (salva no armazenamento interno do app), bio, peso, altura com cálculo automático de IMC, modalidades praticadas, foco principal, restrições alimentares e troca de conta.

---

## Tecnologias utilizadas

- **Kotlin** + **Coroutines / StateFlow**
- **Jetpack Compose** + **Material Design 3** (interface declarativa, sem layouts XML)
- **Navigation Compose** (Activity única com passagem de argumentos tipados, como `detalhe_refeicao/{refeicaoId}`)
- **Arquitetura MVVM** com *Repository Pattern* e *State Hoisting* (separação entre `*Screen` stateful e `*Content` stateless)
- **Room Database (SQLite)** com 7 tabelas relacionais, chaves estrangeiras (`CASCADE`) e índices
- **SensorManager** (`Sensor.TYPE_ACCELEROMETER`) integrado ao ciclo de vida da tela via `DisposableEffect`

---

## Estrutura do projeto

```text
app/src/main/java/com/example/trihb/
├── MainActivity.kt
├── data/
│   ├── ai/NutriAiService.kt
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/        # UsuarioDao, RefeicaoDao, ProgressoDao, PostFeedDao, MensagemDao
│   │   └── entity/     # Usuario, Sessao, Refeicao, Hidratacao, Progresso, PostFeed, Mensagem
│   └── repository/TriHubRepository.kt
├── sensor/ActivitySensorManager.kt
├── ui/
│   ├── AppViewModelProvider.kt
│   ├── components/     # MetaDiariaCard, RefeicaoItemCard, TriHubBottomBar, TutorialOverlay
│   ├── navigation/     # Screen.kt e TriHubNavGraph.kt
│   ├── screens/        # intro, auth, dieta, progresso, feed, chat, perfil
│   └── theme/          # Color.kt, Type.kt, Theme.kt
└── util/DataHora.kt
```

---

## Como executar

1. Clone o repositório:
   ```bash
   git clone https://github.com/Alanzww/Trihb.git
   ```
2. Abra a pasta no **Android Studio** e aguarde a sincronização do Gradle.
3. Execute em um dispositivo físico ou emulador com Android 7.0+ (`minSdk 24`).

Para gerar o APK de debug pelo terminal:
```powershell
.\gradlew.bat assembleDebug
```
O arquivo gerado fica em `app/build/outputs/apk/debug/app-debug.apk`.
