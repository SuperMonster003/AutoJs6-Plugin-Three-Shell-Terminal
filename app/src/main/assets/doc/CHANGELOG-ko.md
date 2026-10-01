******

### 릴리스 기록

******

# v1.0.0

###### 2026/10/01

* `힌트` P0 개발 미리보기: 저장소 뼈대, AutoJs6 플러그인 센터가 인식하는 플러그인 신원, pty / 저장소 / Node.js 런처 스파이크. Binder 계약, 세션 코어, 터미널 화면, 스크립트 API, 설정 페이지는 ROADMAP.md 의 단계에 따라 진행됩니다.
* `기능` 플러그인 신원 `three-shell-terminal` (engine `terminal`), INFO 서비스, Wake Activity, 호스트 발견용 `org.autojs.plugin.TERMINAL` 서비스 뼈대
* `기능` ABI 별 APK (arm64-v8a, armeabi-v7a, x86_64, x86) 와 universal APK, 16 KB 페이지에 정렬된 네이티브 라이브러리
* `기능` 10 개 언어의 README, 플러그인 센터 안내, 변경 기록
* `의존성` 터미널 에뮬레이션과 pty 네이티브 라이브러리로 jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) 추가, `locks/vendored-aars.lock` 에 해시 고정
* `의존성` 공유 플러그인 계약과 Node.js 매니페스트 계약으로 `common-plugin-api.aar` 와 `nodejs-api.aar` (AutoJs6 모듈 `plugin-api/common-plugin-api` 와 `plugin-api/nodejs-api`, 호스트 빌드 6.8.0 / 5303, MPL 2.0) 추가, `locks/host-api-aars.lock` 에 해시 고정
* `의존성` 터미널 계약 V1 (`ITerminalPlugin` / `ITerminalCallback`, 신원, 상한, 오류 코드) 로 `terminal-api.aar` (AutoJs6 모듈 `plugin-api/terminal-api`, 호스트 빌드 6.8.0 / 5304, MPL 2.0) 추가. 플러그인 신원 상수는 이제 여기서 가져오며 `locks/host-api-aars.lock` 에 해시 고정
