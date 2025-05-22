package com.example.pi_03_equipe_01

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
import android.view.MotionEvent
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.pi_03_equipe_01.databinding.ActivitySignUpBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUp : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private val auth = FirebaseAuth.getInstance()
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyPhoneMask(binding.signUpPhoneEditText)

        binding.signUpBtn.setOnClickListener { view ->
            val name = binding.signUpFullNameEditText.text.toString().trim()
            val phone = binding.signUpPhoneEditText.text.toString().trim()
            val email = binding.signUpEmailEditText.text.toString().trim()
            val password = binding.signUpPasswordEditText.text.toString()

            val phoneDigits = phone.replace(Regex("[^\\d]"), "")

            when {
                name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                    showSnackbar(view, "Preencha todos os campos!", Color.RED)
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    showSnackbar(view, "E-mail inválido!", Color.RED)
                }
                password.length < 6 -> {
                    showSnackbar(view, "A senha deve ter pelo menos 6 caracteres!", Color.RED)
                }
                phoneDigits.length !in 10..11 -> {
                    showSnackbar(view, "Número de telefone inválido!", Color.RED)
                }
                else -> {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid
                                saveUserInDatabase(userId, name, phone, email)
                                clearFields()
                                showSnackbar(view, "Sucesso ao cadastrar usuário!", Color.GREEN)

                                startActivity(Intent(this, Risk::class.java).apply {
                                    putExtra("USER_ID", userId)
                                })
                            }
                        }
                        .addOnFailureListener { exception ->
                            showSnackbar(view, "Erro: ${exception.message}", Color.RED)
                        }
                }
            }
        }

        binding.signUpLoginText1.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }

        @Suppress("ClickableViewAccessibility")
        binding.signUpPasswordEditText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.signUpPasswordEditText.compoundDrawablesRelative[2]
                drawableEnd?.let {
                    val touchAreaStart = binding.signUpPasswordEditText.width -
                            binding.signUpPasswordEditText.paddingEnd -
                            it.intrinsicWidth
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

    private fun saveUserInDatabase(userId: String?, name: String, phone: String, email: String) {
        val ref = FirebaseDatabase.getInstance().getReference("user")
        val userInfo = mapOf(
            "userId" to userId,
            "name" to name,
            "phone" to phone,
            "email" to email,
            "role" to "User"
        )
        userId?.let {
            ref.child(it).setValue(userInfo)
        }
    }

    private fun clearFields() {
        binding.signUpFullNameEditText.setText("")
        binding.signUpPhoneEditText.setText("")
        binding.signUpEmailEditText.setText("")
        binding.signUpPasswordEditText.setText("")
    }

    private fun togglePasswordVisibility(visible: Boolean) {
        binding.signUpPasswordEditText.inputType = if (visible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val icon = if (visible) R.drawable.password_eye_off_24 else R.drawable.password_eye_24
        binding.signUpPasswordEditText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, icon, 0)
        binding.signUpPasswordEditText.setSelection(binding.signUpPasswordEditText.text.length)
    }

    private fun applyPhoneMask(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                val unmasked = s.toString().replace(Regex("[^\\d]"), "")
                val mask = if (unmasked.length > 10) "(##) #####-####" else "(##) ####-####"
                val masked = StringBuilder()

                var i = 0
                for (m in mask) {
                    if (m == '#') {
                        if (i < unmasked.length) masked.append(unmasked[i++])
                        else break
                    } else {
                        if (i < unmasked.length) masked.append(m)
                        else break
                    }
                }

                isUpdating = true
                editText.setText(masked.toString())
                editText.setSelection(masked.length)
                isUpdating = false
            }
        })
    }

    private fun showSnackbar(view: android.view.View, message: String, color: Int) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(color)
            .show()
    }
}
