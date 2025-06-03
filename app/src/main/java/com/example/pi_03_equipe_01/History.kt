package com.example.pi_03_equipe_01

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import android.widget.ImageButton        // ← IMPORT NECESSÁRIO
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pi_03_equipe_01.databinding.ActivityHistoryBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class History : AppCompatActivity(), DatePickerDialog.OnDateSetListener {

    private lateinit var drawerLayout: DrawerLayout
    private val database = FirebaseDatabase.getInstance().getReference("risk")

    // Lista completa (vinda do Firebase) — sempre ordenada por data
    private val allHistoryList = mutableListOf<HistoryItem>()

    // Lista que o adapter exibe (pode ter filtro aplicado)
    private val historyList = mutableListOf<HistoryItem>()

    private lateinit var adapter: HistoryAdapter
    private lateinit var binding: ActivityHistoryBinding

    // Botão “Filtrar” (ImageButton, pois no XML você usou <ImageButton>)
    private lateinit var filterButton: ImageButton

    // Variáveis para data de início e data de fim
    private var startDate: Date? = null
    private var endDate: Date? = null

    // Formatadores para parsing (ajuste conforme o formato exato de created_at)
    private val formatWithTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    private val formatWithoutTime = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Para saber se o próximo DatePicker é para início (0) ou fim (1)
    private var pickingField = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa DrawerLayout e NavigationView (sem alterações)
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
                R.id.nav_maps -> {
                    startActivity(Intent(this, Maps::class.java))
                    finish()
                }
                R.id.nav_sair -> finishAffinity()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // 1) Vincula o ImageButton “Filtrar”
        filterButton = findViewById(R.id.filterButton)

        // 2) Configura RecyclerView (sem alterações)
        initRecyclerView()

        // 3) Ao clicar em “Filtrar”, primeiro escolhe startDate, depois endDate
        filterButton.setOnClickListener {
            // Reinicia datas (se o usuário clicar novamente)
            startDate = null
            endDate = null
            pickingField = 0
            showDatePicker()
        }

        // 4) Busca dados do Firebase
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

    private fun fetchHistoryById() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FIREBASE", "Snapshot recebido: ${snapshot.childrenCount}")

                allHistoryList.clear()
                historyList.clear()

                val currentUser = FirebaseAuth.getInstance().currentUser
                val userId = currentUser?.uid

                for (itemSnapshot in snapshot.children) {
                    val id = itemSnapshot.child("riskID").value?.toString() ?: ""
                    if (id.isBlank()) continue

                    val iduser = itemSnapshot.child("created_by_userID").value?.toString() ?: ""
                    if (iduser == userId) {
                        val date = itemSnapshot.child("created_at").value?.toString() ?: ""
                        val statusStr = itemSnapshot.child("status").value?.toString() ?: "NAO_INICIADO"

                        try {
                            val status = HistoryItem.Status.valueOf(
                                statusStr.replace(" ", "_").uppercase()
                            )
                            val historyItem = HistoryItem(id, date, status)
                            allHistoryList.add(historyItem)
                        } catch (e: Exception) {
                            Log.e("FIREBASE", "Status inválido ou erro: $statusStr", e)
                        }
                    }
                }

                // Ordena a lista completa por data
                allHistoryList.sortBy { parseDateLegacy(it.date)?.time ?: 0L }

                // Exibe tudo inicialmente (sem filtro)
                historyList.addAll(allHistoryList)
                adapter.notifyDataSetChanged()
                Log.d("FIREBASE", "Itens carregados: ${historyList.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Erro de leitura: ${error.message}")
            }
        })
    }

    // Abre o DatePickerDialog (sempre para o campo indicado em pickingField)
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, this, year, month, day).show()
    }

    // Callback do DatePickerDialog: define startDate ou endDate
    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        // month vem de 0..11, então acrescentamos +1 para exibição
        val displayed = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)

        if (pickingField == 0) {
            // Escolheu data de início
            startDate = parseDateLegacy(displayed)
            // Agora pede data de fim
            pickingField = 1
            showDatePicker()
        } else {
            // Escolheu data de fim
            endDate = parseDateLegacy(displayed)
            // Ambos escolhidos: aplica filtro
            applyDateFilter()
        }
    }

    // Converte string em Date, tentando dois formatos: com ou sem hora
    private fun parseDateLegacy(dateString: String): Date? {
        return try {
            // Primeiro tenta “dd/MM/yyyy HH:mm:ss”
            formatWithTime.parse(dateString)
        } catch (e: ParseException) {
            try {
                // Se falhar, tenta apenas “dd/MM/yyyy”
                formatWithoutTime.parse(dateString)
            } catch (e2: ParseException) {
                null
            }
        }
    }

    // Aplica filtro usando startDate e endDate
    private fun applyDateFilter() {
        val start = startDate
        val end = endDate
        if (start == null || end == null) {
            return
        }

        // Ajusta end para 23:59:59 daquele dia, garantindo inclusividade
        val calendar = Calendar.getInstance()
        calendar.time = end
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endWithTime = calendar.time

        // Filtra todos os itens cujo created_at esteja entre start e endWithTime (inclusive)
        val filtered = allHistoryList.filter { item ->
            val itemDate = parseDateLegacy(item.date)
            itemDate != null && (itemDate >= start && itemDate <= endWithTime)
        }

        // Atualiza a lista do adapter
        historyList.clear()
        historyList.addAll(filtered)
        adapter.notifyDataSetChanged()
    }
}
