3-Shell Terminal es una terminal interactiva en su teléfono: ejecuta el `/system/bin/sh` del sistema en un pty, mantiene varias sesiones abiertas a la vez y las deja en ejecución en segundo plano cuando sale de la pantalla. Con el plugin Node.js Runtime instalado, también puede usar comandos como node y npm. La entrada "Terminal" de AutoJs6 y el objeto `terminal` de los scripts los proporciona este plugin.

### Primeros pasos

1. Toque "Terminal" en el cajón de la página principal de AutoJs6, o elija "Abrir en la terminal" en el menú de una carpeta del gestor de archivos; también puede tocar el icono de 3-Shell Terminal en la pantalla de inicio.
2. Escriba un comando y pulse Intro para ejecutarlo. La barra de teclas de la parte inferior de la pantalla ofrece Esc, Tab, Ctrl, las flechas y símbolos habituales.
3. Cree, cambie o cierre sesiones desde el menú "Sesiones" de la barra de herramientas. Las sesiones siguen en ejecución al salir de la pantalla; toque la notificación para abrir "Gestor del terminal".
4. Antes de entrar en carpetas del almacenamiento compartido como `/sdcard`, toque "Conceder" cuando la terminal se lo indique, o active "Acceso a todos los archivos" en los ajustes.

### Funciones principales

- **Varias sesiones**: cree, cambie y cierre sesiones; "Gestor del terminal" las muestra todas. Un servicio en primer plano mantiene las sesiones en segundo plano, y la notificación muestra la carpeta actual y el número de sesiones.
- **Pantalla de la terminal**: barra de teclas, selección y copia de texto, "Compartir transcripción" y zoom del texto con dos dedos; los ajustes ofrecen 4 esquemas de "Colores de la terminal" y 2 opciones de "Fuente de la terminal".
- **Editor de comandos**: al pegar un comando largo o de varias líneas, primero se muestra el texto completo; puede pegarlo en la sesión actual, o ejecutarlo en una sesión nueva y ver el código de salida.
- **Herramientas de Node.js**: con el plugin Node.js Runtime instalado, puede usar node, npm, npx y corepack (yarn / pnpm). En los ajustes puede elegir el "Registro de npm" o activar "Ignorar scripts de ciclo de vida de npm".
- **Notificaciones de fin de comando**: cuando termina un comando que un script ejecutó sin mantener un shell interactivo, se publica una notificación; tóquela para ver el final de la salida. Puede desactivar "Notificaciones de fin de comando" en los ajustes.
- **Ajustes propios**: el idioma, el modo nocturno y el color del tema siguen a AutoJs6 de forma predeterminada y también pueden ajustarse por separado; el icono del lanzador tiene 4 estilos. Cuando AutoJs6 lo admite, el menú de la página de ajustes ofrece "Abrir la configuración general".

### Uso en scripts

Los scripts usan el objeto global `terminal` (alias `$terminal`) para crear sesiones, ejecutar comandos, escribir entradas y escuchar la salida:

```js
if (!terminal.isAvailable()) exit();
let result = terminal.exec('uname -m', {
    show: false,
    keepOpen: false,
    wait: true,
});
console.log(result.exitCode);
let session = terminal.open({ show: false });
session.on('output', (text) => console.log(text));
session.write('ls\n');
session.show();
```

- De forma predeterminada, `exec` mantiene un shell interactivo después de que termina el comando; para esperar el código de salida, establezca a la vez `keepOpen: false` y `wait: true`.
- Los métodos síncronos bloquean el hilo del script y no se pueden llamar en el hilo de la interfaz; la mayoría de los métodos también tienen una forma `Async` que devuelve una Promise.
- Las sesiones siguen en ejecución después de que el script termina; llame a `session.close()` cuando ya no las necesite. Una operación fallida lanza un `TerminalError` con un `code`.
- Para todas las opciones y eventos, consulte [Documentación de AutoJs6 > Terminal](https://docs.autojs6.com/#/terminal).

### Requisitos

- Android 7.0 o posterior y AutoJs6 6.8.0 o posterior.
- Los comandos como node y npm necesitan el plugin [Node.js Runtime](https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime) 1.5.0 o posterior.
- Entrar en carpetas del almacenamiento compartido requiere "Acceso a todos los archivos" (Android 11 y posteriores) o el permiso de almacenamiento; el permiso de notificaciones se usa para las notificaciones de sesión y de fin de comando.
- Los comandos se ejecutan con la identidad y los permisos propios de este plugin, no con los de AutoJs6; para los comandos que necesitan los permisos de AutoJs6, use `shell()` en un script.

### Preguntas frecuentes

**No se puede entrar en carpetas de `/sdcard`**

Este plugin necesita su propio permiso de almacenamiento. Toque "Conceder" cuando la terminal se lo indique, o active "Acceso a todos los archivos" en los ajustes; en Android 10 y anteriores basta con conceder el permiso de almacenamiento. Una dirección `content://` no es una ruta de archivo y no se puede entrar en ella.

**No se encuentra el comando node**

Compruebe que el plugin Node.js Runtime está instalado y que "Integración de Node.js" está activada en los ajustes, y luego abra "Comprobación del lanzador de Node.js" para ver el motivo. Si el plugin no tiene la firma oficial, esa página muestra el SHA-256 de su firma; una vez que confirme que confía en él, las sesiones nuevas podrán usarlo.

**El sistema cierra las sesiones en segundo plano**

Cuando el sistema cierra el proceso del plugin, sus sesiones no se pueden recuperar. Sistemas como HyperOS y MIUI pueden restringir la actividad en segundo plano; en los ajustes del sistema, permita que 3-Shell Terminal envíe notificaciones y se ejecute en segundo plano.

Las teclas, la interfaz de scripts y los permisos se detallan en la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal).
