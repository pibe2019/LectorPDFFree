# CLAUDE.md

Orientacion para Claude Code en este repositorio. Misma arquitectura y principios que GuiaMedicaSebas (rama `develop`).

## Producto
Lector de PDF gratuito, solo Android. `applicationId = com.antonio.mypdffree`; Play no permite cambiarlo.
Sin permisos (ni almacenamiento ni INTERNET): el acceso a cada PDF lo concede el sistema (SAF / "Abrir con").
Publicacion: `docs/PUBLICACION_PLAY_STORE.md`. Privacidad: `docs/POLITICA_DE_PRIVACIDAD.md`.

## Arquitectura: Clean Architecture + MVVM (influencia MVI)
- 3 capas: `data`, `domain` (Kotlin puro: cero `android.*`, `data.*`, `di.*`), `ui`.
- `sealed interface *Event` + `data class *UiState` + `onEvent()`. Un solo `uiState` = `combine(_uiState, flujos).stateIn(WhileSubscribed(5000))`.
- Eventos de una sola vez como ESTADO con acuse (`userMessages` + `OnMessageShown(id)`, `navigateBack` + `OnNavigatedBack`, `pendingScrollToPage` + `OnScrollHandled`); nunca `Channel`.
- Pantallas en dos capas: `XScreen` (ViewModel, launchers, efectos) y `XContent` (sin estado; previews y tests).
- Sin `init {}`: la carga inicial es un flujo con `stateIn` (en el lector, `SharingStarted.Lazily` para abrir el PDF una sola vez).

## Estructura
```
app/src/main/java/com/antonio/mypdffree/
├── MainActivity.kt          # installSplashScreen, edge-to-edge, intent ACTION_VIEW (content://) -> Reader
├── MyPdfFreeApp.kt          # @HiltAndroidApp
├── data/
│   ├── datasource/device/   # DocumentContentDataSource: ContentResolver (metadatos, descriptor, permisos persistibles)
│   ├── datasource/local/    # LocalRecentDocumentDataSource -> DAO
│   ├── error/               # DomainErrorException: "sobre" cuando el tipo de excepcion es ambiguo (PdfRenderer)
│   ├── local/room/          # AppDatabase v1, RecentDocumentDao (abstract class, @Transaction), RecentDocumentEntity
│   ├── mapper/              # DomainErrorMapper (Throwable.toDomainError, UNICA puerta), RecentDocumentMapper
│   ├── pdf/                 # AndroidPdfDocumentHandle (PdfRenderer + Mutex), AndroidPageBitmap, PageBitmapCache (LRU en bytes)
│   ├── repository/          # PdfDocument, DocumentAccess, RecentDocument (*Impl)
│   ├── time/                # SystemTimeProvider
│   └── util/                # resultOf { } (relanza CancellationException)
├── di/                      # Dispatchers, DataBase, Device (ContentResolver), DataSource, Repository (@Binds, sin scope)
├── domain/
│   ├── error/               # DomainError: Pdf / Local / Validation / Unknown
│   ├── model/               # PdfDocumentInfo, PdfDocumentHandle (AutoCloseable), PageBitmap (opaco), OpenedPdf, RecentDocument
│   │   └── result/          # DomainResult / AppResult (fold, map, getOrElse, getOrNull; sin getOrThrow)
│   ├── repository/          # Interfaces (ISP: abrir PDF y retener acceso son interfaces distintas)
│   ├── usecase/reader/      # OpenPdfDocument, RenderPdfPage (politica de memoria), SaveReadingProgress, ValidatePageNumber
│   ├── usecase/recent/      # ObserveRecentDocuments, RecordRecentDocument, RemoveRecentDocument, ClearRecentDocuments
│   └── util/                # TimeProvider
└── ui/
    ├── core/component/      # AppMessageSnackbarHost + UserMessagesEffect
    ├── core/mapper/         # DomainErrorText (when exhaustivo sin else), PageBitmapExt (UNICA costura ui->data)
    ├── core/navigation/     # RoutesScreens (Nav3 @Serializable), NavigationWrapper, ex/NavExt
    ├── core/util/           # UiText
    ├── modelui/             # AppMessage, UserMessage
    ├── theme/               # Material 3, color dinamico + paleta indigo de respaldo
    ├── home/                # Recientes
    └── reader/              # Lector; component/: PdfZoomState, PdfZoomGestures, PdfPagesList, PdfPageItem, dialogos
```

## Decisiones clave (no "corregirlas" sin motivo)
- **PdfRenderer del sistema**, no `androidx.pdf` (en beta en 10/2026) ni librerias AGPL/de pago.
- **El PDF abierto es un recurso con dueño**: `PdfDocumentHandle` lo devuelve el repositorio y lo cierra el `ReaderViewModel`
  (`addCloseable`). PdfRenderer no es thread-safe y solo admite una pagina abierta: todo pasa por un `Mutex`; `close()`
  nunca cierra mientras se pinta (lo libera quien suelta el mutex). `open()` usa `NonCancellable` y
  `OpenPdfDocumentUseCase` cierra el handle si lo cancelan, para no perder descriptores.
- **Los bitmaps no son estado**: la UI los pide con `PageLoader` (`ReaderViewModel.loadPage`) dentro de `produceState`,
  como una imagen de red. Es la unica excepcion a "la UI solo lee estado".
- **Zoom** = paginas mas anchas en el layout (scroll vertical nativo) + `offsetX` en `graphicsLayer`. El pellizco se
  consume en `PointerEventPass.Initial`. La resolucion de pintado sube por escalones (1x/2x/3x), con limite de 8 MP por pagina.
- **Historial**: solo entra lo que se puede reabrir (permiso persistible); maximo 50 entradas, y al recortar se liberan
  los permisos. Excluido del backup (los permisos de URI no viajan a otro dispositivo).
- **Room NO es cache**: guarda datos del usuario. Cada cambio de esquema lleva `Migration`/`AutoMigration`, nunca
  `fallbackToDestructiveMigration`. Esquemas en `app/schemas/`.
- Repositorios y use cases **sin scope** de Hilt. `@Singleton` solo para Room. Dispatchers inyectados
  (`@IoDispatcher` para IPC/archivos, `@DefaultDispatcher` para pintar). Room no se envuelve en `withContext`: ya es main-safe.

## Stack (catalogo `gradle/libs.versions.toml`, sin versiones a mano en los build.gradle.kts)
AGP 9.4.1 (Kotlin integrado) · Kotlin 2.4.21 · KSP 2.3.11 · Compose BOM 2026.09.00 · Material 3 · Hilt 2.60.1 ·
androidx.hilt 1.4.0 · Navigation 3 1.2.0 · Room 2.8.5 · Lifecycle 2.11.0 · core-splashscreen 1.2.0 ·
kotlinx.serialization 1.11.0 · coroutines 1.11.0 via BOM (dependencia DIRECTA: sin ella llegaba 1.9 transitiva y
coroutines-test fallaba en dispositivo con NoSuchMethodError) · JDK 21 (toolchain) · compileSdk 37 · targetSdk 36 (minimo exigido por Play desde 31/08/2026) · minSdk 26.
Release: `optimization { enable = true }` (R8 de codigo y recursos), reglas en `app/src/main/keepRules/`, firma desde `keystore.properties`.

## Comandos
- `./gradlew testDebugUnitTest`: tests unitarios (use cases, ViewModels con Turbine, PdfZoomState, mapper).
- `./gradlew pixel6api34DebugAndroidTest -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect`:
  tests instrumentados en emulador gestionado (DAO real, PdfRenderer con PDFs generados, HomeContent, E2E "Abrir con").
  Reglas de Compose: API `junit4.v2` (la v1 esta deprecada).
- `ScreenshotTourTest`: recorrido de capturas para revisar la UI a ojo; excluido de Gradle (`notClass`). Instrucciones en
  su KDoc. En imagenes ATD `adb screencap` sale negro: usar este recorrido (`captureToImage`).
- `./gradlew lintDebug assembleRelease`: lint (abortOnError) y release con R8.
- `./gradlew bundleRelease`: AAB para Play.

## Reglas para Claude Code en este proyecto
- Nivel semi-senior/senior, buenas practicas 2026, Material 3, SOLID, complejidad ciclomatica <= 10-15 por funcion (avisar si se supera).
- Comentarios en español explicando el **porque** de cada decision (el desarrollador los usa para aprender).
- Logica de negocio en use cases; los ViewModels solo orquestan. Nunca lanzar excepciones en `domain/` ni en ViewModels.
- Variante nueva de `DomainError` = su rama en `toUiText()` (el compilador lo exige).
- Tras cada cambio: compilar y correr `testDebugUnitTest`.
- Git, GitHub y CI/CD los gestiona el desarrollador: no crearlos por iniciativa propia.
