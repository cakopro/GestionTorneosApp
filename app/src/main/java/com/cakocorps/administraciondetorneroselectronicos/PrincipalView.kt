package com.cakocorps.administraciondetorneroselectronicos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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
import com.google.firebase.auth.FirebaseAuth


class PrincipalView : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

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

        val btnCerrar = findViewById<Button>(R.id.btnSalir)
        btnCerrar.setOnClickListener {
            cerrarSesion()
        }


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
    fun cerrarSesion() {
        firebaseAuth.signOut()

        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}