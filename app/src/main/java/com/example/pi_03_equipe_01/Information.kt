package com.example.pi_03_equipe_01

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.pi_03_equipe_01.databinding.ActivityInformationsBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class Information : AppCompatActivity() {
    private lateinit var binding:ActivityInformationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInformationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val historyId = intent.getStringExtra("HISTORY_ID") ?: return
        val ref = FirebaseDatabase.getInstance().getReference("risk").child(historyId)
        Log.d("FIREBASE", "Id: ${historyId}")
        backToHistory()
        fetchInformationByID(historyId)
    }
    private fun backToHistory(){
        binding.button.setOnClickListener{
            val intent = Intent(this, History::class.java)
            startActivity(intent)
        }

        }
    private fun fetchInformationByID(id:String){
        val ref = FirebaseDatabase.getInstance().getReference("risk").child(id)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val riskID = snapshot.child("riskID").getValue(String::class.java) ?: ""
                    val description = snapshot.child("description").getValue(String::class.java) ?: ""
                    val name = snapshot.child("title").getValue(String::class.java) ?: ""
                    var date = snapshot.child("created_at").getValue(String::class.java) ?: ""
                    val lat = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val long = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                      var localizacao = "Lat: $lat, Long: $long"
                    date = date.format(99/99/99)
                    var status = HistoryItem.Status.NAO_INICIADO.texto
                    var statusimg = ContextCompat.getDrawable(this@Information, R.drawable.ic_circle_gray)
                    val statusStr =
                        snapshot.child("status").getValue(String::class.java) ?: "NAO_INICIADO"
                    if (statusStr=="Finalizado"){
                        val statusName = "Finalizado"
                         status = HistoryItem.Status.FINALIZADO.texto
                        statusimg = ContextCompat.getDrawable(this@Information, R.drawable.ic_circle_green)

                    }
                    else if (statusStr == "Em andamento"){
                        val statusName = "Em andamento"
                        status = HistoryItem.Status.EM_ANDAMENTO.texto
                        statusimg = ContextCompat.getDrawable(this@Information, R.drawable.ic_circle_orange)

                    }
                    else {
                        val statusName= "Não Inciado"
                             status = HistoryItem.Status.NAO_INICIADO.texto
                    }


                    try {

                        val riskid = riskID
                        val descriptionView = description
                        val nameView = name
                        binding.txtIdValor.text = riskid
                        binding.riskTextView.text = nameView
                        binding.txtDataValor.text = date
                        binding.txtStatusValor.text = status
                        binding.txtLocalizacao.text = localizacao
                        binding.statusDotInformations.setImageDrawable(statusimg)
                        binding.descriptionTextView.text = descriptionView
                    } catch (e: Exception) {
                        Log.e("CONSULTA", "Erro ao interpretar o status", e)
                    }
                } else {
                    Log.d("CONSULTA", "Nenhum item encontrado com ID: $id")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CONSULTA", "Erro na consulta: ${error.message}")
            }
        })
    }

}