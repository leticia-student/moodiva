# Moodiva

Aplicativo Android que recomenda músicas de acordo com a **personalidade**, os **gostos** e o **humor do dia** do usuário. Trabalho acadêmico desenvolvido com Spec-Driven Development (SDD).

<p align="center"><img src="docs/assets/icone_moodiva.svg" width="96" alt="Ícone do Moodiva"></p>

## Objetivo e problema

Escolher o que ouvir para um estado de espírito específico dá trabalho, e as recomendações tradicionais olham só o histórico de reprodução. O Moodiva pergunta como você está e se quer **acompanhar** ou **mudar** esse clima, e cruza a resposta com sua personalidade (teste TIPI) e seus gostos para sugerir músicas. Conversa, perfil, músicas salvas e as últimas recomendações funcionam **sem internet**.

## Funcionalidades

| Funcionalidade | Descrição |
|---|---|
| Conta local | Cadastro, login e logout com e-mail e senha, guardados só no aparelho (senha com hash PBKDF2). Cada conta vê apenas os próprios dados. |
| Onboarding | Teste de personalidade TIPI (10 itens, versão brasileira de C. E. Pimentel), gêneros, 1 a 5 artistas favoritos, épocas e preferência por músicas nacionais, internacionais ou ambas. |
| Conversa guiada | Chat sem IA, com respostas por chips: saudação → humor (8 opções) → acompanhar/mudar (só para Triste, Ansioso e Cansado). |
| Recomendações | Até 20 músicas com capa, duração e selo de origem ("Pelo seu humor" / "Pelo seu gosto"). Pull-to-refresh, salvar e abrir no Spotify ou no YouTube (busca, sem API). |
| Músicas salvas | Filtro por status, "Já ouvi", avaliação de 1 a 5 estrelas e remoção com confirmação. |
| Perfil | Traços do TIPI, gostos, refazer o teste, editar gostos, Sobre e sair. |
| Offline | Banner "sem internet"; cache de recomendações de 24 h; capas em cache de disco. |
| Lembrete diário | Notificação local entre 19h e 21h (WorkManager), com explicação antes do pedido de permissão no Android 13+. |
| Sobre | Atribuições ao Last.fm ("powered by AudioScrobbler"), ao TIPI, às fontes e aos ícones. |

## Tecnologias

| Área | Tecnologia |
|---|---|
| Linguagem | Kotlin 2.4 |
| UI | Jetpack Compose + Material 3 (tema próprio, claro e escuro, sem cor dinâmica) |
| Navegação | Navigation Compose com rotas type-safe (kotlinx.serialization) |
| Injeção de dependências | Koin |
| Assíncrono | Coroutines, Flow e StateFlow |
| Rede | Retrofit 3 + OkHttp 5 + kotlinx.serialization |
| Persistência | Room (KSP) |
| Imagens | Coil 3 (cache em disco) |
| Tarefas agendadas | WorkManager |
| Testes | JUnit 4, MockK, kotlinx-coroutines-test, Turbine |
| Build | Gradle Kotlin DSL + Version Catalog, AGP 9.5, `minSdk` 24, `targetSdk` 37 |

## API: Last.fm

[Last.fm API](https://www.last.fm/api) é a única API de música do app. Ela é gratuita para uso não comercial e tem **tags colaborativas de humor** (happy, sad, chill…), **artistas similares** com grau de semelhança e capas reais via `track.getInfo`.

**Por que o Last.fm:** a ideia central do app é recomendar por humor, e o Last.fm é a API pública que organiza músicas por tags de clima atribuídas pelos próprios ouvintes. As alternativas foram descartadas:

- **API do Spotify:** desde novembro de 2024 não oferece mais recomendações nem características de áudio (como valência e energia) para apps novos, além de exigir login OAuth do usuário.
- **Deezer:** foi avaliado no spike apenas para obter capas, mas o `track.getInfo` do Last.fm já fornece capas reais. Uma segunda API aumentaria a complexidade sem ganho.

Métodos usados: `tag.getTopTracks`, `artist.getTopTags`, `artist.getSimilar`, `artist.getTopTracks` e `track.getInfo`. As particularidades do JSON (duração em segundos × milissegundos, números como texto, placeholder de imagem, erro com HTTP 200) estão em `docs/02-planejamento.md`, seção 3.3, e são cobertas por testes com respostas reais.

### Estratégia de recomendação

1. **Pelo seu humor:** músicas das tags do humor, só de artistas cujos gêneros batem com o perfil e cuja origem respeita a preferência.
2. **Pelo seu gosto:** se a primeira fonte trouxer menos de 10 músicas, completa com músicas de artistas similares aos favoritos, numa faixa de semelhança definida pela **Abertura a experiências** (descoberta, equilibrado ou familiaridade).
3. A **Extroversão** só desempata o ranking (tags energéticas × calmas). As músicas marcadas como ouvidas saem da lista.

## Arquitetura

Clean Architecture em um módulo (`:app`), separada por pacotes:

```
presentation  →  domain  ←  data
```

- `domain`: modelos, regras de negócio (`domain/rule`), casos de uso e interfaces de repositório, em Kotlin puro, sem `android.*`, Retrofit, Room ou Koin.
- `data`: Room (única fonte de verdade), Retrofit, DTOs, mappers, autenticação local e implementações dos repositórios.
- `presentation`: telas Compose (Route + Screen sem estado), ViewModels com um `StateFlow<UiState>`, navegação e tema.
- `core`: `AppResult`, `AppError`, dispatchers e monitor de conexão. `di`: módulos Koin. `notification`: WorkManager.

### Offline First

A interface observa **só o banco**; a rede apenas grava no banco. Ao abrir as recomendações:

| Situação | Comportamento |
|---|---|
| Cache válido (< 24 h) | Mostra o cache sem chamar a API |
| Cache vencido e online | Mostra o cache e o substitui quando a busca termina |
| Falha com cache | Mantém as músicas e avisa num Snackbar |
| Sem cache e offline | Explica que a primeira busca precisa de internet, com "Tentar novamente" |

As tags dos artistas ficam em cache **sem validade**, então cada artista é consultado uma vez por aparelho.

## Configuração

1. Crie uma chave gratuita em https://www.last.fm/api/account/create.
2. No `local.properties` (na raiz, fora do Git), acrescente:

   ```properties
   LASTFM_API_KEY=sua_chave_aqui
   ```

A chave vira `BuildConfig.LASTFM_API_KEY` e é acrescentada às requisições por um interceptor. No log de depuração ela aparece como `██`. Sem a chave o projeto compila, e as buscas mostram "A chave do Last.fm não foi configurada corretamente."

## Execução

```bash
./gradlew assembleDebug     # gera app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug      # instala no emulador ou aparelho conectado
```

Testado no emulador **Medium Phone API 37**.

## Testes

```bash
./gradlew testDebugUnitTest
```

São 159 testes unitários JVM: 86 no domain, 28 no data, 43 na presentation e 2 na notificação. O relatório fica em `app/build/reports/tests/testDebugUnitTest/index.html`, e o histórico de execuções e a verificação de cada critério de aceite estão em [TEST_RESULTS.md](TEST_RESULTS.md).

## Evidências

Capturas do app rodando no emulador estão em [`evidencias/`](evidencias/): login e validação, teste TIPI e resultado (CA08), gostos, conversa, recomendações com capas e selos, salvar, músicas salvas com avaliação, perfil, Sobre, pedido de permissão, lembrete diário, modo offline (com e sem cache), isolamento entre contas, fonte "Pelo seu gosto" e tema escuro.

## Limitações conhecidas

- As tags de humor do Last.fm são em inglês e usadas sobretudo para música internacional. Para quem prefere só músicas nacionais, a fonte "Pelo seu gosto" faz a maior parte do trabalho.
- As tags são colaborativas e podem ser imprecisas.
- O app não toca áudio; Spotify e YouTube abrem por **busca** "artista + música".
- Contas e perfil ficam só no aparelho: não há recuperação de senha nem sincronização entre aparelhos. Reinstalar o app apaga os dados.
- A primeira busca de cada humor precisa de internet e pode levar alguns segundos (dezenas de artistas ainda sem cache).
- O WorkManager não garante horário exato (economia de bateria pode atrasar o lembrete).
- Capas só aparecem offline se já tiverem sido vistas antes.

## Créditos

- **Dados musicais:** [Last.fm](https://www.last.fm), powered by AudioScrobbler. Uso não comercial.
- **TIPI:** Gosling, S. D., Rentfrow, P. J., & Swann, W. B., Jr. (2003). *A very brief measure of the Big-Five personality domains*. Versão brasileira de Carlos Eduardo Pimentel (`docs/referencias/`).
- **Fontes:** Nunito (© 2014 The Nunito Project Authors) e Sacramento (© 2012 Brian J. Bonislawsky, Astigmatic), sob a [SIL Open Font License 1.1](docs/licencas/).
- **Ícones:** Material Symbols (Google), licença Apache 2.0.

## Documentação do projeto

- [Especificação](docs/01-especificacao.md) · [Planejamento](docs/02-planejamento.md) · [Tarefas](docs/03-tarefas.md) · [Identidade visual](docs/identidade_visual.md)
