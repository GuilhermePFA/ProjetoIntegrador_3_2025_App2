
package com.example.pi_03_equipe_01

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pi_03_equipe_01.databinding.ActivityHistoryBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth

class History : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private val database = FirebaseDatabase.getInstance().getReference("risk")
    private val historyList = mutableListOf<HistoryItem>()
    private lateinit var adapter: HistoryAdapter
    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialize o DrawerLayout e NavigationView
        drawerLayout = findViewById(R.id.hist)

        val navigationView: NavigationView = findViewById(R.id.nav_view)

        val menuButton: ImageView = findViewById(R.id.menu_button)
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_history -> {
                    startActivity(Intent(this, History::class.java))
                    finish()
                }
                R.id.nav_sair    -> finishAffinity()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        initRecyclerView()
        fetchHistoryById()
    }

    private fun initRecyclerView() {
        adapter = HistoryAdapter(historyList) { historyId ->
            val intent = Intent(this, Information::class.java)
            intent.putExtra("HISTORY_ID", historyId)
            startActivity(intent)
        }
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.setHasFixedSize(true)
        binding.historyRecyclerView.adapter = adapter
    }



    private fun fetchHistoryById(){
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FIREBASE", "Snapshot recebido: ${snapshot.childrenCount}")
                historyList.clear()

                val currentUser = FirebaseAuth.getInstance().currentUser
                val userId = currentUser?.uid




                for (itemSnapshot in snapshot.children) {
                    val id = itemSnapshot.child("riskID").value?.toString() ?: ""
                    if (id.isBlank()) continue
                    val iduser = itemSnapshot.child("created_by_userID").value?.toString() ?: ""
                    if (iduser == userId ){
                        val date = itemSnapshot.child("created_at").value?.toString() ?: ""
                        val statusStr = itemSnapshot.child("status").value?.toString() ?: "NAO_INICIADO"

                        try {
                            val status = HistoryItem.Status.valueOf(statusStr.replace(" ", "_").uppercase())
                            val historyItem = HistoryItem(id, date, status)
                            historyList.add(historyItem)
                        } catch (e: Exception) {
                            Log.e("FIREBASE", "Status inválido ou erro: $statusStr", e)
                        }
                    }

                }

                Log.d("FIREBASE", "Itens carregados: ${historyList.size}")
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Erro de leitura: ${error.message}")
            }
        })
    }
}
