package com.example.pi_03_equipe_01

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import android.widget.ImageButton
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


    private val allHistoryList = mutableListOf<HistoryItem>()


    private val historyList = mutableListOf<HistoryItem>()

    private lateinit var adapter: HistoryAdapter
    private lateinit var binding: ActivityHistoryBinding

    private lateinit var filterButton: ImageButton

    private var startDate: Date? = null
    private var endDate: Date? = null

    private val formatWithTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    private val formatWithoutTime = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var pickingField = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        filterButton = findViewById(R.id.filterButton)

        initRecyclerView()

        filterButton.setOnClickListener {
            startDate = null
            endDate = null
            pickingField = 0
            showDatePicker()
        }

        fetchHistory()
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

    private fun fetchHistory() {
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

                allHistoryList.sortBy { parseDateLegacy(it.date)?.time ?: 0L }

                historyList.addAll(allHistoryList)
                adapter.notifyDataSetChanged()
                Log.d("FIREBASE", "Itens carregados: ${historyList.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Erro de leitura: ${error.message}")
            }
        })
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, this, year, month, day).show()
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        val displayed = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)

        if (pickingField == 0) {
            startDate = parseDateLegacy(displayed)
            pickingField = 1
            showDatePicker()
        } else {
            endDate = parseDateLegacy(displayed)
            applyDateFilter()
        }
    }

    private fun parseDateLegacy(dateString: String): Date? {
        return try {
            formatWithTime.parse(dateString)
        } catch (e: ParseException) {
            try {
                formatWithoutTime.parse(dateString)
            } catch (e2: ParseException) {
                null
            }
        }
    }

    private fun applyDateFilter() {
        val start = startDate
        val end = endDate
        if (start == null || end == null) {
            return
        }

        val calendar = Calendar.getInstance()
        calendar.time = end
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endWithTime = calendar.time

        val filtered = allHistoryList.filter { item ->
            val itemDate = parseDateLegacy(item.date)
            itemDate != null && (itemDate >= start && itemDate <= endWithTime)
        }

        historyList.clear()
        historyList.addAll(filtered)
        adapter.notifyDataSetChanged()
    }
}
