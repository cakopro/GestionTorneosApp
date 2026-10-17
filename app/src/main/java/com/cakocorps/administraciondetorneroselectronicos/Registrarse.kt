package com.cakocorps.administraciondetorneroselectronicos

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import android.content.ContentValues.TAG
import com.google.firebase.auth.auth
import android.widget.Toast

class Registrarse : AppCompatActivity() {

    private lateinit var  firebaseAnalytics: FirebaseAnalytics
    private lateinit var firebaseAuth: FirebaseAuth

    private val TAG = "Registrarse"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registrarse)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        firebaseAnalytics = Firebase.analytics
        firebaseAuth = Firebase.auth
        // Instancia de FB Auth y Listener
        findViewById<Button>(R.id.btnRegistrarse).setOnClickListener { crearRegistro() }
    }

    private fun crearRegistro() {
        val contra = findViewById<TextInputEditText>(R.id.idContra).text.toString()
        val correo = findViewById<TextInputEditText>(R.id.idCorreo).text.toString()
        firebaseAuth.createUserWithEmailAndPassword(correo, contra)
        .addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
            val user = firebaseAuth.currentUser
            Toast.makeText(this, "usuario ha sido creado", Toast.LENGTH_SHORT).show()
            finish() // Reemplaza onBackPressed()
            } else {
            Toast.makeText(this, "Fallo la autenticación", Toast.LENGTH_SHORT).show()
            }

        }
    }



}