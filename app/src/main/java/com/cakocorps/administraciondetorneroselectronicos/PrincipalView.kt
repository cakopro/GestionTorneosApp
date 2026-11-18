package com.cakocorps.administraciondetorneroselectronicos

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.analytics.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class PrincipalView : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_principal_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val tolbar = findViewById<Toolbar>(R.id.toolbar)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val draweLa = findViewById<DrawerLayout>(R.id.main)

        tolbar.inflateMenu(R.menu.tolbar_menu)


        tolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.item_cerrar_sesion -> {
                    cerrarSesion()
                    true
                }
                R.id.item_elimiar_cuenta -> {

                    AlertDialog.Builder(this)
                        .setTitle("Eliminar cuenta")
                        .setMessage("¿ Esta seguro de que desea eliminar su cuenta?")
                        .setPositiveButton("Eliminar"){dialog, _ ->

                            val user = FirebaseAuth.getInstance().currentUser
                            val uid = user?.uid

                            if (uid != null){

                                val batch = db.batch()

                                db.collection("Inscripciones")
                                    .whereEqualTo("idUsuario",uid)
                                    .get()
                                    .addOnSuccessListener { inscripciones ->

                                        for (document in inscripciones){
                                            batch.delete(document.reference)
                                        }
                                        db.collection("Torneos")
                                            .whereEqualTo("idCreador",uid)
                                            .get()
                                            .addOnSuccessListener { torneos ->
                                                for (document in torneos){
                                                    batch.delete(document.reference)
                                                }
                                            val usuarioRef = db.collection("Usuarios").document(uid)
                                                batch.delete(usuarioRef)
                                                batch.commit().addOnSuccessListener {
                                                    eliminarCuenta()
                                                }.addOnFailureListener {
                                                    Toast.makeText(this, "Error al borrar los datos.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    }
                            }
                            dialog.dismiss()

                        }
                        .setNegativeButton("Cancelar") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                   true
                }
                else -> false
            }
        }




        //para que cuando se abra arranque con la lista torneos
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ListaTorneosFragment())
                .commit()
            navView.setCheckedItem(R.id.id_Torneos)
        }


        //abrir y cerrar
        tolbar.setNavigationOnClickListener {
            if (draweLa.isDrawerOpen(GravityCompat.START)) {
                draweLa.closeDrawer(GravityCompat.START)
            } else {
                draweLa.openDrawer(GravityCompat.START)
            }
        }
        // esperar toque de nav
        navView.setNavigationItemSelectedListener {
            menuItem ->
            when (menuItem.itemId){
                //quepantalla cargar
                R.id.id_Torneos ->{
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ListaTorneosFragment())
                        .commit()
                }
                R.id.id_mis_torneos ->{

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, MisTorneosFragment())
                        .commit()
                }
            }
            draweLa.closeDrawer(GravityCompat.START)
            true

        }


    }
    //cerrar sesion
    fun cerrarSesion() {
        firebaseAuth.signOut()

        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    fun eliminarCuenta(){

        val user = firebaseAuth.currentUser

        user?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                googleSignInClient.signOut().addOnCompleteListener {
                    Toast.makeText(this, "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                Toast.makeText(this, "Error al eliminar. Por favor, vuelve a iniciar sesión e intenta de nuevo.", Toast.LENGTH_LONG).show()
            }
        }
    }
}