package com.abdurrahmanbulut.textrecognizer.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException


/**
 * Processes an InputImage to extract text that matches a given pattern.
 *
 * This function uses the ML Kit's Text Recognition API to process the image and identify text blocks.
 * It then cleans and checks each line of text against a provided regular expression to validate it.
 * If a valid match is found, the callback is invoked with the cleaned text. If processing fails or
 * no valid text is found, the callback is invoked with null.
 *
 * @param inputImage The InputImage object that is to be processed for text recognition.
 * @param regexString The regular expression pattern the text is tested against.
 * @param callback A lambda function that is called with the processed text or null.
 */

fun getTextByInputImage(inputImage: InputImage, regexString: String, callback: (String?) -> Unit) {
    val options = TextRecognizerOptions.Builder()
        .build()

    val recognizer = TextRecognition.getClient(options)

    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    var cleanedIban = line.text.replace(" ", "")
                    cleanedIban  = correctOcrMistakes(cleanedIban)

                    if (isValidIban(cleanedIban, regexString)) {
                        callback(cleanedIban)
                        return@addOnSuccessListener
                    }
                }
            }
            callback(null)
        }
        .addOnFailureListener {
            callback(null)
        }
        .addOnCompleteListener {
        }
}

/**
 * Corrects common OCR mistakes in a given text string.
 *
 * This function creates a new string where specific characters are replaced according to a predefined
 * mapping. This is useful for correcting OCR errors that commonly occur, such as misidentifying '0' as 'O'.
 *
 * @param text The string with potential OCR errors.
 * @return The corrected string with specific characters replaced.
 */
fun correctOcrMistakes(text: String): String {
    val corrections = mapOf('O' to '0', 'o' to '0', 'B' to '8', 'I' to '1')
    return text.map { char -> corrections[char] ?: char }.joinToString("")
}

/**
 * Processes a Base64 encoded image to extract text that matches a given pattern.
 *
 * Similar to 'getTextByInputImage', this function decodes a Base64 encoded string into an image
 * and then uses the ML Kit Text Recognition API to process and find text. Each line of detected text
 * is checked against a provided regular expression. The first match found is passed to a callback function.
 * If no match is found, or there is a failure in processing, the callback is invoked with null.
 *
 * @param base64String The Base64 encoded string representing an image.
 * @param regexString The regular expression pattern the text is tested against.
 * @param callback A lambda function that is called with the processed text or null.
 */
fun getTextByBase64Image(base64String: String, regexString: String, callback: (String?) -> Unit) {

    val byteArray = Base64.decode(base64String, Base64.DEFAULT)
    val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    val inputImage = InputImage.fromBitmap(bitmap, 0)

    val options = TextRecognizerOptions.Builder().build()
    val recognizer = TextRecognition.getClient(options)

    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    val cleanedIban = line.text.replace(" ", "")
                    if (isValidIban(cleanedIban, regexString)) {
                        callback(cleanedIban)
                        return@addOnSuccessListener
                    }
                }
            }
            callback(null)
        }
        .addOnFailureListener {
            callback(null)
        }
        .addOnCompleteListener {
        }
}

/**
 * Validates a given IBAN string against a regular expression pattern.
 *
 * This function checks if the provided IBAN string matches a regular expression pattern.
 * It's a private utility function used to validate the format of an IBAN.
 *
 * @param iban The IBAN string to validate.
 * @param regexString The regular expression pattern to validate the IBAN against.
 * @return A Boolean value indicating whether the IBAN is valid (true) or not (false).
 */
private fun isValidIban(iban: String, regexString: String): Boolean {
    val ibanRegex: Regex = regexString.toRegex()
    return ibanRegex.matches(iban)
}


/**
 * Converts a YUV_420_888 image to a Bitmap and rotates it to the specified degrees.
 *
 * This extension function for the Image class allows for the conversion of an image
 * in the YUV_420_888 format to a Bitmap. It also applies rotation to the Bitmap
 * based on the specified degrees.
 *
 * @param rotationDegrees The degrees of rotation to be applied to the bitmap.
 * @return A rotated Bitmap representation of the image.
 */
fun Image.toBitmap(rotationDegrees: Int): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    // U and V are swapped
    yBuffer[nv21, 0, ySize]
    vBuffer[nv21, ySize, vSize]
    uBuffer[nv21, ySize + vSize, uSize]

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()

    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)


    val imageBytes = out.toByteArray()

    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)


    val matrix = Matrix()
    matrix.postRotate(rotationDegrees.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/**
 * Crops a Bitmap to a specified width and height around a central point.
 *
 * This function creates a new Bitmap by cropping the original Bitmap around the specified
 * central point (centerX, centerY) to the specified width and height.
 * If the specified dimensions extend beyond the edge of the source bitmap, the function
 * constrains the dimensions to remain within the source bitmap's bounds.
 *
 * @param centerX The x-coordinate of the central point of the crop area.
 * @param centerY The y-coordinate of the central point of the crop area.
 * @param width The width of the crop area.
 * @param height The height of the crop area.
 * @return The cropped Bitmap.
 */
fun Bitmap.crop(centerX: Int, centerY: Int, width: Int, height: Int): Bitmap {
    val startX = (centerX - width / 2).coerceAtLeast(0)
    val startY = (centerY - height / 2).coerceAtLeast(0)
    return Bitmap.createBitmap(this, startX, startY, width, height)
}

/**
 * Retrieves the dimensions of an image located at a Uri without loading the full image into memory.
 *
 * This function decodes the bounds of the image referenced by the Uri to determine its dimensions.
 * It's useful when you need to know the size of the image before loading it, for instance, to
 * calculate scaling factors or to load a subsampled version of the image.
 *
 * @param activity The Activity context used to retrieve the ContentResolver.
 * @param uri The Uri of the image whose dimensions are being queried.
 * @return A Pair containing the width and height of the image.
 */
fun getImageDimensions(activity: Activity, uri: Uri?): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeStream(activity.contentResolver.openInputStream(uri!!), null, options)
    return Pair(options.outWidth, options.outHeight)
}

/**
 * Loads a Bitmap from a given Uri.
 *
 * This function attempts to open an InputStream from a Uri using the context's ContentResolver
 * and decodes it into a Bitmap. If the file is not found or cannot be opened, it catches
 * a FileNotFoundException, logs the stack trace, and returns null.
 *
 * @param context The context used to access the ContentResolver.
 * @param imageUri The Uri of the image to load.
 * @return The loaded Bitmap, or null if the image cannot be loaded.
 */
fun loadBitmapFromUri(context: Context, imageUri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
        null
    }
}