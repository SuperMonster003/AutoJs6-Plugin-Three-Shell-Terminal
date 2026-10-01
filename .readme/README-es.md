<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-shell-terminal-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Terminal multisesión para AutoJs6 y sus scripts, que ejecuta el shell del sistema en un pty con sesiones en segundo plano, barra de teclas y comandos Node.js</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Idiomas

******

El README.md actual admite los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ar.md)

******

### Introducción

******

3-Shell Terminal asume el terminal integrado de AutoJs6: el interruptor "Terminal" del cajón de inicio, "Abrir en el terminal" en el menú de carpetas del gestor de archivos y en la barra de herramientas del proyecto, y el objeto global `terminal` del lado del script para abrir, controlar y observar sesiones. Cada sesión es un shell del sistema (`/system/bin/sh`) ejecutado en un pty que sigue en segundo plano al salir de la pantalla.

AutoJs6 descubre el plugin mediante su servicio Binder, abre la pantalla del terminal con un Intent explícito y usa el Binder para leer el número de sesiones, cerrarlas todas o controlar sesiones de script; la salida vuelve a los scripts por una tubería. Con el plugin Node.js Runtime instalado, el terminal lee directamente su contrato de manifiesto, verifica la firma y el lanzador y ofrece node / npm / npx / corepack / yarn / pnpm.

******

### Estado

******

Vista previa de desarrollo P2: sesiones shell, acceso al almacenamiento, integración de Node.js con verificación de firmas y control de sesiones desde el anfitrión implementados. La pantalla de terminal, la API de scripts y los ajustes seguirán las etapas de [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md). AutoJs6 6.8.0 (build 5304+).

******

### Funciones

******

El complemento ofrece las siguientes capacidades:

- Varias sesiones: crear, cambiar, cerrar y un gestor de sesiones; un servicio en primer plano mantiene las sesiones tras salir de la pantalla y su notificación muestra la carpeta actual y el número de sesiones con la acción "Cerrar sesiones".
- Pantalla del terminal: barra de teclas de dos filas (Esc / Tab / Ctrl / flechas / símbolos habituales), selección de texto nativa con Copiar / Seleccionar todo, copia y compartición de la transcripción, tamaño del texto, pegar y limpiar.
- Cadena Node.js: con el plugin Node.js Runtime (1.5.0+) instalado quedan disponibles node / npm / npx / corepack / yarn / pnpm, junto con los ajustes de registro npm e "ignorar scripts de instalación" y el menú de paquetes (npm init / install / run script y más).
- Entradas de AutoJs6: el interruptor del cajón de inicio (número de sesiones, cerrar todas), "Abrir en el terminal" en el menú de carpetas del gestor de archivos y en la barra de herramientas del proyecto.
- API de script `terminal` (alias `$terminal`): gestión de sesiones, ejecución visible (`exec`, `npm.run`) y un objeto de sesión con eventos `output` / `exit`, `write` y `waitFor`; cada fallo es un `TerminalError` con un `code` estable.
- Aplicación independiente: el icono del lanzador abre directamente el terminal; página de ajustes (apariencia que sigue a AutoJs6, tamaño del texto, registro npm, integración Node.js, acceso a todos los archivos, borrar datos del terminal), Acerca de e historial de versiones.

******

### Uso

******

1. Instala el APK del plugin correspondiente a la ABI del dispositivo (o el APK universal) desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) en un dispositivo con AutoJs6 build 5304 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, confirma que `3-Shell Terminal` se reconoce y actívalo.
3. Activa "Terminal" en el cajón de inicio de AutoJs6, elige "Abrir en el terminal" sobre una carpeta del gestor de archivos o llama a `terminal.open(...)` desde un script. Concede "Acceso a todos los archivos" cuando el plugin lo pida para entrar en carpetas del almacenamiento compartido como `/sdcard`.

******

### Comandos Node.js

******

Cómo obtiene el terminal node / npm y cuáles son los límites:

- Requiere el plugin Node.js Runtime 1.5.0 o posterior; el plugin lee su contrato de manifiesto, verifica la firma, el lanzador y el archivo npm / corepack, y enlaza los comandos en `PATH` cada vez que arranca una sesión. Sin el plugin, o si la verificación falla, el terminal sigue siendo utilizable sin esos comandos.
- Android impide ejecutar archivos escritos por la aplicación. npm desactiva los enlaces bin de forma predeterminada, por lo que `node_modules/.bin/*` y `npx <paquete>` no pueden ejecutar directamente los puntos de entrada. Usa `node node_modules/<paquete>/<entrada>.js`. Los ejecutables nativos incluidos en paquetes npm fallan con `EACCES`, y no se pueden cargar extensiones nativas (`.node`).
- corepack usa por defecto sus pnpm 11.x y Yarn 1.x integrados (`COREPACK_DEFAULT_TO_LATEST=0`) y descarga una versión nombrada explícitamente bajo petición; el registro npm puede cambiarse a npmmirror o a una URL https personalizada en los ajustes.

******

### Inicio rapido

******

Un script que abre la carpeta del script, instala dependencias de forma visible esperando el resultado y controla un comando interactivo (disponible a partir de la fase P4):

```js
// Open the script directory in the terminal; the screen comes to the front and the session keeps running in the background.
let session = terminal.open(files.cwd());
console.log(session.id, terminal.sessions().length);

// Visible execution: install dependencies in a session the user can watch and wait for the exit code (0 = no timeout).
let install = terminal.exec('npm install', { cwd: '/sdcard/Scripts/my-project', wait: true, timeout: 0 });
toastLog('npm install exited with ' + install.exitCode);

// Drive an interactive command: output / exit events, write and waitFor; every failure is a TerminalError with a stable code.
let init = terminal.exec('npm init', { cwd: files.cwd(), show: true });
init.on('output', line => { if (/package name/i.test(line)) init.write('\n'); });
init.waitFor(/Is this OK\?/i, 60e3);
init.write('yes\n');
init.on('exit', code => console.log('npm init exited with ' + code));
terminal.npm.run('build', files.cwd());
```

******

### Compatibilidad

******

Hechos de la plataforma que delimitan lo que el plugin puede hacer:

- Android 7.0 (API 24) y posteriores; APK para arm64-v8a, armeabi-v7a, x86_64 y x86 más un APK universal, con bibliotecas nativas alineadas a páginas de 16 KB; la build del host y el plugin se verifican juntos en la matriz de dispositivos de [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md).
- Los procesos del terminal se ejecutan con el uid y los permisos del propio plugin y no heredan los de AutoJs6; usa la API de script `shell()` para los comandos que necesiten permisos de AutoJs6.
- Las sesiones viven mientras exista el proceso del plugin; cuando el sistema lo termina no pueden restaurarse, algo que el servicio en primer plano y su notificación hacen improbable.

******

### Preguntas frecuentes

******

- **Por qué falla `cd /sdcard/Scripts`?** El plugin necesita su propio permiso de almacenamiento. Abre los ajustes del plugin o sigue el aviso del terminal para conceder "Acceso a todos los archivos" (Android 11+), o el permiso de almacenamiento en sistemas anteriores.
- **Por qué no hay comando node?** Instala el plugin Node.js Runtime (1.5.0+) desde el centro de plugins de AutoJs6; la "sonda de entorno" de los ajustes del plugin muestra el motivo exacto (ausente, demasiado antiguo, firma no confiable o lanzador no ejecutable).
- **Sigue ejecutándose un comando al salir del terminal?** Sí. Un servicio en primer plano mantiene la sesión y su notificación muestra el número de sesiones; solo "Cerrar sesiones" en la notificación, el interruptor del cajón o la propia sesión terminan el shell.

******

### Permisos y seguridad

******

El plugin sigue límites explícitos:

- El servicio Binder y la entrada de pantalla están protegidos por el permiso de firma `org.autojs.permission.PLUGIN` y verifican la firma del llamante, de modo que solo AutoJs6 puede alcanzarlos; la entrada del lanzador solo abre el terminal y no acepta comandos externos.
- El permiso de almacenamiento ("Acceso a todos los archivos" en Android 11+) se usa únicamente para entrar en las carpetas que elijas; el terminal nunca escanea ni sube archivos.
- El permiso `INTERNET` lo usan los comandos que ejecutas en el shell (por ejemplo `npm install`) y la comprobación manual de versiones contra la API fija de GitHub Releases de este plugin; el plugin en sí nunca se conecta en segundo plano.
- El lanzador del plugin Node.js Runtime solo se ejecuta cuando su firma es la oficial (o coincide con la de este plugin); el plugin no registra la entrada ni la salida de las sesiones y excluye su almacenamiento privado de las copias de seguridad.

Obtenga el plugin únicamente desde la página oficial de [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) o el centro de plugins de AutoJs6. Los paquetes de origen desconocido pueden fallar la verificación del anfitrión o conllevar riesgos aunque el número de versión parezca idéntico.

******

### Interfaz del plugin

******

La siguiente información está dirigida a desarrolladores del anfitrión AutoJs6 y de plugins; el anfitrión usa estos identificadores para descubrir el plugin y negociar la compatibilidad:

```text
application id: io.github.supermonster003.autojs6.plugin.three.shell.terminal
plugin id: three-shell-terminal
engine: terminal
variant: default
service action: org.autojs.plugin.TERMINAL
service category: terminal
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.terminal.api.ITerminalPlugin
minimum host build: 5304 (6.8.0)
```

`ThreeShellTerminalPluginService` responde a `org.autojs.plugin.TERMINAL` (category `terminal`) e implementa el contrato terminal-api del host `org.autojs.plugin.terminal.api.ITerminalPlugin` a partir de la fase P2. `ThreeShellTerminalPluginInfoService` responde a `org.autojs.plugin.INFO` con PluginInfo. `WakeActivity` permite al host activar el plugin; la pantalla del terminal se abre mediante `org.autojs.plugin.TERMINAL_OPEN`.

******

### Hoja de ruta

******

Los planes y el progreso del plugin se mantienen como una lista verificable en ROADMAP.md, organizada por fases con criterios de aceptación y niveles de evidencia. Los elementos sin marcar expresan intención y no capacidades actuales; la discusión mediante Issues es bienvenida.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md)

******

### Historial de versiones

******

#### v1.0.0

_2026/10/02_

- `Aviso` Vista previa de desarrollo P2: sesiones shell, acceso al almacenamiento, integración de Node.js con verificación de firmas y control de sesiones desde el anfitrión implementados. La pantalla de terminal, la API de scripts y los ajustes seguirán las etapas de ROADMAP.md.
- `Función` Identidad del plugin `three-shell-terminal` (engine `terminal`) con el servicio INFO, la Wake Activity y el esqueleto del servicio `org.autojs.plugin.TERMINAL` para el descubrimiento por el host
- `Función` APK separados por ABI (arm64-v8a, armeabi-v7a, x86_64, x86) más un APK universal, con bibliotecas nativas alineadas a páginas de 16 KB
- `Función` README, instrucciones del centro de plugins y registro de cambios en 10 idiomas
- `Función` Núcleo de sesiones portado desde el terminal del host: sesiones de shell sobre pty con un registro a nivel de proceso (título y código de salida registrados para el Binder), entorno de sesión y estructura de directorios bajo el directorio de archivos del plugin, detección del lanzador de Node.js con el instalador de npm / corepack, y servicio en primer plano que mantiene las sesiones con una notificación "Cerrar sesiones" (canal `three.shell.terminal.sessions`)
- `Función` Resolución del acceso al almacenamiento (`StorageAccess`): estado de permisos propio del plugin (permisos en tiempo de ejecución heredados por debajo de API 30, "Acceso a todos los archivos" desde API 30), detección del almacenamiento compartido para `/sdcard`, `/storage/...` y las carpetas `Android/{data,obb,media}` propias, retorno del directorio inicial a `$HOME` con `STORAGE_PERMISSION_REQUIRED` o `DIRECTORY_INACCESSIBLE`, y los Intents de ajustes que abren el acceso a todos los archivos
- `Función` Integración de Node.js con confianza del firmante (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): el plugin Node.js Runtime solo se usa cuando está firmado por la clave oficial de plugins de AutoJs6 o por la clave propia de este plugin, el interruptor de ajustes cortocircuita antes de cualquier búsqueda, cada resultado se asigna a los estados `node-cli` del contrato (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`), y cada inicio de sesión renueva los enlaces de comandos de `usr/bin`, extrae el archivo npm / corepack una sola vez por resumen y exporta el entorno npm / corepack
- `Función` AutoJs6 puede crear y controlar hasta 16 sesiones de terminal, recibir salida en directo con hasta 4 oyentes por sesión, leer la salida reciente y consultar el entorno shell. Las sesiones continúan al cerrar AutoJs6; las solicitudes no válidas se rechazan con un motivo concreto.
- `Función` La gestión de paquetes admite npm init, instalación de dependencias y paquetes, listado y ejecución de scripts de package.json, comandos Yarn / pnpm y búsqueda en npm. Se puede elegir npmjs, npmmirror o una URL HTTPS personalizada e ignorar scripts de instalación. Borrar los datos cierra todas las sesiones antes de restablecer home / usr, conservando los ajustes y proyectos externos. Los menús y la interfaz de ajustes llegarán en etapas posteriores.
- `Corrección` Las restricciones del sistema sobre la actividad en segundo plano ya no provocan un cierre del complemento al iniciar una sesión. La sesión continúa sin la protección del servicio en primer plano.
- `Corrección` La lectura de transcripciones largas conserva el texto más reciente sin superar el límite de tamaño de las respuestas entre procesos.
- `Dependencia` Se añade jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) como emulación de terminal y bibliotecas nativas pty, con hash bloqueado en `locks/vendored-aars.lock`
- `Dependencia` Se añaden `common-plugin-api.aar` y `nodejs-api.aar` (módulos AutoJs6 `plugin-api/common-plugin-api` y `plugin-api/nodejs-api`, build del host 6.8.0 / 5303, MPL 2.0) como contrato de plugin compartido y contrato de manifiesto Node.js, con hash bloqueado en `locks/host-api-aars.lock`
- `Dependencia` Se añade `terminal-api.aar` (módulo AutoJs6 `plugin-api/terminal-api`, build del host 6.8.0 / 5304, MPL 2.0) como contrato de terminal V1 (`ITerminalPlugin` / `ITerminalCallback`, identidad, límites y códigos de error); las constantes de identidad del plugin provienen ahora de él, con hash bloqueado en `locks/host-api-aars.lock`

##### Para más historial de versiones

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

******

### Compilación y verificación

******

Esta sección está dirigida a desarrolladores que quieran compilar el plugin desde el código fuente; los usuarios normales pueden instalar simplemente el APK precompilado de la página Releases.

Compilar un APK de depuración:

```powershell
.\gradlew.bat :app:assembleDebug
```

Ejecutar las pruebas unitarias JVM y compilar el APK de pruebas de instrumentación:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Compilar el APK de release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Recopilar el artefacto de release y añadir la versión y el resumen CRC32 a su nombre de archivo:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Verificar que las fuentes de documentación multilingüe y los artefactos generados están sincronizados (también lo exige la CI):

```powershell
py .python\generate_markdown.py --check
```

La compilación requiere JDK 21 o posterior y Android SDK 37; las versiones de Gradle y de los plugins se gestionan de forma centralizada mediante `version.properties` e `io.github.supermonster003.autojs6-platform-versions`.

******

### Localización y generación de documentación

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

Los archivos JSON de idioma en `.readme/` y `.changelog/` son la única fuente del README, las instrucciones del centro de plugins y el registro de cambios. Edite siempre esas fuentes JSON y vuelva a ejecutar `py .python/generate_markdown.py`; los artefactos generados de README, `plugin_instruction.md` y registro de cambios nunca se editan a mano. Ejecute `py .python/generate_markdown.py --check` para verificar todos los artefactos generados.

******

### Licencia

******

El código del proyecto se distribuye bajo la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE). Los componentes de terceros y sus licencias se listan en los [Avisos de terceros](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md).

******

### Enlaces

******

- Proyecto AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentación de AutoJs6: https://docs.autojs6.com
- Documentación del módulo terminal: https://docs.autojs6.com/#/terminal
- Plugin Node.js Runtime: https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime
- jackpal Android-Terminal-Emulator (emulación de terminal y bibliotecas nativas pty, Apache-2.0): https://github.com/jackpal/Android-Terminal-Emulator
- Avisos de terceros: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md
