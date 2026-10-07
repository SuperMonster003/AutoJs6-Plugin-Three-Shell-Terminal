******

### Historial de versiones

******

# v1.0.0

###### 2026/10/07

* `Aviso` La versión 1.0.0 ofrece terminal independiente, sesiones en segundo plano, API de scripts interactivos y ajustes. Las API completas requieren AutoJs6 6.8.0 compilación 5315 o posterior; el protocolo básico requiere la compilación 5304. Compatible con Android 7.0 o posterior
* `Función` Varias sesiones: crear, cambiar, cerrar y un gestor de sesiones; un servicio en primer plano mantiene las sesiones tras salir de la pantalla y su notificación muestra la carpeta actual y el número de sesiones con la acción "Cerrar sesiones"
* `Función` Pantalla del terminal: barra de teclas de dos filas (Esc / Tab / Ctrl / flechas / símbolos habituales), selección de texto nativa con Copiar / Seleccionar todo, copia y compartición de la transcripción, tamaño del texto, pegar y limpiar
* `Función` Cadena Node.js: con el plugin Node.js Runtime (1.5.0+) instalado quedan disponibles node / npm / npx / corepack / yarn / pnpm, junto con los ajustes de registro npm e "ignorar scripts de instalación" y el menú de paquetes (npm init / install / run script y más)
* `Función` Entradas de AutoJs6: el interruptor del cajón de inicio (número de sesiones, cerrar todas), "Abrir en el terminal" en el menú de carpetas del gestor de archivos y en la barra de herramientas del proyecto
* `Función` API de script `terminal` (alias `$terminal`): gestión de sesiones, ejecución visible (`exec`, `npm.run`) y un objeto de sesión con eventos `output` / `exit`, `write` y `waitFor`; cada fallo es un `TerminalError` con un `code` estable
* `Función` Aplicación independiente: el icono del lanzador abre directamente el terminal; página de ajustes (apariencia que sigue a AutoJs6, tamaño del texto, registro npm, integración Node.js, acceso a todos los archivos, borrar datos del terminal), Acerca de e historial de versiones
* `Función` APK separados por ABI (arm64-v8a, armeabi-v7a, x86_64, x86) más un APK universal, con bibliotecas nativas alineadas a páginas de 16 KB
* `Función` README, instrucciones del centro de plugins y registro de cambios en 10 idiomas
* `Corrección` Las restricciones del sistema sobre la actividad en segundo plano ya no provocan un cierre del complemento al iniciar una sesión. La sesión continúa sin la protección del servicio en primer plano.
* `Corrección` La lectura de transcripciones largas conserva el texto más reciente sin superar el límite de tamaño de las respuestas entre procesos.
* `Corrección` La lectura y reproducción de la salida omiten las líneas vacías de relleno de pantalla y conservan los espacios del indicador y la separación de la salida siguiente
* `Corrección` Una sesión al iniciarse podía desaparecer brevemente de las consultas del anfitrión e impedir la apertura de su terminal
* `Corrección` La salida continua ya no bloquea la cola de mensajes del terminal; los cierres durante el inicio y la salida natural del proceso liberan los pty y los hilos de entrada y salida
* `Corrección` Abrir un terminal existente vuelve a intentar la protección del servicio en primer plano si las restricciones impidieron iniciarlo
* `Mejora` Tamaño visual uniforme de los iconos del lanzador y del Centro de complementos, con fondos transparentes y diseños en blanco, negro o grises neutros
* `Mejora` Los iconos del centro de plugins usan los tamaños, posiciones, imágenes claras y oscuras y fondos circulares ajustados en Icon Studio, conservando fuentes y parámetros reproducibles
* `Mejora` Los iconos de información de la aplicación de Android comparten las imágenes y los fondos claros y oscuros de Icon Studio, conservando las imágenes transparentes del centro de plugins y las opciones del lanzador
* `Dependencia` Se añade jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42-p6.1, libtermexec 1.0, Apache-2.0) como emulación de terminal y bibliotecas nativas pty, con hash bloqueado en `locks/vendored-aars.lock`
* `Dependencia` Se añaden `common-plugin-api.aar` y `nodejs-api.aar` (módulos AutoJs6 `plugin-api/common-plugin-api` y `plugin-api/nodejs-api`, build del host 6.8.0 / 5303, MPL 2.0) como contrato de plugin compartido y contrato de manifiesto Node.js, con hash bloqueado en `locks/host-api-aars.lock`
* `Dependencia` Se añade `terminal-api.aar` (módulo AutoJs6 `plugin-api/terminal-api`, build del host 6.8.0 / 5304, MPL 2.0) como contrato de terminal V1 (`ITerminalPlugin` / `ITerminalCallback`, identidad, límites y códigos de error); las constantes de identidad del plugin provienen ahora de él, con hash bloqueado en `locks/host-api-aars.lock`
