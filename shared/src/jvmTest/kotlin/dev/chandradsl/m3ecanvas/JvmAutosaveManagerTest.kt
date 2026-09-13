package dev.chandradsl.m3ecanvas

import dev.chandradsl.m3ecanvas.editor.persistence.JvmAutosaveManager
import dev.chandradsl.m3ecanvas.editor.persistence.LoadResult
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmAutosaveManagerTest {

    @Test
    fun testAutosaveManagerOperations() = runBlocking {
        val tempDir = File.createTempFile("m3e_autosave_dir", "").apply { delete(); mkdirs() }
        try {
            val autosaveFile = File(tempDir, "autosave.json")
            val recentsFile = File(tempDir, "recents.json")

            val manager = JvmAutosaveManager(autosaveFile = autosaveFile, recentsFile = recentsFile)
            assertFalse(manager.hasAutosave())

            val project = EditorController.newProject("AutosaveTest")
            manager.saveAutosave(project)
            assertTrue(manager.hasAutosave())

            val loaded = manager.loadAutosave()
            assertTrue(loaded is LoadResult.Success)
            assertEquals("AutosaveTest", loaded.project.name)

            manager.clearAutosave()
            assertFalse(manager.hasAutosave())

            // Recents
            manager.addRecentProject("Recent 1", "/path/1")
            manager.addRecentProject("Recent 2", "/path/2")
            val recents = manager.getRecentProjects()
            assertEquals(2, recents.size)
            assertEquals("Recent 2", recents[0].name)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
