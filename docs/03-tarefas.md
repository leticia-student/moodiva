# 03 — Tarefas do Moodiva

> Decomposição de `02-planejamento.md`. Uma por vez, nesta ordem, um commit por tarefa (sem push).

## Definição de pronto (vale para todas)

- `./gradlew assembleDebug` compila.
- `./gradlew testDebugUnitTest` passa (e o resultado é registrado em `TEST_RESULTS.md` quando a tarefa tem testes).
- Regra de dependência entre camadas respeitada (domain sem `android.*`, Retrofit, Room ou Koin).
- UI conforme `docs/identidade_visual.md`, com `@PreviewLightDark` em toda tela.
- Checkbox da tarefa marcado; README atualizado só com o que existe.
- Commit em Conventional Commits, em português, sem acento no assunto.

## Documentação

### [x] T00 — Planejamento, tarefas e CLAUDE.md atualizado
- **Descrição:** `docs/02-planejamento.md`, `docs/03-tarefas.md`; `CLAUDE.md` com decisões resolvidas, login local e autonomia.
- **Pronto quando:** documentos existem e não há decisão pendente.
- **Relacionado:** todas · Seções 1–13
- **Commit:** `docs: adiciona planejamento e tarefas e atualiza claude.md`

## Configuração

### [x] T01 — Build, dependências e README inicial
- **Descrição:** `libs.versions.toml` com a stack da seção 4; plugins serialization e KSP; `BuildConfig.LASTFM_API_KEY` a partir do `local.properties`; permissões `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`; remover testes de exemplo e Espresso; README inicial.
- **Pronto quando:** `assembleDebug` compila com todas as dependências.
- **Relacionado:** RNF03, RNF04 · Seções 1, 4
- **Commit:** `chore: configura dependencias e chave do last.fm`

### [x] T02 — Tema da identidade visual e ícone
- **Descrição:** `presentation/theme/` (Color, Type, Theme, Dimens), `themes.xml` sem barra de ação, ícone adaptativo de `docs/`, ícones vetoriais (nota musical, nuvem cortada, fone).
- **Pronto quando:** app abre com o tema; previews claro/escuro.
- **Relacionado:** RNF05–RNF07 · Seção 9.3
- **Commit:** `feat: aplica identidade visual e icone do app`

### [x] T03 — Core, Application e Koin
- **Descrição:** `AppResult`, `AppError`, `AppDispatchers`, `NetworkMonitor` + `AndroidNetworkMonitor`; `MoodivaApp` iniciando o Koin; módulos vazios.
- **Pronto quando:** app inicia com o Koin sem erro.
- **Relacionado:** RNF01, RNF02 · Seções 2, 10
- **Commit:** `feat: adiciona pacote core e injecao de dependencias`

## Domain

### [x] T04 — Modelos de domínio e interfaces de repositório
- **Descrição:** modelos da seção 2.3 e interfaces `AuthRepository`, `ProfileRepository`, `MusicRepository`, `RecommendationRepository`, `SavedTrackRepository`.
- **Pronto quando:** compila; nenhum import proibido no domain.
- **Relacionado:** RNF02 · Seção 2.3
- **Commit:** `feat: adiciona modelos e contratos do dominio`

### [x] T05 — Regras de negócio + testes
- **Descrição:** classes de `domain/rule` da seção 2.4 com testes unitários (inclui CA08, CA13–CA15, CA21, CA23).
- **Pronto quando:** testes passam e estão registrados.
- **Relacionado:** RN01–RN37 · Seção 2.4
- **Commit:** `feat: implementa regras de negocio com testes`

### [x] T06 — Casos de uso + testes
- **Descrição:** casos de uso da seção 2.4, incluindo o motor de recomendação (seção 3.4), com fakes em memória.
- **Pronto quando:** testes de motor, cache, sessão e salvas passam.
- **Relacionado:** RF01–RF26, RN18–RN26 · Seções 2.4, 3.4, 6
- **Commit:** `feat: implementa casos de uso com testes`

## Data

### [ ] T07 — API do Last.fm, DTOs, interceptor e mappers + testes
- **Descrição:** `LastFmApi`, DTOs com `@SerialName`, `LenientStringSerializer`, `SingleOrListSerializer`, `ApiKeyInterceptor`, `RemoteMappers` (duração s/ms, placeholder, `error` 6).
- **Pronto quando:** testes de parsing com JSON real passam.
- **Relacionado:** RNF03, RN27 · Seção 3
- **Commit:** `feat: adiciona cliente do last.fm com testes de mapeamento`

### [ ] T08 — Room, autenticação local e repositórios + testes
- **Descrição:** entidades, DAOs, `AppDatabase`, `PasswordHasher`, `SessionStore`, implementações dos repositórios, `safeApiCall`/`safeDbCall`; registro no Koin.
- **Pronto quando:** testes com MockK (Offline First, transação, uid, hash) passam.
- **Relacionado:** RNF01, RNF08, RNF14, RN16, RN25, RN32 · Seções 5–7
- **Commit:** `feat: implementa camada de dados offline first com testes`

## Presentation

### [ ] T09 — Navegação, raiz do app e componentes de estado
- **Descrição:** rotas, `MoodivaNavHost`, `MoodivaAppViewModel` (sessão + conexão), splash, banner offline, Loading/Empty/Error, `ErrorMessageMapper`.
- **Pronto quando:** app navega para Auth sem sessão; testes do ViewModel raiz passam.
- **Relacionado:** RF04, RF22 · Seção 9.1
- **Commit:** `feat: adiciona navegacao e estrutura raiz do app`

### [ ] T10 — Login e cadastro
- **Relacionado:** RF01–RF03, CA01–CA04 · **Commit:** `feat: adiciona login e cadastro locais`

### [ ] T11 — Onboarding (TIPI e gostos)
- **Relacionado:** RF04–RF10, RF12, CA05–CA09, CA11 · **Commit:** `feat: adiciona onboarding com teste tipi e gostos`

### [ ] T12 — Conversa guiada
- **Relacionado:** RF13, RF14, CA12–CA15 · **Commit:** `feat: adiciona conversa guiada`

### [ ] T13 — Recomendações
- **Relacionado:** RF15–RF18, CA16–CA26 · **Commit:** `feat: adiciona tela de recomendacoes`

### [ ] T14 — Músicas salvas
- **Relacionado:** RF19–RF21, RF26, CA27–CA30, CA38 · **Commit:** `feat: adiciona lista de musicas salvas`

### [ ] T15 — Perfil e Sobre
- **Relacionado:** RF11, RF12, RF25, CA10, CA34 · **Commit:** `feat: adiciona perfil e tela sobre`

## Notificação

### [ ] T16 — Lembrete diário
- **Descrição:** Worker, canal, agendador, diálogo explicativo e permissão (Android 13+).
- **Relacionado:** RF23, RF24, RN34, RN35, CA32, CA33 · **Commit:** `feat: adiciona lembrete diario`

## Validação

### [ ] T17 — Validação final
- **Descrição:** testes completos, teste ponta a ponta no emulador Medium Phone API 37 com capturas em `evidencias/`, README completo, tabela CA → verificação em `TEST_RESULTS.md`.
- **Relacionado:** CA01–CA38 · **Commit:** `docs: registra validacao final`
