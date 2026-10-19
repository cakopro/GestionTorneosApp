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
import android.content.Intent
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
        val contra = findViewById<TextInputEditText>(R.id.idContra).text.toString().trim()
        val correo = findViewById<TextInputEditText>(R.id.idCorreo).text.toString().trim()


        //validaciones
        if (contra.isEmpty() || correo.isEmpty()){
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (contra.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }


        firebaseAuth.createUserWithEmailAndPassword(correo, contra)
        .addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {

                val user = firebaseAuth.currentUser
            Toast.makeText(this, "usuario ha sido creado", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, PrincipalView::class.java)
                startActivity(intent)
                finish()

            } else {
                val errorReal = task.exception?.message ?: "Error desconocido"
                Toast.makeText(this, "Fallo: $errorReal", Toast.LENGTH_LONG).show()
            }

        }
    }



}