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
import android.util.Log
import android.content.ContentValues.TAG
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var  firebaseAnalytics: FirebaseAnalytics
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    //google
    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {

            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                //conectarese con google
                val account = task.getResult(ApiException::class.java)!!

                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Error al seleccionar cuenta de Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //si ya metiste la cuenta inicia de una
    override fun onStart(){
        super.onStart()
        val user = firebaseAuth.currentUser

        if (user != null){

            FirebaseFirestore.getInstance().collection("Usuarios").document(user.uid)
                .get()
                .addOnSuccessListener { documento ->

                if (documento.exists()) {
                    DatosRepository.usuarioActual = documento.toObject(Usuario::class.java)
                } else {
                    //por si no tiene documento
                    DatosRepository.usuarioActual = Usuario(id = user.uid, nombre = "Jugador", correo = user.email ?: "", rol = "Usuario")
                }

                    if (DatosRepository.usuarioActual?.rol == "Admin"){
                        val intent = Intent(this, DiagnosticoPruebas::class.java)
                        startActivity(intent)
                        finish()
                    }else{

                        startActivity(Intent(this, PrincipalView::class.java))
                        finish()
                    }
                }
                .addOnFailureListener {

                    //basico si es que falla
                    DatosRepository.usuarioActual = Usuario(id = user.uid, nombre = "Jugador", correo = user.email ?: "", rol = "Usuario")
                    startActivity(Intent(this, PrincipalView::class.java))
                    finish()

                }
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //extraer boton, txt
        val botonIngresar = findViewById<Button>(R.id.btnIngresar)
        val contra = findViewById<TextInputEditText>(R.id.idContra)
        val correo = findViewById<TextInputEditText>(R.id.idCorreo)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAuth = FirebaseAuth.getInstance()


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        botonIngresar.setOnClickListener {

            val correotxt = correo.text.toString()
            val contratxt = contra.text.toString()


            if (correotxt.isEmpty() || contratxt.isEmpty()){
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener}

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correotxt).matches()) {
                Toast.makeText(this, "Ingresa un formato de correo electrónico válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contratxt.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            ingresar(correotxt, contratxt)

                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN){
                    param(FirebaseAnalytics.Param.METHOD,"correo_contrasena") }
            }

        btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleLauncher.launch(signInIntent)
        }


//registrarse
        findViewById<Button>(R.id.btnRegistrar).setOnClickListener {
            val intent = Intent(this, Registrarse::class.java)
            startActivity(intent)
        }

    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, PrincipalView::class.java)

                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Fallo al autenticar con google. Intentalo denuevo", Toast.LENGTH_SHORT).show()
                }
            }
    }



    private fun ingresar(email: String, password: String) {
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        btnIngresar.isEnabled = false
        btnIngresar.text = "Iniciando..."

        firebaseAuth .signInWithEmailAndPassword(email, password)

        .addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {

                val user = firebaseAuth .currentUser

                if(user != null){

                    FirebaseFirestore.getInstance().collection("Usuarios").document(user.uid)
                        .get()
                        .addOnSuccessListener { documento ->

                            if (documento.exists()){
                                val miUsuario = documento.toObject(Usuario::class.java)
                                DatosRepository.usuarioActual = miUsuario

                                if (miUsuario?.rol == "Admin") {
                                    Toast.makeText(this, "¡Bienvenido Administrador ${miUsuario.nombre}!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, DiagnosticoPruebas::class.java)
                                    startActivity(intent)
                                    finish()

                                } else {
                                    Toast.makeText(this, "¡Bienvenido ${miUsuario?.nombre}!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, PrincipalView::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            }

                        }.addOnFailureListener {
                            Toast.makeText(this, "Error al descargar perfil", Toast.LENGTH_SHORT).show()
                            btnIngresar.isEnabled = true
                            btnIngresar.text = "Ingresar"
                        }
                }
            } else {
                val errorMensaje = when (task.exception){
                    is FirebaseAuthInvalidUserException -> "No existe cuenta con este correo"
                    is FirebaseAuthInvalidCredentialsException -> "la contraseña es incorrecta"
                    else -> "Error al iniciar sesion, compruebe conexion"
                }
                Toast.makeText(this, errorMensaje, Toast.LENGTH_SHORT).show()
                btnIngresar.isEnabled = true
                btnIngresar.text = "Ingresar"
            }
        }

    }


}