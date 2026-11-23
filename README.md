# Sudoku by Altikore — 100% Offline Logic Game

Aplicación Android nativa (`com.altikore.sudoku`) desarrollada por **Altikore**: **Sudoku 100 % sin conexión**, sin anuncios, sin analítica y sin permisos de red (`INTERNET` ausente). Construida en Kotlin con Jetpack Compose, Material 3, arquitectura MVVM + flujo unidireccional de datos (UDF), persistencia local con Room + DataStore y diseñada como primer módulo de una colección extensible de minijuegos de lógica.

---

## Características principales

- **Primer valor en < 10 segundos:** inicio limpio directo al tablero o continuación exacta de la última partida, sin registro ni tutorial bloqueante.
- **Motor puro en Kotlin (`:core:sudoku`):**
  - Generador con solución **única garantizada** mediante backtracking bitmask MRV.
  - Solver lógico con 12 técnicas humanas (*Naked/Hidden Single*, *Pointing Candidates*, *Box-Line Reduction*, *Naked/Hidden Pair*, *Naked/Hidden Triple*, *X-Wing*, *XY-Wing*, *Naked Quad*, *Swordfish*).
  - 5 niveles calibrados: *Muy fácil*, *Fácil*, *Medio*, *Difícil* y *Experto*.
  - Precarga en segundo plano (`PuzzlePreloader` + Room) y **Puzzle diario offline** determinista por fecha (`DailySeed`).
- **Experiencia de juego completa:**
  - Notas (lápiz) con borrado automático en vecinos, Undo/Redo ilimitados con restauración exacta de notas, teclado con contador restante, pistas explicativas o revelación directa, verificación de errores configurable (*Inmediata*, *Bajo demanda*, *Desactivada*), pausa automática al salir de la app ocultando el tablero y pantalla de victoria con confeti.
  - Estadísticas por dificultad, calendario diario de 14 días, rachas y 8 logros locales.
- **Diseño, accesibilidad e idiomas:**
  - Material You dinámico (Android 12+), modo Claro/Oscuro/Auto, 4 paletas propias (*Clásico*, *Papel*, *Océano*, *Bosque*) y **Modo daltónico** de alto contraste con marcadores geométricos.
  - Layout adaptable con `WindowSizeClass` (teléfonos verticales, landscape, tablets y plegables), Edge-to-Edge, Predictive Back, soporte TalkBack completo, audio procedural sutil y hápticos opcionales (desactivados por defecto).
  - Localización completa en **Español (`es`)**, **Inglés (`en`)** y **Portugués (`pt`)** con cambio de idioma dentro de la app.

---

## Requisitos del entorno

- **JDK:** 17+ (verificado con OpenJDK 25 compilando a bytecode JVM 17)
- **Android SDK:** `compileSdk 36`, `targetSdk 36`, `minSdk 26`, Build Tools `36.1.0`
- **Gradle:** Wrapper `9.3.1` + AGP `9.1.0` + Kotlin `2.3.20` + KSP `2.3.6`

---

## Compilación y pruebas

```powershell
# Ejecutar todas las pruebas unitarias y de UI en Compose (Robolectric)
.\gradlew.bat :core:common:test :core:sudoku:test :core:data:testDebugUnitTest :feature:sudoku:testDebugUnitTest --console=plain

# Verificar Android Lint en todos los módulos Android
.\gradlew.bat :app:lintDebug :feature:sudoku:lintDebug :core:data:lintDebug :core:ui:lintDebug --console=plain

# Compilar APK de depuración instalable
.\gradlew.bat :app:assembleDebug

# Compilar pruebas instrumentadas
.\gradlew.bat :app:compileDebugAndroidTestKotlin

# Compilar APK y AAB de release (R8 + reducción de recursos)
.\gradlew.bat :app:assembleRelease :app:bundleRelease
```

Salidas principales:
- **APK Debug:** `app/build/outputs/apk/debug/app-debug.apk`
- **APK Release:** `app/build/outputs/apk/release/`
- **AAB Release (Google Play):** `app/build/outputs/bundle/release/app-release.aab`

---

## Cómo firmar el release (Keystore para Google Play — Altikore)

La forma más rápida es ejecutar el asistente PowerShell incluido en el repositorio:

```powershell
.\scripts\build-play-release.ps1
```

Este script crea `altikore-sudoku-release.jks` y `keystore.properties` (si no existen), compila el `.aab` y `.apk` de release con R8 y verifica la firma con `jarsigner`.

Si prefieres hacerlo manualmente:

1. **Generar tu keystore de subida (Upload Key):**
   ```powershell
   keytool -genkeypair -v `
     -keystore altikore-sudoku-release.jks `
     -alias altikore-sudoku `
     -storetype PKCS12 `
     -keyalg RSA -keysize 2048 -validity 10000 `
     -dname "CN=Altikore, O=Altikore"
   ```
2. **Configurar credenciales localmente** (nunca subas el `.jks` ni `keystore.properties` a Git):
   - Copia `keystore.properties.example` a `keystore.properties` en la raíz del proyecto y completa:
     ```properties
     storeFile=altikore-sudoku-release.jks
     storePassword=TU_STORE_PASSWORD
     keyAlias=altikore-sudoku
     keyPassword=TU_KEY_PASSWORD
     ```
   - Alternativamente en CI/CD, define las variables de entorno `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` y `RELEASE_KEY_PASSWORD`.
3. **Generar el App Bundle firmado:**
   ```powershell
   .\gradlew.bat :app:bundleRelease
   ```
   El archivo generado en `app/build/outputs/bundle/release/app-release.aab` queda minificado con R8, con reducción de recursos (`isShrinkResources = true`), Baseline Profile compilado y listo para subir a **Google Play Console** (recomendado activar *Play App Signing*). Revisa también la guía completa paso a paso en `docs/store/PLAY_CONSOLE_CHECKLIST.md`.

---

## Estructura del proyecto

```text
Sudoku/
├── app/                      # Single-Activity (com.altikore.sudoku), NavHost, Koin, Splash, íconos, Baseline Profile
├── core/
│   ├── common/               # Kotlin JVM puro: contratos compartidos de la colección (GameId, DifficultyTier, DailySeed)
│   ├── sudoku/               # Kotlin JVM puro (0 deps Android): motor, generador, solver lógico y tests
│   ├── data/                 # Room DB (5 tablas), DAOs, DataStore Preferences, repositorios y calculadoras
│   └── ui/                   # Material 3, paletas, modo daltónico, íconos vectoriales, confeti, feedback
├── feature/
│   └── sudoku/               # Pantallas Home, Game, Stats/Achievements, Settings + ViewModels + tests UI
├── docs/
│   ├── index.html            # Web estática ES/EN/PT de Política de Privacidad lista para GitHub Pages
│   └── store/                # Ícono 512x512, feature graphics 1024x500, capturas, ficha ASO y checklist Play Console
├── scripts/
│   └── build-play-release.ps1
├── keystore.properties.example
└── gradle/libs.versions.toml
```

---

## Cómo añadir un nuevo minijuego a la colección

La arquitectura desacopla la lógica pura, la persistencia indexada por `GameId` y el diseño compartido para incorporar nuevos juegos (p. ej., **Nonogram**, **Buscaminas** o **Kakuro**) sin reescribir la base:

1. **Registrar el identificador en `:core:common`:**
   - Añade `val NONOGRAM = GameId("nonogram")` en `KnownGames` (`core/common/.../GameId.kt`).
   - Implementa `DifficultyTier` en el enum de dificultades del nuevo juego.
   - Usa `DailySeed.dailySeed(KnownGames.NONOGRAM, epochDay)` para su puzzle diario offline.
2. **Crear `:core:<juego>` (Kotlin JVM puro):**
   - Incluye `implementation(project(":core:common"))` en `build.gradle.kts` sin dependencias de Android.
   - Implementa el generador determinista, validador y reducer inmutable con sus tests unitarios.
3. **Reutilizar `:core:data`:**
   - Todas las tablas de `MinigamesDatabase` (`active_games`, `preloaded_puzzles`, `difficulty_stats`, `daily_records`, `achievements`) ya están particionadas por `gameId`.
   - Expón un repositorio o amplía `RoomSudokuDataRepository` filtrando por el nuevo `GameId`.
4. **Crear `:feature:<juego>`:**
   - Depende de `:core:common`, `:core:<juego>`, `:core:ui` y `:core:data`.
   - Usa `MinigamesTheme`, `AppIcons`, `AndroidGameFeedback` y `WindowSizeClass`.
   - Declara su módulo Koin (`<juego>FeatureModule`) e inclúyelo en `SudokuApplication`.
5. **Conectar rutas en `:app`:**
   - Añade las rutas `@Serializable` del nuevo juego en `SudokuNavHost.kt` y un selector de colección/hub como pantalla de entrada cuando existan 2 o más juegos registrados.
