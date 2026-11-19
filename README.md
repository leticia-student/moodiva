# Moodiva

Aplicativo Android que recomenda músicas de acordo com a **personalidade**, os **gostos** e o **humor do dia** do usuário. Trabalho acadêmico desenvolvido com Spec-Driven Development.

> README em construção: descreve apenas o que já existe no projeto.

## Documentação

- [Especificação](docs/01-especificacao.md)
- [Planejamento técnico](docs/02-planejamento.md)
- [Tarefas](docs/03-tarefas.md)
- [Identidade visual](docs/identidade_visual.md)

## Tecnologias

Kotlin · Jetpack Compose + Material 3 · Navigation Compose · Koin · Coroutines/Flow · Retrofit + OkHttp + kotlinx.serialization · Room · Coil · WorkManager · JUnit 4, MockK, Turbine.

## Configuração

1. Crie uma chave gratuita da API do Last.fm em https://www.last.fm/api/account/create.
2. No arquivo `local.properties` (na raiz, fora do Git), acrescente:

   ```properties
   LASTFM_API_KEY=sua_chave_aqui
   ```

3. Compile com `./gradlew assembleDebug`.

Sem a chave o projeto compila, mas as buscas de músicas falham com uma mensagem explicando o problema.
