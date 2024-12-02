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
    private var isDeleting = false
/*Рад, что удалось решить проблему! Давайте я объясню, почему это решение работает:

1. Флаг `isDeleting` помогает различать между добавлением новой графемы и удалением существующей, что предотвращает нежелательные состояния клавиатуры.

2. При удалении графемы происходит полный сброс состояния и последовательное восстановление оставшихся графем, что гарантирует правильный порядок и набор доступных графем.

3. Последовательное добавление графем заново (`currentGraphemes.forEach`) обеспечивает корректное обновление состояния клавиатуры в соответствии с логикой сервера*/
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
        translateButton.setOnClickListener {
            val character = characterInput.text.toString()
            if (character.isNotEmpty()) {
                startTranslationActivity(character)
            } else {
                Toast.makeText(this, "Введите иероглиф", Toast.LENGTH_SHORT).show()
            }
        }

        // Обработчик для кнопки подтверждения графем
        confirmGraphemesButton.setOnClickListener {
            if (selectedGraphemes.isNotEmpty()) {
                checkHieroglyphExists()
            } else {
                Toast.makeText(this, "Выберите графемы", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun startTranslationActivity(character: String) {
        val intent = Intent(this, TranslationActivity::class.java)
        intent.putExtra("character", character)
        startActivity(intent)
    }

    private fun createImageButtons() {
        buttonGrid.removeAllViews()
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
        if (isDeleting) {
            isDeleting = false
            createImageButtons()
            return
        }

        val grapheme = fileName.removeSuffix(".png")
        Log.d("MainActivity", "Clicked grapheme: $grapheme")

        selectedGraphemes.add(grapheme)
        updateSelectedGraphemesView()
        fetchAvailableGraphemes()
    }


    private fun updateSelectedGraphemesView() {
        selectedGraphemesContainer.removeAllViews()

        val screenWidth = resources.displayMetrics.widthPixels
        val imageWidth = (screenWidth * 0.09).toInt()
        val margin = (resources.displayMetrics.density * 4).toInt()

        val graphemeIndices = selectedGraphemes.withIndex().toList()

        for ((index, grapheme) in graphemeIndices) {
            val imageView = ImageView(this).apply {
                setImageBitmap(assets.open("graphems/$grapheme.png").use {
                    android.graphics.BitmapFactory.decodeStream(it)
                })

                layoutParams = LinearLayout.LayoutParams(imageWidth, imageWidth).apply {
                    setMargins(margin, margin, margin, margin)
                }

                setOnClickListener {
                    isDeleting = true
                    selectedGraphemes.removeAt(index)
                    updateSelectedGraphemesView()

                    // Полностью сбрасываем состояние клавиатуры
                    if (selectedGraphemes.isEmpty()) {
                        createImageButtons()
                    } else {
                        // Заново запрашиваем доступные графемы с начала
                        val currentGraphemes = selectedGraphemes.toList()
                        selectedGraphemes.clear()
                        createImageButtons()

                        // Последовательно добавляем графемы заново
                        currentGraphemes.forEach { g ->
                            selectedGraphemes.add(g)
                            fetchAvailableGraphemes()
                        }
                    }
                }
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

                runOnUiThread {
                    buttonGrid.removeAllViews()
                    if (response.available_graphemes.isEmpty()) {
                        createImageButtons()
                    } else {
                        val screenWidth = resources.displayMetrics.widthPixels
                        val imageWidth = (screenWidth * 0.09).toInt()
                        val imageMargin = (screenWidth * 0.005).toInt()

                        response.available_graphemes.forEach { grapheme ->
                            try {
                                val imageView = createImageView("$grapheme.png", imageWidth, imageMargin)
                                buttonGrid.addView(imageView)
                            } catch (e: IOException) {
                                Log.e("MainActivity", "Error creating image view for grapheme: $grapheme", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching available graphemes", e)
                runOnUiThread {
                    createImageButtons()
                }
            }
        }
    }

    private fun checkHieroglyphExists() {
        lifecycleScope.launch {
            try {
                val request = GraphemeRequest(selectedGraphemes)

                // Затем пытаемся получить иероглиф
                val response = apiService.getHieroglyph(request)

                characterInput.setText(response.hieroglyph)

                Toast.makeText(
                    this@MainActivity,
                    "Иероглиф найден: ${response.hieroglyph}",
                    Toast.LENGTH_SHORT
                ).show()

                selectedGraphemes.clear()
                updateSelectedGraphemesView()
                createImageButtons()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking hieroglyph", e)

                val errorMessage = when (e) {
                    is retrofit2.HttpException -> {
                        val errorBody = e.response()?.errorBody()?.string()
                        Log.e("MainActivity", "Server error response: $errorBody")
                        "Иероглиф не найден: ${errorBody ?: e.message()}"
                    }
                    else -> "Ошибка: ${e.message}"
                }

                Toast.makeText(
                    this@MainActivity,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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
                // Используем Set для исключения дубликатов
                val uniqueGraphemes = availableGraphemes.toSet()
                for (grapheme in uniqueGraphemes) {
                    try {
                        val imageView = createImageView("$grapheme.png", imageWidth, imageMargin)
                        buttonGrid.addView(imageView)
                    } catch (e: IOException) {
                        Log.e(
                            "MainActivity",
                            "Error creating image view for grapheme: $grapheme",
                            e
                        )
                    }
                }
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
}