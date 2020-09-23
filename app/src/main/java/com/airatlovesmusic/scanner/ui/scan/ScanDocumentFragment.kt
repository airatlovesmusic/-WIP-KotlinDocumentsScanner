package com.airatlovesmusic.scanner.ui.scan

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import androidx.lifecycle.whenStarted
import com.airatlovesmusic.scanner.R
import com.airatlovesmusic.scanner.entity.Corners
import com.airatlovesmusic.scanner.model.opencv.GetImageCorners
import com.airatlovesmusic.scanner.model.opencv.Loader
import com.airatlovesmusic.scanner.ui.AppActivity
import kotlinx.android.synthetic.main.fragment_document_scan.*
import kotlinx.coroutines.*
import java.io.File
import java.lang.Runnable
import java.util.*

class ScanDocumentFragment: Fragment(R.layout.fragment_document_scan) {

    private lateinit var camera: Camera

    private val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    private val imageCapture: ImageCapture by lazy {
        ImageCapture.Builder()
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initCamera()
        lifecycleScope.launch {
            whenCreated {
                Loader(requireContext()).load {}
            }
            whenStarted {
                var documentFound = false
                while (!documentFound) {
                    val bitmap = viewFinder.bitmap
                    if (bitmap == null) { delay(200); continue }
                    val corners = withContext(Dispatchers.IO) { GetImageCorners().getDocumentEdges(bitmap) }
                    if (corners != null) {
                        documentFound = true
                        hud.onCornersDetected(corners)
                        takePicture(corners)
                    }
                    else hud.onCornersNotDetected()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpTapToFocus()
    }

    private fun takePicture(corners: Corners) {
        val photoFile = File(
            getOutputDirectory(),
            UUID.randomUUID().toString() + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    (requireActivity() as AppActivity).goToCrop(outputFileResults.savedUri!!, corners)
                }
                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                }
            }
        )
    }

    private fun initCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }
            runCatching {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            }.onFailure { it.printStackTrace() }
        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    private fun getOutputDirectory(): File {
        val appContext = requireContext().applicationContext
        val mediaDir = activity?.externalMediaDirs?.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext.filesDir
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpTapToFocus() {
        viewFinder.setOnTouchListener { view, event ->
            if (event.action != MotionEvent.ACTION_UP) {
                return@setOnTouchListener false
            }

            val factory = DisplayOrientedMeteringPointFactory(
                view.display,
                camera.cameraInfo,
                view.width.toFloat(),
                view.height.toFloat()
            )
            val point = factory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point).build()
            camera.cameraControl.startFocusAndMetering(action)
            return@setOnTouchListener true
        }
    }

}