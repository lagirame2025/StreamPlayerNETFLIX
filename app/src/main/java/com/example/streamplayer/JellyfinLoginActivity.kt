package com.example.streamplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class JellyfinLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sudah pernah login -> langsung ke daftar film
        if (JellyfinApi.loadSession(this) != null) {
            startActivity(Intent(this, JellyfinLibraryActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_jf_login)
        val etServer = findViewById<EditText>(R.id.etServer)
        val etUser = findViewById<EditText>(R.id.etJfUser)
        val etPass = findViewById<EditText>(R.id.etJfPass)
        val btn = findViewById<Button>(R.id.btnLogin)
        val tv = findViewById<TextView>(R.id.tvStatus)

        etServer.setText(JellyfinApi.lastServer(this))
        etUser.setText(JellyfinApi.lastUser(this))

        btn.setOnClickListener {
            val server = JellyfinApi.normalize(etServer.text.toString())
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString()
            if (server.isEmpty() || user.isEmpty()) {
                tv.text = "Alamat server dan username wajib diisi"
                return@setOnClickListener
            }
            tv.text = "Masuk..."
            btn.isEnabled = false
            Thread {
                try {
                    val session = JellyfinApi.login(server, user, pass)
                    runOnUiThread {
                        JellyfinApi.saveSession(this, session, user)
                        startActivity(Intent(this, JellyfinLibraryActivity::class.java))
                        finish()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        tv.text = "Gagal masuk: ${e.message}"
                        btn.isEnabled = true
                    }
                }
            }.start()
        }
    }
}
