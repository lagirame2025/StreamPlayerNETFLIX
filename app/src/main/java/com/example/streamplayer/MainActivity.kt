package com.example.streamplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private data class Item(val url: String, val user: String, val pass: String)

    private val items = mutableListOf<Item>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var etUrl: EditText
    private lateinit var etUser: EditText
    private lateinit var etPass: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etUrl = findViewById(R.id.etUrl)
        etUser = findViewById(R.id.etUser)
        etPass = findViewById(R.id.etPass)
        val lv = findViewById<ListView>(R.id.lvSaved)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        lv.adapter = adapter
        load()

        findViewById<Button>(R.id.btnJellyfin).setOnClickListener {
            startActivity(Intent(this, JellyfinLoginActivity::class.java))
        }

        findViewById<Button>(R.id.btnPlay).setOnClickListener { play(currentItem()) }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val item = currentItem() ?: return@setOnClickListener
            items.removeAll { it.url == item.url }
            items.add(0, item)
            persist()
            refresh()
            Toast.makeText(this, "Tersimpan", Toast.LENGTH_SHORT).show()
        }

        lv.setOnItemClickListener { _, _, pos, _ -> play(items[pos]) }
        lv.setOnItemLongClickListener { _, _, pos, _ ->
            items.removeAt(pos)
            persist()
            refresh()
            true
        }
    }

    private fun currentItem(): Item? {
        val url = etUrl.text.toString().trim()
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            Toast.makeText(this, "URL harus diawali http:// atau https://", Toast.LENGTH_SHORT).show()
            return null
        }
        return Item(url, etUser.text.toString(), etPass.text.toString())
    }

    private fun play(item: Item?) {
        if (item == null) return
        startActivity(
            Intent(this, PlayerActivity::class.java)
                .putExtra(PlayerActivity.EXTRA_URL, item.url)
                .putExtra(PlayerActivity.EXTRA_USER, item.user)
                .putExtra(PlayerActivity.EXTRA_PASS, item.pass)
        )
    }

    private fun refresh() {
        adapter.clear()
        adapter.addAll(items.map { it.url })
        adapter.notifyDataSetChanged()
    }

    private fun persist() {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("u", it.url).put("n", it.user).put("p", it.pass))
        }
        getSharedPreferences("saved", Context.MODE_PRIVATE).edit()
            .putString("list", arr.toString()).apply()
    }

    private fun load() {
        val raw = getSharedPreferences("saved", Context.MODE_PRIVATE).getString("list", "[]") ?: "[]"
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            items.add(Item(o.getString("u"), o.optString("n"), o.optString("p")))
        }
        refresh()
    }
}
