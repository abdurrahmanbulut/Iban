package com.abdurrahmanbulut.textrecognizer.scan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ScannerActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private val viewModel: ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ibanRegex = intent.getStringExtra("ibanRegex") ?: """TR\d{24}"""

        val defaultColor = Color.White.toArgb()
        val colorInt = intent?.getIntExtra("appBarColorRGB", defaultColor) ?: defaultColor
        val color = Color(colorInt)

        viewModel.appBarBackgroundColor = color
        viewModel.regex = ibanRegex

        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            ScannerActivityContent(viewModel) {
                returnWithResult(iban = viewModel.iban)
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
                    returnWithResult()
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        returnWithResult(iban = viewModel.iban, resultCode = "1")
                    }, 2000)
                }
            }
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
