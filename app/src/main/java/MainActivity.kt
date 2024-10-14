package ru.example.dictionary

import android.os.Bundle
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

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

        for (i in 1..444) {
            val imageView = ImageView(this).apply {
                val resourceId = resources.getIdentifier("grapheme_${String.format("%03d", i)}", "drawable", packageName)
                setImageResource(resourceId)
                setOnClickListener { onImageClick(i) }
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

    private fun onImageClick(number: Int) {
        val currentText = resultEditText.text.toString()
        resultEditText.setText(currentText + number.toString())
    }
}