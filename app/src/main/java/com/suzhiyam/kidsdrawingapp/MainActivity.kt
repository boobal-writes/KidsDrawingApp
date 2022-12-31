package com.suzhiyam.kidsdrawingapp

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.get

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var brushButton: ImageButton? = null
    private var selectedColorButton: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        brushButton = findViewById(R.id.brush)
        drawingView!!.setBrushThickness(20.toFloat())

        val linearLayout = findViewById<LinearLayout>(R.id.colors)
        selectedColorButton = linearLayout[0] as ImageButton

        selectedColorButton!!.setImageDrawable(
            ContextCompat.getDrawable(
                this, R.drawable.pallet_pressed
            )
        )

        brushButton!!.setOnClickListener {
            showBrushSizeDialog()
        }
    }

    fun showBrushSizeDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_brush_size)
        dialog.setTitle("Brush Size: ")
        val buttons = mapOf<ImageButton, Float>(
            dialog.findViewById<ImageButton>(R.id.small_brush) to 10.toFloat(),
            dialog.findViewById<ImageButton>(R.id.medium_brush) to 20.toFloat(),
            dialog.findViewById<ImageButton>(R.id.large_brush) to 30.toFloat()
        )
        buttons.forEach { imageButtonBrushSizeEntry ->
            imageButtonBrushSizeEntry.key.setOnClickListener {
                drawingView!!.setBrushThickness(imageButtonBrushSizeEntry.value)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    fun paintClicked(view: View) {
        if (view is ImageButton && view != selectedColorButton) {
            val newColor = view.tag.toString()
            drawingView!!.setColor(newColor)

            view.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            selectedColorButton!!.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                )
            )
            selectedColorButton = view
        }
    }
}