package com.abdurrahmanbulut.textrecognizer.gallery

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.abdurrahmanbulut.textrecognizer.UIEvent
import com.abdurrahmanbulut.textrecognizer.utils.getImageDimensions
import com.abdurrahmanbulut.textrecognizer.utils.getTextByInputImage
import com.google.mlkit.vision.common.InputImage



@OptIn(ExperimentalGetImage::class)
internal  class GalleryViewModel: ViewModel() {

    var iban by mutableStateOf("")
    var regex by mutableStateOf("""TR\d{24}""")

    val croppedImage = mutableStateOf<Bitmap?>(null)
    var originalImage = mutableStateOf<Uri?>(null)

    private val _ibanEvent = MutableLiveData<UIEvent<String>>()
    val ibanEvent: LiveData<UIEvent<String>> get() = _ibanEvent
    var isIbanCaptured by mutableStateOf(false)


    fun processGalleryImage(bitmap: Bitmap) {

        val image: InputImage = InputImage.fromBitmap(bitmap, 0)

        getTextByInputImage(image, regex) { result ->

            if (result != null) {
                iban = result
                isIbanCaptured = true
                _ibanEvent.value = UIEvent(result)
            } else {
                _ibanEvent.value = UIEvent("")
            }
        }
    }
    fun onBack(){
        _ibanEvent.value = UIEvent("")
    }


    fun startCrop(imageUri: Uri?, activity: Activity, cropImageResultLauncher:ActivityResultLauncher<Intent>) {

        val (originalWidth, originalHeight) = getImageDimensions(activity, imageUri)
        val newOutputX = originalWidth.coerceAtMost(640)  // max 640 or less
        val newOutputY = (newOutputX / 5.0).toInt()  // keeping the 4:1 ratio

        try {

            val cropIntent = Intent("com.android.camera.action.CROP")
            cropIntent.setDataAndType(imageUri, "image/*")
            cropIntent.putExtra("crop", "true")

            cropIntent.putExtra("aspectX", 4)
            cropIntent.putExtra("aspectY", 1)

            cropIntent.putExtra("outputX", newOutputX)
            cropIntent.putExtra("outputY", newOutputY)

            cropIntent.putExtra("return-data", true)
            cropImageResultLauncher.launch(cropIntent)

        } catch (anfe: ActivityNotFoundException) {
            //println("Your device doesn't support the crop action!")
        }
    }
}