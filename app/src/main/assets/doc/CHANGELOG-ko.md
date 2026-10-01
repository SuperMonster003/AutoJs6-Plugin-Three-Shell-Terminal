******

### 릴리스 기록

******

# v1.0.0

###### 2026/10/02

* `힌트` P2 개발 미리보기: shell 세션, 저장소 접근, 서명을 검증하는 Node.js 통합, 호스트 세션 제어가 구현되었습니다. 터미널 화면, 스크립트 API, 설정 페이지는 ROADMAP.md의 단계에 따라 구현됩니다.
* `기능` 플러그인 신원 `three-shell-terminal` (engine `terminal`), INFO 서비스, Wake Activity, 호스트 발견용 `org.autojs.plugin.TERMINAL` 서비스 뼈대
* `기능` ABI 별 APK (arm64-v8a, armeabi-v7a, x86_64, x86) 와 universal APK, 16 KB 페이지에 정렬된 네이티브 라이브러리
* `기능` 10 개 언어의 README, 플러그인 센터 안내, 변경 기록
* `기능` 호스트 터미널에서 세션 코어 이식: pty 기반 셸 세션과 프로세스 전역 레지스트리 (Binder 용 제목과 종료 코드 기록), 플러그인 자체 파일 디렉터리 아래의 세션 환경과 디렉터리 구성, Node.js 런처 탐색과 npm / corepack 설치기, 세션을 유지하며 "세션 닫기" 알림을 제공하는 포그라운드 서비스 (채널 `three.shell.terminal.sessions`)
* `기능` 저장소 접근 판정 (`StorageAccess`): 플러그인 자체의 권한 상태로 판정 (API 30 미만은 기존 런타임 권한, API 30 부터는 "모든 파일 접근"), `/sdcard`, `/storage/...` 등 공유 저장소와 자체 `Android/{data,obb,media}` 폴더 식별, 시작 디렉터리의 `$HOME` 폴백과 `STORAGE_PERMISSION_REQUIRED` / `DIRECTORY_INACCESSIBLE` 사유, 모든 파일 접근 설정을 여는 Intent
* `기능` 서명자 신뢰를 갖춘 Node.js 연동 (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): Node.js Runtime 플러그인은 AutoJs6 공식 플러그인 키 또는 이 플러그인 자체 키로 서명된 경우에만 사용, 설정 스위치는 어떤 조회보다 먼저 단락, 모든 결과를 계약의 `node-cli` 상태 (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`) 에 대응, 세션 시작마다 `usr/bin` 명령 링크를 갱신하고 npm / corepack 아카이브를 다이제스트당 한 번만 추출하며 npm / corepack 환경 변수를 내보냄
* `기능` AutoJs6에서 최대 16개의 터미널 세션을 만들고 제어하며, 세션마다 최대 4개의 리스너로 실시간 출력을 받을 수 있습니다. 최근 출력과 shell 환경도 조회할 수 있습니다. AutoJs6를 닫아도 세션은 계속 실행되며, 잘못된 요청은 구체적인 이유와 함께 거부됩니다.
* `수정` 시스템이 백그라운드 활동을 제한해도 세션 시작 시 플러그인이 충돌하지 않습니다. 세션은 포그라운드 서비스 보호 없이 계속 실행됩니다.
* `의존성` 터미널 에뮬레이션과 pty 네이티브 라이브러리로 jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) 추가, `locks/vendored-aars.lock` 에 해시 고정
* `의존성` 공유 플러그인 계약과 Node.js 매니페스트 계약으로 `common-plugin-api.aar` 와 `nodejs-api.aar` (AutoJs6 모듈 `plugin-api/common-plugin-api` 와 `plugin-api/nodejs-api`, 호스트 빌드 6.8.0 / 5303, MPL 2.0) 추가, `locks/host-api-aars.lock` 에 해시 고정
* `의존성` 터미널 계약 V1 (`ITerminalPlugin` / `ITerminalCallback`, 신원, 상한, 오류 코드) 로 `terminal-api.aar` (AutoJs6 모듈 `plugin-api/terminal-api`, 호스트 빌드 6.8.0 / 5304, MPL 2.0) 추가. 플러그인 신원 상수는 이제 여기서 가져오며 `locks/host-api-aars.lock` 에 해시 고정
