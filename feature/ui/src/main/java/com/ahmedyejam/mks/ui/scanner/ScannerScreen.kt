package com.ahmedyejam.mks.ui.scanner

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.util.MksLogger
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    quizId: Long,
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    val context = LocalContext.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(uiState) {
        if (uiState is ScannerUiState.Saved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_questions_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (cameraPermissionState.status.isGranted) {
                when (val state = uiState) {
                    is ScannerUiState.Idle -> {
                        CameraPreview(
                            imageCapture = imageCapture,
                            cameraExecutor = cameraExecutor,
                            onImageCaptured = { bitmap ->
                                viewModel.onImageCaptured(bitmap, quizId)
                            }
                        )
                    }
                    is ScannerUiState.Processing -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(stringResource(R.string.processing_image), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    is ScannerUiState.Success -> {
                        QuestionReviewList(
                            questions = state.questions,
                            onSave = { viewModel.saveQuestions(state.questions) },
                            onCancel = { viewModel.reset() },
                            onUpdateQuestion = { index, question -> viewModel.updateQuestion(index, question) },
                            onDeleteQuestion = { index -> viewModel.deleteQuestion(index) }
                        )
                    }
                    is ScannerUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.import_error, state.message), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { viewModel.reset() }) {
                                Text(stringResource(R.string.try_again))
                            }
                        }
                    }
                    else -> {}
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(stringResource(R.string.camera_permission_required), style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text(stringResource(R.string.request_permission))
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    cameraExecutor: ExecutorService,
    onImageCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                MksLogger.e("ScannerScreen", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        
        IconButton(
            onClick = {
                imageCapture.takePicture(
                    cameraExecutor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = image.toBitmap().rotate(image.imageInfo.rotationDegrees.toFloat())
                            image.close()
                            onImageCaptured(bitmap)
                        }
                        override fun onError(exception: ImageCaptureException) {
                            MksLogger.e("ScannerScreen", "Photo capture failed", exception)
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(80.dp)
                .background(Color.White.copy(alpha = 0.8f), shape = CircleShape)
                .border(4.dp, Color.White, CircleShape)
        ) {
            Icon(Icons.Default.Camera, contentDescription = "Capture", tint = Color.Black, modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
fun QuestionReviewList(
    questions: List<QuestionEntity>,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onUpdateQuestion: (Int, QuestionEntity) -> Unit,
    onDeleteQuestion: (Int) -> Unit
) {
    var editingIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.review_recognized_questions, questions.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(questions) { index, question ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.question_number_prefix, index + 1),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { editingIndex = index }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { onDeleteQuestion(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Text(question.text, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                        question.options.forEachIndexed { optIndex, option ->
                            val isCorrect = question.correctAnswers.contains(optIndex)
                            val isDark = isSystemInDarkTheme()
                            val successBg = if (isDark) Color(0xFF1B5E20).copy(alpha = 0.3f) else Color(0xFF4CAF50).copy(alpha = 0.1f)
                            val successText = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .background(
                                        if (isCorrect) successBg else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    "${(optIndex + 65).toChar()}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(24.dp),
                                    color = if (isCorrect) successText else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    option,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCorrect) successText else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.discard))
            }
            Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.import_all))
            }
        }
    }

    editingIndex?.let { index ->
        EditQuestionDialog(
            question = questions[index],
            onDismiss = { editingIndex = null },
            onConfirm = { updated ->
                onUpdateQuestion(index, updated)
                editingIndex = null
            }
        )
    }
}

@Composable
fun EditQuestionDialog(
    question: QuestionEntity,
    onDismiss: () -> Unit,
    onConfirm: (QuestionEntity) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(question.text) }
    var options by rememberSaveable { mutableStateOf(question.options) }
    var correctAnswers by rememberSaveable { mutableStateOf(question.correctAnswers) }
    var explanation by rememberSaveable { mutableStateOf(question.explanation ?: "") }
    var hint by rememberSaveable { mutableStateOf(question.hint ?: "") }
    var reference by rememberSaveable { mutableStateOf(question.reference ?: "") }
    var additionalInfo by rememberSaveable { mutableStateOf(question.additionalInfo ?: "") }
    var weight by rememberSaveable { mutableStateOf(question.weight.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_question)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.question_text_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(stringResource(R.string.options_label), style = MaterialTheme.typography.labelMedium)
                options.forEachIndexed { index, option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = correctAnswers.contains(index),
                            onCheckedChange = { checked ->
                                val newCorrect = if (checked) {
                                    (correctAnswers + index).distinct()
                                } else {
                                    if (correctAnswers.size > 1) correctAnswers - index else correctAnswers
                                }
                                correctAnswers = newCorrect
                            }
                        )
                        OutlinedTextField(
                            value = option,
                            onValueChange = { newVal ->
                                val newOptions = options.toMutableList()
                                newOptions[index] = newVal
                                options = newOptions
                            },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                TextButton(
                    onClick = { options = options + "" },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Option")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text(stringResource(R.string.explanation_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text(stringResource(R.string.hint_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text(stringResource(R.string.reference_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = additionalInfo,
                    onValueChange = { additionalInfo = it },
                    label = { Text(stringResource(R.string.info_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = weight,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) weight = it },
                    label = { Text("Weight") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(question.copy(
                    text = text, 
                    options = options.filter { it.isNotBlank() }, 
                    correctAnswers = correctAnswers.filter { it < options.size },
                    type = if (correctAnswers.size > 1) com.ahmedyejam.mks.data.local.entity.QuestionType.MULTIPLE_CHOICE 
                           else com.ahmedyejam.mks.data.local.entity.QuestionType.SINGLE_CHOICE,
                    explanation = explanation.ifBlank { null },
                    hint = hint.ifBlank { null },
                    reference = reference.ifBlank { null },
                    additionalInfo = additionalInfo.ifBlank { null },
                    weight = weight.toIntOrNull() ?: 1
                ))
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}



fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
