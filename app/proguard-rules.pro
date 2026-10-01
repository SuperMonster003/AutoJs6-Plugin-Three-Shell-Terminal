# PluginInfo (common-plugin-api) is annotated with @Parcelize; the annotation is source-retained and absent at run time.
-dontwarn kotlinx.parcelize.Parcelize

# Host discovery entry points are looked up by class name from the manifest.
-keep class io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPluginInfoService { *; }
-keep class io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPluginService { *; }
-keep class io.github.supermonster003.autojs6.plugin.three.shell.terminal.WakeActivity { *; }

# Host contract AARs: parcelables and AIDL stubs are resolved reflectively across processes.
-keep class org.autojs.plugin.common.api.** { *; }
-keep class org.autojs.plugin.nodejs.api.** { *; }

# jackpal Android-Terminal-Emulator: JNI_OnLoad of libjackpal-androidterm5.so registers Exec natives by name,
# and libjackpal-termexec2.so exports Java_jackpal_androidterm_TermExec_* symbols (roadmap D13 / AGENTS.md 9).
-keep class jackpal.androidterm.Exec { *; }
-keep class jackpal.androidterm.TermExec { *; }
-keep class jackpal.androidterm.libtermexec.v1.** { *; }
-keep class jackpal.androidterm.compat.FileCompat$Api8OrEarlier { *; }
-dontwarn jackpal.androidterm.**
