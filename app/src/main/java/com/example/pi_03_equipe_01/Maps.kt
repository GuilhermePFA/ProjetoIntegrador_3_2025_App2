package com.example.pi_03_equipe_01

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.pi_03_equipe_01.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*

class Maps : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Referência do Firebase
        database = FirebaseDatabase.getInstance().getReference("risk")
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        fetchAndDisplayRisks()
    }

    private fun fetchAndDisplayRisks() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (riskSnapshot in snapshot.children) {
                        val title = riskSnapshot.child("title").getValue(String::class.java) ?: "Sem título"
                        val lat = riskSnapshot.child("latitude").getValue(Double::class.java) ?: continue
                        val long = riskSnapshot.child("longitude").getValue(Double::class.java) ?: continue

                        val position = LatLng(lat, long)
                        mMap.addMarker(MarkerOptions().position(position).title(title))
                    }

                    // Foca no primeiro ponto (opcional)
                    val first = snapshot.children.firstOrNull()
                    val lat = first?.child("latitude")?.getValue(Double::class.java)
                    val long = first?.child("longitude")?.getValue(Double::class.java)
                    if (lat != null && long != null) {
                        val firstPosition = LatLng(lat, long)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstPosition, 14f))
                    }

                } else {
                    Log.d("MAPS", "Nenhum risco encontrado")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MAPS", "Erro na leitura do banco: ${error.message}")
            }
        })
    }
}
