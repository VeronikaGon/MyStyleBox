package com.hfad.mystylebox.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.hfad.mystylebox.R

class WelcomeActivity: AppCompatActivity() {
    private lateinit var buttonlogin: AppCompatButton
    private lateinit var buttonregistration: AppCompatButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        buttonlogin = findViewById(R.id.buttonLogin);
        buttonregistration = findViewById(R.id.buttonRegistration);
        buttonlogin.setOnClickListener{   startActivity(Intent(this, LoginActivity::class.java))}
        buttonregistration.setOnClickListener{   startActivity(Intent(this, RegistrationActivity::class.java))}
    }
}