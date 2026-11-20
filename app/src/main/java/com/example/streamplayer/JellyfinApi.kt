package com.example.streamplayer

import android.content.Context
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object JellyfinApi {

    data class Session(val server: String, val token: String, val userId: String)
    data class Media(val id: String, val title: String)

    private const val PREFS = "jf"

    // ---------- penyimpanan sesi ----------
    fun saveSession(ctx: Context, s: Session, user: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("server", s.server)
            .putString("token", s.token)
            .putString("userId", s.userId)
            .putString("user", user)
            .apply()
    }

    fun loadSession(ctx: Context): Session? {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val token = p.getString("token", "").orEmpty()
        val userId = p.getString("userId", "").orEmpty()
        val server = p.getString("server", "").orEmpty()
        if (token.isEmpty() || userId.isEmpty() || server.isEmpty()) return null
        return Session(server, token, userId)
    }

    fun lastServer(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("server", "").orEmpty()

    fun lastUser(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("user", "").orEmpty()

    fun clearSession(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove("token").remove("userId").apply()
    }

    // ---------- API ----------
    fun normalize(input: String): String {
        var s = input.trim().trimEnd('/')
        if (s.isNotEmpty() && !s.startsWith("http://") && !s.startsWith("https://")) s = "http://$s"
        return s
    }

    private fun authHeader(token: String?): String {
        val base = "MediaBrowser Client=\"StreamPlayer\", Device=\"Android\", " +
            "DeviceId=\"streamplayer-android\", Version=\"1.0\""
        return if (token != null) "$base, Token=\"$token\"" else base
    }

    private fun request(url: String, method: String = "GET", body: String? = null, token: String? = null): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 15_000
            c.readTimeout = 25_000
            c.setRequestProperty("Authorization", authHeader(token))
            c.setRequestProperty("X-Emby-Authorization", authHeader(token))
            c.setRequestProperty("Accept", "application/json")
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = c.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    fun login(server: String, user: String, pass: String): Session {
        val body = JSONObject().put("Username", user).put("Pw", pass).toString()
        val res = JSONObject(request("$server/Users/AuthenticateByName", "POST", body))
        val token = res.getString("AccessToken")
        val userId = res.getJSONObject("User").getString("Id")
        return Session(server, token, userId)
    }

    fun items(s: Session, search: String): List<Media> {
        val q = StringBuilder(
            "${s.server}/Users/${s.userId}/Items?Recursive=true" +
                "&IncludeItemTypes=Movie,Episode&SortBy=SortName&SortOrder=Ascending&Limit=500"
        )
        if (search.isNotBlank()) q.append("&SearchTerm=").append(URLEncoder.encode(search.trim(), "UTF-8"))

        val arr = JSONObject(request(q.toString(), token = s.token)).getJSONArray("Items")
        val out = mutableListOf<Media>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val name = o.optString("Name")
            val title = if (o.optString("Type") == "Episode") {
                val series = o.optString("SeriesName")
                val sn = o.optInt("ParentIndexNumber", 0)
                val en = o.optInt("IndexNumber", 0)
                "$series ${String.format("S%02dE%02d", sn, en)} - $name".trim()
            } else {
                val year = o.optInt("ProductionYear", 0)
                if (year > 0) "$name ($year)" else name
            }
            out.add(Media(o.getString("Id"), title))
        }
        return out
    }

    fun streamUrl(s: Session, id: String, transcode: Boolean): String =
        if (transcode) {
            "${s.server}/Videos/$id/master.m3u8?MediaSourceId=$id&VideoCodec=h264" +
                "&AudioCodec=aac&TranscodingMaxAudioChannels=2&api_key=${s.token}"
        } else {
            "${s.server}/Videos/$id/stream?static=true&MediaSourceId=$id&api_key=${s.token}"
        }
}
