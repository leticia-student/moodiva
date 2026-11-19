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
