# Resultados dos testes — Moodiva

- **Comando:** `./gradlew testDebugUnitTest`
- **Relatório HTML:** `app/build/reports/tests/testDebugUnitTest/index.html`
- Cada entrada abaixo é uma execução real; os números vêm dos relatórios XML do Gradle.

---

## 2026-11-19 19:39 — T05: regras de negócio do domain

| Item | Valor |
|---|---|
| Comando | `./gradlew testDebugUnitTest` |
| Total | 56 |
| Aprovados | 56 |
| Falhas | 0 |
| Ignorados | 0 |
| Correções aplicadas | Nenhuma |

---

## 2026-11-19 19:44 — T06: casos de uso (motor de recomendação, cache, conta, salvas)

| Item | Valor |
|---|---|
| Comando | `./gradlew testDebugUnitTest` |
| Total | 85 |
| Aprovados | 85 |
| Falhas | 0 |
| Ignorados | 0 |
| Correções aplicadas | Expectativa do teste do complemento corrigida: a escolha espaçada de 8 entre 10 artistas não inclui o 9º; o teste passou a usar o 10º (menor match). |

---

## 2026-11-19 19:46 — T07: cliente do Last.fm (parsing de JSON real, mappers, interceptor)

| Item | Valor |
|---|---|
| Comando | `./gradlew testDebugUnitTest` |
| Total | 96 |
| Aprovados | 96 |
| Falhas | 0 |
| Ignorados | 0 |
| Correções aplicadas | Nenhuma |

---

## 2026-11-19 19:50 — T08: Room, autenticação local e repositórios (MockK)

| Item | Valor |
|---|---|
| Comando | `./gradlew testDebugUnitTest` |
| Total | 113 |
| Aprovados | 113 |
| Falhas | 0 |
| Ignorados | 0 |
| Correções aplicadas | Nenhuma |

---

## 2026-11-19 19:56 — T09: ViewModel raiz (sessão, perfil, conexão) e utilitários de apresentação

| Item | Valor |
|---|---|
| Comando | `./gradlew testDebugUnitTest` |
| Total | 119 |
| Aprovados | 119 |
| Falhas | 0 |
| Ignorados | 0 |
| Correções aplicadas | Nenhuma |

---

## 2026-11-19 19:59 — T10: login e cadastro (AuthViewModel)

| Item | Valor |
|---|---|
| Comando | `./gradlew testDebugUnitTest` |
| Total | 125 |
| Aprovados | 125 |
| Falhas | 0 |
| Ignorados | 0 |
| Correções aplicadas | Nenhuma |

---

## 2026-11-19 20:05 — T11: onboarding (OnboardingViewModel)

| Item | Valor |
|---|---|
| Comando | `./gradlew testDebugUnitTest` |
| Total | 132 |
| Aprovados | 132 |
| Falhas | 0 |
| Ignorados | 0 |
| Correções aplicadas | Nenhuma |

---

## 2026-11-19 20:08 — T12: conversa guiada (ChatViewModel)

| Item | Valor |
|---|---|
| Comando | `./gradlew testDebugUnitTest` |
| Total | 139 |
| Aprovados | 139 |
| Falhas | 0 |
| Ignorados | 0 |
| Correções aplicadas | Nenhuma |

---

## 2026-11-19 20:14 — T13: recomendações (RecommendationsViewModel)

| Item | Valor |
|---|---|
| Comando | `./gradlew testDebugUnitTest` |
| Total | 149 |
| Aprovados | 149 |
| Falhas | 0 |
| Ignorados | 0 |
| Correções aplicadas | Teste 'música marcada como ouvida' passou a conferir o estado final: o combine emite uma vez para a lista de salvas e outra para as recomendações. |

---

## 2026-11-19 20:17 — T14: músicas salvas (SavedViewModel)

| Item | Valor |
|---|---|
| Comando | `./gradlew testDebugUnitTest` |
| Total | 154 |
| Aprovados | 154 |
| Falhas | 0 |
| Ignorados | 0 |
| Correções aplicadas | Nenhuma |

---

## 2026-11-19 20:20 — T15: perfil e Sobre (ProfileViewModel)

| Item | Valor |
|---|---|
| Comando | `./gradlew testDebugUnitTest` |
| Total | 156 |
| Aprovados | 156 |
| Falhas | 0 |
| Ignorados | 0 |
| Correções aplicadas | Nenhuma |

---

## 2026-11-19 20:24 — T16: lembrete diário (NotificationScheduler)

| Item | Valor |
|---|---|
| Comando | `./gradlew testDebugUnitTest` |
| Total | 158 |
| Aprovados | 158 |
| Falhas | 0 |
| Ignorados | 0 |
| Correções aplicadas | Nenhuma |

---

## 2026-11-19 20:32 — Ajuste: alternância de artistas na fonte Pelo seu gosto

| Item | Valor |
|---|---|
| Comando | `./gradlew testDebugUnitTest` |
| Total | 159 |
| Aprovados | 159 |
| Falhas | 0 |
| Ignorados | 0 |
| Correções aplicadas | Nenhuma |

---

## 2026-11-19 20:34 — T17: execução final (clean build)

| Item | Valor |
|---|---|
| Comando | `./gradlew testDebugUnitTest` |
| Total | 159 |
| Aprovados | 159 |
| Falhas | 0 |
| Ignorados | 0 |
| Correções aplicadas | Nenhuma |

---

## Validação final — critérios de aceite

Legenda: **T** = teste unitário · **E** = evidência no emulador Medium Phone API 37 (`evidencias/`) · **M** = verificação manual/comando.

| CA | Critério | Como foi verificado |
|---|---|---|
| CA01 | Cadastro válido cria a conta e leva ao onboarding | T (`AccountUseCasesTest`, `AuthRepositoryImplTest`, `AuthViewModelTest`) · E (cadastro no emulador → onboarding) |
| CA02 | Sessão mantida ao reabrir, mesmo offline | T (`AuthRepositoryImplTest`: sessão reflete o banco) · E (app reaberto com `force-stop` e sem rede continuou logado) |
| CA03 | Senha errada mostra erro e mantém no login | T (`AuthViewModelTest`, `AuthRepositoryImplTest`) |
| CA04 | Conta B não vê dados da conta A | T (`ProfileUseCasesTest`, `SavedTrackUseCasesTest`) · E (`17-isolamento-conta-b.png`) |
| CA05 | Conta sem perfil vai ao onboarding | T (`MoodivaAppViewModelTest`) · E |
| CA06 | Aviso de que o teste não é avaliação psicológica | E (tela inicial do onboarding e resultado, `06-resultado-tipi-ca08.png`) |
| CA07 | Teste com 9 respostas não conclui | T (`TipiCalculatorTest`, `OnboardingViewModelTest`) |
| CA08 | Respostas de exemplo → 5,5 / 5,5 / 6,5 / 5,5 / 6,0 | T (`TipiCalculatorTest`) · E (`06-resultado-tipi-ca08.png`) |
| CA09 | Gostos válidos salvam o perfil e levam à conversa | T (`OnboardingViewModelTest`) · E |
| CA10 | Perfil exibe traços e gostos offline | E (`11-perfil.png`) |
| CA11 | Refazer o teste recalcula e descarta o cache | T (`OnboardingViewModelTest`, `ProfileUseCasesTest`) |
| CA12 | Conversa funciona offline | T (`ChatViewModelTest`) · E (`16-offline-sem-cache.png`) |
| CA13 | "Feliz" vai direto às recomendações | T (`ConversationMachineTest`, `ChatViewModelTest`) |
| CA14 | "Triste" pergunta o modo | T (`ConversationMachineTest`, `ChatViewModelTest`) · E (`07-conversa.png`) |
| CA15 | Cansado + mudar = energetic, upbeat | T (`MoodTagMapperTest`, `GenerateRecommendationsUseCaseTest`) |
| CA16 | Cache válido aparece sem chamar a API | T (`RecommendationUseCasesTest`, `RecommendationsViewModelTest`) |
| CA17 | Cache vencido é exibido e substituído | T (`RecommendationUseCasesTest`) |
| CA18 | Falha com cache mantém músicas + Snackbar | T (`RecommendationsViewModelTest`) · E (`15-offline-cache.png`) |
| CA19 | Sem cache e sem rede: vazio + "Tentar novamente" | T (`RecommendationsViewModelTest`, `ChatViewModelTest`) · E (`16-offline-sem-cache.png`) |
| CA20 | Pull-to-refresh busca mesmo com cache válido | T (`RecommendationsViewModelTest`, `RecommendationUseCasesTest`) · E (`18-pelo-seu-gosto.png`, após puxar) |
| CA21 | "Nacionais" só traz artistas brasileiros | T (`GenerateRecommendationsUseCaseTest`, `MoodSourceRankerTest`) · E (`18-pelo-seu-gosto.png`) |
| CA22 | Fonte principal fraca é completada com "Pelo seu gosto" | T (`GenerateRecommendationsUseCaseTest`) · E (`18-pelo-seu-gosto.png`) |
| CA23 | Músicas iguais após normalização aparecem uma vez | T (`TextNormalizerTest`, `RecommendationMergerTest`) |
| CA24 | Música ouvida some das recomendações | T (`RecommendationUseCasesTest`, `RecommendationsViewModelTest`) · E (`15-offline-cache.png`) |
| CA25 | Capa placeholder → "capa indisponível" | T (`LastFmParsingTest`) · E (`18-pelo-seu-gosto.png`, nota musical) |
| CA26 | Spotify/YouTube abrem a busca | M (toque em "Spotify" abriu `open.spotify.com/search/…` no navegador do emulador) |
| CA27 | Música já salva não duplica | T (`SavedTrackUseCasesTest`, `RecommendationsViewModelTest`) |
| CA28 | Filtro "Ouvidas" | T (`SavedViewModelTest`) |
| CA29 | Música não ouvida não pode ser avaliada | T (`SavedTrackUseCasesTest`) |
| CA30 | Avaliação salva e exibida offline | T (`SavedViewModelTest`) · E (`10-musicas-salvas-avaliacao.png`) |
| CA31 | Banner aparece sem rede e some ao voltar | T (`MoodivaAppViewModelTest`) · E (`15-offline-cache.png`; banner sumiu ao religar o Wi-Fi) |
| CA32 | Explica antes de pedir a permissão; negar não quebra o app | E (`13-permissao-notificacao.png`; com a permissão negada o worker terminou sem notificar) |
| CA33 | Lembrete aparece e abre o app | T (`NotificationSchedulerTest`, `NotificationWindowTest`) · E (`14-lembrete-diario.png`; toque abriu a conversa) |
| CA34 | "Sobre" com Last.fm, fontes e TIPI | E (`12-sobre-atribuicoes.png`) |
| CA35 | Domain sem imports proibidos | M (`grep` por `android.`, `retrofit2`, `okhttp3`, `androidx.room`, `org.koin` em `domain/`: 0 ocorrências) |
| CA36 | Chave fora do repositório | M (`git log -p --all` sem a chave; log HTTP com `api_key=██`) |
| CA37 | Todos os testes passam | M (`./gradlew clean testDebugUnitTest`: 159/159) |
| CA38 | Remover pede confirmação | T (`SavedViewModelTest`) |
