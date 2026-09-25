package com.cakocorps.administraciondetorneroselectronicos

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat


class PrincipalView : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_principal_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tolbar = findViewById<Toolbar>(R.id.toolbar)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val draweLa = findViewById<DrawerLayout>(R.id.main)

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
}