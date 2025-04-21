package com.hfad.mystylebox.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hfad.mystylebox.R

class AboutActivity : AppCompatActivity() {
    private lateinit var btnVk: ImageButton
    private lateinit var btnTelegram: ImageButton
    private lateinit var btnGitHub: ImageButton
    private lateinit var tvEmail1: TextView
    private lateinit var tvEmail2: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        btnVk = findViewById(R.id.btnVk)
        btnTelegram = findViewById(R.id.btnTelegram)
        btnGitHub = findViewById(R.id.btnGitHub)
        tvEmail1 = findViewById(R.id.tvEmail1)
        tvEmail2 = findViewById(R.id.tvEmail2)

        tvEmail1.setOnClickListener { sendEmail("sparepost05@mail.ru") }
        tvEmail2.setOnClickListener { sendEmail("goncharuk-v@internet.ru") }

        btnVk.setOnClickListener { openUrl("https://vk.com/veronika_vivi") }
        btnTelegram.setOnClickListener { openUrl("https://t.me/Vivi_Vivivka") }
        btnGitHub.setOnClickListener { openUrl("https://github.com/VeronikaGon") }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun sendEmail(address: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$address")
        }
        startActivity(intent)
    }
}