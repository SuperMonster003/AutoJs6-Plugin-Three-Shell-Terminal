3-Shell Terminal asume el terminal integrado de AutoJs6: el interruptor "Terminal" del cajón de inicio, "Abrir en el terminal" en el menú de carpetas del gestor de archivos y en la barra de herramientas del proyecto, y el objeto global `terminal` del lado del script para abrir, controlar y observar sesiones. Cada sesión es un shell del sistema (`/system/bin/sh`) ejecutado en un pty que sigue en segundo plano al salir de la pantalla.

Vista previa local P4: interfaz de terminal, sesiones múltiples y API de scripts con eventos de salida, entrada interactiva y espera del código de salida. Los ajustes independientes siguen en P5 de [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md). La API requiere una compilación de AutoJs6 que incluya P4.

### Uso

1. Instala el APK del plugin correspondiente a la ABI del dispositivo (o el APK universal) desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) en un dispositivo con AutoJs6 build 5304 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, confirma que `3-Shell Terminal` se reconoce y actívalo.
3. Activa "Terminal" en el cajón de inicio de AutoJs6, elige "Abrir en el terminal" sobre una carpeta del gestor de archivos o llama a `terminal.open(...)` desde un script. Concede "Acceso a todos los archivos" cuando el plugin lo pida para entrar en carpetas del almacenamiento compartido como `/sdcard`.

### Comandos Node.js

- Requiere el plugin Node.js Runtime 1.5.0 o posterior; el plugin lee su contrato de manifiesto, verifica la firma, el lanzador y el archivo npm / corepack, y enlaza los comandos en `PATH` cada vez que arranca una sesión. Sin el plugin, o si la verificación falla, el terminal sigue siendo utilizable sin esos comandos.
- Android impide ejecutar archivos escritos por la aplicación. npm desactiva los enlaces bin de forma predeterminada, por lo que `node_modules/.bin/*` y `npx <paquete>` no pueden ejecutar directamente los puntos de entrada. Usa `node node_modules/<paquete>/<entrada>.js`. Los ejecutables nativos incluidos en paquetes npm fallan con `EACCES`, y no se pueden cargar extensiones nativas (`.node`).
- corepack usa por defecto sus pnpm 11.x y Yarn 1.x integrados (`COREPACK_DEFAULT_TO_LATEST=0`) y descarga una versión nombrada explícitamente bajo petición; el registro npm puede cambiarse a npmmirror o a una URL https personalizada en los ajustes.

Consulta el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md) para la guía de instalación y el progreso actual.
