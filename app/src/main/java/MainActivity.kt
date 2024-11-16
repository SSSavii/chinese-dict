package ru.example.dictionary

import android.widget.Toast
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var selectedGraphemesContainer: LinearLayout
    private lateinit var buttonGrid: GridLayout
    private lateinit var characterInput: EditText
    private lateinit var confirmGraphemesButton: ImageButton
    private lateinit var translateButton: Button
    private val selectedGraphemes = mutableListOf<String>()

    private val apiService = RetrofitClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupListeners()
        createImageButtons()
    }

    private fun initializeViews() {
        selectedGraphemesContainer = findViewById(R.id.selectedGraphemesContainer)
        buttonGrid = findViewById(R.id.buttonGrid)
        characterInput = findViewById(R.id.characterInput)
        confirmGraphemesButton = findViewById(R.id.confirmGraphemesButton)
        translateButton = findViewById(R.id.translateButton)
    }

    private fun setupListeners() {
        confirmGraphemesButton.setOnClickListener {
            confirmGraphemes()
        }

        translateButton.setOnClickListener {
            val character = characterInput.text.toString()
            if (character.isNotEmpty()) {
                startTranslationActivity(character)
            } else {
                Toast.makeText(this, "Введите иероглиф", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startTranslationActivity(character: String) {
        val intent = Intent(this, TranslationActivity::class.java)
        intent.putExtra("character", character)
        startActivity(intent)
    }

    private fun createImageButtons() {
        val screenWidth = resources.displayMetrics.widthPixels
        val imageWidth = (screenWidth * 0.09).toInt()
        val imageMargin = (screenWidth * 0.005).toInt()

        try {
            val assetManager = assets
            val files = assetManager.list("graphems") ?: return

            for (file in files) {
                if (file.endsWith(".png")) {
                    val imageView = createImageView(file, imageWidth, imageMargin)
                    buttonGrid.addView(imageView)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun createImageView(fileName: String, width: Int, margin: Int): ImageView {
        return ImageView(this).apply {
            setImageBitmap(assets.open("graphems/$fileName").use {
                android.graphics.BitmapFactory.decodeStream(it)
            })
            setOnClickListener { onImageClick(fileName) }

            layoutParams = GridLayout.LayoutParams().apply {
                this.width = width
                this.height = width
                (this as MarginLayoutParams).setMargins(margin, margin, margin, margin)
            }

            isClickable = true
            isFocusable = true
            background = ResourcesCompat.getDrawable(resources, R.drawable.button_background, null)
        }
    }

    private fun onImageClick(fileName: String) {
        val grapheme = fileName.removeSuffix(".png")
        Log.d("MainActivity", "Clicked grapheme: $grapheme")

        if (selectedGraphemes.contains(grapheme)) {
            selectedGraphemes.remove(grapheme)
            updateSelectedGraphemesView()
        } else {
            selectedGraphemes.add(grapheme)
            updateSelectedGraphemesView()
        }

        if (selectedGraphemes.isNotEmpty()) {
            fetchAvailableGraphemes()
        } else {
            createImageButtons()
        }
    }

    private fun updateSelectedGraphemesView() {
        selectedGraphemesContainer.removeAllViews()

        val screenWidth = resources.displayMetrics.widthPixels
        val imageWidth = (screenWidth * 0.09).toInt()
        val margin = (resources.displayMetrics.density * 4).toInt()

        for (grapheme in selectedGraphemes) {
            val imageView = ImageView(this).apply {
                setImageBitmap(assets.open("graphems/$grapheme.png").use {
                    android.graphics.BitmapFactory.decodeStream(it)
                })

                layoutParams = LinearLayout.LayoutParams(imageWidth, imageWidth).apply {
                    setMargins(margin, margin, margin, margin)
                }

                setOnClickListener {
                    animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction {
                            selectedGraphemes.remove(grapheme)
                            updateSelectedGraphemesView()
                            fetchAvailableGraphemes()
                        }
                        .start()
                }

                alpha = 0f
                animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            selectedGraphemesContainer.addView(imageView)
        }
    }

    private fun fetchAvailableGraphemes() {
        if (selectedGraphemes.isEmpty()) {
            createImageButtons()
            return
        }

        lifecycleScope.launch {
            try {
                val request = GraphemeRequest(selectedGraphemes)
                val response = apiService.getAvailableGraphemes(request)
                updateButtonGrid(response.available_graphemes)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching graphemes", e)
                Toast.makeText(
                    this@MainActivity,
                    "Ошибка при получении графем: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmGraphemes() {
        lifecycleScope.launch {
            try {
                val randomHieroglyph = apiService.getRandomHieroglyph()
                val request = GraphemeRequest(selectedGraphemes)

                val response = apiService.confirmGraphemes(
                    hieroglyph = randomHieroglyph,
                    selected_graphemes = request
                )

                if (response.confirm) {
                    characterInput.setText(randomHieroglyph)
                    selectedGraphemes.clear()
                    updateSelectedGraphemesView()
                    createImageButtons()
                    Toast.makeText(this@MainActivity, "Графемы подтверждены", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Неверная комбинация графем", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun handleError(e: Exception) {
        Log.e("MainActivity", "Error occurred", e)
        val errorMessage = when (e) {
            is retrofit2.HttpException -> {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("MainActivity", "Server error response: $errorBody")
                "Ошибка сервера: ${errorBody ?: e.message()}"
            }
            else -> "Ошибка: ${e.message}"
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun updateButtonGrid(availableGraphemes: List<String>) {
        runOnUiThread {
            buttonGrid.removeAllViews()
            val screenWidth = resources.displayMetrics.widthPixels
            val imageWidth = (screenWidth * 0.09).toInt()
            val imageMargin = (screenWidth * 0.005).toInt()

            if (availableGraphemes.isEmpty()) {
                createImageButtons()
            } else {
                for (grapheme in availableGraphemes) {
                    try {
                        val imageView = createImageView("$grapheme.png", imageWidth, imageMargin)
                        buttonGrid.addView(imageView)
                    } catch (e: IOException) {
                        Log.e("MainActivity", "Error creating image view for grapheme: $grapheme", e)
                    }
                }
            }
        }
    }
}