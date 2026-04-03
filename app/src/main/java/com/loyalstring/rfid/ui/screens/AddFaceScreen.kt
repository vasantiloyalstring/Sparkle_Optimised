package com.loyalstring.rfid.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.loyalstring.rfid.data.local.entity.FaceInfo
import com.loyalstring.rfid.ui.utils.FaceRecognizerHelper
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.FaceLoginViewModel
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@Composable
fun AddFaceScreen(
    navController: NavHostController,
    viewModel: FaceLoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val userPrefs = remember { UserPreferences(context) }
    val faceRecognizer = remember { FaceRecognizerHelper(context) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var faceDetected by remember { mutableStateOf(false) }
    var latestEmbedding by remember { mutableStateOf<FloatArray?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        if (!faceRecognizer.isModelLoaded()) {
            Toast.makeText(
                context,
                "mobile_face_net.tflite file missing in assets",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        if (hasPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val detectorOptions = FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                            .build()

                        val faceDetector = FaceDetection.getClient(detectorOptions)

                        imageAnalysis.setAnalyzer(
                            Executors.newSingleThreadExecutor()
                        ) { imageProxy ->
                            processFaceFrame(
                                imageProxy = imageProxy,
                                detector = faceDetector,
                                faceRecognizer = faceRecognizer,
                                onFaceDetected = { embedding ->
                                    faceDetected = true
                                    latestEmbedding = embedding
                                },
                                onNoFace = {
                                    faceDetected = false
                                },
                                onComplete = {
                                }
                            )
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_FRONT_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("AddFaceScreen", "Camera binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Camera permission required")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (faceDetected) "Face detected" else "No face detected",
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (!faceRecognizer.isModelLoaded()) {
                    Toast.makeText(context, "Face model not loaded", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val employee = userPrefs.getEmployee(com.loyalstring.rfid.data.model.login.Employee::class.java)
                val embedding = latestEmbedding

                if (embedding == null) {
                    Toast.makeText(context, "No face detected", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                viewModel.saveFace(
                    FaceInfo(
                        name = employee?.username ?: "User",
                        employeeId = employee?.id,
                        employeeJson =  employee?.let { com.google.gson.Gson().toJson(it) },
                        username = employee?.username,
                        clientCode = employee?.clientCode,
                        branchId = employee?.defaultBranchId,
                        embedding = embedding.joinToString(",")
                    )
                )

                Toast.makeText(context, "Face saved locally", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            },
            enabled = faceDetected && faceRecognizer.isModelLoaded(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Save Face")
        }
    }
}

@OptIn(ExperimentalGetImage::class)
fun processFaceFrame(
    imageProxy: ImageProxy,
    detector: FaceDetector,
    faceRecognizer: FaceRecognizerHelper,
    onFaceDetected: (FloatArray) -> Unit,
    onNoFace: () -> Unit,
    onComplete: () -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        onComplete()
        return
    }

    if (!faceRecognizer.isModelLoaded()) {
        imageProxy.close()
        onNoFace()
        onComplete()
        return
    }

    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

    detector.process(image)
        .addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val face = faces.first()

                val frameBitmap = imageProxyToBitmap(imageProxy)
                if (frameBitmap != null) {
                    val rotatedBitmap = rotateBitmap(frameBitmap, rotationDegrees.toFloat())
                    val faceBitmap = cropFaceFromBitmap(rotatedBitmap, face.boundingBox)

                    if (faceBitmap != null) {
                        try {
                            val realEmbedding = faceRecognizer.getEmbedding(faceBitmap)
                            onFaceDetected(realEmbedding)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onNoFace()
                        }
                    } else {
                        onNoFace()
                    }
                } else {
                    onNoFace()
                }
            } else {
                onNoFace()
            }
        }
        .addOnFailureListener {
            onNoFace()
        }
        .addOnCompleteListener {
            imageProxy.close()
            onComplete()
        }
}

fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            Rect(0, 0, imageProxy.width, imageProxy.height),
            100,
            out
        )

        val imageBytes = out.toByteArray()
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Float): Bitmap {
    if (rotationDegrees == 0f) return bitmap

    val matrix = Matrix().apply {
        postRotate(rotationDegrees)
    }

    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        matrix,
        true
    )
}

fun cropFaceFromBitmap(bitmap: Bitmap, boundingBox: Rect): Bitmap? {
    return try {
        val left = boundingBox.left.coerceAtLeast(0)
        val top = boundingBox.top.coerceAtLeast(0)
        val right = boundingBox.right.coerceAtMost(bitmap.width)
        val bottom = boundingBox.bottom.coerceAtMost(bitmap.height)

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) return null

        Bitmap.createBitmap(bitmap, left, top, width, height)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}