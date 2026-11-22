# TecnoLuna

Aplicación móvil Android para la **gestión y el seguimiento de solicitudes de clientes** de una empresa de servicios digitales.

- **Proyecto:** EvaluacionApps_Grupo10
- **Nombre de la app:** TecnoLuna
- **Tecnologías:** Kotlin · Jetpack Compose · Material 3 · MVVM · **Room** (persistencia local) · CameraX + ML Kit

## Integrantes

| N° | Nombre completo | Rol principal en el desarrollo |
|---|---|---|
| 1 | [Nombre y apellidos del integrante 1] | Datos, lógica de negocio y lectura de QR |
| 2 | [Nombre y apellidos del integrante 2] | Interfaz y navegación |

| Dato | Información |
|---|---|
| Asignatura | [Nombre de la asignatura] (DSY1105, sección 005D) |
| Profesor(a) | [Nombre del profesor o profesora] |
| Grupo | Grupo 10 |
| Institución | [Nombre de la institución] |

## Descripción del proyecto

TecnoLuna permite que los clientes de una empresa de servicios digitales (sitios web, hosting, correo, marketing) registren sus solicitudes y las sigan hasta su cierre, mientras el equipo de la empresa las clasifica, prioriza, asigna y resuelve. Cada cambio queda registrado en un historial con fecha, usuario y perfil. **Todos los datos se guardan en una base de datos Room** y siguen disponibles al cerrar y volver a abrir la app.

La app tiene **tres perfiles**, y cada uno ve y hace solo lo que le corresponde:

| Perfil | Qué puede hacer |
|---|---|
| **Cliente** | Crear solicitudes, editarlas o eliminarlas mientras estén «Recibidas», adjuntar evidencia, ver estado e historial, responder cuando se le pide información, confirmar la solución o pedir que se reabra, compartir el código por WhatsApp. |
| **Administrador** | Ver todas las solicitudes, buscar y filtrar, clasificar, definir prioridad, asignar responsable, cambiar estados, comentar, editar o eliminar solicitudes, consultar el panel de resumen. |
| **Técnico** | Ver solo las solicitudes asignadas, comenzar el trabajo, registrar avances con evidencia, pedir más información, marcar como resuelta. |

## Funcionalidades implementadas

**CRUD de solicitudes (con Room)**
- **Crear:** formulario con título, descripción, categoría, servicio relacionado y evidencia opcional. Genera un código único (por ejemplo `SOL-1008`), estado inicial «Recibida» y registra fecha y hora.
- **Leer:** lista con buscador y filtros (estado, prioridad, cliente, responsable, atrasadas) y pantalla de detalle con datos, evidencias, comentarios e historial. Las pantallas se actualizan solas cuando cambia la base de datos.
- **Actualizar:** edición de los datos de la solicitud (el cliente mientras esté «Recibida»; el administrador mientras no esté terminada), además de cambios de estado, prioridad, categoría y responsable.
- **Eliminar:** con ventana de confirmación (el cliente mientras esté «Recibida»; el administrador siempre). Se borran también sus comentarios, evidencias e historial, porque viven dentro de la propia solicitud.

**Persistencia con Room**
- **Dos tablas:** `usuarios` y `solicitudes`.
- `usuarios`: una sola tabla para los tres perfiles (columna `perfil`); empresa y teléfono solo se usan en los clientes.
- `solicitudes`: título, descripción, categoría, servicio, **evidencia** (nombre del archivo) y **comentario** (comentarios e historial de la solicitud, ver `Comentario.kt`), más los datos de control que necesitan los tres perfiles (código, cliente, prioridad, estado, responsable y fechas).
- Los datos iniciales se cargan solo la primera vez que se abre la app.

**Formularios y validaciones**
- Título obligatorio (mínimo 5 caracteres), descripción obligatoria (mínimo 20), categoría obligatoria.
- Evidencia solo PNG, JPG o PDF y de hasta 2 MB.
- Errores visibles bajo cada campo, controlados desde el ViewModel.

**Perfiles y reglas**
- Inicio de sesión con validación de campos y credenciales.
- Menú distinto según el perfil.
- Reglas centralizadas (`Reglas.kt`): quién puede ver, editar, eliminar y a qué estados puede pasar cada solicitud.
- Ocho estados con descripción en lenguaje simple, prioridades con plazo y detección de solicitudes atrasadas.

**Administración**
- Panel de resumen con totales, pendientes, atrasadas, conteo por estado y barras por prioridad y por responsable.
- Avisos dentro de la app con indicador de no leídos. Se calculan desde los cambios que otras personas hicieron en las solicitudes (no hay tabla de avisos).

**Recursos de la cámara**
- **Escáner de códigos QR:** botón «Escanear QR» en la esquina superior derecha de *Mi perfil*. Lee el código de una solicitud (o su enlace de seguimiento) con CameraX y ML Kit y muestra código, título, descripción, categoría y estado, con acceso al detalle completo.
- Solicitud del permiso de cámara, con opción de abrir los ajustes si fue rechazado.

**Diseño**
- Paleta en tonos morados, con modo claro y oscuro.
- Diseño adaptable: menú inferior en celulares y lateral en pantallas grandes; listas en una o varias columnas.
- Animaciones de estados, números y barras, e indicadores de carga.

## Estructura del proyecto

```
EvaluacionApps_Grupo10/
├── settings.gradle.kts        Módulos y repositorios de Gradle
├── build.gradle.kts           Plugins del proyecto
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml     Versiones de plugins y dependencias
│   └── wrapper/               Versión de Gradle (8.9)
├── app/
│   ├── build.gradle.kts       Configuración y dependencias de la app
│   └── src/main/              Código (java/), recursos (res/) y AndroidManifest.xml
└── codigos_qr_prueba/         Imágenes QR para probar el escáner
```

## Arquitectura

MVVM con separación real de responsabilidades: las pantallas solo dibujan y nunca acceden directamente a la base de datos.

```
app/src/main/java/com/example/tecnoluna/
├── model/            Estados de pantalla y sus errores (SolicitudUiState + SolicitudErrores,
│   │                 LoginUiState + LoginErrores), enums (Perfil, Estado, Prioridad, Categoria,
│   │                 ResultadoQr), Comentario, Aviso, Filtros, Resumen, QrResult, Adjuntos y Reglas
│   └── data/local/   Las 2 tablas de Room (Usuario, Solicitud), sus DAO (UsuarioDao, SolicitudDao)
│                     y AppDatabase
├── repository/   SolicitudRepository, QrRepository
├── viewmodel/    AuthViewModel, SolicitudViewModel, NuevaSolicitudViewModel, QrViewModel
├── navigation/   Routes, AppNavigation
├── utils/        QrScanner (CameraX + ML Kit), CameraPermissionHelper
├── ui/
│   ├── theme/        Color, Theme (paleta morada)
│   ├── components/   Componentes reutilizables
│   ├── screen/       Splash, Login, Main, Lista, Detalle, Formulario, Resumen, Avisos,
│   │                 Perfil, Escáner QR
│   └── utils/        Tamaño de pantalla y formato de fechas
└── MainActivity.kt
```

Flujo de datos: `Pantalla → ViewModel → Repository → DAO → Room`. Cada pantalla tiene su `UiState` (lo que se está escribiendo) y sus `Errores`, separados de las tablas. Los DAO devuelven `Flow`, los ViewModels los convierten en `StateFlow` y las pantallas los observan con `collectAsState`.

## Requisitos

| Qué | Detalle |
|---|---|
| Android Studio | Ladybug (2024.2) o más reciente. Descarga: developer.android.com/studio |
| Android SDK | **Plataforma 35 (Android 15)**. Android Studio ofrece instalarla solo al abrir el proyecto. |
| JDK | El que trae Android Studio (JetBrains Runtime 17 o 21). No hay que instalar nada aparte. |
| Equipo | 8 GB de RAM recomendados y unos 5 GB libres (Gradle, SDK y emulador). |
| Internet | Necesaria solo la primera sincronización. |
| Cámara | Una webcam en el computador (o un celular real) para probar el escáner QR. |
| Dispositivo | Emulador o celular con **Android 8.0 (API 26) o superior**. |

### Plugins de Android Studio

No hay que instalar plugins adicionales. Solo comprueba en *File > Settings > Plugins > Installed* (en Mac: *Android Studio > Settings*) que estos vengan activados, porque por defecto lo están:

- **Android**
- **Kotlin**
- **Gradle**
- **Jetpack Compose** (vista previa y herramientas de Compose)

### Plugins de Gradle (ya configurados en el proyecto)

| Plugin | Versión | Para qué sirve |
|---|---|---|
| `com.android.application` (AGP) | 8.7.3 | Construir la app Android |
| `org.jetbrains.kotlin.android` | 2.0.21 | Compilar Kotlin |
| `org.jetbrains.kotlin.plugin.compose` | 2.0.21 | Compilador de Jetpack Compose |
| `com.google.devtools.ksp` | 2.0.21-1.0.28 | Genera el código de Room |
| Gradle (wrapper) | 8.9 | Se descarga solo |

### Dependencias (ya configuradas en `app/build.gradle.kts`)

| Dependencia | Versión | Para qué sirve |
|---|---|---|
| Compose BOM + `ui`, `material3`, `material3-window-size-class` | 2024.10.01 | Pantallas, diseño Material 3 y diseño adaptable |
| `androidx.core:core-ktx` | 1.15.0 | Utilidades de Android |
| `androidx.activity:activity-compose` | 1.9.3 | Actividad con Compose, botón atrás y permisos |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7 | ViewModels en Compose |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.8.7 | Ciclo de vida en Compose |
| `androidx.navigation:navigation-compose` | 2.8.5 | Navegación entre pantallas |
| `androidx.room:room-runtime`, `room-ktx`, `room-compiler` (con `ksp`) | 2.6.1 | Base de datos local |
| `androidx.camera:camera-core`, `camera2`, `lifecycle`, `view` | 1.4.2 | Cámara del escáner QR |
| `com.google.mlkit:barcode-scanning` | 17.3.0 | Lectura de códigos QR |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.9.0 | Corrutinas |

Todas las versiones están en `gradle/libs.versions.toml` y fueron elegidas para que sean compatibles entre sí. KSP debe empezar con la misma versión de Kotlin (aquí 2.0.21). Si más adelante actualizan una, deben actualizar también la otra.

## Pasos para ejecutar en Android Studio

1. **Obtener el proyecto.** Descomprime el zip (o clona el repositorio con `git clone [URL-del-repositorio]`). Debe quedar una carpeta `EvaluacionApps_Grupo10` que contiene `settings.gradle.kts` y la carpeta `app`.
2. **Abrir el proyecto.** En Android Studio elige *File > Open*, selecciona la carpeta `EvaluacionApps_Grupo10` (la que tiene `settings.gradle.kts`) y presiona *OK*. Si pregunta *Trust Project*, acepta.
3. **Esperar la sincronización de Gradle.** Aparece una barra de progreso abajo a la derecha. La primera vez descarga Gradle 8.9 y todas las librerías; puede tardar varios minutos. Termina cuando dice *BUILD SUCCESSFUL* o desaparece la barra.
4. **Instalar lo que falte.** Si aparece un aviso para instalar *Android SDK Platform 35* o *Build-Tools*, presiona *Install* y acepta las licencias (también se puede hacer en *Tools > SDK Manager*). Si Android Studio ofrece actualizar el Android Gradle Plugin, elige *Remind me later*: no hace falta para ejecutar.
5. **Crear el emulador.** En *Tools > Device Manager* presiona *Create Device*:
   - Dispositivo: *Medium Phone* (o *Pixel 7*).
   - Imagen del sistema: Android 13 (API 33) o superior; descárgala si hace falta.
   - Al terminar, edita el dispositivo (lápiz) > *Show Advanced Settings* > *Camera* y elige `Webcam0` en las cámaras frontal y trasera.
6. **Ejecutar.** Selecciona el emulador en la barra superior y presiona *Run* (▶). La app se instala con el nombre **TecnoLuna**.
7. **Iniciar sesión** con una de las cuentas de abajo y seguir el recorrido sugerido.

Para ejecutar en un **celular real**: activa *Opciones de desarrollador* y *Depuración USB*, conéctalo por cable, acepta la autorización en el celular y selecciónalo en la barra superior.

### Si algo falla

| Problema | Solución |
|---|---|
| *SDK location not found* | Android Studio lo crea solo al abrir el proyecto; si no, elige la ruta del SDK en *File > Project Structure > SDK Location*. |
| Error de versión de Java (*Unsupported class file* o *Java 17 required*) | *File > Settings > Build, Execution, Deployment > Build Tools > Gradle* y en *Gradle JDK* elige el *jbr* incluido en Android Studio. |
| *Could not resolve ...* o la sincronización no avanza | Revisa la conexión a internet y presiona *Sync Project with Gradle Files* (ícono del elefante). Si hay proxy, configúralo en Gradle. |
| Error de KSP o Room (*Unsupported Kotlin plugin version*, *Cannot find KSP*) | Verifica que `kotlin` y `ksp` en `libs.versions.toml` conserven el mismo prefijo (2.0.21). |
| La cámara del emulador se ve negra o sin imagen | Configura `Webcam0` como se indicó, borra los datos del emulador (*Cold Boot Now*) y vuelve a ejecutar. Con la cámara *Emulated* el escáner no lee QR. |
| La app se cierra al abrir la cámara | Acepta el permiso de cámara. Si lo rechazaste, actívalo en *Ajustes > Apps > TecnoLuna > Permisos*. |
| Después de cambiar una entidad de Room la app se cierra | Desinstala la app del emulador y vuelve a ejecutar (o sube la versión de `AppDatabase` y define una migración). |

### Cuentas de acceso (contraseña de todas: `Luna1234`)

| Perfil | Correo |
|---|---|
| Cliente | `camila@tecnoluna.cl` · `diego@tecnoluna.cl` |
| Administrador | `admin@tecnoluna.cl` |
| Técnico | `tomas@tecnoluna.cl` · `tania@tecnoluna.cl` |

En el login hay botones de «Ingreso rápido» que rellenan cada cuenta.

### Recorrido sugerido

1. **Cliente:** *Nueva solicitud* > completar > enviar. Se muestra el código de seguimiento.
2. **Cliente:** abrir la solicitud nueva y probar *Editar solicitud* (cambiar el título y guardar).
3. **Administrador:** asignarle categoría, prioridad y responsable.
4. **Técnico:** comenzar a trabajar, registrar un avance y marcarla como resuelta.
5. **Cliente:** confirmar la solución para cerrar la solicitud.
6. **Persistencia:** cerrar la app por completo y volver a abrirla; las solicitudes, comentarios e historial siguen ahí.
7. **Eliminar:** como cliente, crear otra solicitud y eliminarla desde su detalle (confirmación incluida).
8. **Cualquier perfil:** *Mi perfil > Escanear QR* y leer uno de los códigos de `codigos_qr_prueba/` (por ejemplo `QR_SOL-1001.png`, mostrado en otra pantalla frente a la cámara).

### Volver a los datos iniciales

Los datos viven en el dispositivo. Para empezar de cero, borra los datos de la app (*Ajustes > Apps > TecnoLuna > Almacenamiento > Borrar datos*) o desinstálala. `AppDatabase` está en la versión 2 con `fallbackToDestructiveMigration()`: si el celular tenía la base anterior de 7 tablas, se recrea sola. Si cambian una entidad en el futuro, suban la versión.

## Alcance y limitaciones

- «Compartir por WhatsApp» arma el mensaje con código, título, categoría y enlace de seguimiento y deja constancia en el historial, pero no abre WhatsApp.
- Los avisos se muestran dentro de la app; no usan notificaciones del sistema.
- No hay servidor: los datos viven solo en el dispositivo (Room).
- Los comentarios y el historial se guardan como texto dentro de la columna `comentario` de cada solicitud.
- «Marcar leídos» en Avisos se conserva solo mientras la app está abierta.
- La sesión no se conserva al cerrar la app: hay que iniciar sesión de nuevo.

## Documentación relacionada

- Informe técnico del proyecto, con capturas de cada hito.
- Guion de presentación del código.
- Tablero Trello con la distribución de tareas del equipo.
