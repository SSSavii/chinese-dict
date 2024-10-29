package ru.example.dictionary

import android.util.Log
import android.os.Bundle
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var resultEditText: EditText
    private lateinit var buttonGrid: GridLayout
    private val selectedGraphemes = mutableListOf<String>()

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8000") // Замените на URL вашего сервера
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultEditText = findViewById(R.id.resultEditText)
        buttonGrid = findViewById(R.id.buttonGrid)

        createImageButtons()
    }

    private fun createImageButtons() {
        val screenWidth = resources.displayMetrics.widthPixels
        val imageWidth = (screenWidth * 0.09).toInt() // 9% of screen width
        val imageMargin = (screenWidth * 0.005).toInt() // 0.5% of screen width

        try {
            val assetManager = assets
            val files = assetManager.list("graphems") ?: return

            for (file in files) {
                if (file.endsWith(".png") || file.endsWith(".jpg") || file.endsWith(".jpeg")) {
                    val imageView = ImageView(this).apply {
                        setImageBitmap(assetManager.open("graphems/$file").use {
                            android.graphics.BitmapFactory.decodeStream(it)
                        })
                        setOnClickListener { onImageClick(file) }
                    }

                    val params = GridLayout.LayoutParams().apply {
                        width = imageWidth
                        height = imageWidth // Make it square
                        setMargins(imageMargin, imageMargin, imageMargin, imageMargin)
                    }
                    imageView.layoutParams = params

                    buttonGrid.addView(imageView)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun onImageClick(fileName: String) {
        val grapheme = fileName.removeSuffix(".png")
        Log.d("MainActivity", "Clicked grapheme: $grapheme")

        if (selectedGraphemes.contains(grapheme)) {
            // Если графема уже выбрана, удаляем её
            selectedGraphemes.remove(grapheme)
        } else {
            // Иначе добавляем
            selectedGraphemes.add(grapheme)
        }

        Log.d("MainActivity", "Current selected graphemes: $selectedGraphemes")
        updateResultEditText()
        fetchAvailableGraphemes()
    }

    private fun updateResultEditText() {
        val text = selectedGraphemes.joinToString(" ")
        Log.d("MainActivity", "Updating EditText with: $text")
        resultEditText.setText(text)
    }

    private fun fetchAvailableGraphemes() {
        Log.d("MainActivity", "Fetching available graphemes")
        lifecycleScope.launch {
            try {
                val response = apiService.getAvailableGraphemes(GraphemeRequest(selectedGraphemes))
                Log.d("MainActivity", "Received response: ${response.available_graphemes}")
                updateButtonGrid(response.available_graphemes)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching graphemes", e)
                // Показать сообщение об ошибке пользователю
            }
        }
    }

    private fun updateButtonGrid(availableGraphemes: List<String>) {
        Log.d("MainActivity", "Updating button grid with: $availableGraphemes")
        runOnUiThread {
            buttonGrid.removeAllViews()
            val screenWidth = resources.displayMetrics.widthPixels
            val imageWidth = (screenWidth * 0.09).toInt()
            val imageMargin = (screenWidth * 0.005).toInt()


            for (grapheme in availableGraphemes) {
                try {
                    val imageView = ImageView(this).apply {
                        setImageBitmap(assets.open("graphems/$grapheme.png").use {
                            android.graphics.BitmapFactory.decodeStream(it)
                        })
                        setOnClickListener { onImageClick("$grapheme.png") }
                    }

                    val params = GridLayout.LayoutParams().apply {
                        width = imageWidth
                        height = imageWidth
                        setMargins(imageMargin, imageMargin, imageMargin, imageMargin)
                    }
                    imageView.layoutParams = params

                    buttonGrid.addView(imageView)
                } catch (e: IOException) {
                    Log.e("MainActivity", "Error loading image for grapheme: $grapheme", e)
                }
            }
            Log.d("MainActivity", "Button grid updated")
        }
    }
}