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
import com.google.firebase.firestore.FirebaseFirestore

class Registrarse : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    private val db = FirebaseFirestore.getInstance()

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
        firebaseAuth = Firebase.auth
        // Instancia de FB Auth y Listener
        findViewById<Button>(R.id.btnRegistrarse).setOnClickListener { crearRegistro() }
    }

    private fun crearRegistro() {
        val contra = findViewById<TextInputEditText>(R.id.idContra).text.toString().trim()
        val correo = findViewById<TextInputEditText>(R.id.idCorreo).text.toString().trim()
        val nombre = findViewById<TextInputEditText>(R.id.idNombre).text.toString().trim()

        //validaciones
        if (contra.isEmpty() || correo.isEmpty() || nombre.isEmpty()){
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (contra.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarse)
        btnRegistrar.isEnabled = false
        btnRegistrar.text = "Verificando..."


        db.collection("Usuarios")
            .whereEqualTo("nombre",nombre)
            .get()
            .addOnSuccessListener { result ->
                //buscar un igual si encuentra denegar poder hacer cuenta
                if(!result.isEmpty){

                    Toast.makeText(this, "El nickname '$nombre' ya está en uso", Toast.LENGTH_SHORT).show()
                    btnRegistrar.isEnabled = true
                    btnRegistrar.text = "Registrarse"
                }else{

                    btnRegistrar.text = "Creando cuenta..."
                    registrarEnAuthYFirestore(correo, contra, nombre, btnRegistrar)
                }

            }
            .addOnFailureListener {
                Toast.makeText(this, "Error de red al verificar usuario", Toast.LENGTH_SHORT).show()
                btnRegistrar.isEnabled = true
                btnRegistrar.text = "Registrarse"
            }

    }

    private fun  registrarEnAuthYFirestore(correo: String, contra: String, nombre: String, btnRegistrar: Button) {
        firebaseAuth.createUserWithEmailAndPassword(correo, contra)
            .addOnCompleteListener(this) { task ->

            if (task.isSuccessful){
                val user = firebaseAuth.currentUser

                    if (user != null) {
                        val usuario = Usuario(
                            id = user.uid,
                            nombre = nombre,
                            correo = correo,
                            rol = "Usuario"
                        )

                        db.collection("Usuarios").document(user.uid)
                            .set(usuario)
                            .addOnSuccessListener {
                                Toast.makeText(this, "¡Usuario creado con éxito!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, PrincipalView::class.java)
                                DatosRepository.usuarioActual = usuario
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al guardar el perfil en la base de datos", Toast.LENGTH_SHORT).show()
                                btnRegistrar.isEnabled = true
                                btnRegistrar.text = "Registrarse"
                            }
                }

            }else{
                val errorReal = task.exception?.message ?: "Error desconocido"
                Toast.makeText(this, "Fallo al crear cuenta: $errorReal", Toast.LENGTH_LONG).show()
                btnRegistrar.isEnabled = true
                btnRegistrar.text = "Registrarse" }
            }

    }
}



