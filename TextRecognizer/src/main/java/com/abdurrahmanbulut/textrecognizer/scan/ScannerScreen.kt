package com.abdurrahmanbulut.textrecognizer.scan

import android.annotation.SuppressLint
import android.app.Activity
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.abdurrahmanbulut.textrecognizer.scan.result_screen.ResultScreen
import kotlinx.coroutines.delay


@Composable
internal fun ScannerActivityContent(
    viewModel: ScannerViewModel,
    onFinish: () -> Unit
) {

    if (viewModel.isIbanCaptured) {
        ResultScreen(viewModel)
    } else {
        ScannerScreen(viewModel)
    }

    LaunchedEffect(viewModel.isIbanCaptured) {
        if (viewModel.isIbanCaptured) {
            delay(2000)
            onFinish()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
internal fun ScannerScreen(viewModel: ScannerViewModel) {
    var viewFinder: PreviewView? by remember { mutableStateOf(null) }
    val lifecycleOwner = LocalLifecycleOwner.current


    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val view = LocalView.current

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = viewModel.appBarBackgroundColor.toArgb()
    }

    Scaffold(
            topBar = {
                TopAppBar(title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onBack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = viewModel.appBarBackgroundColor,
                    )
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreview(onPreviewViewCreated = {
                viewFinder = it

                viewModel.setupCamera(viewFinder!!, lifecycleOwner)
            })
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7F))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(screenWidth * 0.8f, screenHeight * 0.1f)
                    .background(Color.Transparent, shape = RoundedCornerShape(12.dp))
                    .clearAndOutlineBackground()
            )
        }
    }
}

internal fun Modifier.clearAndOutlineBackground(): Modifier = composed {
    this
        .graphicsLayer(
            clip = false
        )
        .background(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, Color.Transparent),
                startY = 0.0f,
                endY = Float.POSITIVE_INFINITY
            ),
            shape = RoundedCornerShape(12.dp)
        )
        .drawWithContent {
            drawRoundRect(
                color = Color.Transparent,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(24F, 24F),
                blendMode = BlendMode.Clear
            )
            drawContent()
        }
}

@Composable
internal fun CameraPreview(onPreviewViewCreated: (PreviewView) -> Unit) {
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
            }
        },
        update = {
            onPreviewViewCreated(it)
        },
        modifier = Modifier.fillMaxSize()
    )
}