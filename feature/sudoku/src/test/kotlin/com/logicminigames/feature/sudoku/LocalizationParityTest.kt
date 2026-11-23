package com.logicminigames.feature.sudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationParityTest {

    private fun resolveModuleDir(moduleRelative: String): File {
        val candidates = listOf(
            File(moduleRelative),
            File("..", moduleRelative),
            File("../..", moduleRelative),
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Could not locate directory $moduleRelative from ${File(".").absolutePath}")
    }

    private fun extractStringKeys(xmlFile: File): Set<String> {
        assertTrue("Missing localization file: ${xmlFile.path}", xmlFile.exists())
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val nodes = doc.getElementsByTagName("string")
        val keys = LinkedHashSet<String>(nodes.length)
        for (i in 0 until nodes.length) {
            val name = nodes.item(i).attributes?.getNamedItem("name")?.nodeValue
            if (!name.isNullOrBlank()) {
                keys.add(name)
            }
        }
        return keys
    }

    @Test
    fun `feature sudoku has identical string keys in EN, ES, and PT`() {
        val resDir = resolveModuleDir("feature/sudoku/src/main/res")
        val en = extractStringKeys(File(resDir, "values/strings.xml"))
        val es = extractStringKeys(File(resDir, "values-es/strings.xml"))
        val pt = extractStringKeys(File(resDir, "values-pt/strings.xml"))

        assertTrue(en.isNotEmpty())
        assertEquals("ES keys differ from EN", en, es)
        assertEquals("PT keys differ from EN", en, pt)
    }

    @Test
    fun `app module has identical string keys in EN, ES, and PT`() {
        val resDir = resolveModuleDir("app/src/main/res")
        val en = extractStringKeys(File(resDir, "values/strings.xml"))
        val es = extractStringKeys(File(resDir, "values-es/strings.xml"))
        val pt = extractStringKeys(File(resDir, "values-pt/strings.xml"))

        assertTrue(en.isNotEmpty())
        assertEquals("App ES keys differ from EN", en, es)
        assertEquals("App PT keys differ from EN", en, pt)
    }
}
