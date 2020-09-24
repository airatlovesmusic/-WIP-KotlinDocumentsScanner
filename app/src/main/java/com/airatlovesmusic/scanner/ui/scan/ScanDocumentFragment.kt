package com.airatlovesmusic.scanner.ui.scan

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.airatlovesmusic.scanner.R
import com.airatlovesmusic.scanner.entity.Point
import com.airatlovesmusic.scanner.model.opencv.GetImageCorners
import com.airatlovesmusic.scanner.model.opencv.Loader
import com.airatlovesmusic.scanner.ui.AppActivity
import kotlinx.android.synthetic.main.fragment_document_scan.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class ScanDocumentFragment: Fragment(R.layout.fragment_document_scan) {

    private lateinit var camera: Camera

    private val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    private val imageAnalysis: ImageAnalysis by lazy {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    private val imageCapture: ImageCapture by lazy {
        ImageCapture.Builder()
            .build()
    }

    private var torchEnabled = false
        set(value) {
            ib_flash_controller?.setImageResource(
                if (value) R.drawable.ic_baseline_flash_off_24
                else R.drawable.ic_baseline_flash_on_24
            )
            camera.cameraControl.enableTorch(value)
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initCamera()
        lifecycleScope.launch {
            whenCreated { Loader(requireContext()).load {} }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpTapToFocus()
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        ib_flash_controller.setOnClickListener { torchEnabled = !torchEnabled }
        ib_capture.setOnClickListener { takePicture() }
    }

    private fun takePicture() {
        imageAnalysis.clearAnalyzer()
        val bitmap = viewFinder.bitmap ?: return
        iv_preview.setImageBitmap(bitmap)
        val photoFile = File(
            requireContext().cacheDir,
            UUID.randomUUID().toString() + ".jpg"
        ).apply { createNewFile() }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val bitmapHeight = bitmap.height
                    val bitmapWidth = bitmap.width
                    val corners = GetImageCorners().getDocumentEdges(bitmap) ?:
                        // default points
                        listOf(Point(bitmapWidth.times(0.2), bitmapHeight.times(0.8)), Point(bitmapWidth.times(0.8), bitmapHeight.times(0.8)),
                            Point(bitmapWidth.times(0.8), bitmapHeight.times(0.2)), Point(bitmapWidth.times(0.2), bitmapHeight.times(0.2)))
                    (requireActivity() as AppActivity).goToCrop(photoFile.toUri(), corners)
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
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), { proxy ->
                    viewFinder?.bitmap?.let {
                        val corners = GetImageCorners().getDocumentEdges(it)
                        if (corners != null) {
                            hud.onCornersDetected(corners)
                        }
                        proxy.close()
                    }
                })
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis)
            }.onFailure { it.printStackTrace() }
        }, ContextCompat.getMainExecutor(requireActivity()))
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