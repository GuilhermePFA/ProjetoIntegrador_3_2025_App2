package com.example.pi_03_equipe_01

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.pi_03_equipe_01.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString().trim()
            val password = binding.loginPasswordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showSnackbar("Preencha todos os campos!", Color.RED)
            } else {
                realizarLogin(email, password)
            }
        }


        @Suppress("ClickableViewAccessibility")
        binding.loginPasswordEditText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.loginPasswordEditText.compoundDrawablesRelative[2]
                if (drawableEnd != null) {
                    val touchAreaStart = binding.loginPasswordEditText.width -
                            binding.loginPasswordEditText.paddingEnd -
                            drawableEnd.intrinsicWidth
                    if (event.rawX >= touchAreaStart) {
                        v.performClick()
                        isPasswordVisible = !isPasswordVisible
                        togglePasswordVisibility(isPasswordVisible)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }

    private fun realizarLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    authUserByRole(userId, "admin") { autenticado ->
                        if (autenticado) {
                            Log.d("LOGIN", "Usuário logado com sucesso: $userId")
                            val intent = Intent(this, History::class.java)
                            intent.putExtra("USER_ID", userId)
                            startActivity(intent)
                            finish()
                        } else {
                            Log.d("LOGIN", "Usuário não autorizado com role 'admin': $userId")
                            showSnackbar("Você não tem permissão para acessar", Color.RED)
                        }
                    }
                } else {
                    val exception = task.exception
                    val message = when (exception) {
                        is FirebaseAuthInvalidUserException ->
                            "Conta não encontrada. Verifique o e-mail."

                        is FirebaseAuthInvalidCredentialsException ->
                            "E-mail ou senha incorretos."

                        is FirebaseNetworkException ->
                            "Sem conexão com a internet."

                        else ->
                            if (exception?.message?.contains("TOO_MANY_ATTEMPTS_TRY_LATER") == true)
                                "Muitas tentativas. Tente novamente mais tarde."
                            else
                                "Erro ao fazer login: ${exception?.localizedMessage ?: "Erro desconhecido"}"
                    }
                    showSnackbar(message, Color.RED)
                    Log.e("LOGIN", message, exception)
                }
            }
    }


    private fun authUserByRole(userID: String, roleMock: String, callback: (Boolean) -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("user").child(userID)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val role = snapshot.child("role").getValue(String::class.java) ?: ""
                val autenticado = role == roleMock
                callback(autenticado)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CONSULTA", "Erro na consulta: ${error.message}")
                callback(false)
            }
        })
    }


    private fun togglePasswordVisibility(visible: Boolean) {
        val inputType = if (visible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.loginPasswordEditText.inputType = inputType

        val icon = if (visible) R.drawable.password_eye_off_24 else R.drawable.password_eye_24
        binding.loginPasswordEditText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, icon, 0)
        binding.loginPasswordEditText.setSelection(binding.loginPasswordEditText.text?.length ?: 0)
    }

    private fun showSnackbar(message: String, color: Int) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(color)
            .show()
    }
}
