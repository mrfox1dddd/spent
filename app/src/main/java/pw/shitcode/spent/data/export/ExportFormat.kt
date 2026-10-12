package pw.shitcode.spent.data.export

import java.time.LocalDate

/**
 * Одна строка экспорта: приложение за один день.
 * launches: null для приблизительных дней (запусков по ним нет).
 * notifications: null для дней до начала подсчёта (истории уведомлений до выдачи доступа нет).
 */
class ExportRow(
    val date: LocalDate,
    val pkg: String,
    val label: String,
    val usageMs: Long,
    val launches: Int?,
    val notifications: Int?,
    val approx: Boolean,
)

/** Чистое форматирование без Android, чтобы проверять юнит-тестами на компьютере. */
object ExportFormat {
    const val CSV_HEADER = "date,package,app,usage_ms,launches,notifications,approx"

    /** UTF-8 с BOM, иначе Excel ломает кириллицу в названиях. Переводы строк CRLF по RFC 4180. */
    fun writeCsv(rows: List<ExportRow>, out: Appendable) {
        out.append('\uFEFF')
        out.append(CSV_HEADER).append("\r\n")
        for (r in rows) {
            out.append(r.date.toString()).append(',')
            out.append(csvField(r.pkg)).append(',')
            out.append(csvField(r.label)).append(',')
            out.append(r.usageMs.toString()).append(',')
            out.append(r.launches?.toString().orEmpty()).append(',')
            out.append(r.notifications?.toString().orEmpty()).append(',')
            out.append(r.approx.toString()).append("\r\n")
        }
    }

    fun writeJson(rows: List<ExportRow>, out: Appendable, exportedAt: String) {
        out.append("{\n  \"app\": \"Spent\",\n  \"format\": 1,\n  \"exported_at\": ")
        out.append(jsonString(exportedAt))
        out.append(",\n  \"rows\": [")
        rows.forEachIndexed { i, r ->
            out.append(if (i == 0) "\n    " else ",\n    ")
            out.append("{\"date\": ").append(jsonString(r.date.toString()))
            out.append(", \"package\": ").append(jsonString(r.pkg))
            out.append(", \"app\": ").append(jsonString(r.label))
            out.append(", \"usage_ms\": ").append(r.usageMs.toString())
            out.append(", \"launches\": ").append(r.launches?.toString() ?: "null")
            out.append(", \"notifications\": ").append(r.notifications?.toString() ?: "null")
            out.append(", \"approx\": ").append(r.approx.toString())
            out.append('}')
        }
        out.append(if (rows.isEmpty()) "]\n}\n" else "\n  ]\n}\n")
    }

    internal fun csvField(s: String): String {
        val needsQuotes = s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + s.replace("\"", "\"\"") + "\"" else s
    }

    internal fun jsonString(s: String): String {
        val sb = StringBuilder(s.length + 2)
        sb.append('"')
        for (ch in s) {
            when {
                ch == '"' -> sb.append("\\\"")
                ch == '\\' -> sb.append("\\\\")
                ch == '\n' -> sb.append("\\n")
                ch == '\r' -> sb.append("\\r")
                ch == '\t' -> sb.append("\\t")
                ch < ' ' -> sb.append("\\u").append(String.format("%04x", ch.code))
                else -> sb.append(ch)
            }
        }
        sb.append('"')
        return sb.toString()
    }
}
