package ru.example.dictionary

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class TranslationActivity : AppCompatActivity() {
    private lateinit var characterTextView: TextView
    private lateinit var pinyinTextView: TextView
    private lateinit var meaningsTextView: TextView
    private lateinit var backButton: Button

    private val apiService = RetrofitClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translation)

        initializeViews()
        setupListeners()

        val character = intent.getStringExtra("character") ?: return
        characterTextView.text = character
        loadTranslation(character)
    }

    private fun initializeViews() {
        characterTextView = findViewById(R.id.characterTextView)
        pinyinTextView = findViewById(R.id.pinyinTextView)
        meaningsTextView = findViewById(R.id.meaningsTextView)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadTranslation(character: String) {
        lifecycleScope.launch {
            try {
                val request = TranslationRequest(text = character)
                val response = apiService.getTranslation(request)
                if (response.tokens.isNotEmpty()) {
                    displayTranslation(response.tokens)
                } else {
                    displayError("Перевод не найден")
                }
            } catch (e: Exception) {
                Log.e("TranslationActivity", "Error loading translation", e)
                val errorMessage = when (e) {
                    is retrofit2.HttpException -> {
                        val errorBody = e.response()?.errorBody()?.string()
                        Log.e("TranslationActivity", "Server error response: $errorBody")
                        "Ошибка сервера: ${errorBody ?: e.message()}"
                    }
                    else -> "Ошибка: ${e.message}"
                }
                displayError(errorMessage)
            }
        }
    }

    private fun displayTranslation(tokens: List<TokenDetail>) {
        runOnUiThread {
            if (tokens.isNotEmpty()) {
                val token = tokens[0]
                pinyinTextView.text = token.pinyin
                meaningsTextView.text = token.meanings.joinToString("\n") { "• $it" }
            }
        }
    }

    private fun displayError(message: String) {
        runOnUiThread {
            pinyinTextView.text = ""
            meaningsTextView.text = message
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}