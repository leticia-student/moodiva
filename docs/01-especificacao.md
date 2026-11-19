# 01 — Especificação do Moodiva

> Fase 1 do SDD. Responde **o quê** o app faz e **por quê**. O **como** fica em `02-planejamento.md`.
> Todas as decisões pendentes do `CLAUDE.md` foram resolvidas em 2026-11-19 (seção 10), com o critério de "melhor experiência do usuário" delegado pela responsável do projeto.

## 1. Proposta

O **Moodiva** é um aplicativo Android que recomenda músicas de acordo com a **personalidade**, os **gostos musicais** e o **humor do dia** do usuário. A experiência é de "uma amiga diva que entende seu humor": uma conversa guiada pergunta como a pessoa está e se ela quer músicas que **acompanhem** ou que **mudem** esse clima. Em seguida, o app apresenta músicas que podem ser abertas no Spotify ou no YouTube, salvas numa lista e avaliadas.

## 2. Problema que resolve

- Os catálogos de streaming são enormes, e escolher o que ouvir para um estado de espírito específico dá trabalho.
- As recomendações tradicionais usam só o histórico de reprodução. Elas ignoram o humor do momento e a intenção de quem ouve: às vezes a pessoa quer abraçar a tristeza, às vezes quer sair dela.
- Muitos apps dependem de internet o tempo todo. O Moodiva mantém conversa, perfil, músicas salvas e as últimas recomendações disponíveis offline.

## 3. Objetivos

### 3.1 Objetivo geral

Desenvolver um app Android nativo que gere recomendações musicais personalizadas cruzando humor, intenção (acompanhar ou mudar o clima), personalidade (TIPI) e gostos do usuário, com arquitetura limpa, Offline First e regras de negócio cobertas por testes unitários.

### 3.2 Objetivos específicos

1. Aplicar o teste de personalidade TIPI (10 itens) e calcular os cinco traços.
2. Coletar os gostos musicais: gêneros, artistas favoritos, épocas e preferência de origem.
3. Conduzir uma conversa guiada sem IA (máquina de estados com chips).
4. Gerar recomendações pela estratégia de duas fontes ("Pelo seu humor" e "Pelo seu gosto") usando a API do Last.fm.
5. Permitir salvar, marcar como ouvida e avaliar músicas.
6. Funcionar sem internet nas funcionalidades principais.
7. Demonstrar Clean Architecture, injeção de dependências com Koin e testes unitários.

## 4. Público-alvo

Jovens e adultos que ouvem música no celular, usam Spotify ou YouTube e querem descobrir músicas ligadas ao seu estado emocional, sem precisar montar playlists manualmente. Idioma da interface: português do Brasil.

## 5. Funcionalidades (visão geral)

| # | Funcionalidade | Resumo |
|---|---|---|
| F1 | Conta | Cadastro, login e logout com e-mail e senha (Firebase Authentication). |
| F2 | Onboarding | Teste TIPI e perguntas de gosto musical, feitos uma vez por conta neste aparelho. |
| F3 | Perfil | Resultado do teste e gostos, com opção de refazer. |
| F4 | Conversa guiada | Chat com chips: saudação → humor → modo (quando aplicável) → recomendações. |
| F5 | Recomendações | Lista de músicas com selo de origem, capa, pull-to-refresh e botões Spotify/YouTube. |
| F6 | Músicas salvas | Lista com filtro por status, marcação como ouvida e avaliação de 1 a 5 estrelas. |
| F7 | Modo offline | Banner "sem internet" e cache local. |
| F8 | Notificação diária | Lembrete local para conversar com o app. |
| F9 | Sobre | Atribuições à API do Last.fm e às fontes. |

**Fora do escopo:** IA ou interpretação de texto livre, reprodução de áudio dentro do app, API do Spotify, API do Deezer, sincronização do perfil na nuvem, busca livre por músicas.

## 6. Requisitos funcionais (RF)

| Código | Requisito | Prioridade |
|---|---|---|
| RF01 | O app deve permitir criar uma conta com e-mail e senha (mínimo de 6 caracteres, regra do Firebase), com confirmação de senha e opção de mostrar/ocultar a senha. | Alta |
| RF02 | O app deve permitir entrar com e-mail e senha e manter a sessão entre aberturas do app. | Alta |
| RF03 | O app deve permitir sair da conta. | Alta |
| RF04 | Após o primeiro login de uma conta no aparelho, o app deve conduzir o usuário ao onboarding antes de qualquer outra tela. | Alta |
| RF05 | O app deve apresentar as 10 afirmações do TIPI, cada uma com escala de 1 a 7, e só concluir o teste com as 10 respostas preenchidas. | Alta |
| RF06 | O app deve informar, antes do teste, que ele serve apenas para personalizar recomendações e não é uma avaliação psicológica. | Alta |
| RF07 | O app deve permitir escolher gêneros favoritos a partir de uma lista fechada (RN14). | Alta |
| RF08 | O app deve permitir informar artistas favoritos (RN15). | Alta |
| RF09 | O app deve permitir escolher as épocas musicais preferidas (opcional; ver RN36). | Média |
| RF10 | O app deve permitir escolher a preferência de origem: nacionais, internacionais ou ambas. | Alta |
| RF11 | O app deve exibir o perfil com os cinco traços, os gêneros, os artistas, as épocas e a preferência de origem. | Alta |
| RF12 | O app deve permitir refazer o teste e alterar os gostos a partir do perfil. | Média |
| RF13 | O app deve conduzir uma conversa guiada em formato de chat, com respostas por chips, sem texto livre. | Alta |
| RF14 | A conversa deve perguntar o humor (8 opções) e, quando aplicável, o modo acompanhar/mudar. | Alta |
| RF15 | O app deve exibir as recomendações para o humor e o modo escolhidos, com nome, artista, capa (ou estado "capa indisponível") e selo de origem. | Alta |
| RF16 | O app deve permitir atualizar as recomendações com pull-to-refresh. | Média |
| RF17 | O app deve permitir abrir uma música no Spotify ou no YouTube por busca, sem API. | Alta |
| RF18 | O app deve permitir salvar uma música recomendada na lista de músicas salvas. | Alta |
| RF19 | O app deve listar as músicas salvas com filtro por status (todas, quero ouvir, ouvidas). | Alta |
| RF20 | O app deve permitir marcar uma música salva como ouvida. | Alta |
| RF21 | O app deve permitir avaliar com 1 a 5 estrelas uma música marcada como ouvida. | Alta |
| RF22 | O app deve exibir um banner "sem internet" enquanto não houver conexão. | Média |
| RF23 | O app deve enviar uma notificação local diária sugerindo conversar com o app; tocar nela abre o app. | Média |
| RF24 | No Android 13 ou superior, o app deve explicar o motivo e então pedir a permissão de notificações. | Média |
| RF25 | O app deve ter uma tela "Sobre" com as atribuições ao Last.fm (RN37), às fontes Nunito e Sacramento e ao TIPI (RN05). | Média |
| RF26 | O app deve permitir remover uma música da lista de salvas, com pedido de confirmação. | Média |

## 7. Requisitos não funcionais (RNF)

| Código | Requisito |
|---|---|
| RNF01 | **Offline First:** conversa, perfil, músicas salvas e as últimas recomendações em cache funcionam sem internet. O Room é a única fonte de verdade; a UI observa `Flow` do banco e a rede apenas atualiza o banco. |
| RNF02 | **Arquitetura:** Clean Architecture em módulo único (`:app`), `presentation → domain ← data`. O `domain` é Kotlin puro (sem `android.*`, Retrofit, Room, Koin ou Firebase). |
| RNF03 | **Segurança da chave:** a chave do Last.fm fica em `local.properties`, é exposta via `BuildConfig` e nunca aparece em código, commits ou documentação. |
| RNF04 | **Stack obrigatória:** Kotlin, Jetpack Compose + Material 3, Navigation Compose type-safe, Koin, Coroutines/Flow/StateFlow, Retrofit + OkHttp + kotlinx.serialization, Room (KSP), Coil, Firebase Authentication, WorkManager, JUnit 4, MockK, kotlinx-coroutines-test, Turbine. Proibidos: Hilt, Dagger, RxJava, LiveData, layouts XML, Gson. |
| RNF05 | **Identidade visual:** segue `docs/identidade_visual.md`. Temas claro e escuro conforme o sistema, sem cor dinâmica; cores, tipografia e formas apenas via `MaterialTheme`. |
| RNF06 | **Acessibilidade:** contraste mínimo de 4,5:1, alvo de toque de 48dp e `contentDescription` em português nos botões só com ícone. |
| RNF07 | **Fontes embutidas:** Nunito e Sacramento empacotadas no app (sem downloadable fonts), com licenças OFL em `docs/licencas/`. |
| RNF08 | **Isolamento por conta:** todo dado no Room é associado ao `uid` do Firebase; dados de uma conta nunca aparecem para outra no mesmo aparelho. |
| RNF09 | **Privacidade:** o perfil (respostas, traços e gostos) fica somente no aparelho; não é sincronizado com a nuvem. |
| RNF10 | **Uso moderado da API:** os termos do Last.fm (cláusula 4.4) não fixam um número; o app adota um limite próprio conservador de no máximo **5 requisições por segundo** e reduz chamadas com cache (gêneros de artistas sem validade, recomendações por 24 h, capas sob demanda). Uso estritamente não comercial (cláusula 3.1). O volume de dados do Last.fm guardado no aparelho fica bem abaixo do teto de 100 MB (cláusula 4.3.4). |
| RNF11 | **Testabilidade:** todas as regras de negócio, os mappers do Last.fm, os ViewModels e os cenários Offline First dos repositórios têm testes unitários JVM. |
| RNF12 | **Compatibilidade:** `minSdk` 24 (Android 7.0); `targetSdk` e `compileSdk` conforme o projeto. |
| RNF13 | **Idioma:** interface e documentação em português do Brasil; nomes de código em inglês. |

### Limitações conhecidas (aceitas nesta especificação)

- **L1.** As tags de humor do Last.fm são em inglês e usadas sobretudo por ouvintes de música internacional. Para gêneros brasileiros, é esperado que a fonte "Pelo seu gosto" traga a maior parte das músicas.
- **L2.** Os dados vêm de tags colaborativas do Last.fm, que podem ser imprecisas: uma música pode aparecer num humor discutível.
- **L3.** O app não toca áudio. Abrir no Spotify ou no YouTube faz uma **busca** por "artista + música", então o primeiro resultado pode não ser a gravação exata.
- **L4.** Como o perfil fica só no aparelho, quem reinstalar o app ou trocar de celular refaz o teste.
- **L5.** A primeira busca de recomendações para um humor e modo exige internet.
- **L6.** Artistas sem nenhuma tag de nacionalidade contam como internacionais (RN13). Alguns artistas brasileiros pouco "tagueados" podem ser filtrados incorretamente.
- **L7.** O cadastro e o primeiro login exigem internet. Depois, a sessão do Firebase fica salva no aparelho e o app abre offline.

## 8. Regras de negócio (RN)

### 8.1 Teste de personalidade (TIPI)

| Código | Regra |
|---|---|
| RN01 | Cada uma das 10 respostas deve estar entre 1 e 7; valores fora do intervalo são rejeitados. |
| RN02 | O teste só é concluído com as 10 respostas preenchidas. |
| RN03 | Itens invertidos (R) têm pontuação `8 - resposta`. |
| RN04 | Cada traço é a média dos seus dois itens: Extroversão = (1, 6R); Amabilidade = (2R, 7); Conscienciosidade = (3, 8R); Estabilidade emocional = (4R, 9); Abertura a experiências = (5, 10R). O resultado vai de 1,0 a 7,0. |
| RN05 | O texto das afirmações é o da **versão brasileira de Carlos Eduardo Pimentel (Universidade de Brasília)**, distribuída pelo autor do TIPI (gosling.psy.utexas.edu), copiada sem alterações. Enunciado: "Eu me vejo como alguém…". Itens: 1 Extrovertido, entusiasta · 2 Crítico, briguento · 3 Confiável, auto-disciplinado · 4 Ansioso, que se chateia facilmente · 5 Aberto a novas experiências, complexo · 6 Reservado, quieto · 7 Simpático, acolhedor · 8 Desorganizado, descuidado · 9 Calmo, emocionalmente estável · 10 Convencional, sem criatividade. Escala: 1 Discordo fortemente · 2 Discordo moderadamente · 3 Discordo um pouco · 4 Nem concordo nem discordo · 5 Concordo um pouco · 6 Concordo moderadamente · 7 Concordo fortemente. Documento original em `docs/referencias/TIPI-pt-BR-Pimentel.doc`. Referência do instrumento: Gosling, S. D., Rentfrow, P. J., & Swann, W. B., Jr. (2003). A very brief measure of the Big-Five personality domains. *Journal of Research in Personality, 37*, 504–528. |

### 8.2 Influência da personalidade

| Código | Regra |
|---|---|
| RN06 | **Abertura a experiências** define o perfil de descoberta e a faixa mínima de `match` dos artistas similares (fonte "Pelo seu gosto"): **Descoberta** (≥ 5,0) aceita `match` ≥ **0,10**; **Equilibrado** (entre 3,0 e 5,0, exclusivos) aceita ≥ **0,25**; **Familiaridade** (≤ 3,0) aceita ≥ **0,50**. O `match` máximo é sempre 1,0. |
| RN07 | **Extroversão** é usada **somente como desempate** no ranking: ≥ 5,0 favorece músicas com tags energéticas (`energetic`, `upbeat`, `party`, `dance`); ≤ 3,0 favorece tags calmas (`calm`, `relaxing`, `chill`, `mellow`, `acoustic`); entre esses valores, não há desempate por personalidade. Nunca altera quais humores são buscados. |
| RN08 | Os demais traços (Amabilidade, Conscienciosidade, Estabilidade emocional) aparecem apenas no perfil. A Estabilidade emocional **nunca** decide se o usuário deve mudar de clima. |

### 8.3 Humor, modo e conversa

| Código | Regra |
|---|---|
| RN09 | **Humor → tags** (somente em inglês). Feliz: happy, feel good, upbeat. Triste: acompanhar = sad, melancholy; mudar = feel good, uplifting. Animado: energetic, party, dance. Ansioso: acompanhar = calm, relaxing, chill; mudar = calm, relaxing. Cansado: acompanhar = mellow, acoustic, chill; mudar = energetic, upbeat. Romântico: love, romantic. Nostálgico: oldies, 80s, 90s. Focado: instrumental, ambient. |
| RN10 | A conversa só pergunta o modo quando as tags de acompanhar e de mudar são diferentes, ou seja, para **Triste, Ansioso e Cansado**. Nos demais humores, o modo é acompanhar. "Ansioso" sempre recebe músicas calmas, em qualquer modo. |
| RN11 | A conversa é uma máquina de estados: saudação → escolha do humor → (se aplicável) escolha do modo → recomendações. Cada passo tem uma pergunta e um conjunto fechado de respostas, e cada resposta define o próximo passo. Funciona totalmente offline. |

### 8.4 Textos, gêneros e nacionalidade

| Código | Regra |
|---|---|
| RN12 | **Normalização:** antes de comparar nomes de tags, artistas ou músicas, converter para minúsculas, remover acentos, remover espaços nas pontas e reduzir espaços internos repetidos a um só. Ex.: `"Bossa  Nova "` = `"bossa nova"`. |
| RN13 | **Nacionalidade:** um artista é brasileiro se tiver, após normalização, as tags `brazilian`, `brasil` ou `brazil`. Artistas sem nenhuma dessas tags contam como internacionais. |
| RN14 | **Gêneros de um artista:** uma tag do `artist.getTopTags` só conta como gênero se, após normalizada, pertencer à lista fechada de gêneros (seção 8.8) **e** tiver `count` ≥ **10**. Outras tags ("female vocalists", "british") são ignoradas. |
| RN15 | **Artistas favoritos:** o usuário informa de **1 a 5** artistas, digitados por nome (campo de texto que vira um chip ao confirmar, com opção de remover). Nomes vazios são ignorados e repetidos (após normalização) contam uma vez. O nome digitado não é validado na API no onboarding, para que o onboarding funcione offline. |
| RN16 | Gêneros e nacionalidade de cada artista ficam em cache local **sem prazo de validade**, compartilhados entre humores. |

### 8.5 Preferência de origem

| Código | Regra |
|---|---|
| RN17 | "Nacionais": somente artistas brasileiros (RN13). "Internacionais": exclui artistas brasileiros. "Ambas": sem filtro. |

### 8.6 Estratégia de recomendação

| Código | Regra |
|---|---|
| RN18 | **Fonte principal ("Pelo seu humor", `MOOD`):** para cada tag do humor e modo, analisar no máximo as **30** primeiras músicas do `tag.getTopTracks`. Manter apenas as de artistas que tenham ao menos um gênero em comum com os gêneros do usuário (RN14) e que respeitem a preferência de origem (RN17). |
| RN19 | **Ordem da fonte principal:** músicas que aparecem em mais tags do humor vêm primeiro; o empate é resolvido pela melhor posição na tag e, depois, pela Extroversão (RN07). |
| RN20 | **Fonte de complemento ("Pelo seu gosto", `TASTE`):** usada somente se a fonte principal trouxer menos de **10** músicas. Busca artistas similares aos favoritos (`artist.getSimilar`) dentro da faixa de `match` (RN06), aplica a preferência de origem e seleciona candidatos. Os **20** melhores candidatos (maior `match`) passam pelo `track.getInfo`; os que têm alguma tag do humor escolhido sobem no ranking. |
| RN21 | **Tamanho da lista:** no máximo **20** músicas por recomendação: primeiro as da fonte principal e depois as de complemento até completar. |
| RN22 | Toda música recomendada guarda a fonte que a trouxe (`MOOD` ou `TASTE`) e exibe o selo correspondente: "Pelo seu humor" ou "Pelo seu gosto". |
| RN23 | **Deduplicação:** duas músicas são iguais quando artista e nome, normalizados (RN12), são iguais. Nunca exibir duplicadas; se a mesma música vier das duas fontes, prevalece `MOOD`. |
| RN24 | Músicas salvas com status `LISTENED` são removidas das recomendações. |
| RN25 | **Cache de recomendações:** vale **24 horas** por combinação de conta, humor e modo. |
| RN26 | Ao refazer o teste ou mudar os gostos, o cache de recomendações da conta é descartado, para que as próximas recomendações já reflitam o novo perfil. O cache de gêneros dos artistas (RN16) é mantido, porque não depende do perfil. |

### 8.7 Capas

| Código | Regra |
|---|---|
| RN27 | As imagens de `tag.getTopTracks` e `artist.getSimilar` nunca são usadas. A capa válida vem de `album.image` (tamanho `extralarge`) no `track.getInfo`. URL vazia, ausência de álbum ou URL contendo o hash do placeholder (`2a96cbd8b46e442fc41c2b86b821562f`) significam "capa indisponível". |
| RN28 | Na fonte principal, a capa é buscada sob demanda, quando o card aparece na tela. Na fonte de complemento, ela já vem da chamada usada no ranking. |

### 8.8 Lista fechada de gêneros

O usuário escolhe **ao menos 1** gênero. Cada gênero exibido corresponde a uma ou mais tags do Last.fm, comparadas após normalização.

| Gênero exibido | Tags aceitas |
|---|---|
| Pop | pop |
| Rock | rock, classic rock, alternative rock |
| Indie | indie, indie pop, indie rock |
| Eletrônica | electronic, dance, house, edm |
| Hip-hop / Rap | hip-hop, hip hop, rap |
| R&B / Soul | rnb, r&b, soul |
| Jazz | jazz |
| Blues | blues |
| Clássica | classical |
| Metal | metal, heavy metal |
| Reggae | reggae |
| Country | country |
| Latina | latin, reggaeton |
| K-pop | k-pop, kpop |
| Gospel | gospel |
| MPB | mpb |
| Samba / Pagode | samba, pagode |
| Bossa Nova | bossa nova |
| Sertanejo | sertanejo |
| Forró | forro |
| Funk brasileiro | funk carioca, brazilian funk |
| Axé | axe |

### 8.9 Músicas salvas

| Código | Regra |
|---|---|
| RN29 | Uma música não pode ser salva duas vezes pela mesma conta (igualdade conforme RN23). |
| RN30 | Toda música salva começa como `WANT_TO_LISTEN` e passa a `LISTENED` somente por ação manual do usuário. A transição é de mão única. |
| RN31 | A avaliação (1 a 5 estrelas) só é permitida para músicas `LISTENED`; valores fora de 1..5 são rejeitados. A avaliação pode ser alterada depois. |

### 8.10 Conta, dados e notificação

| Código | Regra |
|---|---|
| RN32 | Todo dado local (perfil, músicas salvas, caches de recomendação) é associado ao `uid` da conta. O cache de gêneros e nacionalidade dos artistas é público (não pertence a uma conta) e pode ser compartilhado. |
| RN33 | Uma conta sem perfil completo no aparelho (por exemplo, após reinstalar o app) deve refazer o onboarding. |
| RN34 | Uma notificação local diária é agendada com `PeriodicWorkRequest` (WorkManager), por volta das **19h** (horário local), momento em que a maioria das pessoas já saiu do trabalho ou da aula. |
| RN35 | Se a permissão de notificação for negada, o app continua funcionando normalmente e não volta a pedi-la automaticamente. |

### 8.11 Épocas e atribuição

| Código | Regra |
|---|---|
| RN36 | **Épocas** (opcional, múltipla escolha): 60s, 70s, 80s, 90s, 2000s, 2010s, 2020s. Aparecem apenas no perfil e **não** influenciam as recomendações, porque nenhuma regra do `CLAUDE.md` as usa e o Last.fm não informa o ano das músicas nos endpoints adotados. |
| RN37 | **Atribuição ao Last.fm** (termos da API, cláusulas 2.7 e 4.2.2): a tela "Sobre" exibe um dos botões oficiais "powered by AudioScrobbler" de last.fm/resources, com link para last.fm, e o texto "Dados musicais fornecidos pelo Last.fm". |

## 9. Critérios de aceite

### Conta e onboarding

- **CA01 (RF01):** Dado um e-mail ainda não cadastrado e uma senha com 6 caracteres ou mais, quando o usuário se cadastra, então a conta é criada e ele é levado ao onboarding.
- **CA02 (RF02):** Dado um usuário logado, quando ele fecha e reabre o app, mesmo sem internet, então continua logado.
- **CA03 (RF02):** Dado um e-mail ou senha incorretos, quando o usuário tenta entrar, então vê uma mensagem de erro em português e permanece na tela de login.
- **CA04 (RF03, RNF08):** Dado o usuário A logado com perfil e músicas salvas, quando ele sai e o usuário B entra no mesmo aparelho, então B não vê nenhum dado de A.
- **CA05 (RF04, RN33):** Dada uma conta sem perfil no aparelho, quando ela entra, então é levada ao onboarding antes da conversa.
- **CA06 (RF06):** Dado o início do teste, quando o usuário o abre, então vê o aviso de que o teste não é uma avaliação psicológica.
- **CA07 (RF05, RN01, RN02):** Dado o teste com 9 respostas, quando o usuário tenta concluir, então o app não conclui e indica o item pendente.
- **CA08 (RN03, RN04):** Dadas as respostas 1=5, 2=3, 3=6, 4=2, 5=7, 6=2, 7=6, 8=1, 9=5, 10=3, quando o perfil é calculado, então Extroversão = 5,5; Amabilidade = 5,5; Conscienciosidade = 6,5; Estabilidade emocional = 5,5; Abertura = 6,0.
- **CA09 (RF07–RF10):** Dado o onboarding de gostos, quando o usuário escolhe ao menos um gênero, de 1 a 5 artistas e uma preferência de origem, então o perfil é salvo e ele é levado à conversa.

### Perfil

- **CA10 (RF11):** Dado um perfil completo, quando o usuário abre o perfil, então vê os cinco traços com seus valores e os gostos informados, mesmo sem internet.
- **CA11 (RF12, RN26):** Dado um perfil existente, quando o usuário refaz o teste e confirma, então os traços são recalculados e o cache de recomendações da conta é descartado.

### Conversa

- **CA12 (RF13, RN11):** Dado o app sem internet, quando o usuário abre a conversa, então ela funciona normalmente até o passo de recomendações.
- **CA13 (RF14, RN10):** Dado o humor "Feliz", quando o usuário o escolhe, então a conversa vai direto às recomendações, sem perguntar o modo.
- **CA14 (RF14, RN10):** Dado o humor "Triste", quando o usuário o escolhe, então a conversa pergunta se ele quer acompanhar ou mudar o clima.
- **CA15 (RN09):** Dado o humor "Cansado" no modo mudar, quando as tags são calculadas, então são `energetic` e `upbeat`.

### Recomendações

- **CA16 (RF15, RNF01, RN25):** Dado um cache com menos de 24 h para o humor e modo, quando o usuário chega às recomendações, então elas aparecem imediatamente, sem chamada à API.
- **CA17 (RNF01):** Dado um cache com mais de 24 h e conexão disponível, quando o usuário chega às recomendações, então o cache aparece de imediato e é substituído pelos dados novos ao fim da busca.
- **CA18 (RNF01):** Dado um cache existente e uma falha na API, quando a busca termina, então as músicas em cache continuam na tela e um aviso discreto (Snackbar) informa o erro.
- **CA19 (RNF01, L5):** Dado que não há cache nem conexão, quando o usuário chega às recomendações, então vê o estado vazio explicando que é preciso internet para a primeira busca, com o botão "Tentar novamente".
- **CA20 (RF16):** Dadas recomendações na tela e conexão disponível, quando o usuário puxa para atualizar, então uma nova busca é feita, mesmo com o cache válido.
- **CA21 (RN18, RN17):** Dada a preferência "Nacionais", quando as recomendações são geradas, então nenhuma música é de artista sem tag de nacionalidade brasileira.
- **CA22 (RN20):** Dada uma fonte principal com menos músicas que o mínimo, quando as recomendações são geradas, então a lista é completada com músicas da fonte "Pelo seu gosto", com o selo correspondente.
- **CA23 (RN23):** Dadas "Águas de Março" e "aguas de marco " do mesmo artista, quando as recomendações são montadas, então aparece só uma delas.
- **CA24 (RN24):** Dada uma música salva como ouvida, quando novas recomendações são geradas, então ela não aparece.
- **CA25 (RN27):** Dada uma capa cuja URL contém o hash do placeholder, quando o card é exibido, então aparece o estado "capa indisponível".
- **CA26 (RF17):** Dada uma música, quando o usuário toca em "Spotify" (ou "YouTube"), então o app abre a busca por "artista música" no app correspondente ou, se ele não estiver instalado, no navegador.

### Músicas salvas

- **CA27 (RF18, RN29):** Dada uma música já salva, quando o usuário tenta salvá-la de novo, então ela não é duplicada.
- **CA28 (RF19):** Dadas músicas com os dois status, quando o usuário filtra por "Ouvidas", então só aparecem as `LISTENED`.
- **CA29 (RF21, RN31):** Dada uma música `WANT_TO_LISTEN`, quando o usuário tenta avaliá-la, então a avaliação não é permitida.
- **CA30 (RF20, RF21):** Dada uma música marcada como ouvida, quando o usuário dá 4 estrelas, então a avaliação fica salva e aparece na lista, mesmo offline.
- **CA38 (RF26):** Dada uma música salva, quando o usuário pede para removê-la e confirma, então ela sai da lista; se cancelar, nada muda.

### Offline, notificação e Sobre

- **CA31 (RF22):** Dado o app aberto, quando a conexão cai, então o banner "sem internet" aparece; quando volta, o banner some.
- **CA32 (RF23, RF24, RN35):** Dado o Android 13 ou superior, quando o app pede a permissão de notificação, então primeiro explica o motivo; se o usuário negar, o app segue funcionando normalmente.
- **CA33 (RF23):** Dado o agendamento feito, quando chega o horário da notificação, então ela aparece e, ao ser tocada, abre o app.
- **CA34 (RF25, RN37):** Dada a tela "Sobre", quando o usuário a abre, então vê o botão "powered by AudioScrobbler" com link para o Last.fm e as atribuições à Nunito, à Sacramento e ao TIPI.

### Arquitetura e segurança

- **CA35 (RNF02):** Dado o código do pacote `domain`, quando se procuram imports de `android.`, `retrofit2.`, `androidx.room.`, `org.koin.` ou `com.google.firebase.`, então nenhum é encontrado.
- **CA36 (RNF03):** Dado o histórico do repositório, quando se procura a chave do Last.fm, então ela não é encontrada.
- **CA37 (RNF11):** Dado o projeto, quando se executa `./gradlew testDebugUnitTest`, então todos os testes passam.

## 10. Decisões registradas

| Data | Decisão | Motivo |
|---|---|---|
| 2026-11-19 | Last.fm como única API de música; Deezer descartado. | O `track.getInfo` já fornece capas reais (spike). |
| 2026-11-19 | Estratégia de duas fontes ("Pelo seu humor" e "Pelo seu gosto"). | A primeira acerta o humor; a segunda, o gosto, e cobre gêneros brasileiros (spike). |
| 2026-11-19 | Tags de humor só em inglês; "study" e tags em português descartadas. | Alcance muito baixo (ex.: "triste" com 386 usuários; "study" com 585). |
| 2026-11-19 | Descoberta controlada pelo `match` do `artist.getSimilar`, conforme a Abertura. | Personaliza o grau de novidade sem IA. |
| 2026-11-19 | Preferência de origem no onboarding e selo de origem nos cards. | Transparência e controle sobre música nacional e internacional. |
| 2026-11-19 | Testes de repositório com MockK, conforme o `CLAUDE.md`. | Decisão da responsável pelo projeto. |
| 2026-11-19 | `minSdk` mantido em 24. | Decisão da responsável pelo projeto; os PNGs do ícone ficam como fallback. |
| 2026-11-19 | Fontes organizadas em `res/font/` e licenças OFL em `docs/licencas/`. | Padrão da `identidade_visual.md` (fontes embutidas, Offline First). |
| 2026-11-19 | Valores da recomendação: `count` ≥ 10; `match` ≥ 0,10 / 0,25 / 0,50; mínimo de 10 músicas da fonte principal; lista de até 20. | Equilíbrio entre acerto de gênero e variedade; 20 músicas cabem em poucas rolagens sem cansar. |
| 2026-11-19 | TIPI pt-BR: versão de Carlos Eduardo Pimentel (UnB). | É a versão brasileira indicada no site do autor do TIPI, que permite o uso livre do instrumento. |
| 2026-11-19 | Atribuição: botão "powered by AudioScrobbler" + texto na tela "Sobre". | Exigência das cláusulas 2.7 e 4.2.2 dos termos da API do Last.fm. |
| 2026-11-19 | Cache de recomendações descartado ao mudar o perfil. | Evita recomendações que não refletem o perfil novo. |
| 2026-11-19 | Artistas sem tag de nacionalidade contam como internacionais. | Regra simples e previsível; limitação L6 documentada. |
| 2026-11-19 | Package `com.leticia.moodiva`. | Já criado no template do Android Studio. |
| 2026-11-19 | Lista fechada de 22 gêneros (seção 8.8), ao menos 1 escolhido. | Cobre gêneros nacionais e internacionais mais comuns. |
| 2026-11-19 | Artistas favoritos: de 1 a 5, digitados, sem validação online. | Onboarding funciona offline e não fica longo. |
| 2026-11-19 | Épocas opcionais e só exibidas no perfil. | Nenhuma regra as usa e o Last.fm não informa o ano nos endpoints adotados. |
| 2026-11-19 | Notificação por volta das 19h. | Horário em que a maioria já saiu do trabalho ou da aula. |
| 2026-11-19 | Senha com mínimo de 6 caracteres; "esqueci minha senha" fora do escopo. | Regra do Firebase; mantém o escopo do `CLAUDE.md`. |
| 2026-11-19 | Remoção de música salva com confirmação (RF26). | Permite corrigir um salvamento por engano sem perder dados por toque acidental. |
| 2026-11-19 | Limite próprio de 5 requisições/s ao Last.fm; uso não comercial. | Os termos não fixam um número (cláusula 4.4); o app é acadêmico e não comercial (cláusula 3.1). |

## 11. Pontos de atenção para a responsável

- Os termos do Last.fm pedem que projetos de **pesquisa ou acadêmicos** avisem antes de usar a API, pelo e-mail partners [at] last [dot] fm. Recomenda-se enviar uma mensagem curta descrevendo o Moodiva como trabalho acadêmico, não comercial.
- O site do TIPI pede, por cortesia, que quem usa uma tradução contate o autor dela (Carlos Eduardo Pimentel). Não é obrigatório.
- Os itens do TIPI brasileiro estão no masculino ("Extrovertido", "Reservado"). Pela regra de não alterar o texto validado, eles são exibidos como estão.
