package ru.example.dictionary

import android.util.Log
import android.os.Bundle
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var selectedGraphemesContainer: LinearLayout
    private lateinit var buttonGrid: GridLayout
    private val selectedGraphemes = mutableListOf<String>()

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8000")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectedGraphemesContainer = findViewById(R.id.selectedGraphemesContainer)
        buttonGrid = findViewById(R.id.buttonGrid)

        createImageButtons()
    }

    private fun createImageButtons() {
        val screenWidth = resources.displayMetrics.widthPixels
        val imageWidth = (screenWidth * 0.09).toInt()
        val imageMargin = (screenWidth * 0.005).toInt()

        try {
            val assetManager = assets
            val files = assetManager.list("graphems") ?: return

            for (file in files) {
                if (file.endsWith(".png") || file.endsWith(".jpg") || file.endsWith(".jpeg")) {
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

            // Добавляем эффект нажатия
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

        fetchAvailableGraphemes()
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
        Log.d("MainActivity", "Fetching available graphemes")
        lifecycleScope.launch {
            try {
                val response = apiService.getAvailableGraphemes(GraphemeRequest(selectedGraphemes))
                updateButtonGrid(response.available_graphemes)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching graphemes", e)
            }
        }
    }

    private fun updateButtonGrid(availableGraphemes: List<String>) {
        runOnUiThread {
            buttonGrid.removeAllViews()
            val screenWidth = resources.displayMetrics.widthPixels
            val imageWidth = (screenWidth * 0.09).toInt()
            val imageMargin = (screenWidth * 0.005).toInt()

            // Если нет выбранных графем или список доступных графем пуст,
            // показываем исходный набор кнопок
            if (selectedGraphemes.isEmpty() || availableGraphemes.isEmpty()) {
                try {
                    val assetManager = assets
                    val files = assetManager.list("graphems") ?: return@runOnUiThread

                    for (file in files) {
                        if (file.endsWith(".png") || file.endsWith(".jpg") || file.endsWith(".jpeg")) {
                            val imageView = createImageView(file, imageWidth, imageMargin)
                            buttonGrid.addView(imageView)
                        }
                    }
                } catch (e: IOException) {
                    Log.e("MainActivity", "Error loading initial images", e)
                }
            } else {
                // Показываем доступные графемы
                for (grapheme in availableGraphemes) {
                    try {
                        val imageView = createImageView("$grapheme.png", imageWidth, imageMargin)
                        buttonGrid.addView(imageView)
                    } catch (e: IOException) {
                        Log.e("MainActivity", "Error loading image for grapheme: $grapheme", e)
                    }
                }
            }
        }
    }
}