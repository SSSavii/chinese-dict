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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var selectedGraphemesContainer: LinearLayout
    private lateinit var buttonGrid: GridLayout
    private lateinit var characterInput: EditText
    private lateinit var confirmGraphemesButton: ImageButton
    private lateinit var translateButton: Button
    private lateinit var clearGraphemesButton: ImageButton
    private lateinit var exitButton: Button

    private val selectedGraphemes = mutableListOf<String>()
    private var isDeleting = false
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
        clearGraphemesButton = findViewById(R.id.clearGraphemesButton)
        exitButton = findViewById(R.id.exitButton)
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

        clearGraphemesButton.setOnClickListener {
            selectedGraphemes.clear()
            updateSelectedGraphemesView()
            createImageButtons()
        }

        confirmGraphemesButton.setOnClickListener {
            if (selectedGraphemes.isNotEmpty()) {
                checkHieroglyphExists()
            } else {
                Toast.makeText(this, "Выберите графемы", Toast.LENGTH_SHORT).show()
            }
        }

        exitButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Подтверждение")
                .setMessage("Вы действительно хотите выйти?")
                .setPositiveButton("Да") { _, _ -> finish() }
                .setNegativeButton("Нет", null)
                .show()
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
            assets.list("graphems")?.forEach { file ->
                if (file.endsWith(".png")) {
                    val imageView = createImageView(file, imageWidth, imageMargin)
                    buttonGrid.addView(imageView)
                }
            }
        } catch (e: IOException) {
            Log.e("MainActivity", "Error loading graphemes", e)
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
        selectedGraphemes.add(grapheme)
        updateSelectedGraphemesView()
        fetchAvailableGraphemes()
    }

    private fun updateSelectedGraphemesView() {
        selectedGraphemesContainer.removeAllViews()

        val screenWidth = resources.displayMetrics.widthPixels
        val imageWidth = (screenWidth * 0.09).toInt()
        val margin = (resources.displayMetrics.density * 4).toInt()

        selectedGraphemes.forEachIndexed { index, grapheme ->
            val imageView = ImageView(this).apply {
                try {
                    setImageBitmap(assets.open("graphems/$grapheme.png").use {
                        android.graphics.BitmapFactory.decodeStream(it)
                    })
                } catch (e: IOException) {
                    Log.e("MainActivity", "Error loading grapheme image: $grapheme", e)
                    return@apply
                }

                layoutParams = LinearLayout.LayoutParams(imageWidth, imageWidth).apply {
                    setMargins(margin, margin, margin, margin)
                }

                setOnClickListener {
                    isDeleting = true
                    selectedGraphemes.removeAt(index)
                    updateSelectedGraphemesView()

                    if (selectedGraphemes.isEmpty()) {
                        createImageButtons()
                    } else {
                        val currentGraphemes = selectedGraphemes.toList()
                        selectedGraphemes.clear()
                        createImageButtons()
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
        lifecycleScope.launch {
            try {
                val response = apiService.getAvailableGraphemes(GraphemeRequest(selectedGraphemes))

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
                                Log.e("MainActivity", "Error creating image view: $grapheme", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching graphemes", e)
                runOnUiThread { createImageButtons() }
            }
        }
    }

    private fun checkHieroglyphExists() {
        lifecycleScope.launch {
            try {
                val response = apiService.getHieroglyph(GraphemeRequest(selectedGraphemes))

                runOnUiThread {
                    val currentText = characterInput.text.toString()
                    characterInput.setText(
                        if (currentText.isEmpty()) response.hieroglyph
                        else "$currentText ${response.hieroglyph}"
                    )

                    selectedGraphemes.clear()
                    updateSelectedGraphemesView()
                    createImageButtons()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking hieroglyph", e)
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Иероглиф не найден: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}