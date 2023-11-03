package com.abdurrahmanbulut.textrecognizer.gallery

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb


class GalleryActivity : ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ibanRegex = intent.getStringExtra("ibanRegex") ?: """TR\d{24}"""

        val defaultColor = Color.White.toArgb()
        val colorInt = intent?.getIntExtra("appBarColorRGB", defaultColor) ?: defaultColor
        val color = Color(colorInt)

        viewModel.regex = ibanRegex
        openGallery()

        setContent {
            val isIbanCaptured = viewModel.isIbanCaptured

            if (isIbanCaptured) {
                GalleryScreen(viewModel.iban, viewModel.originalImage.value, appBarBackgroundColor = color)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        observeIbanEvents()
    }

    private fun observeIbanEvents() {
        viewModel.ibanEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let { iban ->

                if (viewModel.iban.isEmpty()) {
                    viewModel.isIbanCaptured = false
                    returnWithResult()
                } else {
                    viewModel.isIbanCaptured = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        returnWithResult(iban = viewModel.iban, resultCode = "1")
                    }, 2000)
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryResultLauncher.launch(intent)
    }

    private val galleryResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK && result.data != null) {

                val imageUri = result.data?.data

                if (imageUri != null) {
                    viewModel.originalImage.value = imageUri
                    viewModel.startCrop(imageUri, this, cropImageResultLauncher)
                } else {
                    returnWithResult()
                }
            } else {
                returnWithResult()
            }
        }

    private val cropImageResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK && result.data != null) {

                val extras = result.data?.extras
                val selectedBitmap = extras?.getParcelable<Bitmap>("data")
                viewModel.croppedImage.value = selectedBitmap

                if (selectedBitmap != null) {
                    viewModel.processGalleryImage(selectedBitmap)
                } else {
                    returnWithResult()
                }

            } else {
                returnWithResult()
            }
        }

    override fun onBackPressed() {
        super.onBackPressedDispatcher.onBackPressed()

        if (viewModel.iban.isEmpty()) {
            returnWithResult()

        } else {
            returnWithResult(iban = viewModel.iban, resultCode = "1")
        }
    }
    private fun returnWithResult(iban: String? = null, resultCode: String = "0") {
        val resultIntent = Intent().apply {
            iban?.let { putExtra("IBAN", it) }
            putExtra("resultCode", resultCode)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
