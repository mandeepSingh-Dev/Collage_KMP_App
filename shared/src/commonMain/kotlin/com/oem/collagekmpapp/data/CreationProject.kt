package com.oem.collagekmpapp.data

// commonMain/kotlin/com/oem/collagekmpapp/data/CreationProject.kt
//
// Re-editable projects: every exporter can save a small JSON "sidecar" next to its image
// (same base name: Collage_1764000000000.png -> Collage_1764000000000.json).
// The Creations screen reads it back (Creation.projectJson), parses it with [CreationProject.parse]
// and reopens the right editor with the original photos; the editor picks up the rest of the state
// (text layers, spacing, filters, ...) through [ProjectRestore].

/** Which editor a project re-opens in. [id] is what is stored in the JSON. */
enum class ProjectEditor(val id: String) {
    COLLAGE("collage"),
    FREESTYLE("freestyle"),
    PHOTO("photo"),
    REMOVE_BG("removebg");

    companion object {
        fun fromId(id: String?): ProjectEditor? = entries.firstOrNull { it.id == id }
    }
}

/** One text layer. x / y are normalized (0..1) to the canvas, colour is 0xAARRGGBB. */
data class ProjectTextLayer(
    val text: String,
    val x: Float,
    val y: Float,
    val sizeSp: Float,
    val colorArgb: Long,
    val rotation: Float = 0f,
    val fontId: String? = null
)

/** Filter + adjustments of one photo (photoIndex = index into [CreationProject.photos]). */
data class ProjectPhotoFilter(
    val photoIndex: Int,
    val filterId: String? = null,
    val intensity: Float? = null,
    val adjustments: Map<String, Float> = emptyMap()   // e.g. "brightness" -> 0.1, "contrast" -> 1.2
)

/**
 * Schema v1 of a saved project. Common fields are typed; anything editor-specific (freestyle item transforms,
 * frames, drawing strokes, ...) goes into [extras] as plain JSON values (String, Number, Boolean, List, Map, null).
 */
data class CreationProject(
    val editor: ProjectEditor,
    val photos: List<String>,
    val layoutId: String? = null,
    val templateId: String? = null,
    val spacing: Float? = null,
    val cornerRadius: Float? = null,
    val backgroundArgb: Long? = null,
    val photoFilters: List<ProjectPhotoFilter> = emptyList(),
    val texts: List<ProjectTextLayer> = emptyList(),
    val extras: Map<String, Any?> = emptyMap(),
    val version: Int = SCHEMA_VERSION
) {
    fun toJson(): String = MiniJson.write(
        linkedMapOf(
            "schema" to SCHEMA_NAME,
            "version" to version,
            "editor" to editor.id,
            "photos" to photos,
            "layoutId" to layoutId,
            "templateId" to templateId,
            "spacing" to spacing,
            "cornerRadius" to cornerRadius,
            "backgroundArgb" to backgroundArgb,
            "filters" to photoFilters.map {
                linkedMapOf(
                    "photoIndex" to it.photoIndex,
                    "filterId" to it.filterId,
                    "intensity" to it.intensity,
                    "adjustments" to it.adjustments
                )
            },
            "texts" to texts.map {
                linkedMapOf(
                    "text" to it.text, "x" to it.x, "y" to it.y, "sizeSp" to it.sizeSp,
                    "colorArgb" to it.colorArgb, "rotation" to it.rotation, "fontId" to it.fontId
                )
            },
            "extras" to extras
        )
    )

    companion object {
        const val SCHEMA_NAME = "collage-studio-project"
        const val SCHEMA_VERSION = 1

        /** null when [json] is not a valid project (wrong schema, unknown editor, no photos, broken JSON). */
        fun parse(json: String): CreationProject? = runCatching {
            val m = MiniJson.parse(json) as? Map<*, *> ?: return null
            if (m["schema"] != SCHEMA_NAME) return null
            val editor = ProjectEditor.fromId(m["editor"] as? String) ?: return null
            val photos = (m["photos"] as? List<*>)?.filterIsInstance<String>().orEmpty()
            if (photos.isEmpty()) return null
            CreationProject(
                editor = editor,
                photos = photos,
                layoutId = m["layoutId"] as? String,
                templateId = m["templateId"] as? String,
                spacing = m.num("spacing")?.toFloat(),
                cornerRadius = m.num("cornerRadius")?.toFloat(),
                backgroundArgb = m.num("backgroundArgb")?.toLong(),
                photoFilters = (m["filters"] as? List<*>).orEmpty().mapNotNull { f ->
                    val fm = f as? Map<*, *> ?: return@mapNotNull null
                    ProjectPhotoFilter(
                        photoIndex = fm.num("photoIndex")?.toInt() ?: return@mapNotNull null,
                        filterId = fm["filterId"] as? String,
                        intensity = fm.num("intensity")?.toFloat(),
                        adjustments = (fm["adjustments"] as? Map<*, *>).orEmpty()
                            .mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? Number)?.let { key to it.toFloat() } } }
                            .toMap()
                    )
                },
                texts = (m["texts"] as? List<*>).orEmpty().mapNotNull { t ->
                    val tm = t as? Map<*, *> ?: return@mapNotNull null
                    ProjectTextLayer(
                        text = tm["text"] as? String ?: return@mapNotNull null,
                        x = tm.num("x")?.toFloat() ?: 0.5f,
                        y = tm.num("y")?.toFloat() ?: 0.5f,
                        sizeSp = tm.num("sizeSp")?.toFloat() ?: 24f,
                        colorArgb = tm.num("colorArgb")?.toLong() ?: 0xFFFFFFFFL,
                        rotation = tm.num("rotation")?.toFloat() ?: 0f,
                        fontId = tm["fontId"] as? String
                    )
                },
                extras = (m["extras"] as? Map<*, *>).orEmpty()
                    .mapNotNull { (k, v) -> (k as? String)?.let { it to v } }.toMap(),
                version = m.num("version")?.toInt() ?: SCHEMA_VERSION
            )
        }.getOrNull()

        private fun Map<*, *>.num(key: String): Number? = this[key] as? Number
    }
}

/**
 * Hand-off from the Creations screen to the editor it opens. The screen calls [offer] right before navigating;
 * the editor calls [consume] once when it starts and applies the returned project's text layers, spacing,
 * filters, ... An offer only matches the same editor + the same photo list and expires after a few seconds,
 * so opening those photos normally later never restores an old project by accident.
 */
object ProjectRestore {
    private const val TTL_MS = 10_000L
    private var pending: CreationProject? = null
    private var offeredAt = 0L

    fun offer(project: CreationProject) {
        pending = project
        offeredAt = platformNowMillis()
    }

    fun consume(editor: ProjectEditor, photos: List<String>): CreationProject? {
        val p = pending ?: return null
        if (platformNowMillis() - offeredAt > TTL_MS) { pending = null; return null }
        if (p.editor != editor || p.photos != photos) return null
        pending = null
        return p
    }

    fun clear() { pending = null }
}

// =====================================================================================
//  Minimal JSON reader / writer (no serialization plugin needed)
//  Values: null, Boolean, Number (parsed as Double), String, List<Any?>, Map<String, Any?>
// =====================================================================================

internal object MiniJson {

    fun write(value: Any?): String = StringBuilder().also { writeValue(it, value) }.toString()

    fun parse(text: String): Any? = Parser(text).run {
        val v = readValue()
        skipWs()
        require(pos == text.length) { "Trailing characters at $pos" }
        v
    }

    private fun writeValue(sb: StringBuilder, v: Any?) {
        when (v) {
            null -> sb.append("null")
            is String -> writeString(sb, v)
            is Boolean -> sb.append(v)
            is Int, is Long, is Short, is Byte -> sb.append(v.toString())
            is Float -> if (v.isFinite()) sb.append(v.toString()) else sb.append("null")
            is Double -> if (v.isFinite()) sb.append(v.toString()) else sb.append("null")
            is Number -> sb.append(v.toString())
            is Map<*, *> -> {
                sb.append('{')
                var first = true
                for ((k, value) in v) {
                    if (!first) sb.append(',')
                    first = false
                    writeString(sb, k.toString())
                    sb.append(':')
                    writeValue(sb, value)
                }
                sb.append('}')
            }
            is Iterable<*> -> {
                sb.append('[')
                var first = true
                for (item in v) {
                    if (!first) sb.append(',')
                    first = false
                    writeValue(sb, item)
                }
                sb.append(']')
            }
            is Array<*> -> writeValue(sb, v.asList())
            else -> writeString(sb, v.toString())
        }
    }

    private fun writeString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c < ' ') {
                    sb.append("\\u")
                    val hex = c.code.toString(16)
                    repeat(4 - hex.length) { sb.append('0') }
                    sb.append(hex)
                } else sb.append(c)
            }
        }
        sb.append('"')
    }

    private class Parser(val s: String) {
        var pos = 0

        fun skipWs() {
            while (pos < s.length && (s[pos] == ' ' || s[pos] == '\n' || s[pos] == '\r' || s[pos] == '\t')) pos++
        }

        fun readValue(): Any? {
            skipWs()
            require(pos < s.length) { "Unexpected end of JSON" }
            return when (val c = s[pos]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> if (c == '-' || c.isDigit()) readNumber() else throw IllegalArgumentException("Unexpected '$c' at $pos")
            }
        }

        private fun literal(word: String, value: Any?): Any? {
            require(s.startsWith(word, pos)) { "Expected $word at $pos" }
            pos += word.length
            return value
        }

        private fun readNumber(): Double {
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] in "+-.eE")) pos++
            return s.substring(start, pos).toDouble()
        }

        private fun readString(): String {
            pos++   // opening quote
            val sb = StringBuilder()
            while (true) {
                require(pos < s.length) { "Unterminated string" }
                val c = s[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        require(pos < s.length) { "Bad escape" }
                        when (val e = s[pos++]) {
                            '"' -> sb.append('"'); '\\' -> sb.append('\\'); '/' -> sb.append('/')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n'); 'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'u' -> {
                                require(pos + 4 <= s.length) { "Bad \\u escape" }
                                sb.append(s.substring(pos, pos + 4).toInt(16).toChar())
                                pos += 4
                            }
                            else -> throw IllegalArgumentException("Bad escape \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun readArray(): List<Any?> {
            pos++
            val out = ArrayList<Any?>()
            skipWs()
            if (pos < s.length && s[pos] == ']') { pos++; return out }
            while (true) {
                out += readValue()
                skipWs()
                require(pos < s.length) { "Unterminated array" }
                when (s[pos++]) {
                    ',' -> continue
                    ']' -> return out
                    else -> throw IllegalArgumentException("Expected , or ] at ${pos - 1}")
                }
            }
        }

        private fun readObject(): Map<String, Any?> {
            pos++
            val out = LinkedHashMap<String, Any?>()
            skipWs()
            if (pos < s.length && s[pos] == '}') { pos++; return out }
            while (true) {
                skipWs()
                require(pos < s.length && s[pos] == '"') { "Expected key at $pos" }
                val key = readString()
                skipWs()
                require(pos < s.length && s[pos] == ':') { "Expected : at $pos" }
                pos++
                out[key] = readValue()
                skipWs()
                require(pos < s.length) { "Unterminated object" }
                when (s[pos++]) {
                    ',' -> continue
                    '}' -> return out
                    else -> throw IllegalArgumentException("Expected , or } at ${pos - 1}")
                }
            }
        }
    }
}