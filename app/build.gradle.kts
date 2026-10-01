import com.android.apksig.ApkVerifier
import com.android.build.api.variant.FilterConfiguration
import org.autojs.build.alignment.VerifyNativePageAlignment
import org.gradle.api.provider.Property
import java.security.MessageDigest
import java.util.Properties
import java.util.zip.ZipFile

plugins {
    id("io.github.supermonster003.autojs6-native-alignment")
    id("org.autojs.build.utils")
    id("org.autojs.build.versions")
    id("org.autojs.build.signs")
    id("org.autojs.build.jvm-convention")
    id("com.android.application")
}

val globalApplicationId = "io.github.supermonster003.autojs6.plugin.three.shell.terminal"
val buildTypeDebug = "debug"
val buildTypeRelease = "release"

// Native ABIs shipped by the vendored jackpal libtermexec AAR (roadmap D14). x86 stays so the API 24 x86
// CI emulator can load the pty bridge; universal carries all four.
val nativeAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
val nativeLibraryNames = listOf("libjackpal-androidterm5.so", "libjackpal-termexec2.so")

// ---------------------------------------------------------------------------
// Hash-locked AAR inputs. Host protocol AARs live in libs/ (locks/host-api-aars.lock); the jackpal
// terminal AARs live in libs/jackpal/ (locks/vendored-aars.lock). The build refuses missing files,
// debug host artifacts, placeholder hashes, extra lock entries and digest mismatches.
// ---------------------------------------------------------------------------

fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun Properties.requiredValue(key: String): String =
    getProperty(key)?.trim()?.takeIf(String::isNotEmpty)
        ?: error("Missing required lock value: $key")

fun File.loadUniqueLock(): Properties {
    val lock = Properties()
    useLines(Charsets.UTF_8) { lines ->
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#') || trimmed.startsWith('!')) {
                return@forEachIndexed
            }
            val separator = trimmed.indexOf('=')
            require(separator > 0) { "Malformed lock line ${index + 1} in $name" }
            val key = trimmed.substring(0, separator).trim()
            val value = trimmed.substring(separator + 1).trim()
            require(!lock.containsKey(key)) { "Duplicate lock key in $name: $key" }
            lock.setProperty(key, value)
        }
    }
    return lock
}

val sha256Pattern = Regex("[0-9a-f]{64}")

fun lockedAars(lockFile: File, ids: List<String>, directory: String, forbidDebug: Boolean): List<File> {
    require(lockFile.isFile) { "Missing AAR lock: ${lockFile.relativeTo(rootProject.projectDir)}" }
    val lock = lockFile.loadUniqueLock()
    val expectedKeys = setOf("format") + ids.flatMap { id -> listOf("$id.file", "$id.sha256") }
    require(lock.stringPropertyNames() == expectedKeys) {
        "${lockFile.name} must contain exactly these keys: ${expectedKeys.sorted()}"
    }
    require(lock.requiredValue("format") == "1") { "Unsupported lock format in ${lockFile.name}" }
    return ids.map { id ->
        val fileName = lock.requiredValue("$id.file")
        val expectedSha256 = lock.requiredValue("$id.sha256").lowercase()
        require(fileName == File(fileName).name && fileName.endsWith(".aar")) { "Invalid $id.file in ${lockFile.name}" }
        require(!forbidDebug || !fileName.endsWith("-debug.aar")) { "Debug AARs are forbidden: $fileName" }
        require(sha256Pattern.matches(expectedSha256)) {
            "Replace $id.sha256 in ${lockFile.name} with the audited AAR SHA-256 before Gradle configuration"
        }
        val artifact = rootProject.file("$directory/$fileName")
        require(artifact.isFile) { "Missing locked AAR: ${artifact.relativeTo(rootProject.projectDir)}" }
        val actualSha256 = artifact.sha256()
        require(actualSha256 == expectedSha256) {
            "SHA-256 mismatch for $fileName: expected $expectedSha256, actual $actualSha256"
        }
        artifact
    }
}

// Roadmap P1.1 appends "terminal-api" once the host contract module is delivered.
val hostApiIds = listOf("common-plugin-api", "nodejs-api")
val hostApiAars = lockedAars(rootProject.file("locks/host-api-aars.lock"), hostApiIds, "libs", forbidDebug = true)

// The term AAR is the upstream "term-debug.aar" distribution (its only release form, Java only); the
// debug prohibition applies to host artifacts, not to the vendored jackpal set (roadmap D13).
val vendoredAarIds = listOf("term-1_0_70", "emulatorview-1_0_42", "libtermexec-1_0")
val vendoredAars = lockedAars(rootProject.file("locks/vendored-aars.lock"), vendoredAarIds, "libs/jackpal", forbidDebug = false)

android {
    // The host can select any bundled locale independently of the Android system language.
    bundle { language { enableSplit = false } }
    // Keep only the app's 10 languages (res/xml/locales_config.xml) from AndroidX/Material resources.
    androidResources { localeFilters += setOf("en", "ar", "es", "fr", "ja", "ko", "ru", "zh", "zh-rCN", "zh-rHK", "zh-rTW") }
    namespace = globalApplicationId
    compileSdk = versions.sdkVersionCompile

    defaultConfig {
        applicationId = globalApplicationId
        minSdk = versions.sdkVersionMin
        targetSdk = versions.sdkVersionTarget
        versionCode = versions.appVersionCode
        versionName = versions.appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "plugin_author", "SuperMonster003")
        resValue("string", "plugin_engine", "terminal")
        resValue("string", "plugin_id", "three-shell-terminal")
        resValue("string", "plugin_variant", "default")
        resValue("string", "plugin_version_date", utils.getDateString("MMM d, yyyy", "GMT+08:00"))
    }

    lint {
        abortOnError = true
        // Product text intentionally uses ASCII punctuation in every locale.
        disable += "TypographyEllipsis"
        // core-ktx arrives only transitively with AppCompat; the code base keeps plain platform APIs.
        disable += "UseKtx"
    }

    signingConfigs {
        if (signs.isValid) {
            create(buildTypeRelease) {
                storeFile = signs.properties["storeFile"]?.let { file(it as String) }
                keyPassword = signs.properties["keyPassword"] as String
                keyAlias = signs.properties["keyAlias"] as String
                storePassword = signs.properties["storePassword"] as String
            }
        }
    }

    buildTypes {
        val proguardFiles = arrayOf<Any>(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
        val niceSigningConfig = takeIf { signs.isValid }?.let {
            signingConfigs.getByName(buildTypeRelease)
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        resValues = true
    }

    sourceSets.named("main") {
        kotlin.directories += "src/main/java"
    }

    packaging {
        resources.pickFirsts.addAll(
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.*",
                "META-INF/NOTICE",
                "META-INF/NOTICE.*",
                "META-INF/*.kotlin_module",
            ),
        )
        // The 16 KB verifier inspects the ELF files inside the APK; keep them uncompressed and page aligned.
        jniLibs.useLegacyPackaging = false
    }

    // One APK per ABI plus a universal APK (roadmap D14); getInfo() derives supportedAbis from the
    // libraries actually packaged, so each variant reports only what it carries.
    splits {
        abi {
            isEnable = true
            reset()
            include(*nativeAbis.toTypedArray())
            isUniversalApk = true
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val architecture = output.filters.find { it.filterType == FilterConfiguration.FilterType.ABI }?.identifier ?: "universal"
            val outputFileNameProperty = output.javaClass.methods.firstOrNull {
                it.name == "getOutputFileName" && it.parameterTypes.isEmpty()
            }?.invoke(output) as? Property<*>

            @Suppress("UNCHECKED_CAST")
            (outputFileNameProperty as? Property<String>)?.set(
                output.versionName.map { versionName ->
                    val version = versionName.replace("\\s".toRegex(), "-")
                    "${rootProject.name}-v$version-$architecture.${utils.FILE_EXTENSION_APK}".lowercase()
                },
            )
        }
    }
}

dependencies {
    // PluginInfo, IPluginInfoProvider and the shared plugin constants (host module plugin-api/common-plugin-api);
    // NODE_CLI_* manifest contract constants of the Node.js Runtime plugin (host module plugin-api/nodejs-api).
    implementation(files(hostApiAars))
    // jackpal Android-Terminal-Emulator: TermSession / EmulatorView (emulatorview), Exec JNI declarations (term)
    // and TermExec with the native pty bridge (libtermexec), see THIRD_PARTY_NOTICES.md (roadmap D13).
    implementation(files(vendoredAars))
    implementation(libs.gson)
    // Material 3 widgets and AppCompat theming for the terminal screen and the standalone settings (roadmap P3 / P5).
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)

    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.ext.junit)
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }

    register("appendDigestToReleasedFiles") {
        group = "distribution"
        description = "Builds and verifies the five signed release APKs of 3-Shell Terminal, then appends their CRC32 digests"
        dependsOn("assembleRelease")

        doLast {
            check(signs.isValid) { "Release signing is not configured (sign.properties); refusing to collect unsigned APKs" }
            val extension = utils.FILE_EXTENSION_APK
            val prefix = "${rootProject.name}-v${versions.appVersionName.replace("\\s".toRegex(), "-")}".lowercase()
            val expected = (nativeAbis + "universal").associateBy { "$prefix-$it.$extension" }
            val source = layout.buildDirectory.dir("outputs/apk/$buildTypeRelease").get().asFile
            val apks = source.listFiles { candidate -> candidate.extension == extension }.orEmpty()
            check(apks.map { it.name }.toSet() == expected.keys) {
                "Unexpected release APK set in $source: expected ${expected.keys.sorted()}, actual ${apks.map { it.name }.sorted()}"
            }
            val collected = apks.sortedBy { it.name }.associateWith { apk ->
                val verification = ApkVerifier.Builder(apk).build().verify()
                check(verification.isVerified) { "Invalid or unsigned release APK ${apk.name}: ${verification.errors}" }
                val abi = expected.getValue(apk.name)
                val packagedAbis = if (abi == "universal") nativeAbis else listOf(abi)
                ZipFile(apk).use { zip ->
                    val nativeLibraries = zip.entries().asSequence()
                        .filter { it.name.startsWith("lib/") && it.name.endsWith(".so") }
                        .map { it.name }.toSet()
                    val expectedLibraries = packagedAbis.flatMap { packagedAbi -> nativeLibraryNames.map { "lib/$packagedAbi/$it" } }.toSet()
                    check(nativeLibraries == expectedLibraries) { "Unexpected native library set in ${apk.name}: $nativeLibraries" }
                    for (name in expectedLibraries) {
                        zip.getInputStream(zip.getEntry(name)).use { stream ->
                            check(stream.readNBytes(4).contentEquals(byteArrayOf(0x7f, 0x45, 0x4c, 0x46))) {
                                "Expected ELF bytes in ${apk.name}:$name"
                            }
                        }
                    }
                }
                "$prefix-$abi-${utils.digestCRC32(apk)}.$extension"
            }
            val destination = file("$rootDir/${buildTypeRelease}s")
            check(destination.isDirectory || destination.mkdirs()) { "Cannot create $destination" }
            collected.forEach { (apk, name) -> apk.copyTo(destination.resolve(name), overwrite = true) }
            // Only retire superseded artifacts of this exact version after every input verified.
            destination.listFiles { file -> file.name.startsWith("$prefix-") && file.extension == extension }
                .orEmpty().filter { it.name !in collected.values }.forEach { stale ->
                    check(stale.delete()) { "Cannot remove superseded artifact: $stale" }
                }
            println("Destination: $destination")
            collected.values.forEach { println(it) }
        }
    }
}

extra {
    versions.handleIfNeeded(project, "", listOf(buildTypeDebug, buildTypeRelease))
}

// Bundle the license texts so the About screen (roadmap P5.2) can show them offline.
val bundledLegalAssets = tasks.register<Sync>("bundleLegalAssets") {
    from(listOf(rootProject.file("LICENSE"), rootProject.file("THIRD_PARTY_NOTICES.md"))) { into("legal") }
    from(rootProject.file("native/jackpal-termexec")) {
        include("LICENSE", "NOTICE")
        into("legal/jackpal")
    }
    into(layout.buildDirectory.dir("generated/legal-assets"))
}
android.sourceSets.getByName("main").assets.directories.add(layout.buildDirectory.dir("generated/legal-assets").get().asFile.path)
tasks.named("preBuild").configure { dependsOn(bundledLegalAssets) }

// Every ABI must ship both pty libraries aligned to 16 KB pages (roadmap D14; the host rebuilt them with NDK 28).
nativeAlignment {
    strictAbis.set(nativeAbis.toSet())
}

tasks.withType<VerifyNativePageAlignment>().configureEach {
    // The 1.8.0 plugin also finalizes assemble*UnitTest, which never produces an APK.
    // Keep verification on APK variants and let standalone checks build their inputs.
    val variantSuffix = name.removePrefix("verify").removeSuffix("NativePageAlignment")
    if (variantSuffix.endsWith("UnitTest")) {
        enabled = false
    } else {
        dependsOn("assemble$variantSuffix")
    }
}
