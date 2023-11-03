package com.abdurrahmanbulut.textrecognizer.scan

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.abdurrahmanbulut.textrecognizer.UIEvent
import com.abdurrahmanbulut.textrecognizer.utils.crop
import com.abdurrahmanbulut.textrecognizer.utils.getTextByInputImage
import com.abdurrahmanbulut.textrecognizer.utils.toBitmap
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
internal class ScannerViewModel : ViewModel(){

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    var regex by mutableStateOf("""TR\d{24}""")

    var isIbanCaptured by mutableStateOf(false)
    var iban by mutableStateOf("")
    var cameraWaitingTime by mutableLongStateOf(15000L)

    private val _ibanEvent = MutableLiveData<UIEvent<String>>()
    val ibanEvent: LiveData<UIEvent<String>> get() = _ibanEvent

    var imgBitmap = mutableStateOf<Bitmap?>(null)
    var appBarBackgroundColor by mutableStateOf(Color.White)

    override fun onCleared() {
        super.onCleared()
        executor.shutdown()
    }

    fun onBack(){
        _ibanEvent.value = UIEvent("")
    }

    fun setupCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        val context = previewView.context

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(executor, ImageAnalysis.Analyzer { imageProxy ->
                        processImage(imageProxy)
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {

            }
        }, ContextCompat.getMainExecutor(context))

    }

    @ExperimentalGetImage private fun processImage(imageProxy: ImageProxy) {

        if(isIbanCaptured) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image

        if (mediaImage != null) {

            val imageBitmap = mediaImage.toBitmap(imageProxy.imageInfo.rotationDegrees)
            val croppedBitmap = imageBitmap.crop(imageBitmap.width / 2, imageBitmap.height / 2, 240, 60)
            val croppedInputImage = InputImage.fromBitmap(croppedBitmap, imageProxy.imageInfo.rotationDegrees)

            getTextByInputImage(croppedInputImage, regex) { result ->
                if (result != null) {
                    imgBitmap.value = imageBitmap
                    iban = result
                    isIbanCaptured = true
                    _ibanEvent.value = UIEvent(result)
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        _ibanEvent.value = UIEvent("")
                    }, cameraWaitingTime)
                }
                imageProxy.close()
            }
        } else{
            imageProxy.close()
        }
    }
}