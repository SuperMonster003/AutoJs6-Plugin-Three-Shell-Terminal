# Terminal input and command editing

Version: 1.2.0 / 90. Date: 2026-11-15.

## Report and diagnosis

The maintainer pasted two multiline `su -c` commands containing quoted heredocs and Chinese
comments. Execution succeeded, but long input lines in the terminal transcript appeared to lose
their beginnings. Android mksh scrolls its interactive editing line horizontally, leaving `<`
markers. A screen transcript is consequently not a reliable copy of submitted shell source.

The investigation also found independent input defects:

- The vendored `TermSession.write(int)` subtracts one byte after UTF-8 encoding and reuses a
  two-character buffer without limiting its length. Supplementary code points lose their final
  byte and following BMP characters can encounter a stale surrogate. The plugin now overrides
  that entry point with `Character.toChars` and the existing UTF-8 string path.
- The locked libtermexec `process.cpp` implements `String8::set` by casting every UTF-16 code unit
  to `char`. Commands, directories and environment values were therefore not encoded as UTF-8.
  `PtyBridge` supplies UTF-8 bytes represented as Latin-1 code units at that specific JNI boundary.
  The pinned native binary is unchanged; do not keep this adapter if a future native replacement
  starts encoding Java strings itself.
- Bulk UI writes waited on the terminal's output queue while output processing also needed the
  main thread. A per-session writer now serializes UI and Binder input away from that thread,
  accepts or rejects each whole submission, bounds queued data to 256 KiB, and shuts down with
  the session. On API 24, a 57,290-byte paste additionally reproduced successful tty writes that
  lost bytes. Input now uses 128-byte bursts separated by 4 ms, allowing older tty readers to drain.

## User-visible behavior

- Multiline or longer-than-80-code-point menu/IME pastes open a wrapping command editor.
  A single IME Enter still goes to the terminal. Both IME commitText overloads are handled.
- Paste sends literal text to the existing terminal, normalizes CRLF and bypasses sticky Ctrl/Alt.
- Run starts a new local shell session in the selected session's current directory. It shows the
  complete submitted command, command output and exit code. Commands that explicitly exit/exec
  the shell instead use the existing process-exit display. The editor accepts up to 16 KiB.
- A new session does not inherit an interactive `su` login or temporary shell variables. A command
  that needs Root must include `su`, as in the maintainer's examples. This is stated in the dialog.
- View submitted command preserves exact source independently of terminal wrapping/trimming.
  Source and configuration-change drafts live only in memory; neither is written into saved state
  or a separate command log. Copy remains an explicit user action.
- The command editor scrolls when the IME reduces available space. Its actions remain accessible.
- HOME is canonicalized so the prompt abbreviates the private home directory as `~` despite the
  `/data/user/0` and `/data/data` aliases.

The ordinary interactive mksh editor still has its own constraints, including BMP-only Unicode
editing on the tested systems. The new command execution path bypasses that editor. Terminal
screen text also trims trailing whitespace and may join a hard newline at an exact column
boundary; View submitted command is the exact source, rather than a reconstruction from pixels.

## Validation

All device fixtures write only plugin-owned temporary files. Nested `sh -c` replaces `su -c` in
automated heredoc tests. No ART or boot scripts are changed, and automated KernelSU testing is
not claimed.

- JVM: 204 tests passed, including bounded ordered input, rejection and command validation.
- Android 13 / API 33 / x86_64: 32 tests passed across input integrity, command UI, session lifecycle,
  existing terminal Activity behavior and the Binder contract.
- Android 7 / API 24 / x86: 6 focused tests passed for keyboard Unicode transport, large literal
  paste, nested heredocs at 30 columns, Unicode cwd/env, menu/IME input and command UI recreation.
- Keyboard transport is compared byte for byte using `cat` as the terminal client, separately
  from mksh's restrictions. Chinese, supplementary ideographs, emoji and Latin text are mixed.
- The 700-line paste compares all 57,290 expected bytes. The command test compares file contents
  and actual PTY output, while the viewer test checks the exact retained source.
- Canceled pastes execute nothing. Editor runs create a separate session and preserve the previous
  session. The draft survives Activity recreation, and all three buttons remain visible with IME.
- Debug and signed release builds, lint and native 16 KiB alignment checks are run. Lint reports
  no errors; existing dependency, compatibility and resource warnings remain.
- All 10 README/changelog languages and all 11 string-resource directories are updated together.

Local screenshots and build/device logs are retained in `.bak/terminal-input-20261115/`.
