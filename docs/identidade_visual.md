# Identidade visual — Moodiva

Este documento define o tema do app. Siga-o em toda tela e componente.
Em caso de dúvida sobre aparência, pergunte ao responsável do projeto.

## Princípios

- Sensação: delicada, acolhedora e um pouco glamourosa, como "uma amiga diva que entende seu humor".
- Base de cor: três rosas claros da marca — Orquídea (`#FFD1FF`), Algodão (`#FFDFF6`) e Pétala (`#FFF2FB`) — com Ameixa (`#8B2F86`) para texto e ações, Violeta para apoio e Dourado para avaliações.
- Os rosas da marca têm contraste baixo (1,33:1 contra branco) e **nunca** são usados como cor de texto ou de botão sobre fundo claro. No tema claro eles são as superfícies; no tema escuro, a Orquídea vira a cor principal sobre o fundo ameixa.
- Temas claro e escuro são obrigatórios e seguem o sistema (`isSystemInDarkTheme()`).
- Todas as combinações de texto e fundo deste documento foram verificadas com contraste mínimo de 4,5:1 (WCAG AA).

## Regras para o agente

1. **Cores somente via `MaterialTheme.colorScheme`.** Nunca use `Color(0xFF...)` em telas ou componentes. Valores hexadecimais existem apenas em `Color.kt`. Diferente do KeveOQ, não há objeto de cores de marca: os rosas da marca já fazem parte do `colorScheme`.
2. **Não use cor dinâmica (Material You).** O tema não tem o parâmetro `dynamicColor`; não o adicione.
3. **Tipografia somente via `MaterialTheme.typography`.** Não defina `fontSize` ou `fontFamily` diretamente em telas.
4. **A fonte manuscrita (Sacramento) é exclusiva do nome "Moodiva"** e só aparece pelos estilos `displayLarge` e `displayMedium`. Nunca a use em frases, nomes de músicas, botões ou qualquer texto abaixo de 36sp.
5. **Não crie cores, fontes ou formas novas.** Se algo parecer faltar, pergunte.
6. Todo `@Composable` de tela deve ter preview nos dois temas com `@PreviewLightDark`.

## Arquivos do tema

Os arquivos ficam em `presentation/theme/`. O template do Android Studio os cria em `ui/theme/`: mova-os para `presentation/theme/` e ajuste o `package`, conforme a arquitetura do `CLAUDE.md`.

### Color.kt

```kotlin
package com.seunome.moodiva.presentation.theme

import androidx.compose.ui.graphics.Color

// ---------- Tema claro ----------
internal val PrimaryLight = Color(0xFF8B2F86)            // Ameixa
internal val OnPrimaryLight = Color(0xFFFFFFFF)
internal val PrimaryContainerLight = Color(0xFFFFD1FF)   // Orquídea (marca)
internal val OnPrimaryContainerLight = Color(0xFF3D0A42)
internal val InversePrimaryLight = Color(0xFFFFD1FF)

internal val SecondaryLight = Color(0xFF6A3FA0)          // Violeta
internal val OnSecondaryLight = Color(0xFFFFFFFF)
internal val SecondaryContainerLight = Color(0xFFE9DDFF)
internal val OnSecondaryContainerLight = Color(0xFF2A1060)

internal val TertiaryLight = Color(0xFF7A5800)           // Dourado escuro
internal val OnTertiaryLight = Color(0xFFFFFFFF)
internal val TertiaryContainerLight = Color(0xFFFFE3A1)
internal val OnTertiaryContainerLight = Color(0xFF3A2400)

internal val BackgroundLight = Color(0xFFFFF2FB)         // Pétala (marca)
internal val OnBackgroundLight = Color(0xFF2E1230)
internal val SurfaceVariantLight = Color(0xFFF6D9EE)
internal val OnSurfaceVariantLight = Color(0xFF6E4A69)
internal val SurfaceDimLight = Color(0xFFF0D3E8)
internal val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
internal val SurfaceContainerLowLight = Color(0xFFFFF8FD)  // cards de música, levemente "acima" do fundo
internal val SurfaceContainerLight = Color(0xFFFFDFF6)     // Algodão (marca): balões do app no chat
internal val SurfaceContainerHighLight = Color(0xFFFBD5EF)
internal val SurfaceContainerHighestLight = Color(0xFFF5CBE7)
internal val InverseSurfaceLight = Color(0xFF3A2438)
internal val InverseOnSurfaceLight = Color(0xFFFFEAF8)

internal val OutlineLight = Color(0xFF8E6E89)
internal val OutlineVariantLight = Color(0xFFE8C9E1)
internal val ErrorLight = Color(0xFFBA1A1A)
internal val OnErrorLight = Color(0xFFFFFFFF)
internal val ErrorContainerLight = Color(0xFFFFDAD6)
internal val OnErrorContainerLight = Color(0xFF410002)

// ---------- Tema escuro ----------
internal val PrimaryDark = Color(0xFFFFD1FF)             // Orquídea (marca)
internal val OnPrimaryDark = Color(0xFF57105A)
internal val PrimaryContainerDark = Color(0xFF6F2A6E)
internal val OnPrimaryContainerDark = Color(0xFFFFD1FF)
internal val InversePrimaryDark = Color(0xFF8B2F86)

internal val SecondaryDark = Color(0xFFD4BBFF)
internal val OnSecondaryDark = Color(0xFF3B1F6B)
internal val SecondaryContainerDark = Color(0xFF52348A)
internal val OnSecondaryContainerDark = Color(0xFFE9DDFF)

internal val TertiaryDark = Color(0xFFF2C25B)            // Dourado
internal val OnTertiaryDark = Color(0xFF3F2E00)
internal val TertiaryContainerDark = Color(0xFF5C4300)
internal val OnTertiaryContainerDark = Color(0xFFFFE3A1)

internal val BackgroundDark = Color(0xFF1E101F)
internal val OnBackgroundDark = Color(0xFFFFF2FB)        // Pétala (marca)
internal val SurfaceVariantDark = Color(0xFF4A3249)
internal val OnSurfaceVariantDark = Color(0xFFD8C0D3)
internal val SurfaceBrightDark = Color(0xFF3F2A3F)
internal val SurfaceContainerLowestDark = Color(0xFF170B18)
internal val SurfaceContainerLowDark = Color(0xFF2A1A2A)
internal val SurfaceContainerDark = Color(0xFF2E1B2E)
internal val SurfaceContainerHighDark = Color(0xFF392639)
internal val SurfaceContainerHighestDark = Color(0xFF443044)
internal val InverseSurfaceDark = Color(0xFFFFF2FB)
internal val InverseOnSurfaceDark = Color(0xFF2E1230)

internal val OutlineDark = Color(0xFFA08A9C)
internal val OutlineVariantDark = Color(0xFF4A3249)
internal val ErrorDark = Color(0xFFFFB4AB)
internal val OnErrorDark = Color(0xFF690005)
internal val ErrorContainerDark = Color(0xFF93000A)
internal val OnErrorContainerDark = Color(0xFFFFDAD6)
```

### Type.kt

As fontes ficam embutidas no app (não usar "downloadable fonts"), porque o app é Offline First. Os arquivos `.ttf` são adicionados manualmente em `app/src/main/res/font/` com estes nomes exatos:

- `sacramento_regular.ttf`
- `nunito_regular.ttf`
- `nunito_bold.ttf`
- `nunito_extrabold.ttf`

**Se os arquivos não existirem, pare e me peça.** Não baixe, não substitua por outra fonte e não remova a referência. O texto da licença (OFL) fica em `docs/licencas/` e as fontes são citadas no README.

A Sacramento tem traço muito fino. Por isso ela só existe em tamanhos grandes (36sp ou mais) e sempre sobre fundo de alto contraste (`primary` sobre `background`).

```kotlin
package com.seunome.moodiva.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.seunome.moodiva.R

private val Sacramento = FontFamily(
    Font(R.font.sacramento_regular, FontWeight.Normal),
)

private val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

internal val MoodivaTypography = Typography(
    // Somente para o nome "Moodiva"
    displayLarge = TextStyle(fontFamily = Sacramento, fontWeight = FontWeight.Normal, fontSize = 64.sp, lineHeight = 76.sp),
    displayMedium = TextStyle(fontFamily = Sacramento, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 48.sp),
    // Demais textos
    displaySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
)
```

### Theme.kt

```kotlin
package com.seunome.moodiva.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = PrimaryLight, onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight, onPrimaryContainer = OnPrimaryContainerLight,
    inversePrimary = InversePrimaryLight,
    secondary = SecondaryLight, onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight, onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight, onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight, onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight, onBackground = OnBackgroundLight,
    surface = BackgroundLight, onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight, onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = PrimaryLight,
    surfaceDim = SurfaceDimLight, surfaceBright = BackgroundLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    inverseSurface = InverseSurfaceLight, inverseOnSurface = InverseOnSurfaceLight,
    outline = OutlineLight, outlineVariant = OutlineVariantLight,
    error = ErrorLight, onError = OnErrorLight,
    errorContainer = ErrorContainerLight, onErrorContainer = OnErrorContainerLight,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark, onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark, onPrimaryContainer = OnPrimaryContainerDark,
    inversePrimary = InversePrimaryDark,
    secondary = SecondaryDark, onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark, onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark, onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark, onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark, onBackground = OnBackgroundDark,
    surface = BackgroundDark, onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = PrimaryDark,
    surfaceDim = BackgroundDark, surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    inverseSurface = InverseSurfaceDark, inverseOnSurface = InverseOnSurfaceDark,
    outline = OutlineDark, outlineVariant = OutlineVariantDark,
    error = ErrorDark, onError = OnErrorDark,
    errorContainer = ErrorContainerDark, onErrorContainer = OnErrorContainerDark,
)

private val MoodivaShapes = Shapes(
    small = RoundedCornerShape(12.dp),   // capas de álbum e chips
    medium = RoundedCornerShape(20.dp),  // cards de música e itens de lista
    large = RoundedCornerShape(28.dp),   // painéis, bottom sheets e diálogos
)

/**
 * Formas dos balões do chat. Não existem no Shapes do Material 3,
 * por isso ficam aqui. O canto de 4dp "aponta" para quem falou.
 * São as ÚNICAS formas extras permitidas.
 */
object ChatBubbleShapes {
    val App = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp)
    val User = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 4.dp, bottomStart = 20.dp)
}

@Composable
fun MoodivaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MoodivaTypography,
        shapes = MoodivaShapes,
        content = content,
    )
}
```

`ChatBubbleShapes` usa `start`/`end` em vez de esquerda/direita: em idiomas escritos da direita para a esquerda, os balões se espelham sozinhos.

Na `MainActivity`, chame `enableEdgeToEdge()` antes de `setContent { MoodivaTheme { ... } }` e trate os insets com `Scaffold` ou `WindowInsets`.

## Onde usar cada cor

| Elemento | Papel |
|---|---|
| Fundo das telas | `background` |
| Nome "Moodiva" (barra superior, splash, login, Sobre) | `primary` |
| Balão de mensagem do app | `surfaceContainer` / `onSurface`, forma `ChatBubbleShapes.App` |
| Balão de resposta do usuário | `primary` / `onPrimary`, forma `ChatBubbleShapes.User` |
| Chip de resposta não selecionado | `surfaceContainerLowest` com borda `outlineVariant` (1dp) |
| Chip de resposta selecionado | `primaryContainer` / `onPrimaryContainer` com borda `primary` (2dp) |
| Card de música | `surfaceContainerLow` |
| Botão principal (ex.: "Ouvir", "Começar teste") | `Button` padrão → `primary` / `onPrimary` |
| Botão secundário (ex.: "Já ouvi") | `OutlinedButton` → borda `outline`, texto `primary` |
| Chip de gênero musical | `secondaryContainer` / `onSecondaryContainer` |
| Estrelas de avaliação | `tertiary` (estrela vazia: `outline`) |
| Escala de resposta do teste (1 a 7) selecionada | `primary` / `onPrimary`; não selecionada: borda `outlineVariant` |
| Banner "sem internet" | `tertiaryContainer` / `onTertiaryContainer` |
| Itens da lista de músicas salvas | `surfaceContainerLow` |
| Capa indisponível (sem capa ou offline) | `primaryContainer` com ícone de nota musical em `primary` |
| Textos secundários (artista, "atualizado há…") | `onSurfaceVariant` |
| Mensagens de erro | `errorContainer` / `onErrorContainer` |

## Onde usar cada estilo de texto

| Uso | Estilo |
|---|---|
| Nome "Moodiva" na splash, login e tela Sobre | `displayLarge` (Sacramento) |
| Nome "Moodiva" na barra superior | `displayMedium` (Sacramento) |
| Frases de destaque ("Como você está hoje?", resultado do teste) | `displaySmall` |
| Título de tela ("Músicas salvas", "Seu perfil") | `headlineSmall` |
| Nome da música | `titleLarge` na tela de detalhes; `titleMedium` no card |
| Nome do traço de personalidade no perfil | `titleMedium` |
| Texto dos balões do chat e das perguntas do teste | `bodyLarge` |
| Artista e informações secundárias | `bodyMedium` |
| Legendas e dicas | `bodySmall` |
| Botões | `labelLarge` (padrão dos botões M3) |
| Chips | `labelMedium` |

## Espaçamento e toque

- Margem lateral das telas: 20dp.
- Espaço entre itens de lista: 16dp; entre seções: 24dp.
- Espaço entre balões do chat: 8dp entre mensagens do mesmo autor; 16dp quando o autor muda.
- Largura máxima de um balão: 80% da largura da tela.
- Alvo de toque mínimo: 48dp (botões de ícone usam `IconButton`, que já garante isso).
- Capa de álbum no card: 56×56dp, forma `small`. Na tela de detalhes, 240×240dp centralizada.

## Ícones

- Use os ícones do conjunto core (`Icons.Rounded`) quando existirem.
- Para ícones que não estão no core (ex.: nota musical, fone de ouvido, nuvem cortada), importe vetores individuais pelo *Vector Asset* do Android Studio para `res/drawable`. Não adicione a dependência `material-icons-extended` sem perguntar.
- Botões só com ícone devem ter `contentDescription` em português (ex.: "Voltar", "Salvar música").

## Ícone do app

Conceito: fone de ouvido em Ameixa com um coração dourado entre as conchas, sobre fundo Pétala. Representa "música" (o fone) e "sentimento" (o coração).

Os arquivos já estão prontos e substituem os do template:

- `res/drawable/ic_launcher_foreground.xml`: desenho (camada da frente).
- `res/drawable/ic_launcher_background.xml`: fundo Pétala chapado.
- `res/drawable/ic_launcher_monochrome.xml`: versão monocromática para ícones temáticos (Android 13+).
- `res/mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml`: juntam as três camadas.
- `docs/assets/icone_moodiva.svg`: versão para documentação e apresentação.

Não altere o desenho do ícone. Os PNGs antigos das pastas `mipmap-*dpi` do template podem continuar como fallback para Android 7 e anteriores; se o `minSdk` for 26 ou mais, eles podem ser apagados (pergunte antes).

## Estados das telas

- **Carregando:** `CircularProgressIndicator` centralizado em `primary`. No chat, enquanto as recomendações carregam, mostre um balão do app com o indicador dentro.
- **Vazio:** frase curta explicando o motivo e uma ação (ex.: "Você ainda não salvou nenhuma música." + botão "Conversar com a Moodiva").
- **Erro:** o que aconteceu e como resolver, sem pedir desculpas (ex.: "Não foi possível buscar músicas. Verifique sua conexão." + botão "Tentar novamente").
