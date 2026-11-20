package com.example.streamplayer

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class JellyfinLibraryActivity : AppCompatActivity() {

    private lateinit var session: JellyfinApi.Session
    private val media = mutableListOf<JellyfinApi.Media>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var etSearch: EditText
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val s = JellyfinApi.loadSession(this)
        if (s == null) {
            startActivity(Intent(this, JellyfinLoginActivity::class.java))
            finish()
            return
        }
        session = s

        setContentView(R.layout.activity_jf_library)
        etSearch = findViewById(R.id.etSearch)
        tvStatus = findViewById(R.id.tvLibStatus)
        val cb = findViewById<CheckBox>(R.id.cbTranscode)
        val lv = findViewById<ListView>(R.id.lvMedia)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        lv.adapter = adapter

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                load(); true
            } else false
        }

        lv.setOnItemClickListener { _, _, pos, _ ->
            val url = JellyfinApi.streamUrl(session, media[pos].id, cb.isChecked)
            startActivity(
                Intent(this, PlayerActivity::class.java).putExtra(PlayerActivity.EXTRA_URL, url)
            )
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            JellyfinApi.clearSession(this)
            startActivity(Intent(this, JellyfinLoginActivity::class.java))
            finish()
        }

        load()
    }

    private fun load() {
        val term = etSearch.text.toString()
        tvStatus.text = "Memuat..."
        Thread {
            try {
                val list = JellyfinApi.items(session, term)
                runOnUiThread {
                    media.clear()
                    media.addAll(list)
                    adapter.clear()
                    adapter.addAll(list.map { it.title })
                    adapter.notifyDataSetChanged()
                    tvStatus.text = if (list.isEmpty()) "Tidak ada video ditemukan" else "${list.size} video"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (e.message?.contains("401") == true) {
                        JellyfinApi.clearSession(this)
                        startActivity(Intent(this, JellyfinLoginActivity::class.java))
                        finish()
                    } else {
                        tvStatus.text = "Gagal memuat: ${e.message}"
                    }
                }
            }
        }.start()
    }
}
