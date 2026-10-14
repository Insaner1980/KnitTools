package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AutomaticBackupRulesTest {
    @Test
    fun `automatic backup and device transfer exclude every storage domain`() {
        val expected =
            setOf(
                "root",
                "file",
                "database",
                "sharedpref",
                "external",
                "device_root",
                "device_file",
                "device_database",
                "device_sharedpref",
            )
        val rules =
            mapOf(
                "backup_rules.xml" to listOf("full-backup-content"),
                "data_extraction_rules.xml" to listOf("cloud-backup", "device-transfer"),
            )
        for ((fileName, sections) in rules) {
            val file =
                sequenceOf(File("src/main/res/xml/$fileName"), File("app/src/main/res/xml/$fileName"))
                    .first(File::isFile)
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            for (section in sections) {
                val nodes = document.getElementsByTagName(section)
                assertEquals("$fileName/$section", 1, nodes.length)
                val element = nodes.item(0) as Element
                assertEquals(0, element.getElementsByTagName("include").length)
                val excludes = element.getElementsByTagName("exclude")
                val domains =
                    (0 until excludes.length)
                        .map { index ->
                            val exclude = excludes.item(index) as Element
                            assertEquals("$fileName/$section", ".", exclude.getAttribute("path"))
                            exclude.getAttribute("domain")
                        }.toSet()
                assertEquals("$fileName/$section", expected, domains)
            }
        }
    }
}
