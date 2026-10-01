package io.github.supermonster003.autojs6.plugin.three.shell.terminal.storage

import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPlugin
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.storage.StorageAccess.PathKind
import org.autojs.plugin.terminal.api.TerminalContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Path normalization and the shared-storage decision table behind `resolveDirectory` (roadmap D18). */
class StorageAccessTest {

    private val externalRoot = "/storage/emulated/0"
    private val ownPackage = ThreeShellTerminalPlugin.PACKAGE_NAME

    private fun kind(path: String?) = StorageAccess.classify(path, externalRoot, ownPackage)

    @Test
    fun normalizeCollapsesLexicalNoiseWithoutTouchingSymlinks() {
        assertEquals("/sdcard/Scripts", StorageAccess.normalize("//sdcard//Scripts/"))
        assertEquals("/sdcard/a/b", StorageAccess.normalize("/sdcard/./a/./b"))
        assertEquals("/data/data/$ownPackage", StorageAccess.normalize("/sdcard/../data/data/$ownPackage"))
        assertEquals("/etc", StorageAccess.normalize("/../../etc"))
        assertEquals("/", StorageAccess.normalize("/"))
        assertEquals("/", StorageAccess.normalize("/.."))
        assertEquals("/sdcard/脚本", StorageAccess.normalize("  /sdcard/脚本  "))
    }

    @Test
    fun blankAndRelativePathsAreInvalid() {
        assertNull(StorageAccess.normalize(null))
        assertNull(StorageAccess.normalize(""))
        assertNull(StorageAccess.normalize("   "))
        assertNull(StorageAccess.normalize("sdcard"))
        assertNull(StorageAccess.normalize("foo/bar"))
        assertNull(StorageAccess.normalize("./x"))
        listOf(null, "", "   ", "sdcard", "foo/bar", "./x").forEach { assertEquals(it.toString(), PathKind.INVALID, kind(it)) }
    }

    @Test
    fun sharedStorageRootsAreRecognizedBySegment() {
        assertEquals(PathKind.SHARED, kind("/sdcard/脚本"))
        assertEquals(PathKind.SHARED, kind("/sdcard"))
        assertEquals(PathKind.SHARED, kind("/storage/emulated/0/x"))
        assertEquals(PathKind.SHARED, kind("/storage/emulated/10/x"))
        assertEquals(PathKind.SHARED, kind("/storage/self/primary/Download"))
        assertEquals(PathKind.SHARED, kind("/storage/1234-5678/DCIM"))
        assertEquals(PathKind.SHARED, kind("/storage"))
        assertEquals(PathKind.SHARED, kind("/mnt/sdcard/x"))
        assertEquals(PathKind.SHARED, kind("/mnt/user/0/primary/x"))
        assertEquals(PathKind.SHARED, kind("//sdcard//Scripts/"))
        assertEquals(PathKind.SHARED, kind("/storage/emulated/0/Android/data/other.pkg/files"))
        assertEquals(PathKind.SHARED, kind("/storage/emulated/0/Android/data"))
    }

    @Test
    fun privatePathsNeedNoGrant() {
        assertEquals(PathKind.PRIVATE, kind("/data/data/$ownPackage/files"))
        assertEquals(PathKind.PRIVATE, kind("/data/user/0/$ownPackage/files/terminal/home"))
        assertEquals(PathKind.PRIVATE, kind("/"))
        assertEquals(PathKind.PRIVATE, kind("/system/bin"))
        assertEquals(PathKind.PRIVATE, kind("/storageX/y"))
        assertEquals(PathKind.PRIVATE, kind("/sdcardX"))
        assertEquals(PathKind.PRIVATE, kind("/sdcard/../data/data/$ownPackage"))
    }

    @Test
    fun theOwnAppFoldersOnSharedStorageArePrivate() {
        assertEquals(PathKind.PRIVATE, kind("/storage/emulated/0/Android/data/$ownPackage"))
        assertEquals(PathKind.PRIVATE, kind("/storage/emulated/0/Android/data/$ownPackage/files/x"))
        assertEquals(PathKind.PRIVATE, kind("/sdcard/Android/obb/$ownPackage"))
        assertEquals(PathKind.PRIVATE, kind("/sdcard/Android/media/$ownPackage/clips"))
        assertEquals(PathKind.SHARED, kind("/sdcard/Android/media/$ownPackage.other"))
        assertEquals(PathKind.SHARED, kind("/sdcard/Android/$ownPackage"))
    }

    @Test
    fun aCustomExternalRootIsHonored() {
        assertEquals(PathKind.SHARED, StorageAccess.classify("/mnt/shell/emulated/0/x", "/mnt/shell/emulated/0/", ownPackage))
        assertEquals(PathKind.PRIVATE, StorageAccess.classify("/mnt/shell/emulated/1/x", "/mnt/shell/emulated/0", ownPackage))
    }

    @Test
    fun statesMapOntoTheContractVocabulary() {
        assertEquals("granted", StorageAccess.State.GRANTED.contractValue)
        assertEquals("denied", StorageAccess.State.DENIED_LEGACY.contractValue)
        assertEquals("denied", StorageAccess.State.DENIED_ALL_FILES.contractValue)
        assertEquals("not_applicable", StorageAccess.State.NOT_APPLICABLE.contractValue)
        StorageAccess.State.values().forEach { state ->
            assertTrue(state.name, state.contractValue in TerminalContract.STORAGE_ACCESS_STATES)
            assertEquals(state.name, state == StorageAccess.State.GRANTED, state.isGranted)
        }
    }

}
