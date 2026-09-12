package dev.chandradsl.m3ecanvas

import dev.chandradsl.m3ecanvas.domain.model.M3EProject
import dev.chandradsl.m3ecanvas.editor.persistence.FileProjectRepository
import dev.chandradsl.m3ecanvas.editor.persistence.LoadResult
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class FileProjectRepositoryTest {

    @Test
    fun testLoadReturnsNotFoundWhenFileDoesNotExist() {
        runBlocking {
            val tempFile = File.createTempFile("m3e_test_nonexistent", ".json")
            tempFile.delete()
            assertFalse(tempFile.exists())

            val repo = FileProjectRepository(file = tempFile)
            val result = repo.load()

            assertTrue(result is LoadResult.NotFound)
        }
    }

    @Test
    fun testSaveAndLoadReturnsSuccess() {
        runBlocking {
            val tempFile = File.createTempFile("m3e_test_valid", ".json")
            try {
                val repo = FileProjectRepository(file = tempFile)
                val project = M3EProject(name = "TestProject")

                repo.save(project)
                assertTrue(tempFile.exists())

                val result = repo.load()
                assertIs<LoadResult.Success>(result)
                assertEquals("TestProject", result.project.name)
            } finally {
                tempFile.delete()
            }
        }
    }

    @Test
    fun testLoadReturnsCorruptedWhenFileContainsInvalidJson() {
        runBlocking {
            val tempFile = File.createTempFile("m3e_test_corrupt", ".json")
            try {
                tempFile.writeText("{ this is invalid json !!! }")
                val repo = FileProjectRepository(file = tempFile)
                val result = repo.load()

                assertIs<LoadResult.Corrupted>(result)
                assertTrue(result.reason.isNotEmpty())
                assertNotNull(result.cause)
            } finally {
                tempFile.delete()
            }
        }
    }
}
