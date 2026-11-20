# 02 — Planejamento técnico do Moodiva

> Fase 2 do SDD. Responde **como** a especificação (`01-especificacao.md`) será construída.
> Códigos RF/RNF/RN/CA referem-se à especificação.

## 1. Ponto de partida

O projeto veio do template "Empty Activity" do Android Studio (AGP 9.5.1, Kotlin embutido no AGP, `compileSdk`/`targetSdk` 37, `minSdk` 24, pacote `com.leticia.moodiva`).

Ajustes no template:

- Mover `ui/theme/` para `presentation/theme/` (identidade visual).
- Remover `ExampleUnitTest`, `ExampleInstrumentedTest` e as dependências de Espresso/teste instrumentado (os testes do projeto são JVM).
- Substituir o ícone do template pelos XMLs de `docs/` (`res/drawable` e `res/mipmap-anydpi-v26`). Os PNGs `mipmap-*dpi` ficam como fallback para Android 7 (minSdk 24).
- Projeto de referência com a mesma stack e versões testadas nesta máquina: KeveOQ (`C:\dev\keveoq-app`).

## 2. Arquitetura

### 2.1 Camadas e regra de dependência

```
presentation  →  domain  ←  data
      ↘          ↑          ↙
         core (AppResult, AppError, AppDispatchers, NetworkMonitor)
di (Koin) conhece todas as camadas · notification usa o domain para o horário
```

- `domain` é Kotlin puro: sem `android.*`, Retrofit, Room ou Koin (CA35). Usa `java.time` e `kotlinx.coroutines` (Flow).
- `data` implementa as interfaces do domain; DTOs e entidades não saem dela.
- `presentation` só conversa com casos de uso.

### 2.2 Estrutura de pacotes

```
com.leticia.moodiva/
├── MoodivaApp.kt, MainActivity.kt
├── core/          result/ (AppResult, AppError) · dispatcher/ · network/
├── di/            appModule, dataModule, domainModule, presentationModule
├── domain/
│   ├── model/     Mood, ConversationMode, ConversationStep, PersonalityProfile, Trait,
│   │              TasteProfile, Genre, Era, OriginPreference, UserProfile, Track,
│   │              TrackSource, RecommendedTrack, Recommendations, SavedTrack,
│   │              ListenStatus, User, ArtistTag, SimilarArtist, TagTrack, TrackDetails
│   ├── repository/ AuthRepository, ProfileRepository, MusicRepository,
│   │              RecommendationRepository, SavedTrackRepository
│   ├── rule/      TipiCalculator, TextNormalizer, MoodTagMapper, OpennessProfile,
│   │              ExtraversionTiebreak, ArtistClassifier, OriginFilter, TrackKey,
│   │              MoodSourceRanker, TasteSourceRanker, RecommendationMerger,
│   │              ConversationMachine, SavedTrackRules, CredentialRules, CachePolicy,
│   │              NotificationWindow
│   └── usecase/   (seção 2.4)
├── data/
│   ├── remote/    LastFmApi, ApiKeyInterceptor, dto/, LenientStringSerializer, SingleOrListSerializer
│   ├── auth/      PasswordHasher, SessionStore, AuthRepositoryImpl
│   ├── local/     AppDatabase, Converters, dao/, entity/
│   ├── mapper/    RemoteMappers, LocalMappers
│   └── repository/ *RepositoryImpl, SafeApiCall, SafeDbCall
├── notification/  DailyReminderWorker, NotificationChannels, NotificationScheduler,
│                  NotificationPermissionPrefs
└── presentation/
    ├── navigation/ Routes, MoodivaNavHost
    ├── theme/      Color, Type, Theme, Dimens
    ├── components/ StateViews, OfflineBanner, TrackCover, SourceBadge, RatingBar,
    │               ChatBubble, MoodivaTopBar, TrackCard
    ├── util/       ErrorMessageMapper, LabelMappers, ExternalLinks
    └── screens/    auth/, onboarding/, chat/, recommendations/, saved/, profile/, about/
```

O `CLAUDE.md` coloca o Worker em `data/notification/`. Ele fica em `notification/` na raiz porque depende de Android (WorkManager, NotificationCompat) e da `MainActivity`, e não é uma fonte de dados. Esse é o mesmo padrão da skill `android-workmanager-notification`.

### 2.3 Modelos de domínio (principais)

| Modelo | Campos |
|---|---|
| `User` | `id: String` (UUID), `email: String` |
| `Trait` | enum `EXTRAVERSION, AGREEABLENESS, CONSCIENTIOUSNESS, EMOTIONAL_STABILITY, OPENNESS` |
| `PersonalityProfile` | `answers: List<Int>` (10), `scores: Map<Trait, Double>` |
| `Genre` | enum com `tags: Set<String>` normalizadas (seção 8.8 da especificação) |
| `Era` | enum `SIXTIES … TWENTY_TWENTIES` |
| `OriginPreference` | enum `NATIONAL, INTERNATIONAL, BOTH` |
| `TasteProfile` | `genres: Set<Genre>`, `favoriteArtists: List<String>`, `eras: Set<Era>`, `origin: OriginPreference` |
| `UserProfile` | `personality: PersonalityProfile`, `taste: TasteProfile` |
| `Mood` | enum de 8 humores; `ConversationMode` = `MATCH, SHIFT` |
| `TrackSource` | `MOOD, TASTE` |
| `Track` | `name`, `artist`, `key` (normalizado, RN23) |
| `RecommendedTrack` | `track`, `source`, `coverUrl: String?`, `coverChecked: Boolean`, `position` |
| `Recommendations` | `tracks: List<RecommendedTrack>`, `updatedAt: Instant?` |
| `ListenStatus` | `WANT_TO_LISTEN, LISTENED` |
| `SavedTrack` | `track`, `coverUrl`, `source`, `status`, `rating: Int?`, `savedAt` |
| `ConversationStep` | `Greeting`, `AskMood`, `AskMode(mood)`, `Done(mood, mode)` |

### 2.4 Regras e casos de uso

| Classe | RN/RF | Descrição |
|---|---|---|
| `TipiCalculator` | RN01–RN04 | Valida as respostas e calcula os 5 traços. |
| `TextNormalizer` | RN12 | Minúsculas, sem acentos (`java.text.Normalizer`), espaços colapsados. |
| `TrackKey` | RN23 | `normalize(artist) + "|" + normalize(name)`. |
| `MoodTagMapper` | RN09, RN10 | Tags por humor e modo; `asksForMode(mood)`. |
| `OpennessProfile` | RN06 | Faixa de `match` a partir da Abertura. |
| `ExtraversionTiebreak` | RN07 | Pontuação de desempate pelas tags da música. |
| `ArtistClassifier` | RN13, RN14 | Gêneros (lista fechada, `count` ≥ 10) e nacionalidade. |
| `OriginFilter` | RN17 | Aceita ou rejeita um artista pela preferência. |
| `MoodSourceRanker` | RN18, RN19 | Ordena e filtra as músicas da fonte principal. |
| `TasteSourceRanker` | RN06, RN20 | Escolhe os artistas similares e ordena os candidatos. |
| `RecommendationMerger` | RN20–RN24 | Junta as fontes, deduplica e limita a 20. |
| `ConversationMachine` | RN10, RN11 | Máquina de estados da conversa. |
| `SavedTrackRules` | RN29–RN31 | Transição de status e validação de nota. |
| `CredentialRules` | RF01 | Formato do e-mail, senha ≥ 6 e confirmação. |
| `CachePolicy` | RN25 | Cache válido por 24 h. |
| `NotificationWindow` | RN34 | Atraso até a próxima janela das 19h às 21h. |
| `SignUpUseCase`, `SignInUseCase`, `SignOutUseCase`, `ObserveSessionUseCase` | RF01–RF04 | Conta local. |
| `SaveProfileUseCase`, `ObserveProfileUseCase`, `CalculatePersonalityUseCase` | RF05–RF12, RN26 | Perfil; ao salvar, limpa o cache de recomendações. |
| `GenerateRecommendationsUseCase` | RN18–RN24 | O motor das duas fontes (usa `MusicRepository`). |
| `RefreshRecommendationsUseCase` | RN25, RNF01 | Decide se busca (cache, conexão, `force`) e grava. |
| `ObserveRecommendationsUseCase` | RN24 | Cache + remoção das músicas `LISTENED`. |
| `LoadCoverUseCase` | RN27, RN28 | Capa sob demanda para a fonte principal. |
| `SaveTrackUseCase`, `ObserveSavedTracksUseCase`, `MarkAsListenedUseCase`, `RateTrackUseCase`, `RemoveSavedTrackUseCase` | RF18–RF21, RF26 | Músicas salvas. |
| `ObserveConnectivityUseCase` | RF22 | Estado da conexão. |
| `CalculateNotificationDelayUseCase` | RN34 | Atraso inicial da notificação. |

O "usuário atual" vem do `AuthRepository.currentUserId` (`StateFlow<String?>`). Os casos de uso que mexem em dados pessoais leem o uid dali e devolvem `AppError.NotSignedIn` se não houver sessão.

## 3. API do Last.fm

### 3.1 Por que esta API

É gratuita para uso não comercial e tem tags colaborativas de humor (happy, sad, chill…), artistas similares com grau de semelhança e capas reais via `track.getInfo`. O Deezer foi descartado no spike.

### 3.2 Autenticação

`LASTFM_API_KEY` em `local.properties` → `BuildConfig.LASTFM_API_KEY` → `ApiKeyInterceptor`, que acrescenta `api_key` e `format=json` à query de toda requisição. O log HTTP (só em debug) usa nível `BASIC` e remove a chave da URL registrada.

### 3.3 Endpoints (base `https://ws.audioscrobbler.com/2.0/`)

| Método | Parâmetros | Uso | Particularidades verificadas em 2026-11-19 |
|---|---|---|---|
| `tag.getTopTracks` | `tag`, `limit=30`, `page=1` | Fonte principal | `duration` em **segundos** (texto); imagem sempre placeholder; `@attr.rank` |
| `artist.getTopTags` | `artist`, `autocorrect=1` | Gêneros e nacionalidade | `count` chega como **número** (o app aceita número ou texto) |
| `artist.getSimilar` | `artist`, `limit=30`, `autocorrect=1` | Fonte de complemento | `match` em texto ("0.690513") |
| `artist.getTopTracks` | `artist`, `limit=3`, `autocorrect=1` | Candidatos do complemento | `listeners` em texto; imagem placeholder |
| `track.getInfo` | `artist`, `track`, `autocorrect=1` | Capa, tags da música | `duration` em **milissegundos**; `album` pode faltar; `toptags.tag` pode vir vazio |

Erros: "não encontrado" chega com **HTTP 200** e corpo `{"error": 6, "message": …}`; chave inválida chega com **HTTP 403** e `error: 10`; limite excedido chega com `error: 29`. O mapper trata o `error` 6 como "sem dados" (artista sem tags = sem gêneros; música não encontrada = capa indisponível). Os demais erros viram `AppError.Api(code)`.

Arrays com um único item: o Last.fm às vezes entrega um objeto no lugar de uma lista. O `SingleOrListSerializer` aceita os dois formatos.

### 3.4 Algoritmo de recomendação (`GenerateRecommendationsUseCase`)

1. `tags = MoodTagMapper.tagsFor(mood, mode)`.
2. **Fonte principal:** para cada tag, `tag.getTopTracks(limit=30)`. Para cada artista distinto, `artistTags(artist)`: primeiro o cache sem validade (`artist_tags`), depois a rede. O `ArtistClassifier` dá os gêneros e a nacionalidade. Ficam só as músicas com gênero em comum com o perfil e que passam no `OriginFilter`. O `MoodSourceRanker` ordena por (nº de tags do humor em que a música aparece ↓, melhor rank ↑, desempate da Extroversão ↓).
3. Se a fonte principal tiver **≥ 10** músicas, a lista final são as 20 primeiras.
4. **Fonte de complemento** (se < 10):
   - para cada favorito, `artist.getSimilar(limit=30)`;
   - juntar e remover favoritos e repetidos, ficando com o maior `match` de cada artista;
   - filtrar pela faixa de `match` da Abertura;
   - ordenar por `match` ↓ e escolher **8 artistas espaçados uniformemente** na lista. Na faixa "descoberta" isso inclui artistas mais distantes; na "familiaridade" são os mais próximos;
   - filtrar pela origem (`artistTags`);
   - `artist.getTopTracks(limit=3)` de cada artista; candidatos intercalados por artista (1º de cada, 2º de cada…);
   - os 20 primeiros candidatos passam por `track.getInfo` (capa + tags);
   - o `TasteSourceRanker` ordena por (tem tag do humor ↓, `match` do artista ↓, desempate da Extroversão ↓).
5. O `RecommendationMerger` junta as fontes: principal primeiro e complemento depois, sem duplicadas (`TrackKey`; empate fica com `MOOD`), até 20.
6. Falhas: se uma chamada da fonte principal falhar, a geração inteira falha e o cache anterior fica intacto. Se só a fonte de complemento falhar e já houver músicas pelo humor, a lista fica só com elas (melhor do que nenhuma). Erros "não encontrado" só pulam o item.
7. Concorrência: as chamadas independentes rodam em paralelo com um `Semaphore(4)` (RNF10).

## 4. Stack e versões

As mesmas versões já validadas no KeveOQ nesta máquina:

| Biblioteca | Versão | Motivo |
|---|---|---|
| AGP | 9.5.1 | Template do Android Studio; Kotlin embutido |
| Kotlin (compose/serialization plugins) | 2.4.21 | Compatível com AGP 9.5 e KSP 2.3.14 |
| KSP | 2.3.14 | Compilador do Room |
| Compose BOM | 2026.09.00 | Material 3, PullToRefreshBox |
| Lifecycle | 2.11.0 | `collectAsStateWithLifecycle`, ViewModel |
| Navigation Compose | 2.10.2 | Rotas type-safe |
| Koin BOM | 4.2.2 | DI (`koin-android`, `koin-androidx-compose`) |
| Coroutines | 1.11.0 | Flow, `Semaphore`, testes |
| kotlinx.serialization | 1.13.0 | JSON e rotas |
| Retrofit | 3.0.0 | + `converter-kotlinx-serialization` |
| OkHttp | 5.5.1 | + `logging-interceptor` |
| Room | 2.8.5 | Banco local |
| Coil | 3.6.3 | `coil-compose` + `coil-network-okhttp`, cache em disco |
| WorkManager | 2.12.0 | Lembrete diário |
| JUnit 4.13.2 · MockK 1.14.12 · Turbine 1.2.1 | — | Testes JVM |

Nenhuma dependência do Firebase. O hash de senha usa `javax.crypto` (PBKDF2WithHmacSHA256), que já faz parte do Android.

## 5. Modelo de dados (Room, `moodiva.db`, versão 1)

```
users                 profiles                          saved_tracks
──────────────        ──────────────────────            ─────────────────────────
uid (PK)              uid (PK)                          uid ─┐ (PK composta)
email (UNIQUE)        answers  "5,3,6,…"                track_key ─┘
password_hash         extraversion … openness (REAL)    name, artist, cover_url
password_salt         genres "POP,MPB"                  source, status, rating
created_at            favorite_artists (JSON)           saved_at
                      eras "EIGHTIES"
                      origin, updated_at

recommendations                       recommendation_meta       artist_tags (público)
──────────────────────────            ────────────────────      ─────────────────────
uid, mood, mode, track_key (PK)       uid, mood, mode (PK)      artist_key (PK)
position, name, artist, source        updated_at                tags (JSON [{name,count}])
                                                                fetched_at
track_covers (público)
──────────────────────
track_key (PK), cover_url (nulo = indisponível), fetched_at
```

- As recomendações são lidas com `LEFT JOIN track_covers` para trazer a capa já buscada.
- A `artist_tags` guarda as tags **brutas**: a classificação (gêneros e nacionalidade) acontece no domain, testável, a cada uso.
- A substituição das recomendações de (uid, humor, modo) é feita numa `@Transaction`: apaga as antigas, insere as novas e atualiza a `meta`.
- A sessão (uid logado) fica em `SharedPreferences` (`SessionStore`). Não é dado sensível: a senha nunca é guardada.

## 6. Offline First e sincronização

| Situação | Comportamento |
|---|---|
| Cache válido (< 24 h) | Mostra o cache; não chama a API (CA16) |
| Cache vencido + online | Mostra o cache; busca; o banco emite os dados novos (CA17) |
| Falha com cache | Mantém o cache; Snackbar com o erro (CA18) |
| Sem cache + offline | Estado vazio "preciso de internet na primeira busca" + "Tentar novamente" (CA19) |
| Pull-to-refresh | Busca mesmo com cache válido, com as mesmas regras de erro (CA20) |
| Capa | `LoadCoverUseCase` só chama a API se a capa nunca foi buscada e houver conexão |

A conversa, o perfil, as músicas salvas, o login e o cadastro são 100% locais.

## 7. Tratamento de erros

### 7.1 Conversão de exceções (`safeApiCall` / `safeDbCall`)

| Exceção / situação | `AppError` |
|---|---|
| `IOException` (sem rede, timeout, DNS) | `Network` |
| `HttpException` 403 / corpo `error` 10 ou 26 | `InvalidApiKey` |
| corpo `error` 29 | `RateLimited` |
| outro `HttpException` | `Http(code)` |
| outro `error` no corpo | `Api(code)` |
| `SerializationException` | `Serialization` |
| sem sessão | `NotSignedIn` |
| outras | `Unknown` |

### 7.2 Mensagens ao usuário (`ErrorMessageMapper`)

| `AppError` | Mensagem |
|---|---|
| `Network` | "Não foi possível buscar músicas. Verifique sua conexão." |
| `InvalidApiKey` | "A chave do Last.fm não foi configurada corretamente." |
| `RateLimited` | "Muitas buscas em pouco tempo. Tente de novo em alguns minutos." |
| `Http` 5xx / `Api` | "O Last.fm está instável agora. Tente novamente mais tarde." |
| demais | "Algo deu errado. Tente novamente." |

## 8. Tarefas em segundo plano

- `DailyReminderWorker` (`CoroutineWorker`): publica "Como você está hoje?" no canal "Lembretes diários". Se a permissão foi negada, termina sem notificar.
- `NotificationScheduler`: `PeriodicWorkRequest` de 1 dia, `initialDelay` até a janela das 19h às 21h (`NotificationWindow`, com `Clock` injetado), `ExistingPeriodicWorkPolicy.KEEP`.
- Permissão (Android 13+): depois do onboarding, na conversa, um diálogo explica o motivo ("Quer que eu te lembre de conversar comigo todo dia?"). Em "Permitir", o app pede a permissão do sistema; em "Agora não", não pergunta de novo (`NotificationPermissionPrefs`).

## 9. Interface e navegação

### 9.1 Rotas (type-safe)

| Rota | Tela |
|---|---|
| `AuthRoute` | Login e cadastro (alternância na mesma tela) |
| `OnboardingRoute(mode)` | `FULL`, `RETAKE_TEST` ou `EDIT_TASTE` |
| `HomeRoute` | Barra inferior: Conversa · Salvas · Perfil |
| `RecommendationsRoute(mood, mode)` | Lista de músicas |
| `AboutRoute` | Sobre |

O destino inicial depende da sessão (`MoodivaAppViewModel`): sem usuário → `AuthRoute`; usuário sem perfil → `OnboardingRoute(FULL)`; com perfil → `HomeRoute`. Enquanto a sessão carrega, aparece a "splash" com o nome Moodiva (`displayLarge`).

### 9.2 Telas e estados

| Tela | Estados / elementos |
|---|---|
| Auth | Nome Moodiva, e-mail, senha (mostrar/ocultar), confirmação no cadastro, erros por campo, indicador no botão |
| Onboarding | Aviso (RF06) → 10 itens TIPI (um por página, escala 1–7, progresso "3 de 10", avanço automático) → gêneros (chips) → artistas (campo + chips) → épocas (opcional) → origem → resultado |
| Conversa | Balões (app/usuário), chips de resposta, balão com indicador durante a busca, erro com "Tentar novamente"; ao concluir, abre as recomendações |
| Recomendações | Loading / Success (cards com capa, selo, Spotify, YouTube, salvar) / Empty / Error; pull-to-refresh; Snackbar |
| Salvas | Filtro (Todas, Quero ouvir, Ouvidas); "Já ouvi"; estrelas; remover com confirmação; vazio com "Conversar com a Moodiva" |
| Perfil | Traços (barra 1–7), gêneros, artistas, épocas, origem; Refazer teste, Editar gostos, Sobre, Sair |
| Sobre | Nome, versão, atribuições (Last.fm, Nunito, Sacramento, TIPI) |

### 9.3 Padrões de implementação

O padrão é o da skill `android-compose-screen`:

- `XRoute` (com estado, `koinViewModel`) e `XScreen` (sem estado, com previews `@PreviewLightDark`);
- um `StateFlow<UiState>` por ViewModel e eventos únicos via `Channel`;
- textos em `strings.xml`;
- cores, tipografia e formas só via `MaterialTheme`;
- espaçamentos em `Dimens`.

## 10. Injeção de dependências

| Módulo | Registra | Escopo |
|---|---|---|
| `appModule` | `AppDispatchers`, `Clock`, `NetworkMonitor`, WorkManager, `NotificationScheduler`, `NotificationPermissionPrefs` | `single` |
| `dataModule` | `Json`, OkHttp, Retrofit, `LastFmApi`, `AppDatabase` e DAOs, `PasswordHasher`, `SessionStore`, repositórios (pela interface) | `single` |
| `domainModule` | Casos de uso | `factory` |
| `presentationModule` | ViewModels (`parametersOf` para humor e modo) | `viewModel` |

## 11. Estratégia de testes

| Camada | O quê | Como |
|---|---|---|
| Domain/rules | Todas as RN (TIPI com o CA08, normalização, tags, abertura, extroversão, classificação, origem, rankers, merger, conversa, salvas, credenciais, cache, janela) | JUnit puro |
| Domain/usecases | Motor das duas fontes, refresh/cache/offline, perfil limpa cache, salvar/avaliar | Fakes em memória dos repositórios + `Clock` fixo |
| Data/mappers | Duração s × ms, placeholder de capa, `count` número/texto, `error` 6, lista × objeto único | JSON real copiado das respostas |
| Data/repositórios | Offline First, transação, isolamento por uid, hash de senha | MockK (API, DAO, `NetworkMonitor`) |
| Presentation | Sequência de `UiState` e eventos | `MainDispatcherRule` + Turbine |
| Ponta a ponta | Fluxo completo no emulador Medium Phone API 37 | Manual, com capturas em `evidencias/` |

## 12. Riscos e limitações

- Primeira busca de um humor pode levar alguns segundos (dezenas de artistas sem cache). Mitigação: cache sem validade dos artistas, paralelismo limitado e o balão de "carregando" na conversa.
- As tags de humor favorecem música internacional (L1). Para quem escolhe só "Nacionais", o complemento faz a maior parte do trabalho.
- O WorkManager não garante horário exato (economia de bateria).

## 13. Decisões registradas

| Data | Decisão | Motivo |
|---|---|---|
| 2026-11-19 | Versões idênticas às do KeveOQ | Já compilam e passam nos testes nesta máquina |
| 2026-11-19 | *Core library desugaring* (`desugar_jdk_libs` 2.1.5) | Mantém o `minSdk` 24 escolhido, com `java.time` disponível no Android 7 |
| 2026-11-19 | Tags brutas no cache, classificação no domain | Mantém RN13/RN14 testáveis e permite mudar a regra sem limpar o cache |
| 2026-11-19 | Sessão em `SharedPreferences` | Só um uid; evita adicionar DataStore (fora da stack) |
| 2026-11-19 | Worker em `notification/` (raiz) | Depende de Android e da MainActivity; não é fonte de dados |
| 2026-11-19 | Chips de resposta na conversa e navegação automática às recomendações ao concluir | Menos toques; o usuário volta à conversa para escolher outro humor |
| 2026-11-19 | TIPI com um item por página e avanço automático | Reduz carga visual e erros de toque numa escala de 7 pontos |
| 2026-11-19 | Selo "Pelo seu humor" em `primaryContainer` e "Pelo seu gosto" em `secondaryContainer`, forma `small` | A identidade visual não define o selo; usa só papéis já existentes e distingue as fontes |
| 2026-11-19 | Duração exibida no card ("4:21") | Dá uso real aos mappers de duração (segundos × milissegundos) exigidos pelo `CLAUDE.md` |
| 2026-11-19 | Log HTTP de depuração com `api_key` ocultado (`redactQueryParams`) | O Last.fm exige a chave na URL; o nível BASIC registra a URL inteira |
| 2026-11-19 | Componente `SecondaryButton` (borda `outline`, texto `primary`) | O `OutlinedButton` padrão do Material 3 atual usa `outlineVariant` e texto neutro, diferente da identidade visual |
