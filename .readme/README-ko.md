<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-shell-terminal-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>AutoJs6 와 스크립트를 위한 다중 세션 터미널. pty 에서 시스템 셸을 실행하며 백그라운드 실행, 키 바, Node.js 명령을 지원</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 언어

******

현재 README.md는 다음 언어를 지원합니다:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ja.md)
- 한국어 [ko] # 현재
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ar.md)

******

### 소개

******

3-Shell Terminal 은 AutoJs6 의 내장 터미널을 이어받습니다: 홈 드로어의 "터미널" 스위치, 파일 관리자 디렉터리 메뉴와 프로젝트 도구 모음의 "터미널에서 열기", 그리고 세션을 열고 제어하고 관찰하는 스크립트 측 전역 객체 `terminal`. 각 세션은 pty 에서 실행되는 시스템 셸 (`/system/bin/sh`) 이며 화면을 떠나도 백그라운드에서 계속 실행됩니다.

AutoJs6 는 Binder 서비스로 플러그인을 발견하고, 명시적 Intent 로 터미널 화면을 열며, Binder 를 통해 세션 수 조회, 전체 세션 종료, 스크립트 세션 제어를 수행합니다. 세션 출력은 파이프로 스크립트에 전달됩니다. Node.js Runtime 플러그인이 설치되어 있으면 터미널이 그 매니페스트 계약을 직접 읽고 서명과 런처를 검증한 뒤 node / npm / npx / corepack / yarn / pnpm 을 제공합니다.

******

### 현재 상태

******

1.0.0은 독립 터미널, 백그라운드 세션, 대화형 스크립트 API와 설정을 제공합니다. 전체 스크립트 API는 AutoJs6 6.8.0 빌드 5315 이상이 필요하며, 기본 플러그인 프로토콜은 빌드 5304부터 지원합니다. Android 7.0 이상을 지원합니다.

******

### 기능

******

플러그인은 다음 기능을 제공합니다:

- 다중 세션: 생성, 전환, 종료와 세션 관리자. 화면을 떠난 뒤에도 포그라운드 서비스가 세션을 유지하며, 알림에 현재 디렉터리와 세션 수, "세션 종료" 동작을 표시합니다.
- 터미널 화면: 두 줄 키 바 (Esc / Tab / Ctrl / 방향키 / 자주 쓰는 기호), 네이티브 텍스트 선택과 복사 / 전체 선택, 기록 복사와 공유, 글자 크기, 붙여넣기와 지우기.
- Node.js 도구 체인: Node.js Runtime 플러그인 (1.5.0+) 을 설치하면 node / npm / npx / corepack / yarn / pnpm 을 사용할 수 있고, npm 레지스트리와 "설치 스크립트 무시" 설정, 패키지 메뉴 (npm init / install / run script 등) 도 제공됩니다.
- AutoJs6 진입점: 홈 드로어 스위치 (세션 수, 전체 종료), 파일 관리자 디렉터리 메뉴와 프로젝트 도구 모음의 "터미널에서 열기".
- 스크립트 API `terminal` (별칭 `$terminal`): 세션 관리, 보이는 실행 (`exec`, `npm.run`), `output` / `exit` 이벤트와 `write`, `waitFor` 를 갖춘 세션 객체. 모든 실패는 안정적인 `code` 를 가진 `TerminalError` 입니다.
- 독립 앱: 런처 아이콘이 바로 터미널을 열고, 설정 페이지 (AutoJs6 를 따르는 외관, 글자 크기, npm 레지스트리, Node.js 통합, 모든 파일 접근, 터미널 데이터 지우기), 정보와 릴리스 기록을 제공합니다.

******

### 사용 방법

******

1. AutoJs6 build 5304 (6.8.0) 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) 에서 기기 ABI 에 맞는 플러그인 APK (또는 universal APK) 를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `3-Shell Terminal` 이 인식되는지 확인하고 활성화합니다.
3. AutoJs6 홈 드로어에서 "터미널" 을 켜거나, 파일 관리자에서 디렉터리의 "터미널에서 열기" 를 선택하거나, 스크립트에서 `terminal.open(...)` 을 호출합니다. `/sdcard` 같은 공유 저장소의 디렉터리에 들어가려면 플러그인의 안내에 따라 "모든 파일 접근" 을 허용하세요.

******

### Node.js 명령

******

터미널이 node / npm 을 얻는 방법과 제한:

- Node.js Runtime 플러그인 1.5.0 이상이 필요합니다. 플러그인은 그 매니페스트 계약을 읽고 서명, 런처, npm / corepack 아카이브를 검증한 뒤 세션이 시작될 때마다 명령을 `PATH` 에 연결합니다. 플러그인이 없거나 검증에 실패해도 이 명령들을 제외하고 터미널은 계속 사용할 수 있습니다.
- Android는 앱이 작성한 파일의 실행을 금지합니다. npm은 기본적으로 bin 링크를 비활성화하므로 `node_modules/.bin/*`와 `npx <패키지>`로 패키지 진입점을 직접 실행할 수 없습니다. `node node_modules/<패키지>/<진입점>.js`를 사용하세요. npm 패키지의 네이티브 실행 파일은 `EACCES`로 실패하며, 네이티브 애드온 (`.node`)은 로드할 수 없습니다.
- corepack 은 기본적으로 내장된 pnpm 11.x 와 Yarn 1.x 를 사용하며 (`COREPACK_DEFAULT_TO_LATEST=0`), 명시적으로 지정한 버전은 요청 시 내려받습니다. npm 레지스트리는 설정에서 npmmirror 또는 사용자 지정 https URL 로 바꿀 수 있습니다.

******

### 빠른 시작

******

스크립트 디렉터리를 열고, 의존성을 보이게 설치하며 결과를 기다리고, 대화형 명령을 제어하는 스크립트 (로드맵 P4 부터 사용 가능):

```js
// Open the script directory in the terminal; the screen comes to the front and the session keeps running in the background.
let session = terminal.open(files.cwd());
console.log(session.id, terminal.sessions().length);

// Visible execution: install dependencies in a session the user can watch and wait for the exit code (0 = no timeout).
let install = terminal.exec('npm install', { cwd: '/sdcard/Scripts/my-project', keepOpen: false, wait: true, timeout: 0 });
toastLog('npm install exited with ' + install.exitCode);

// Drive an interactive command: output / exit events, write and waitFor; every failure is a TerminalError with a stable code.
let driven = terminal.exec('sleep 1; printf "name? "; read name; echo "received:$name"; sleep 1', {
    cwd: files.cwd(), show: true, keepOpen: false,
});
driven.on('output', line => { if (/name\?/.test(line)) driven.write('AutoJs6\n'); });
driven.on('exit', code => console.log('Session exited with ' + code));
console.log(driven.waitFor(/received:AutoJs6/, 15e3));
```

******

### 호환성

******

플러그인의 능력을 결정하는 플랫폼 사실:

- Android 7.0 (API 24) 이상. arm64-v8a, armeabi-v7a, x86_64, x86 용 APK 와 universal APK 를 제공하며 네이티브 라이브러리는 16 KB 페이지에 정렬되어 있습니다. 호스트 빌드와 플러그인은 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md) 의 기기 매트릭스에서 함께 검증됩니다.
- 터미널 프로세스는 플러그인 자체의 uid 와 권한으로 실행되며 AutoJs6 의 권한을 물려받지 않습니다. AutoJs6 권한이 필요한 명령에는 스크립트의 `shell()` API 를 사용하세요.
- 세션은 플러그인 프로세스가 살아 있는 동안만 존재합니다. 시스템이 프로세스를 종료하면 복원할 수 없지만, 포그라운드 서비스와 알림이 그럴 가능성을 낮춥니다.

******

### 자주 묻는 질문

******

- **`cd /sdcard/Scripts` 가 왜 실패하나요?** 플러그인에는 자체 저장소 권한이 필요합니다. 플러그인 설정을 열거나 터미널 배너의 안내에 따라 "모든 파일 접근" (Android 11+) 또는 이전 시스템에서는 저장소 권한을 허용하세요.
- **node 명령이 왜 없나요?** AutoJs6 플러그인 센터에서 Node.js Runtime 플러그인 (1.5.0+) 을 설치하세요. 플러그인 설정의 "환경 탐색" 이 정확한 이유 (미설치, 너무 오래됨, 신뢰할 수 없는 서명, 실행 불가 런처) 를 보여 줍니다.
- **시스템이 백그라운드 세션을 중지할 수 있나요?** 예. 플러그인 프로세스가 종료되면 세션을 복원할 수 없습니다. HyperOS, MIUI 등에서는 Android 설정에서 알림과 백그라운드 실행을 허용하세요. 터미널을 열면 허용되는 경우 포그라운드 보호를 다시 시도합니다. 보통 화면을 떠나도 세션은 유지되지만 포그라운드 서비스도 시스템 제한을 우회할 수 없습니다.

******

### 권한과 보안

******

플러그인은 명확한 경계를 따릅니다:

- Binder 서비스와 화면 진입점은 서명 권한 `org.autojs.permission.PLUGIN` 으로 보호되고 호출자 서명을 검증하므로 AutoJs6 만 접근할 수 있습니다. 런처 진입점은 터미널을 열 뿐 외부 명령을 받지 않습니다.
- 저장소 권한 (Android 11+ 에서는 "모든 파일 접근") 은 사용자가 선택한 디렉터리에 들어가는 데만 사용됩니다. 터미널은 파일을 검색하거나 업로드하지 않습니다.
- `INTERNET` 권한은 셸에서 실행하는 명령 (예: `npm install`) 과 이 플러그인의 고정된 GitHub Releases API 에 대한 수동 업데이트 확인에 사용됩니다. 플러그인 자체는 백그라운드에서 네트워크에 접속하지 않습니다.
- Node.js Runtime 플러그인의 런처는 그 서명이 공식 서명 (또는 이 플러그인과 동일) 일 때만 실행됩니다. 플러그인은 세션 입출력을 기록하지 않으며 개인 저장소를 백업에서 제외합니다.

플러그인은 공식 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) 페이지 또는 AutoJs6 플러그인 센터에서만 받으세요. 출처를 알 수 없는 패키지는 버전 번호가 같아 보여도 호스트 검증에 실패하거나 위험을 동반할 수 있습니다.

******

### 플러그인 인터페이스

******

다음 정보는 AutoJs6 호스트와 플러그인 개발자를 위한 것입니다. 호스트는 이 식별자로 플러그인을 발견하고 호환성을 협상합니다:

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

`ThreeShellTerminalPluginService` 는 `org.autojs.plugin.TERMINAL` (category `terminal`) 에 응답하며 로드맵 P2 부터 호스트 terminal-api 계약 `org.autojs.plugin.terminal.api.ITerminalPlugin` 를 구현합니다. `ThreeShellTerminalPluginInfoService` 는 `org.autojs.plugin.INFO` 에 PluginInfo 로 응답합니다. `WakeActivity` 는 호스트가 플러그인을 활성화하는 데 쓰이며, 터미널 화면은 `org.autojs.plugin.TERMINAL_OPEN` 으로 열립니다.

******

### 로드맵

******

플러그인의 계획과 진행 상황은 ROADMAP.md에 체크 가능한 목록으로 관리되며, 단계별로 수락 기준과 증거 수준이 함께 기록됩니다. 체크되지 않은 항목은 현재 기능이 아니라 의도를 나타냅니다. Issues를 통한 논의를 환영합니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.0.0

_2026/10/07_

- `힌트` 1.0.0은 독립 터미널, 백그라운드 세션, 대화형 스크립트 API와 설정을 제공합니다. 전체 스크립트 API는 AutoJs6 6.8.0 빌드 5315 이상이 필요하며, 기본 플러그인 프로토콜은 빌드 5304부터 지원합니다. Android 7.0 이상을 지원합니다
- `기능` 호스트 검색, 터미널 제어 및 보호된 UI/설정 진입점을 제공하는 독립 3-Shell Terminal 플러그인
- `기능` ABI 별 APK (arm64-v8a, armeabi-v7a, x86_64, x86) 와 universal APK, 16 KB 페이지에 정렬된 네이티브 라이브러리
- `기능` 10 개 언어의 README, 플러그인 센터 안내, 변경 기록
- `기능` 호스트 터미널에서 세션 코어 이식: pty 기반 셸 세션과 프로세스 전역 레지스트리 (Binder 용 제목과 종료 코드 기록), 플러그인 자체 파일 디렉터리 아래의 세션 환경과 디렉터리 구성, Node.js 런처 탐색과 npm / corepack 설치기, 세션을 유지하며 "세션 닫기" 알림을 제공하는 포그라운드 서비스 (채널 `three.shell.terminal.sessions`)
- `기능` 저장소 접근 판정 (`StorageAccess`): 플러그인 자체의 권한 상태로 판정 (API 30 미만은 기존 런타임 권한, API 30 부터는 "모든 파일 접근"), `/sdcard`, `/storage/...` 등 공유 저장소와 자체 `Android/{data,obb,media}` 폴더 식별, 시작 디렉터리의 `$HOME` 폴백과 `STORAGE_PERMISSION_REQUIRED` / `DIRECTORY_INACCESSIBLE` 사유, 모든 파일 접근 설정을 여는 Intent
- `기능` 서명자 신뢰를 갖춘 Node.js 연동 (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): Node.js Runtime 플러그인은 AutoJs6 공식 플러그인 키 또는 이 플러그인 자체 키로 서명된 경우에만 사용, 설정 스위치는 어떤 조회보다 먼저 단락, 모든 결과를 계약의 `node-cli` 상태 (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`) 에 대응, 세션 시작마다 `usr/bin` 명령 링크를 갱신하고 npm / corepack 아카이브를 다이제스트당 한 번만 추출하며 npm / corepack 환경 변수를 내보냄
- `기능` AutoJs6에서 최대 16개의 터미널 세션을 만들고 제어하며, 세션마다 최대 4개의 리스너로 실시간 출력을 받을 수 있습니다. 최근 출력과 shell 환경도 조회할 수 있습니다. AutoJs6를 닫아도 세션은 계속 실행되며, 잘못된 요청은 구체적인 이유와 함께 거부됩니다.
- `기능` npm init, 의존성/패키지 설치, package.json 스크립트 검색/실행 및 npm 검색; npmjs, npmmirror, 사용자 HTTPS 레지스트리와 ignore-scripts 설정. 데이터 삭제 시 세션을 닫고 전용 home/usr를 재구성하며 설정과 외부 프로젝트를 유지
- `기능` 터미널 화면 (`TerminalActivity`): 호스트의 터미널 UI를 플러그인 고유의 Material 3 테마로 이식. 키 바 (Esc / Tab / Ctrl / Alt / 방향 / 페이지 이동), 핀치로 글자 크기 조절, 길게 눌러 텍스트 선택 및 복사, 세션 / 텍스트 / 패키지 관리 / 설정 / 도움말 메뉴, 셸의 현재 디렉터리를 표시하고 탭하면 복사하는 툴바 부제목, Node.js Runtime이 없거나 신뢰되지 않거나 오래되었거나 비활성화된 이유를 설명하고 설치 / 업데이트 / 활성화 / 세부 정보 동작을 제공하는 Node.js 배너 포함. 공유 저장소 디렉터리에 들어갈 수 없으면 저장소 배너가 나타나 "허용"과 "디렉터리 다시 들어가기"를 제공. 화면은 호스트 설정 제공자를 통해 AutoJs6의 언어, 야간 모드, 테마 색상을 따르고 호스트가 없으면 시스템 값과 공용 `#FFDEAD` 색상으로 대체
- `기능` 세션 관리자: 접을 수 있는 상태 / 제어 / 세션 / 설정 섹션으로 구성된 대화상자로, 실행 중인 모든 세션을 디렉터리, PID, 실행 시간과 함께 나열하고, 개별 세션 열기 / 닫기, 새 세션 만들기, 모두 닫기, 세션 세부 정보 보기와 복사, 글자 크기, npm 레지스트리, ignore-scripts 설정을 제공합니다. 호스트의 `onSessionsChanged`와 같은 세션 레지스트리를 따르며 터미널 메뉴와 세션 알림 (탭)에서 열 수 있고, 호스트의 `manager=true` 진입은 투명한 `TerminalManagerActivity`가 맡아 닫아도 터미널 화면이 남지 않습니다
- `기능` 호스트 진입과 런처: `org.autojs.permission.PLUGIN` 서명 권한으로 보호되는 내보낸 `TERMINAL_OPEN` 진입 Activity 는 식별 가능한 호출자의 권한 보유와 플러그인과 같은 서명을 확인하고, `directory` / `sessionId` / `newSession` / `command` / `manager` extras 를 계약 상한에 맞춰 검증한 뒤 자체 태스크의 터미널 화면이나 호출자 위에 겹치는 세션 관리자로 전달합니다. `LauncherActivity` (아이콘 alias 의 대상) 는 최근 세션을 복원하거나 홈에서 새 세션을 시작합니다. 호스트에서 연 터미널에서 뒤로 가면 호스트로, 런처에서 연 경우 홈 화면으로 돌아가며 터미널 태스크는 최근 앱에서 사라져 시작 요청이 다시 실행되지 않습니다
- `기능` 런처 아이콘 선택: 비공개 런처 전달 Activity 위에 4개의 `MAIN / LAUNCHER` activity alias (적응형 밝게, 적응형 어둡게, 적응형 자동, 투명 배경) 를 두고 기본값으로 자동 아이콘을 활성화; 선택은 PackageManager 가 유일하게 활성화된 alias 로 보존하며 화면을 열 때마다와 패키지 업데이트 후 복구되고, 고정 또는 동적 바로가기는 선택한 alias 로 이동
- `기능` 정보, 버전 기록, 업데이트 확인: 설치된 버전, 빌드 번호와 날짜, 개발자, 플러그인 라이선스 (MPL 2.0) 를 표시하고 타사 구성 요소 섹션에서 번들된 jackpal Android-Terminal-Emulator 라이선스와 고지 (Apache-2.0), AutoJs6 플러그인 API 고지 (MPL 2.0), 라이브러리 고지를 오프라인으로 여는 정보 화면; WebView 없이 현재 언어로 번들된 변경 기록을 렌더링하는 문서 화면 (영어로 대체); HTTPS 로만 GitHub Releases API 에 질의하고 취소와 시간 초과를 지원하며 결과를 12시간 재사용하고 특정 버전을 무시할 수 있으며 출시 페이지 또는 내장 기록을 여는 수동 "업데이트 확인". 터미널 오버플로 메뉴에 "정보" 추가
- `기능` 독립 설정 화면을 세 그룹으로 제공: 모양 (언어, 야간 모드, 테마 색상, 런처 아이콘. 모두 기본적으로 AutoJs6 를 따르며 테마 색상은 16 개의 사전 설정과 HEX / rgb() 입력 지원), 터미널 (글자 크기, npm 레지스트리, ignore-scripts, Node.js 통합 스위치, 환경 진단 화면, 모든 파일 접근, 데이터 지우기), 정보 (업데이트 확인, 버전 기록, 정보). 선택 대화 상자는 적용 전에 확인하며 변경 사항은 즉시 적용. 터미널 오버플로 메뉴의 "설정" 은 이 화면을 바로 열고 (기존 빠른 설정 하위 메뉴 제거), AutoJs6 플러그인 센터의 "설정" 은 TERMINAL_SETTINGS 로 도달하며 플러그인은 이제 settings 기능을 선언
- `수정` 시스템이 백그라운드 활동을 제한해도 세션 시작 시 플러그인이 충돌하지 않습니다. 세션은 포그라운드 서비스 보호 없이 계속 실행됩니다.
- `수정` 긴 출력 기록은 최신 텍스트를 유지하면서 프로세스 간 응답 크기 제한을 넘지 않도록 합니다.
- `수정` 스크립트 출력 읽기 및 재생 시 화면을 채우는 끝부분의 빈 줄을 제거하고 프롬프트 공백과 후속 출력의 줄 경계를 유지
- `수정` 시작 중인 세션이 호스트 조회에서 일시적으로 사라져 화면에 표시되는 실행에서 터미널을 열 수 없던 문제
- `수정` 대량의 연속 출력이 터미널 메시지 큐를 차단하지 않으며, 시작 중 세션 종료와 자식 프로세스의 정상 종료 시 pty와 입출력 스레드를 해제
- `수정` 백그라운드 제한으로 포그라운드 서비스가 시작되지 않은 경우 기존 터미널을 열 때 보호 시작을 다시 시도
- `개선` 런처와 플러그인 센터 아이콘의 시각적 크기를 통일하고 투명 배경과 흑백 또는 중성 회색조 적용
- `개선` 플러그인 센터 아이콘에 Icon Studio에서 조정한 크기, 위치, 밝은 이미지와 어두운 이미지 및 원형 배경을 적용하고 재생성 가능한 원본과 매개변수를 유지
- `개선` Android 앱 정보 아이콘에 Icon Studio의 그림과 밝은 배경 및 어두운 배경을 사용하고 플러그인 센터의 투명 그림과 기존 런처 옵션을 유지
- `의존성` 터미널 에뮬레이션과 pty 네이티브 라이브러리로 jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42-p6.1, libtermexec 1.0, Apache-2.0) 추가, `locks/vendored-aars.lock` 에 해시 고정
- `의존성` 공유 플러그인 계약과 Node.js 매니페스트 계약으로 `common-plugin-api.aar` 와 `nodejs-api.aar` (AutoJs6 모듈 `plugin-api/common-plugin-api` 와 `plugin-api/nodejs-api`, 호스트 빌드 6.8.0 / 5303, MPL 2.0) 추가, `locks/host-api-aars.lock` 에 해시 고정
- `의존성` 터미널 계약 V1 (`ITerminalPlugin` / `ITerminalCallback`, 신원, 상한, 오류 코드) 로 `terminal-api.aar` (AutoJs6 모듈 `plugin-api/terminal-api`, 호스트 빌드 6.8.0 / 5304, MPL 2.0) 추가. 플러그인 신원 상수는 이제 여기서 가져오며 `locks/host-api-aars.lock` 에 해시 고정

##### 더 많은 릴리스 기록

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

******

### 빌드와 검증

******

이 섹션은 소스에서 플러그인을 빌드하려는 개발자를 위한 것입니다. 일반 사용자는 Releases 페이지의 미리 빌드된 APK를 설치하면 됩니다.

디버그 APK 빌드:

```powershell
.\gradlew.bat :app:assembleDebug
```

JVM 단위 테스트 실행 및 계측 테스트 APK 빌드:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

릴리스 APK 빌드:

```powershell
.\gradlew.bat :app:assembleRelease
```

릴리스 산출물을 수집하고 파일 이름에 버전과 CRC32 다이제스트를 추가:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

다국어 문서 소스와 생성된 산출물이 동기화되어 있는지 검증 (CI에서도 적용):

```powershell
py .python\generate_markdown.py --check
```

빌드에는 JDK 21 이상과 Android SDK 37이 필요합니다. Gradle과 플러그인 버전은 `version.properties`와 `io.github.supermonster003.autojs6-platform-versions`로 중앙에서 관리됩니다.

******

### 현지화와 문서 생성

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

`.readme/`와 `.changelog/`의 언어 JSON 파일이 README, 플러그인 센터 안내, 변경 기록의 유일한 소스입니다. 항상 이 JSON 소스를 편집하고 `py .python/generate_markdown.py`를 다시 실행하세요. 생성된 README, `plugin_instruction.md`, 변경 기록 산출물은 절대 손으로 편집하지 않습니다. `py .python/generate_markdown.py --check`를 실행하면 모든 생성 산출물을 검증할 수 있습니다.

******

### 라이선스

******

프로젝트 코드는 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE)에 따라 제공됩니다. 서드파티 구성 요소와 라이선스는 [서드파티 고지](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md)에 나열되어 있습니다.

******

### 링크

******

- AutoJs6 프로젝트: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 문서: https://docs.autojs6.com
- 터미널 모듈 문서: https://docs.autojs6.com/#/terminal
- Node.js Runtime 플러그인: https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime
- jackpal Android-Terminal-Emulator (터미널 에뮬레이션과 pty 네이티브 라이브러리, Apache-2.0): https://github.com/jackpal/Android-Terminal-Emulator
- 서드파티 고지: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md
