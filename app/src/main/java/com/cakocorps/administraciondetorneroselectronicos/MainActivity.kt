package com.cakocorps.administraciondetorneroselectronicos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

class MainActivity : AppCompatActivity() {

    private lateinit var  firebaseAnalytics: FirebaseAnalytics
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val botonIngresar = findViewById<Button>(R.id.btnIngresar)
        val contra = findViewById<TextInputEditText>(R.id.idContra)
        val correo = findViewById<TextInputEditText>(R.id.idCorreo)

        firebaseAnalytics = Firebase.analytics

        botonIngresar.setOnClickListener {

            val correotxt = correo.text.toString()
            val contratxt = contra.text.toString()

            if (correotxt.isEmpty() || contratxt.isEmpty()){
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val usuarioEncontrado = DatosRepository.listaUsuarios.find { usuario ->
                usuario.correo == correotxt && usuario.contrasena == contratxt
            }

            if (usuarioEncontrado != null){

                DatosRepository.usuarioActual = usuarioEncontrado
                val intent = Intent(this, PrincipalView::class.java)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN){
                    param(FirebaseAnalytics.Param.METHOD,"correo_contrasena")
                }
                startActivity(intent)
                finish()

            }else{
                Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()

            }

        }



    }

}