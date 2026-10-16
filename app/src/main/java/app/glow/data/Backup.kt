package app.glow.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Plain JSON backup: everything stays on the device unless the user exports it.
 * Screenshots of kind words travel inside the file as base64 so one file restores everything.
 */
object Backup {
    private const val VERSION = 1

    fun encode(s: Snapshot, images: File): String = JSONObject()
        .put("app", "glow")
        .put("version", VERSION)
        .put("wins", JSONArray(s.wins.map {
            JSONObject().put("id", it.id).put("at", it.at).put("text", it.text).put("size", it.size)
                .put("category", it.category).put("praise", it.praise).put("voice", it.voice)
        }))
        .put("kindWords", JSONArray(s.kindWords.map { k ->
            val file = k.image?.let { File(images, it) }?.takeIf { it.exists() }
            JSONObject().put("id", k.id).put("at", k.at).put("text", k.text).put("from", k.from).put("source", k.source)
                .put("image", k.image ?: JSONObject.NULL)
                .put("imageData", file?.let { Base64.encodeToString(it.readBytes(), Base64.NO_WRAP) } ?: JSONObject.NULL)
        }))
        .put("evenings", JSONArray(s.evenings.map {
            JSONObject().put("day", it.day).put("one", it.one).put("two", it.two).put("three", it.three).put("at", it.at)
        }))
        .toString(1)

    /** Throws [IllegalArgumentException] when the text is not a Glow backup. Writes screenshots into [images]. */
    fun decode(text: String, images: File): Snapshot {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "glow") { "Not a Glow backup" }
        images.mkdirs()
        return Snapshot(
            wins = root.list("wins") {
                Win(getLong("id"), getLong("at"), getString("text"), getInt("size"), optString("category"), optString("praise"), optString("voice"))
            },
            kindWords = root.list("kindWords") {
                val name = if (isNull("image")) null else optString("image").takeIf { it.isNotEmpty() }
                if (name != null && !isNull("imageData")) {
                    File(images, File(name).name).writeBytes(Base64.decode(getString("imageData"), Base64.NO_WRAP))
                }
                KindWord(getLong("id"), getLong("at"), getString("text"), optString("from"), name?.let { File(it).name }, optInt("source"))
            },
            evenings = root.list("evenings") {
                Evening(getLong("day"), optString("one"), optString("two"), optString("three"), getLong("at"))
            },
        )
    }

    private fun <T> JSONObject.list(key: String, read: JSONObject.() -> T): List<T> {
        val arr = optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).map { arr.getJSONObject(it).read() }
    }
}
