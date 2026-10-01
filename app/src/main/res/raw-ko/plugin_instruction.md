3-Shell Terminal 은 AutoJs6 의 내장 터미널을 이어받습니다: 홈 드로어의 "터미널" 스위치, 파일 관리자 디렉터리 메뉴와 프로젝트 도구 모음의 "터미널에서 열기", 그리고 세션을 열고 제어하고 관찰하는 스크립트 측 전역 객체 `terminal`. 각 세션은 pty 에서 실행되는 시스템 셸 (`/system/bin/sh`) 이며 화면을 떠나도 백그라운드에서 계속 실행됩니다.

버전 1.0.0 은 P0 개발 미리보기입니다: 저장소 뼈대, AutoJs6 플러그인 센터가 인식하는 플러그인 신원, 그리고 pty / 저장소 / Node.js 런처 스파이크. Binder 계약, 세션 코어, 터미널 화면, 스크립트 API, 설정 페이지는 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md) 의 단계에 따라 진행됩니다. AutoJs6 6.8.0 (build 5304) 이상이 필요합니다.

### 사용 방법

1. AutoJs6 build 5304 (6.8.0) 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) 에서 기기 ABI 에 맞는 플러그인 APK (또는 universal APK) 를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `3-Shell Terminal` 이 인식되는지 확인하고 활성화합니다.
3. AutoJs6 홈 드로어에서 "터미널" 을 켜거나, 파일 관리자에서 디렉터리의 "터미널에서 열기" 를 선택하거나, 스크립트에서 `terminal.open(...)` 을 호출합니다. `/sdcard` 같은 공유 저장소의 디렉터리에 들어가려면 플러그인의 안내에 따라 "모든 파일 접근" 을 허용하세요.

### Node.js 명령

- Node.js Runtime 플러그인 1.5.0 이상이 필요합니다. 플러그인은 그 매니페스트 계약을 읽고 서명, 런처, npm / corepack 아카이브를 검증한 뒤 세션이 시작될 때마다 명령을 `PATH` 에 연결합니다. 플러그인이 없거나 검증에 실패해도 이 명령들을 제외하고 터미널은 계속 사용할 수 있습니다.
- Android 는 앱이 기록한 파일의 실행을 거부합니다: `node_modules/.bin/*` 와 npm 패키지에 포함된 네이티브 실행 파일은 `EACCES` 로 실패하므로 `node <진입 파일>` 또는 `npx` 를 사용하세요. 네이티브 애드온 (`.node`) 은 로드할 수 없습니다.
- corepack 은 기본적으로 내장된 pnpm 11.x 와 Yarn 1.x 를 사용하며 (`COREPACK_DEFAULT_TO_LATEST=0`), 명시적으로 지정한 버전은 요청 시 내려받습니다. npm 레지스트리는 설정에서 npmmirror 또는 사용자 지정 https URL 로 바꿀 수 있습니다.

설치 안내와 현재 진행 상황은 [프로젝트 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal)와 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md)를 참고하세요.
