package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceDataSourceTest {
    @Test
    fun `reference data documents source URLs`() {
        val sourceFiles =
            listOf(
                "app/src/main/java/com/finnvek/knittools/domain/calculator/NeedleSizeData.kt",
                "app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt",
                "app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt",
                "app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt",
            )

        sourceFiles.forEach { path ->
            val source = ProjectSourceFiles.read(path)
            assertTrue(
                "$path ei dokumentoi Craft Yarn Council -lähdettä",
                source.contains("craftyarncouncil.com/standards/"),
            )
        }
    }
}
