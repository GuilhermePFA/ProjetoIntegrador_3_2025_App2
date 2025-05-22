package com.example.pi_03_equipe_01
import androidx.drawerlayout.widget.DrawerLayout
import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Base64
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.example.pi_03_equipe_01.databinding.ActivityRiskBinding
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class Risk : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()

    private lateinit var binding: ActivityRiskBinding
    private var imageBase64Firebase: String? = null
    private var userLatitude: Double? = null
    private var userLongitude: Double? = null

    private val PERM_LOCATION = 100
    private val PERM_CAMERA = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Menu lateral
        binding.menuButton.setOnClickListener {
            hideKeyboard()
            binding.main.openDrawer(GravityCompat.START)
        }

        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_history -> startActivity(Intent(this, History::class.java)).also { finish() }
                R.id.nav_sair -> finishAffinity()
            }
            binding.main.closeDrawer(GravityCompat.START)
            true
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.riskLocEditText.apply {
            inputType = InputType.TYPE_NULL
            setOnClickListener { requestLocation(fusedLocationClient) }
        }

        binding.riskAnexEditIcon.setOnClickListener {
            hideKeyboard()
            AlertDialog.Builder(this)
                .setTitle("Imagem")
                .setMessage("Deseja tirar uma foto?")
                .setPositiveButton("Sim") { _, _ -> checkCameraPermission() }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.riskSent.setOnClickListener { view ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                showSnackbar(view, "Usuário não autenticado!", false)
                return@setOnClickListener
            }

            val localizacao = binding.riskLocEditText.text.toString()
            val pic = imageBase64Firebase ?: ""
            val loc = binding.riskLocEditText.text.toString()
            val tipo = binding.riskTypeEditText.text.toString()
            val desc = binding.riskDescEditText.text.toString()

            if (pic.isBlank() || loc.isBlank() || tipo.isBlank() || desc.isBlank()) {
                showSnackbar(view, "Preencha todos os campos!", false)
                return@setOnClickListener
            }

            val dateNow = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
            val riskId = (10000..99999).random().toString()

            val data = mapOf(
                "riskID" to riskId,
                "created_at" to dateNow,
                "created_by_userID" to uid,
                "picture" to pic,
                "latitude" to userLatitude,
                "longitude" to userLongitude,
                "title" to tipo,
                "description" to desc,
                "status" to "NAO_INICIADO"
            )

            FirebaseDatabase.getInstance().getReference("risk").child(riskId).setValue(data)
                .addOnSuccessListener {
                    binding.riskTypeEditText.text?.clear()
                    binding.riskDescEditText.text?.clear()
                    binding.riskLocEditText.text = null
                    binding.riskAnexEditIcon.setImageResource(R.drawable.ic_camera)
                    imageBase64Firebase = null

                    showSnackbar(view, "Risco salvo com ID $riskId!", true)
                }
                .addOnFailureListener {
                    showSnackbar(view, "Erro ao salvar: ${it.message}", false)
                }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun requestLocation(fused: com.google.android.gms.location.FusedLocationProviderClient) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERM_LOCATION
            )
            return
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        fused.lastLocation.addOnSuccessListener { loc: Location? ->
            loc?.let {
                userLatitude = it.latitude
                userLongitude = it.longitude
                binding.riskLocEditText.text = "Lat: ${it.latitude}, Long: ${it.longitude}"
            } ?: showSnackbar(binding.root, "Não foi possível obter a localização", false)
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PERM_CAMERA
            )
        } else {
            cameraLauncher.launch(null)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == PERM_CAMERA && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else if (requestCode == PERM_LOCATION && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            requestLocation(LocationServices.getFusedLocationProviderClient(this))
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        bmp?.let {
            binding.riskAnexEditIcon.setImageBitmap(it)
            imageBase64Firebase = bitmapToBase64(it)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun showSnackbar(view: android.view.View, message: String, success: Boolean) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(if (success) Color.GREEN else Color.RED)
            .show()
    }
}
