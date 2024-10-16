package ru.example.dictionary

import android.os.Bundle
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var resultEditText: EditText
    private lateinit var buttonGrid: GridLayout

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
        val currentText = resultEditText.text.toString()
        resultEditText.setText(currentText + fileName)
    }
}