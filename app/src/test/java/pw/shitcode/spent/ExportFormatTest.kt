package pw.shitcode.spent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pw.shitcode.spent.data.export.ExportFormat
import pw.shitcode.spent.data.export.ExportRow
import java.time.LocalDate

class ExportFormatTest {
    private val plain = ExportRow(LocalDate.of(2026, 10, 12), "com.a", "Plain", 61000, 3, 2, false)
    private val tricky = ExportRow(LocalDate.of(2026, 10, 11), "com.b", "Comma, \"Quote\"\nLine", 5, null, null, true)
    private val unicode = ExportRow(LocalDate.of(2026, 10, 10), "com.c", "Привет \\ \t\u0001", 0, 0, 0, false)

    private fun csv(rows: List<ExportRow>) = StringBuilder().also { ExportFormat.writeCsv(rows, it) }.toString()
    private fun json(rows: List<ExportRow>) =
        StringBuilder().also { ExportFormat.writeJson(rows, it, "2026-10-12T07:55:00+03:00") }.toString()

    @Test
    fun csv_startsWithBomAndHeader() {
        assertTrue(csv(listOf(plain)).startsWith("\uFEFF" + ExportFormat.CSV_HEADER + "\r\n"))
    }

    @Test
    fun csv_plainRow() {
        assertEquals("2026-10-12,com.a,Plain,61000,3,2,false", csv(listOf(plain)).split("\r\n")[1])
    }

    @Test
    fun csv_quotesAndNullsAreEscaped() {
        assertTrue(csv(listOf(tricky)).contains("com.b,\"Comma, \"\"Quote\"\"\nLine\",5,,,true\r\n"))
    }

    @Test
    fun csv_emptyRowsIsHeaderOnly() {
        assertEquals("\uFEFF" + ExportFormat.CSV_HEADER + "\r\n", csv(emptyList()))
    }

    @Test
    fun json_nullsAndEscaping() {
        val out = json(listOf(tricky, unicode))
        assertTrue(out.contains("\"launches\": null, \"notifications\": null, \"approx\": true"))
        assertTrue(out.contains("Comma, \\\"Quote\\\"\\nLine"))
        assertTrue(out.contains("Привет \\\\ \\t\\u0001"))
    }

    @Test
    fun json_emptyRows() {
        assertTrue(json(emptyList()).contains("\"rows\": []"))
    }
}
