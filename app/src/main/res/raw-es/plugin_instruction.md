3-Shell Terminal asume el terminal integrado de AutoJs6: el interruptor "Terminal" del cajón de inicio, "Abrir en el terminal" en el menú de carpetas del gestor de archivos y en la barra de herramientas del proyecto, y el objeto global `terminal` del lado del script para abrir, controlar y observar sesiones. Cada sesión es un shell del sistema (`/system/bin/sh`) ejecutado en un pty que sigue en segundo plano al salir de la pantalla.

Vista previa de desarrollo P2: sesiones shell, acceso al almacenamiento, integración de Node.js con verificación de firmas y control de sesiones desde el anfitrión implementados. La pantalla de terminal, la API de scripts y los ajustes seguirán las etapas de ROADMAP.md. AutoJs6 6.8.0 (build 5304+).

### Uso

1. Instala el APK del plugin correspondiente a la ABI del dispositivo (o el APK universal) desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) en un dispositivo con AutoJs6 build 5304 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, confirma que `3-Shell Terminal` se reconoce y actívalo.
3. Activa "Terminal" en el cajón de inicio de AutoJs6, elige "Abrir en el terminal" sobre una carpeta del gestor de archivos o llama a `terminal.open(...)` desde un script. Concede "Acceso a todos los archivos" cuando el plugin lo pida para entrar en carpetas del almacenamiento compartido como `/sdcard`.

### Comandos Node.js

- Requiere el plugin Node.js Runtime 1.5.0 o posterior; el plugin lee su contrato de manifiesto, verifica la firma, el lanzador y el archivo npm / corepack, y enlaza los comandos en `PATH` cada vez que arranca una sesión. Sin el plugin, o si la verificación falla, el terminal sigue siendo utilizable sin esos comandos.
- Android se niega a ejecutar archivos escritos por las aplicaciones: `node_modules/.bin/*` y los ejecutables nativos de los paquetes npm fallan con `EACCES`; usa `node <archivo de entrada>` o `npx`. Los complementos nativos (`.node`) no se pueden cargar.
- corepack usa por defecto sus pnpm 11.x y Yarn 1.x integrados (`COREPACK_DEFAULT_TO_LATEST=0`) y descarga una versión nombrada explícitamente bajo petición; el registro npm puede cambiarse a npmmirror o a una URL https personalizada en los ajustes.

Consulta el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md) para la guía de instalación y el progreso actual.
