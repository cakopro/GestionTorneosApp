package com.cakocorps.administraciondetorneroselectronicos

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import android.Manifest

class DiagnosticoPruebas : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_LONG).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_diagnostico_pruebas)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar Firebase Auth y Google Sign-In
        firebaseAuth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val botonCerrar = findViewById<Button>(R.id.btnSalirr)
        val btnFatal = findViewById<Button>(R.id.btnFatal)
        val btnCrash = findViewById<Button>(R.id.btnCrash)
        val btnToken = findViewById<Button>(R.id.btnToken)

        // Solicitar permisos al abrir la pantalla de diagnóstico
        solicitarPermisoNotificaciones()

        // 1. Botón para cerrar sesión
        botonCerrar.setOnClickListener {
            cerrarSesion()
        }



        // 2. Botón Crash Fatal (Provoca cierre inmediato de la app)
        btnFatal.setOnClickListener {
            throw RuntimeException("¡Crash fatal provocado para pruebas de diagnóstico!")
        }

        // 3. Botón Error No Fatal (Registra la excepción sin cerrar la app)
        btnCrash.setOnClickListener {
            try {
                throw Exception("¡Error no fatal controlado registrado con éxito!")
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "Error no fatal registrado en Crashlytics", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Botón Obtener Token (Para Cloud Messaging)
        btnToken.setOnClickListener {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(this, "Error al obtener el token FCM", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d("FCM_TOKEN", token ?: "Token vacío")
                Toast.makeText(this, "Token obtenido con éxito (Revisa el Logcat)", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun cerrarSesion() {
        firebaseAuth.signOut()

        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}