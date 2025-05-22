package com.example.pi_03_equipe_01

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.pi_03_equipe_01.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Infle o layout e configure a exibição
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialize o DrawerLayout e NavigationView
        drawerLayout = findViewById(R.id.mainact)
        val navigationView: NavigationView = findViewById(R.id.nav_view)

        // Configure o botão para abrir o menu lateral
        val menuButton: Button = findViewById(R.id.menu_button)
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START) // Abre o menu lateral
        }

        // Configure as ações do menu lateral
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_history -> {
                    // Ação para abrir a tela "Histórico"
                    val intent = Intent(this, History::class.java)
                    startActivity(intent)
                }
                R.id.nav_sair -> {
                    // Encerra a atividade atual
                    finishAffinity()
                }
            }
            drawerLayout.closeDrawers() // Fecha o menu após seleção
            true
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Infla o menu, isso adiciona itens à action bar se estiver presente.
        menuInflater.inflate(R.menu.menu_navigation, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_history -> {
                // Cria uma Intent para iniciar a History Activity
                val intent = Intent(this, History::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}