package com.airatlovesmusic.scanner.ui.scan

import android.annotation.SuppressLint
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.MotionEvent
import android.view.View
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.view.drawToBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.airatlovesmusic.scanner.R
import com.airatlovesmusic.scanner.model.opencv.GetImageCorners
import com.airatlovesmusic.scanner.model.opencv.Loader
import com.airatlovesmusic.scanner.ui.AppActivity
import kotlinx.android.synthetic.main.fragment_document_scan.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileDescriptor
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
                    iv_preview.setImageBitmap(getOriginalBitmap(photoFile.toUri()))
                    val corners = GetImageCorners().getDocumentEdges(iv_preview.drawable.toBitmap())
                    corners ?: return
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
                imageAnalysis.setAnalyzer(
                    ContextCompat.getMainExecutor(requireContext()),
                    { proxy ->
                        viewFinder.bitmap?.let {
                            val corners = GetImageCorners().getDocumentEdges(it)
                            if (corners != null) { hud.onCornersDetected(corners) }
                        }
                        proxy.close()
                    })
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            }.onFailure { it.printStackTrace() }
        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    fun getOriginalBitmap(uri: Uri): Bitmap {
        val parcelFileDescriptor: ParcelFileDescriptor =
            requireContext().contentResolver.openFileDescriptor(uri, "r")!!
        val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        val exif = ExifInterface(uri.path.toString())
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
        }

        val rotatedBitmap =
            Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
        image.recycle()
        return rotatedBitmap
    }

    fun scaleCenterCrop(source: Bitmap, newHeight: Int, newWidth: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        val xScale = newWidth.toFloat() / sourceWidth
        val yScale = newHeight.toFloat() / sourceHeight
        val scale = Math.max(xScale, yScale)

        // Now get the size of the source bitmap when scaled
        val scaledWidth = scale * sourceWidth
        val scaledHeight = scale * sourceHeight

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        val left = (newWidth - scaledWidth) / 2
        val top = (newHeight - scaledHeight) / 2

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        val targetRect = RectF(left, top, left + scaledWidth, top + scaledHeight)

        // Finally, we create a new bitmap of the specified size and draw our new,
        // scaled bitmap onto it.
        val dest = Bitmap.createBitmap(newWidth, newHeight, source.config)
        val canvas = Canvas(dest)
        canvas.drawBitmap(source, null, targetRect, null)
        return dest
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