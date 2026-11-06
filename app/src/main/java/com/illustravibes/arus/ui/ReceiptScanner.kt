package com.illustravibes.arus.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.AttachmentEntity
import com.illustravibes.arus.domain.ReceiptOcrParser
import com.illustravibes.arus.domain.ReceiptOcrReview
import com.illustravibes.arus.ui.designsystem.ArusButton
import com.illustravibes.arus.ui.designsystem.ArusButtonKind
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.OutlinedArusButton
import com.illustravibes.arus.ui.designsystem.Text
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class ReceiptScanStep { SOURCE, CAMERA, EDIT, PROCESSING, RESULT }

@Composable
internal fun ReceiptScanner(
    database: ArusDatabase,
    onClose: () -> Unit,
    onManualEntry: (Long?, ReceiptOcrReview?) -> Unit,
    onConfirmTransaction: (Long, ReceiptOcrReview) -> Unit,
    onConfirmGroupOrder: (Long, ReceiptOcrReview) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var step by remember { mutableStateOf(ReceiptScanStep.SOURCE) }
    var permissionDenied by remember { mutableStateOf(false) }
    var sourceFile by remember { mutableStateOf<File?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var crop by remember { mutableStateOf(ReceiptCrop()) }
    var isRotating by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var finalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rawText by remember { mutableStateOf("") }
    var receiptReview by remember { mutableStateOf<ReceiptOcrReview?>(null) }
    var savedAttachmentId by remember { mutableStateOf<Long?>(null) }
    var processingError by remember { mutableStateOf<String?>(null) }
    var pickerError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(cameraExecutor) {
        onDispose {
            cameraExecutor.shutdown()
            bitmap?.recycle()
            finalBitmap?.recycle()
            sourceFile?.takeIf { it.parentFile == context.cacheDir }?.delete()
        }
    }

    suspend fun openImage(file: File) {
        val decoded = withContext(Dispatchers.IO) { ReceiptImageProcessor.decode(file) }
        bitmap?.recycle()
        sourceFile = file
        bitmap = decoded
        crop = ReceiptCrop()
        pickerError = null
        step = ReceiptScanStep.EDIT
    }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching {
                val copied = withContext(Dispatchers.IO) {
                    val file = File(context.cacheDir, "receipt-${System.nanoTime()}.source")
                    ReceiptImageProcessor.copyFromUri(context.contentResolver, uri, file)
                    file
                }
                openImage(copied)
            }.onFailure { pickerError = it.message ?: "Foto tidak dapat dibuka." }
        }
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        step = if (granted) ReceiptScanStep.CAMERA else ReceiptScanStep.SOURCE
    }

    fun startCamera() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            permissionDenied = false
            step = ReceiptScanStep.CAMERA
        } else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    suspend fun capturePhoto(capture: ImageCapture) {
        val file = File(context.cacheDir, "receipt-${System.nanoTime()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.targetRotation = Surface.ROTATION_0
        capture.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                scope.launch {
                    runCatching { openImage(file) }.onFailure {
                        file.delete()
                        pickerError = it.message ?: "Foto kamera tidak dapat dibuka."
                        step = ReceiptScanStep.CAMERA
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                scope.launch {
                    pickerError = exception.message ?: "Kamera gagal mengambil foto."
                    file.delete()
                }
            }
        })
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ArusSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedArusButton(onClick = onClose) { Text("‹ Kembali") }
            Text("Scan struk", style = ArusTheme.typography.headline, modifier = Modifier.padding(start = ArusSpacing.Small))
        }

        when (step) {
            ReceiptScanStep.SOURCE -> ReceiptSourceChooser(
                cameraPermissionDenied = permissionDenied,
                onCamera = ::startCamera,
                onGallery = { galleryPicker.launch(arrayOf("image/*")) },
                onManual = { onManualEntry(null, null) },
            )

            ReceiptScanStep.CAMERA -> {
                Text("Arahkan kamera ke struk, lalu ambil foto.")
                ReceiptCameraPreview(
                    executor = cameraExecutor,
                    onImageCaptureReady = { imageCapture = it },
                    onError = { pickerError = it },
                )
                imageCapture?.let { capture ->
                    ArusButton(onClick = { scope.launch { capturePhoto(capture) } }) { Text("Ambil foto") }
                }
                OutlinedArusButton(onClick = { step = ReceiptScanStep.SOURCE }) { Text("Pilih galeri") }
                pickerError?.let { Text(it, color = ArusTheme.colors.error) }
            }

            ReceiptScanStep.EDIT -> {
                val currentBitmap = bitmap
                if (currentBitmap != null) {
                    Text("Geser sudut untuk memangkas struk.")
                    ReceiptCropPreview(currentBitmap, crop, onCropChange = { crop = it })
                    ReceiptCropMarginControl("atas", crop.top) { delta ->
                        crop = crop.copy(top = (crop.top + delta).coerceIn(0f, 1f - crop.bottom - 0.12f))
                    }
                    ReceiptCropMarginControl("kanan", crop.right) { delta ->
                        crop = crop.copy(right = (crop.right + delta).coerceIn(0f, 1f - crop.left - 0.12f))
                    }
                    ReceiptCropMarginControl("bawah", crop.bottom) { delta ->
                        crop = crop.copy(bottom = (crop.bottom + delta).coerceIn(0f, 1f - crop.top - 0.12f))
                    }
                    ReceiptCropMarginControl("kiri", crop.left) { delta ->
                        crop = crop.copy(left = (crop.left + delta).coerceIn(0f, 1f - crop.right - 0.12f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                        OutlinedArusButton(onClick = {
                            isRotating = true
                            scope.launch {
                                try {
                                    val rotated = withContext(Dispatchers.Default) { ReceiptImageProcessor.rotate(currentBitmap, 270) }
                                    if (bitmap === currentBitmap) {
                                        bitmap = rotated
                                        currentBitmap.recycle()
                                        crop = ReceiptCrop()
                                    } else rotated.recycle()
                                } finally {
                                    isRotating = false
                                }
                            }
                        }, enabled = !isRotating) { Text("Putar kiri") }
                        OutlinedArusButton(onClick = {
                            isRotating = true
                            scope.launch {
                                try {
                                    val rotated = withContext(Dispatchers.Default) { ReceiptImageProcessor.rotate(currentBitmap, 90) }
                                    if (bitmap === currentBitmap) {
                                        bitmap = rotated
                                        currentBitmap.recycle()
                                        crop = ReceiptCrop()
                                    } else rotated.recycle()
                                } finally {
                                    isRotating = false
                                }
                            }
                        }, enabled = !isRotating) { Text("Putar kanan") }
                    }
                    ArusButton(onClick = {
                        val source = bitmap ?: return@ArusButton
                        step = ReceiptScanStep.PROCESSING
                        processingError = null
                        scope.launch {
                            var cropped: Bitmap? = null
                            var outputFile: File? = null
                            try {
                                cropped = withContext(Dispatchers.IO) { ReceiptImageProcessor.crop(source, crop) }
                                val file = withContext(Dispatchers.IO) { ReceiptImageProcessor.saveJpeg(cropped, context.filesDir) }
                                outputFile = file
                                val recognized = runCatching { ReceiptImageProcessor.recognize(cropped) }
                                rawText = recognized.getOrDefault("")
                                receiptReview = ReceiptOcrParser.parse(rawText)
                                processingError = recognized.exceptionOrNull()?.message
                                    ?.let { "OCR gagal. Foto tetap dapat disimpan dan diisi manual." }
                                savedAttachmentId = withContext(Dispatchers.IO) {
                                    database.attachmentDao().insert(AttachmentEntity(
                                        "PENDING", null, file.relativeTo(context.filesDir).path,
                                        rawText.takeIf(String::isNotBlank), System.currentTimeMillis(),
                                    ))
                                }
                                outputFile = null
                                finalBitmap?.recycle()
                                finalBitmap = cropped
                                cropped = null
                                sourceFile?.takeIf { it.parentFile == context.cacheDir }?.delete()
                                sourceFile = null
                                bitmap?.recycle()
                                bitmap = null
                                step = ReceiptScanStep.RESULT
                            } catch (failure: Exception) {
                                cropped?.recycle()
                                outputFile?.delete()
                                processingError = failure.message ?: "Struk tidak dapat diproses."
                                step = ReceiptScanStep.EDIT
                            }
                        }
                    }, enabled = !isRotating) { Text("Pangkas dan baca struk") }
                }
                pickerError?.let { Text(it, color = ArusTheme.colors.error) }
            }

            ReceiptScanStep.PROCESSING -> {
                Text("Membaca teks dari struk di perangkat…", style = ArusTheme.typography.title)
                Text("Proses tetap lokal. Jangan tutup layar.")
            }

            ReceiptScanStep.RESULT -> {
                finalBitmap?.let { image ->
                    Image(image.asImageBitmap(), contentDescription = "Foto struk", modifier = Modifier.fillMaxWidth().height(220.dp))
                }
                Text("Hasil OCR", style = ArusTheme.typography.title)
                processingError?.let { Text(it, color = ArusTheme.colors.error) }
                receiptReview?.let { review ->
                    review.totalRupiah?.let { value ->
                        Text("Total · ${formatRupiah(value)}")
                        ReceiptConfidenceLabel("Total", review.totalConfidencePercent)
                    }
                    review.date?.let { value ->
                        Text("Tanggal · $value")
                        ReceiptConfidenceLabel("Tanggal", review.dateConfidencePercent)
                    }
                    review.merchant?.let { value ->
                        Text("Merchant · $value")
                        ReceiptConfidenceLabel("Merchant", review.merchantConfidencePercent)
                    }
                    if (review.items.isNotEmpty()) {
                        Text("Item terbaca · ${review.items.size}", style = ArusTheme.typography.label)
                        review.items.forEach { item ->
                            Text("${item.name} · ${formatRupiah(item.amountRupiah)}")
                            ReceiptConfidenceLabel("Item ${item.name}", item.confidencePercent)
                        }
                    }
                }
                if (rawText.isBlank()) {
                    Text("Teks belum terbaca. Foto tersimpan; lanjutkan dengan input manual.")
                } else {
                    ArusCard(modifier = Modifier.fillMaxWidth()) {
                        Text(rawText, modifier = Modifier.padding(ArusSpacing.Medium))
                    }
                }
                if (savedAttachmentId != null) Text("Foto disimpan sebagai lampiran tertunda. Periksa hasil sebelum menautkan ke transaksi.")
                val attachmentId = savedAttachmentId
                val review = receiptReview
                ArusButton(onClick = {
                    if (attachmentId != null && review != null) onConfirmTransaction(attachmentId, review)
                }, enabled = attachmentId != null && review != null) { Text("Jadikan transaksi") }
                OutlinedArusButton(onClick = {
                    if (attachmentId != null && review != null) onConfirmGroupOrder(attachmentId, review)
                }, enabled = attachmentId != null && review != null) { Text("Jadikan Group Order") }
                OutlinedArusButton(onClick = { onManualEntry(attachmentId, review) }) { Text("Lanjut input manual") }
                OutlinedArusButton(onClick = onClose) { Text("Selesai") }
            }
        }
    }
}

@Composable
private fun ReceiptCropMarginControl(label: String, margin: Float, onAdjust: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
        Text("Potongan $label · ${ (margin * 100).roundToInt() }%", modifier = Modifier.weight(1f))
        OutlinedArusButton(
            onClick = { onAdjust(-0.025f) },
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Kurangi potongan $label" },
        ) { Text("−") }
        OutlinedArusButton(
            onClick = { onAdjust(0.025f) },
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Tambah potongan $label" },
        ) { Text("+") }
    }
}

@Composable
internal fun ReceiptSourceChooser(
    cameraPermissionDenied: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onManual: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
        Text("Pilih foto struk")
        if (cameraPermissionDenied) {
            Text("Izin kamera ditolak. Galeri dan input manual tetap tersedia.", color = ArusTheme.colors.error)
        }
        ArusButton(onClick = onCamera, modifier = Modifier.fillMaxWidth()) { Text("Kamera") }
        OutlinedArusButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) { Text("Pilih dari galeri") }
        OutlinedArusButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) { Text("Input manual") }
    }
}

@Composable
internal fun ReceiptConfidenceLabel(field: String, confidencePercent: Int?) {
    if (confidencePercent == null) return
    val needsReview = confidencePercent < 75
    val background = if (needsReview) Color(0xFFFDE68A) else ArusTheme.colors.surfaceVariant
    val label = if (needsReview) "Cek ulang · $field" else "$field terbaca jelas"
    Text(label, modifier = Modifier.background(background).padding(horizontal = 8.dp, vertical = 4.dp),
        style = ArusTheme.typography.label.copy(color = ArusTheme.colors.foreground))
}

@Composable
private fun ReceiptCameraPreview(
    executor: ExecutorService,
    onImageCaptureReady: (ImageCapture?) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        val listener = Runnable {
            runCatching {
                future.get().also { provider = it }
            }.onSuccess { cameraProvider ->
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    onImageCaptureReady(capture)
                }.onFailure { onError(it.message ?: "Kamera tidak tersedia di perangkat ini.") }
            }.onFailure { onError(it.message ?: "Kamera tidak dapat dibuka.") }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            onImageCaptureReady(null)
            provider?.unbindAll()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxWidth().height(320.dp).background(Color.Black),
    )
}

@Composable
private fun ReceiptCropPreview(bitmap: Bitmap, crop: ReceiptCrop, onCropChange: (ReceiptCrop) -> Unit) {
    Canvas(
        Modifier.fillMaxWidth().height(320.dp).background(Color.Black)
            .pointerInput(bitmap, crop) {
                var corner = -1
                var currentCrop = crop
                detectDragGestures(
                    onDragStart = { pointer ->
                        val imageRect = receiptImageRect(bitmap.width, bitmap.height, size.width.toFloat(), size.height.toFloat())
                        val handles = cropHandles(imageRect, currentCrop)
                        corner = handles.indices.minByOrNull { (handles[it] - pointer).getDistance() } ?: -1
                        if (corner >= 0 && (handles[corner] - pointer).getDistance() > 48.dp.toPx()) corner = -1
                    },
                    onDrag = { change, dragAmount ->
                        if (corner < 0) return@detectDragGestures
                        change.consume()
                        val imageRect = receiptImageRect(bitmap.width, bitmap.height, size.width.toFloat(), size.height.toFloat())
                        val dx = dragAmount.x / imageRect.width
                        val dy = dragAmount.y / imageRect.height
                        val minSize = 0.12f
                        currentCrop = when (corner) {
                            0 -> currentCrop.copy(left = (currentCrop.left + dx).coerceIn(0f, 1f - currentCrop.right - minSize), top = (currentCrop.top + dy).coerceIn(0f, 1f - currentCrop.bottom - minSize))
                            1 -> currentCrop.copy(right = (currentCrop.right - dx).coerceIn(0f, 1f - currentCrop.left - minSize), top = (currentCrop.top + dy).coerceIn(0f, 1f - currentCrop.bottom - minSize))
                            2 -> currentCrop.copy(right = (currentCrop.right - dx).coerceIn(0f, 1f - currentCrop.left - minSize), bottom = (currentCrop.bottom - dy).coerceIn(0f, 1f - currentCrop.top - minSize))
                            else -> currentCrop.copy(left = (currentCrop.left + dx).coerceIn(0f, 1f - currentCrop.right - minSize), bottom = (currentCrop.bottom - dy).coerceIn(0f, 1f - currentCrop.top - minSize))
                        }
                        onCropChange(currentCrop)
                    },
                    onDragEnd = { corner = -1 },
                    onDragCancel = { corner = -1 },
                )
            },
    ) {
        val imageRect = receiptImageRect(bitmap.width, bitmap.height, size.width, size.height)
        drawImage(
            image = bitmap.asImageBitmap(),
            dstOffset = IntOffset(imageRect.left.toInt(), imageRect.top.toInt()),
            dstSize = IntSize(imageRect.width.toInt(), imageRect.height.toInt()),
        )
        val cropLeft = imageRect.left + imageRect.width * crop.left
        val cropTop = imageRect.top + imageRect.height * crop.top
        val cropRight = imageRect.right - imageRect.width * crop.right
        val cropBottom = imageRect.bottom - imageRect.height * crop.bottom
        val mask = Color.Black.copy(alpha = 0.52f)
        drawRect(mask, Offset(imageRect.left, imageRect.top), Size(imageRect.width, cropTop - imageRect.top))
        drawRect(mask, Offset(imageRect.left, cropBottom), Size(imageRect.width, imageRect.bottom - cropBottom))
        drawRect(mask, Offset(imageRect.left, cropTop), Size(cropLeft - imageRect.left, cropBottom - cropTop))
        drawRect(mask, Offset(cropRight, cropTop), Size(imageRect.right - cropRight, cropBottom - cropTop))
        drawRect(Color.White, Offset(cropLeft, cropTop), Size(cropRight - cropLeft, cropBottom - cropTop), style = Stroke(2.dp.toPx()))
        cropHandles(imageRect, crop).forEach { point -> drawCircle(Color.White, 8.dp.toPx(), point) }
    }
}

private fun receiptImageRect(bitmapWidth: Int, bitmapHeight: Int, viewWidth: Float, viewHeight: Float): Rect {
    val scale = minOf(viewWidth / bitmapWidth, viewHeight / bitmapHeight)
    val width = bitmapWidth * scale
    val height = bitmapHeight * scale
    val left = (viewWidth - width) / 2f
    val top = (viewHeight - height) / 2f
    return Rect(left, top, left + width, top + height)
}

private fun cropHandles(imageRect: Rect, crop: ReceiptCrop): List<Offset> {
    val left = imageRect.left + imageRect.width * crop.left
    val top = imageRect.top + imageRect.height * crop.top
    val right = imageRect.right - imageRect.width * crop.right
    val bottom = imageRect.bottom - imageRect.height * crop.bottom
    return listOf(Offset(left, top), Offset(right, top), Offset(right, bottom), Offset(left, bottom))
}
