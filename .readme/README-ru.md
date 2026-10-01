<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-shell-terminal-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Многосеансовый терминал для AutoJs6 и его скриптов, запускающий системную оболочку в pty, с фоновыми сеансами, панелью клавиш и командами Node.js</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Языки

******

Текущий README.md поддерживает следующие языки:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ko.md)
- Русский [ru] # текущий
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ar.md)

******

### Введение

******

3-Shell Terminal берет на себя встроенный терминал AutoJs6: переключатель "Терминал" в боковом меню, пункт "Открыть в терминале" в меню папок файлового менеджера и на панели проекта, а также глобальный объект `terminal` на стороне скрипта для открытия, управления и наблюдения за сеансами. Каждый сеанс - это системная оболочка (`/system/bin/sh`) в pty, которая продолжает работать в фоне после выхода с экрана.

AutoJs6 обнаруживает плагин через его Binder-сервис, открывает экран терминала явным Intent и через Binder читает число сеансов, закрывает все сеансы или управляет сеансами скриптов; вывод сеанса возвращается скриптам по каналу. Если установлен плагин Node.js Runtime, терминал напрямую читает его контракт манифеста, проверяет подпись и загрузчик и предоставляет node / npm / npx / corepack / yarn / pnpm.

******

### Состояние

******

Предварительная версия P2: реализованы сеансы shell, доступ к хранилищу, интеграция Node.js с проверкой подписей и управление сеансами из хоста. Экран терминала, API скриптов и настройки будут добавлены по этапам ROADMAP.md. AutoJs6 6.8.0 (build 5304+).

******

### Возможности

******

Плагин предоставляет следующие возможности:

- Несколько сеансов: создание, переключение, закрытие и менеджер сеансов; служба переднего плана сохраняет сеансы после выхода с экрана, а ее уведомление показывает текущую папку и число сеансов с действием "Закрыть сеансы".
- Экран терминала: двухрядная панель клавиш (Esc / Tab / Ctrl / стрелки / частые символы), системное выделение текста с Копировать / Выделить все, копирование и отправка расшифровки, размер текста, вставка и очистка.
- Инструменты Node.js: при установленном плагине Node.js Runtime (1.5.0+) доступны node / npm / npx / corepack / yarn / pnpm, настройки реестра npm и "игнорировать скрипты установки", а также меню пакетов (npm init / install / run script и другое).
- Точки входа AutoJs6: переключатель бокового меню (число сеансов, закрыть все), "Открыть в терминале" в меню папок файлового менеджера и на панели проекта.
- API скриптов `terminal` (псевдоним `$terminal`): управление сеансами, видимое выполнение (`exec`, `npm.run`) и объект сеанса с событиями `output` / `exit`, `write` и `waitFor`; каждая ошибка - `TerminalError` со стабильным `code`.
- Отдельное приложение: значок запуска сразу открывает терминал; страница настроек (оформление вслед за AutoJs6, размер текста, реестр npm, интеграция Node.js, доступ ко всем файлам, очистка данных терминала), О программе и история выпусков.

******

### Использование

******

1. Установите APK плагина, соответствующий ABI устройства (или универсальный APK), из [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) на устройство с AutoJs6 сборки 5304 (6.8.0) или новее.
2. Откройте центр плагинов AutoJs6, убедитесь, что `3-Shell Terminal` распознан, и включите его.
3. Включите "Терминал" в боковом меню AutoJs6, выберите "Открыть в терминале" для папки в файловом менеджере или вызовите `terminal.open(...)` из скрипта. Предоставьте "Доступ ко всем файлам", когда плагин попросит, чтобы входить в папки общего хранилища, например `/sdcard`.

******

### Команды Node.js

******

Как терминал получает node / npm и какие есть ограничения:

- Требуется плагин Node.js Runtime 1.5.0 или новее; плагин читает его контракт манифеста, проверяет подпись, загрузчик и архив npm / corepack и при каждом запуске сеанса связывает команды в `PATH`. Без плагина или при неудачной проверке терминал остается рабочим, но без этих команд.
- Android запрещает исполнять файлы, записанные приложениями: `node_modules/.bin/*` и нативные исполняемые файлы из пакетов npm завершаются с `EACCES`; используйте `node <входной файл>` или `npx`. Нативные дополнения (`.node`) не загружаются.
- corepack по умолчанию использует встроенные pnpm 11.x и Yarn 1.x (`COREPACK_DEFAULT_TO_LATEST=0`) и скачивает явно указанную версию по запросу; реестр npm можно переключить на npmmirror или свой https-адрес в настройках.

******

### Быстрый старт

******

Скрипт, который открывает папку скрипта, видимо устанавливает зависимости и ждет результата, а также управляет интерактивной командой (доступно с этапа P4):

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

### Совместимость

******

Факты платформы, определяющие возможности плагина:

- Android 7.0 (API 24) и новее; APK для arm64-v8a, armeabi-v7a, x86_64 и x86 плюс универсальный APK, нативные библиотеки выровнены по страницам 16 КБ; сборка хоста и плагин проверяются вместе на матрице устройств из [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md).
- Процессы терминала работают под собственным uid и разрешениями плагина и не наследуют разрешения AutoJs6; для команд, которым нужны разрешения AutoJs6, используйте API скриптов `shell()`.
- Сеансы живут, пока жив процесс плагина; после его завершения системой они не восстанавливаются, что служба переднего плана и ее уведомление делают маловероятным.

******

### Частые вопросы

******

- **Почему `cd /sdcard/Scripts` не работает?** Плагину нужно собственное разрешение на хранилище. Откройте настройки плагина или следуйте баннеру терминала, чтобы предоставить "Доступ ко всем файлам" (Android 11+) или разрешение на хранилище в старых системах.
- **Почему нет команды node?** Установите плагин Node.js Runtime (1.5.0+) из центра плагинов AutoJs6; "проверка окружения" в настройках плагина показывает точную причину (отсутствует, слишком старый, недоверенная подпись или неисполняемый загрузчик).
- **Продолжает ли команда работать после выхода из терминала?** Да. Служба переднего плана сохраняет сеанс, а ее уведомление показывает число сеансов; оболочку завершают только "Закрыть сеансы" в уведомлении, переключатель бокового меню или сам сеанс.

******

### Разрешения и безопасность

******

Плагин соблюдает явные границы:

- Binder-сервис и вход на экран защищены разрешением подписи `org.autojs.permission.PLUGIN` и проверяют подпись вызывающего, поэтому доступ есть только у AutoJs6; вход с ярлыка лишь открывает терминал и не принимает внешних команд.
- Разрешение на хранилище ("Доступ ко всем файлам" на Android 11+) используется только для входа в выбранные вами папки; терминал никогда не сканирует и не отправляет файлы.
- Разрешение `INTERNET` используется командами, которые вы запускаете в оболочке (например `npm install`), и ручной проверкой выпусков через фиксированный GitHub Releases API этого плагина; сам плагин никогда не выходит в сеть в фоне.
- Загрузчик плагина Node.js Runtime исполняется только при официальной подписи (или совпадающей с этим плагином); плагин не записывает ввод и вывод сеансов и исключает свое приватное хранилище из резервных копий.

Получайте плагин только со страницы официальных [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) или из центра плагинов AutoJs6. Пакеты из неизвестных источников могут не пройти проверку хоста или нести риски, даже если номер версии выглядит одинаково.

******

### Интерфейс плагина

******

Следующая информация предназначена разработчикам хоста AutoJs6 и плагинов; хост использует эти идентификаторы для обнаружения плагина и согласования совместимости:

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

`ThreeShellTerminalPluginService` отвечает на `org.autojs.plugin.TERMINAL` (category `terminal`) и с этапа P2 реализует контракт хоста terminal-api `org.autojs.plugin.terminal.api.ITerminalPlugin`. `ThreeShellTerminalPluginInfoService` отвечает на `org.autojs.plugin.INFO` объектом PluginInfo. `WakeActivity` позволяет хосту активировать плагин; экран терминала открывается через `org.autojs.plugin.TERMINAL_OPEN`.

******

### Дорожная карта

******

Планы и прогресс плагина ведутся в виде списка с отметками в ROADMAP.md, организованного по этапам с критериями приемки и уровнями доказательств. Неотмеченные пункты выражают намерение, а не текущие возможности; обсуждение через Issues приветствуется.

- [Открыть ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md)

******

### История выпусков

******

#### v1.0.0

_2026/10/02_

- `Подсказка` Предварительная версия P2: реализованы сеансы shell, доступ к хранилищу, интеграция Node.js с проверкой подписей и управление сеансами из хоста. Экран терминала, API скриптов и настройки будут добавлены по этапам ROADMAP.md.
- `Функция` Идентичность плагина `three-shell-terminal` (engine `terminal`) с сервисом INFO, Wake Activity и скелетом сервиса `org.autojs.plugin.TERMINAL` для обнаружения хостом
- `Функция` APK по ABI (arm64-v8a, armeabi-v7a, x86_64, x86) плюс универсальный APK, нативные библиотеки выровнены по страницам 16 КБ
- `Функция` README, описание для центра плагинов и журнал изменений на 10 языках
- `Функция` Ядро сеансов перенесено из терминала хоста: сеансы оболочки на pty с реестром на уровне процесса (заголовок и код выхода записываются для Binder), окружение сеанса и структура каталогов внутри собственного каталога файлов плагина, обнаружение лаунчера Node.js с установщиком npm / corepack, а также служба переднего плана, поддерживающая сеансы, с уведомлением "Закрыть сеансы" (канал `three.shell.terminal.sessions`)
- `Функция` Определение доступа к хранилищу (`StorageAccess`): собственное состояние разрешений плагина (устаревшие разрешения времени выполнения ниже API 30, "Доступ ко всем файлам" начиная с API 30), распознавание общего хранилища для `/sdcard`, `/storage/...` и собственных папок `Android/{data,obb,media}`, откат начального каталога к `$HOME` с `STORAGE_PERMISSION_REQUIRED` или `DIRECTORY_INACCESSIBLE`, а также Intent настроек, открывающие доступ ко всем файлам
- `Функция` Интеграция Node.js с доверием к подписи (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): плагин Node.js Runtime используется только если он подписан официальным ключом плагинов AutoJs6 или собственным ключом этого плагина, переключатель в настройках срабатывает раньше любого поиска, каждый результат отображается на состояния `node-cli` контракта (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`), а каждый запуск сеанса обновляет ссылки команд в `usr/bin`, распаковывает архив npm / corepack один раз на дайджест и экспортирует окружение npm / corepack
- `Функция` AutoJs6 может создавать и управлять до 16 сеансами терминала, получать вывод в реальном времени через 4 слушателя на сеанс, читать недавний вывод и запрашивать окружение shell. Закрытие AutoJs6 не завершает сеансы; неверные запросы отклоняются с указанием причины.
- `Исправление` Системное ограничение фоновой активности больше не вызывает сбой плагина при запуске сеанса. Сеанс продолжает работу без защиты службы переднего плана.
- `Зависимость` Добавлен jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) как эмуляция терминала и нативные библиотеки pty, хэш зафиксирован в `locks/vendored-aars.lock`
- `Зависимость` Добавлены `common-plugin-api.aar` и `nodejs-api.aar` (модули AutoJs6 `plugin-api/common-plugin-api` и `plugin-api/nodejs-api`, сборка хоста 6.8.0 / 5303, MPL 2.0) как общий контракт плагинов и контракт манифеста Node.js, хэши зафиксированы в `locks/host-api-aars.lock`
- `Зависимость` Добавлен `terminal-api.aar` (модуль AutoJs6 `plugin-api/terminal-api`, сборка хоста 6.8.0 / 5304, MPL 2.0) как контракт терминала V1 (`ITerminalPlugin` / `ITerminalCallback`, идентичность, пределы и коды ошибок); константы идентичности плагина теперь берутся из него, хэш зафиксирован в `locks/host-api-aars.lock`

##### Полная история выпусков

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/assets/doc/CHANGELOG-ru.md)

******

### Сборка и проверка

******

Этот раздел предназначен разработчикам, желающим собрать плагин из исходного кода; обычные пользователи могут просто установить готовый APK со страницы Releases.

Собрать отладочный APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Запустить модульные тесты JVM и собрать APK инструментальных тестов:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Собрать выпускной APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

Собрать выпускной артефакт и добавить версию и контрольную сумму CRC32 к имени файла:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Проверить, что источники многоязычной документации и сгенерированные артефакты синхронизированы (также проверяется в CI):

```powershell
py .python\generate_markdown.py --check
```

Для сборки требуются JDK 21 или новее и Android SDK 37; версии Gradle и плагинов централизованно управляются через `version.properties` и `io.github.supermonster003.autojs6-platform-versions`.

******

### Локализация и генерация документации

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

Языковые JSON-файлы в `.readme/` и `.changelog/` являются единственным источником README, инструкций центра плагинов и журнала изменений. Всегда редактируйте эти JSON-источники и перезапускайте `py .python/generate_markdown.py`; сгенерированные README, `plugin_instruction.md` и журнал изменений никогда не правятся вручную. Запустите `py .python/generate_markdown.py --check`, чтобы проверить все сгенерированные артефакты.

******

### Лицензия

******

Код проекта распространяется по лицензии [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE). Сторонние компоненты и их лицензии перечислены в [уведомлениях о сторонних компонентах](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md).

******

### Ссылки

******

- Проект AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Документация AutoJs6: https://docs.autojs6.com
- Документация модуля terminal: https://docs.autojs6.com/#/terminal
- Плагин Node.js Runtime: https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime
- jackpal Android-Terminal-Emulator (эмуляция терминала и нативные библиотеки pty, Apache-2.0): https://github.com/jackpal/Android-Terminal-Emulator
- Уведомления о сторонних компонентах: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md
