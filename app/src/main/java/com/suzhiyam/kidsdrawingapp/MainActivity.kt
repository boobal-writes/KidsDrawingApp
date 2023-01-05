package com.suzhiyam.kidsdrawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK && it.data != null) {
                findViewById<ImageView>(R.id.backgroundImage).setImageURI(it.data?.data)
            }
        }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionGrants ->
            permissionGrants.entries.forEach {
                if (it.value) {
                    Toast.makeText(
                        this,
                        "Permission is granted now. You can read the storage files!",
                        Toast.LENGTH_LONG
                    ).show()
                    val pickImageIntent = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                    openGalleryLauncher.launch(pickImageIntent)
                } else {
                    if (it.key == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            "You just denied the permission!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    private var drawingView: DrawingView? = null
    private var brushButton: ImageButton? = null
    private var selectedColorButton: ImageButton? = null
    private var selectImageButton: ImageButton? = null
    private var undoImageButton: ImageButton? = null
    private var saveImageButton: ImageButton? = null
    private var progressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        brushButton = findViewById(R.id.brush)
        selectImageButton = findViewById(R.id.addImage)
        undoImageButton = findViewById(R.id.undo)
        saveImageButton = findViewById(R.id.save)
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

        selectImageButton!!.setOnClickListener {
            requestStoragePermissions()
        }

        undoImageButton!!.setOnClickListener {
            drawingView!!.onClickUndo()
        }
        saveImageButton!!.setOnClickListener {
            if (isReadStorageAllowed()) {
                showProgressDialog()
                lifecycleScope.launch {
                    val frame = findViewById<FrameLayout>(R.id.frame)
                    saveImage(getBitmapFormView(frame))
                }
            }
        }

    }

    private fun getBitmapFormView(view: View): Bitmap {
        val bitmapToBeReturned =
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapToBeReturned)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return bitmapToBeReturned
    }

    private fun requestStoragePermissions() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showRationaleDialog("Kids Drawing App", "App needs to access external storage")
        } else {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title).setMessage(message).setPositiveButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun showBrushSizeDialog() {
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

    private suspend fun saveImage(bitmap: Bitmap?) {
        withContext(Dispatchers.IO) {
            if (bitmap != null) {
                try {
                    val bitmapOutputSteam = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bitmapOutputSteam)

                    val fileName = listOf(
                        externalCacheDir?.absoluteFile.toString(),
                        "KDA_${System.currentTimeMillis() / 1000}.png"
                    ).joinToString(File.separator)
                    val file = File(fileName)
                    val fileOutputStream = FileOutputStream(file)
                    fileOutputStream.write(bitmapOutputSteam.toByteArray())
                    fileOutputStream.close()
                    bitmapOutputSteam.close()

                    runOnUiThread {
                        cancelProgressDialog()
                        Toast.makeText(
                            this@MainActivity,
                            "Image saved successfully at ${file.absolutePath}!",
                            Toast.LENGTH_LONG
                        ).show()
                        shareImage(file.absolutePath)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Oops! Something went wrong!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun shareImage(filePath: String) {
        MediaScannerConnection.scanFile(this, arrayOf(filePath), null) { path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun cancelProgressDialog() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    private fun showProgressDialog() {
        progressDialog = Dialog(this)
        progressDialog!!.setContentView(R.layout.dialog_custom_progress)
        progressDialog!!.show()
    }
}