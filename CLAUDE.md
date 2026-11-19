# CLAUDE.md — Moodiva

## Contexto do projeto

Este repositório contém o **Moodiva**, um aplicativo Android que recomenda músicas de acordo com a personalidade, os gostos e o humor do dia do usuário. É um trabalho acadêmico, e os critérios de avaliação estão refletidos nestas instruções. Siga-as em todas as sessões.

O fluxo do app tem três momentos:

1. **Onboarding (uma vez):** o usuário responde a um teste de personalidade (TIPI, 10 itens) e a perguntas sobre gostos musicais (gêneros, artistas favoritos, épocas e preferência por músicas nacionais, internacionais ou ambas). O resultado é o perfil do usuário.
2. **Conversa diária:** uma tela de chat guiada, **sem IA**, em que o app pergunta como o usuário está e se ele quer músicas que acompanhem ou que mudem esse clima. O usuário responde tocando em opções (chips).
3. **Recomendações:** as regras do domínio cruzam humor, modo da conversa e perfil, consultam a API do Last.fm e apresentam músicas. Cada música pode ser aberta no Spotify ou no YouTube (o app não toca áudio), salva em uma lista e avaliada.

As funcionalidades principais devem funcionar sem internet: conversa, perfil, músicas salvas e as últimas recomendações em cache.

A identidade visual está em `docs/identidade_visual.md`. Leia-a antes de criar qualquer tela ou componente. A especificação, o planejamento e as tarefas estão em `docs/01-especificacao.md`, `docs/02-planejamento.md` e `docs/03-tarefas.md`.

## Regras de trabalho (leia antes de qualquer ação)

1. Siga o Spec-Driven Development (SDD), nesta ordem: Especificação → Planejamento → Decomposição → Implementação → Validação. Não escreva código de produção antes de os documentos de especificação, planejamento e tarefas existirem.
2. **Autonomia (autorizada em 2026-11-19):** o agente segue todas as fases até o app pronto sem parar para aprovação, fazendo os ajustes necessários desde que não mudem a ideia principal do app. Toda decisão nova é registrada em `docs/01-especificacao.md` (seção 10) ou `docs/02-planejamento.md`.
3. **Git:** faça um commit por tarefa (Conventional Commits em português). **Nunca faça `git push`**: a responsável envia ao GitHub.
4. Trabalhe uma tarefa por vez, na ordem de `docs/03-tarefas.md`. Ao concluir, marque a tarefa como concluída no arquivo.
5. Não adicione funcionalidades, bibliotecas ou telas fora da especificação, salvo quando forem necessárias para cumprir um requisito; nesse caso, registre o motivo.
6. Em caso de ambiguidade, escolha a opção de melhor experiência para o usuário que respeite este arquivo e registre a decisão.
7. Explique as decisões técnicas de forma didática: este é um projeto de aprendizado e preciso entender e conseguir defender cada parte do código.
8. Escreva respostas e documentação em português do Brasil. Nomes de classes, funções e variáveis em inglês.

## Stack obrigatória

| Área | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navegação | Navigation Compose com rotas type-safe (kotlinx.serialization) |
| Injeção de dependências | Koin (koin-android, koin-androidx-compose) |
| Assíncrono | Kotlin Coroutines, Flow e StateFlow |
| Rede | Retrofit + OkHttp + conversor kotlinx.serialization |
| Persistência | Room (com KSP) |
| Imagens | Coil (com cache em disco) |
| Autenticação | Local: contas no Room, senha com hash PBKDF2 (`javax.crypto`), sessão salva no aparelho. **Sem Firebase.** |
| Tarefas agendadas | WorkManager (somente para a notificação diária) |
| Testes | JUnit 4, MockK, kotlinx-coroutines-test, Turbine |
| Build | Gradle Kotlin DSL + Version Catalog (`gradle/libs.versions.toml`) |

Use as versões estáveis mais recentes e compatíveis entre si. Não use Hilt, Dagger, RxJava, LiveData, layouts XML nem Gson.

O WorkManager é o mecanismo padrão do Android para executar trabalho agendado mesmo com o app fechado. **Não use Firebase** (decisão de 2026-11-19): a autenticação é local.

## APIs

### Last.fm (recomendações)

- Documentação: https://www.last.fm/api
- Base URL: `https://ws.audioscrobbler.com/2.0/`
- Toda chamada usa os parâmetros `method`, `api_key` e `format=json`.
- Métodos usados:
  - `tag.getTopTracks` (`tag`, `limit`, `page`): músicas mais associadas a uma tag de humor.
  - `artist.getTopTags` (`artist`): gêneros e nacionalidade de um artista. Cada tag tem um `count` de 0 a 100 (peso relativo).
  - `artist.getSimilar` (`artist`, `limit`): artistas parecidos com os favoritos do usuário. Cada um tem um `match` de 0 a 1 (grau de semelhança).
  - `track.getInfo` (`artist`, `track`): capa do álbum (`album.image`), popularidade (`listeners`) e tags da própria música (`toptags`), numa única chamada.
  - `artist.getTopTracks` (`artist`, `limit`): músicas mais ouvidas de um artista similar, usadas como candidatas na fonte "Pelo seu gosto" (o `artist.getSimilar` devolve só artistas). As imagens também são placeholder.
- O Last.fm é a **única** API de música do app. O Deezer foi avaliado e descartado, porque o `track.getInfo` já fornece capas reais.
- Autenticação: a chave fica em `local.properties` (`LASTFM_API_KEY=...`), é exposta via `BuildConfig` e adicionada às requisições (junto com `format=json`) por um Interceptor do OkHttp. **Nunca** escreva a chave em código, commits ou documentação. Confirme que `local.properties` está no `.gitignore`.
- Uso moderado: o Last.fm pede que as requisições não sejam excessivas. Confira o limite atual nos termos da API e respeite-o (ver "Estratégia de recomendação" para as medidas de economia).
- Particularidades do JSON (tratar nos DTOs e mappers, com teste unitário para cada caso):
  - Chaves com símbolos, como `"#text"` e `"@attr"`, exigem `@SerialName`.
  - Números chegam como texto (`"count"`, `"match"`, `"listeners"`, `"duration"`).
  - **A duração muda de unidade conforme o endpoint:** em `tag.getTopTracks` vem em **segundos** (`"312"`); em `track.getInfo` vem em **milissegundos** (`"250000"`). `"0"` significa duração desconhecida nos dois casos. Cada endpoint tem seu próprio mapper.
  - Nomes de tags misturam maiúsculas e minúsculas (`"mpb"`, `"Bossa Nova"`): compare sempre após a normalização definida nas regras de negócio.
  - O campo `mbid` pode vir vazio: não o use como identificador.
- Imagens:
  - As imagens de músicas em `tag.getTopTracks` e as de artistas em `artist.getSimilar` são sempre um placeholder genérico (arquivo `2a96cbd8b46e442fc41c2b86b821562f.png`). **Não use essas imagens.**
  - A capa válida vem de `album.image` no `track.getInfo` (use o tamanho `extralarge`). Se a URL contiver o hash do placeholder, ou se o álbum não vier, trate como "capa indisponível".
- Atribuição obrigatória na tela "Sobre": "powered by AudioScrobbler" com link para last.fm (termos, cláusulas 2.7 e 4.2.2; RN37).

### Abrir a música em outro app (sem API)

- Spotify: `https://open.spotify.com/search/{artista música}`
- YouTube: `https://www.youtube.com/results?search_query={artista música}`
- Abrir com `Intent(Intent.ACTION_VIEW, uri)`. O texto da busca deve ser codificado com `Uri.encode`. Se o app correspondente estiver instalado, o Android pode oferecê-lo; caso contrário, abre no navegador.

## Regras de negócio (camada Domain)

Estas regras ficam na camada Domain, em Kotlin puro, e devem ser cobertas por testes unitários.

### Teste de personalidade (TIPI)

- 10 afirmações, cada uma respondida numa escala de 1 (discordo totalmente) a 7 (concordo totalmente). Respostas fora de 1..7 são rejeitadas.
- Itens marcados com R são invertidos: `pontuação = 8 - resposta`.
- Cada traço é a média dos seus dois itens:

| Traço | Itens |
|---|---|
| Extroversão | 1 e 6R |
| Amabilidade | 2R e 7 |
| Conscienciosidade | 3 e 8R |
| Estabilidade emocional | 4R e 9 |
| Abertura a experiências | 5 e 10R |

- O teste só é concluído com as 10 respostas preenchidas.
- O texto das afirmações é o da versão brasileira de Carlos Eduardo Pimentel (RN05; `docs/referencias/`). Não traduza por conta própria.
- O app deve informar que o teste serve apenas para personalizar recomendações e não é uma avaliação psicológica.

### Influência da personalidade

Apenas dois traços influenciam as recomendações:

- **Abertura a experiências** define a faixa de `match` dos artistas similares usados na fonte de complemento:
  - ≥ 5,0: perfil de descoberta. Usa também artistas com `match` mais baixo, ou seja, mais distantes dos favoritos.
  - ≤ 3,0: perfil de familiaridade. Usa só artistas com `match` alto, próximos dos favoritos.
  - Entre esses valores: equilibrado.
  - Limites de `match`: descoberta ≥ 0,10; equilibrado ≥ 0,25; familiaridade ≥ 0,50 (RN06).
- **Extroversão:** usada **somente como desempate** no ranking. ≥ 5,0 dá peso extra a músicas com tags energéticas; ≤ 3,0 dá peso extra a tags calmas. Nunca contradiz o humor escolhido.
- Os demais traços aparecem apenas na tela de perfil.
- A estabilidade emocional **nunca** é usada para decidir pelo usuário se ele deve "mudar de clima". Essa escolha é sempre do usuário, na conversa.

### Mapeamento humor → tags do Last.fm

O usuário escolhe o modo na conversa: **acompanhar** (`MATCH`) ou **mudar** (`SHIFT`) o clima.

| Humor | Tags no modo acompanhar | Tags no modo mudar |
|---|---|---|
| Feliz | happy, feel good, upbeat | (iguais ao acompanhar) |
| Triste | sad, melancholy | feel good, uplifting |
| Animado | energetic, party, dance | (iguais ao acompanhar) |
| Ansioso | calm, relaxing, chill | calm, relaxing |
| Cansado | mellow, acoustic, chill | energetic, upbeat |
| Romântico | love, romantic | (iguais ao acompanhar) |
| Nostálgico | oldies, 80s, 90s | (iguais ao acompanhar) |
| Focado | instrumental, ambient | (iguais ao acompanhar) |

- "Ansioso" sempre recebe músicas calmas, em qualquer modo.
- Para humores cujo modo mudar é igual ao acompanhar, a conversa não pergunta o modo.
- As tags de humor são **somente em inglês**. O spike mostrou que tags em português têm alcance muito baixo (ex.: "triste" tem 386 usuários e é compartilhada com outros idiomas). A tag "study" foi removida pelo mesmo motivo (585 usuários).

### Normalização de textos

Antes de comparar nomes de tags, artistas ou músicas: converter para minúsculas, remover acentos e remover espaços extras. Exemplo: `"Bossa Nova"` e `"bossa nova"` são iguais.

### Gêneros e nacionalidade de um artista

- Os gêneros de um artista vêm do `artist.getTopTags`. Uma tag só conta se, após normalizada, estiver na **lista fechada de gêneros** apresentada no onboarding e tiver `count` ≥ 10 (RN14). Tags como "female vocalists" ou "british" não são gêneros e são ignoradas.
- A nacionalidade brasileira é identificada pelas tags `brazilian`, `brasil` ou `brazil` (após normalização).
- Os gêneros e a nacionalidade de cada artista ficam em cache no Room **sem prazo de validade**, porque praticamente não mudam.

### Estratégia de recomendação

A recomendação usa duas fontes, nesta ordem:

1. **Fonte principal: "Pelo seu humor".** Buscar as músicas das tags do humor (`tag.getTopTracks`), analisando no máximo as 30 primeiras de cada tag, e manter só as de artistas cujos gêneros batem com os gostos do usuário e cuja nacionalidade respeita a preferência de origem.
2. **Fonte de complemento: "Pelo seu gosto".** Se a fonte principal trouxer menos de 10 músicas (RN20; lista final de até 20), completar com músicas de artistas similares aos favoritos (`artist.getSimilar`), na faixa de `match` definida pela abertura do usuário. Os 20 melhores candidatos passam pelo `track.getInfo`, e os que têm alguma tag do humor escolhido sobem no ranking.

Regras gerais:

- A fonte principal acerta melhor o humor; a de complemento, o gosto. É esperado que, para gêneros brasileiros, a fonte de complemento traga a maior parte das músicas, porque as tags de humor em inglês são usadas principalmente por ouvintes de música internacional.
- Toda música recomendada guarda a fonte que a trouxe (`MOOD` ou `TASTE`).
- Na fonte principal, a capa é buscada sob demanda (`track.getInfo` só quando o card aparece na tela). Na fonte de complemento, ela já vem da chamada usada no ranking.

### Preferência de origem

- No onboarding, o usuário escolhe: músicas nacionais, internacionais ou ambas.
- "Nacionais": só artistas identificados como brasileiros. "Internacionais": exclui esses artistas. "Ambas": sem filtro.
- Artistas sem nenhuma tag de nacionalidade contam como internacionais.

### Selo de origem da recomendação

- Todo card de música recomendada mostra um selo com a fonte que a trouxe: "Pelo seu humor" (`MOOD`) ou "Pelo seu gosto" (`TASTE`).
- O estilo visual do selo segue `docs/identidade_visual.md`.

### Conversa guiada

- A conversa é uma máquina de estados na camada Domain: cada passo tem uma pergunta e um conjunto fechado de respostas, e cada resposta define o próximo passo.
- Sequência: saudação → escolha do humor → (se aplicável) escolha do modo → recomendações.
- A conversa funciona totalmente offline.

### Recomendações

- Não exibir músicas duplicadas. Duas músicas são iguais quando artista e nome, normalizados (sem diferença de maiúsculas, acentos e espaços extras), são iguais.
- Remover das recomendações as músicas marcadas como ouvidas (`LISTENED`).
- O cache de recomendações vale 24 horas por combinação de humor e modo.

### Músicas salvas

- Uma música não pode ser salva duas vezes.
- Status: `WANT_TO_LISTEN` → `LISTENED`, marcado manualmente pelo usuário.
- Avaliação pessoal (1 a 5 estrelas) só é permitida para músicas `LISTENED`; valores fora do intervalo são rejeitados.

### Perfil e conta

- O perfil (respostas do teste, traços e gostos) fica **somente no Room**, no aparelho. Não sincronizar com a nuvem.
- Todo dado do usuário no Room (perfil, músicas salvas, cache) é associado ao `uid` da conta local. Ao entrar com outra conta no mesmo aparelho, os dados de uma conta nunca aparecem para a outra.
- Se o usuário reinstalar o app, faz o teste de novo (comportamento esperado e documentado).

### Notificação

- Uma notificação local diária sugere ao usuário conversar com o app, agendada com `PeriodicWorkRequest` do WorkManager.
- No Android 13 ou superior, pedir a permissão `POST_NOTIFICATIONS` em tempo de execução, explicando antes o motivo. Se o usuário negar, o app continua funcionando normalmente.

## Escopo

Dentro do escopo:
1. Login e cadastro locais (e-mail e senha), sem Firebase.
2. Onboarding: teste TIPI e perguntas de gosto musical, incluindo a preferência por músicas nacionais, internacionais ou ambas.
3. Tela de perfil com o resultado do teste e a opção de refazê-lo.
4. Conversa guiada (chat com chips) sobre o humor do dia e o modo acompanhar/mudar.
5. Recomendações de músicas, com pull-to-refresh, selo de origem ("Pelo seu humor" / "Pelo seu gosto") e botões para abrir no Spotify ou no YouTube.
6. Lista de músicas salvas, com filtro por status, marcação como ouvida e avaliação.
7. Indicador de modo offline.
8. Notificação diária sugerindo usar o app.
9. Tela "Sobre" com as atribuições às APIs e às fontes.

Fora do escopo: IA ou interpretação de texto livre, reprodução de áudio dentro do app, API do Spotify, API do Deezer, sincronização do perfil na nuvem, busca livre por músicas.

## Arquitetura

Clean Architecture em um único módulo `:app`, com separação por pacotes e regra de dependência rígida:

```
presentation  →  domain  ←  data
```

- **domain**: modelos, interfaces de repositório, casos de uso e regras de negócio. Kotlin puro: é **proibido** importar `android.*`, Retrofit, Room ou Koin.
- **data**: implementa os repositórios do domain. Contém DTOs, entidades Room, DAOs, APIs Retrofit, autenticação local (hash de senha e sessão) e mappers. DTOs e entidades nunca saem desta camada.
- **presentation**: telas Compose, ViewModels, UiState e navegação. Acessa apenas casos de uso.
- **di**: módulos Koin; é o único lugar que conhece todas as camadas.

```
com.leticia.moodiva/
├── MoodivaApp.kt          # Application: inicializa o Koin
├── MainActivity.kt
├── di/                    # dataModule, domainModule, presentationModule
├── core/                  # AppResult, AppError, dispatchers, monitor de conectividade
├── domain/
│   ├── model/             # Mood, ConversationMode, ConversationStep, PersonalityProfile,
│   │                      # TasteProfile, OriginPreference, Track, TrackSource,
│   │                      # SavedTrack, ListenStatus, User
│   ├── repository/        # interfaces
│   └── usecase/
├── data/
│   ├── remote/            # LastFmApi, DTOs, interceptor da chave
│   ├── auth/              # autenticação local: hash de senha e sessão
│   ├── local/             # AppDatabase, DAOs, entidades (inclui cache de gêneros dos artistas)
│   ├── notification/      # Worker da notificação diária
│   ├── mapper/
│   └── repository/        # implementações
└── presentation/
    ├── navigation/
    ├── theme/
    ├── components/
    └── screens/           # auth/, onboarding/, profile/, chat/, saved/, about/
```

## Offline First e sincronização

- O **Room é a única fonte de verdade**. A UI observa `Flow` vindos do Room; a rede apenas atualiza o banco.
- Ao chegar às recomendações de um humor e modo:
  1. Emitir imediatamente o que estiver em cache.
  2. Se o cache estiver vazio ou tiver mais de 24 h e houver conexão, buscar na API e gravar no Room (o Flow emite os dados novos sozinho).
  3. Se a requisição falhar e houver cache, manter os dados na tela e mostrar o erro de forma discreta (ex.: Snackbar), sem apagar o cache.
  4. Se não houver cache nem conexão, mostrar o estado vazio explicando que é preciso internet para a primeira busca, com o botão "Tentar novamente".
- O pull-to-refresh força uma nova busca, respeitando as mesmas regras de erro.
- O cache de gêneros e nacionalidade dos artistas não expira e é reaproveitado entre humores, reduzindo as requisições com o uso.
- Capas que não puderem ser carregadas, ou que forem o placeholder do Last.fm, mostram o estado "capa indisponível" definido na identidade visual.
- O banner "sem internet" aparece enquanto o monitor de conectividade indicar ausência de conexão.

## Testes

- Camada Domain: testes unitários para todas as regras de negócio (TIPI, influência da personalidade, humor → tags, normalização, gêneros e nacionalidade de artistas, preferência de origem, estratégia de duas fontes, máquina de estados da conversa, deduplicação, regras de músicas salvas).
- Mappers: testes para as particularidades do JSON do Last.fm, em especial a duração em segundos versus milissegundos e a detecção do placeholder de imagem.
- ViewModels: testes com `kotlinx-coroutines-test` e Turbine para verificar a sequência de `UiState`.
- Repositórios: testes com MockK simulando API, DAO e conectividade, cobrindo os cenários de Offline First acima.

## Decisões resolvidas (2026-11-19)

As antigas "decisões pendentes" foram resolvidas e estão registradas em `docs/01-especificacao.md`, seção 10: valores numéricos da recomendação (RN06, RN14, RN20, RN21), versão do TIPI (RN05), atribuição do Last.fm (RN37), descarte do cache ao mudar o perfil (RN26), artistas sem nacionalidade como internacionais (RN13) e package `com.leticia.moodiva`. Também foi decidido usar **login local, sem Firebase**.

## Decisões já tomadas (spike do Last.fm)

Registro para consulta e para a apresentação do projeto:

- Estratégia combinada de duas fontes ("Pelo seu humor" e "Pelo seu gosto").
- Last.fm como única API de música; Deezer descartado porque o `track.getInfo` traz capas reais.
- Descoberta controlada pelo `match` do `artist.getSimilar`.
- Tags de humor só em inglês; "study" e tags em português descartadas por baixo alcance.
- Preferência de origem (nacionais, internacionais ou ambas) incluída no onboarding.
- Selo de origem da recomendação incluído nos cards.
