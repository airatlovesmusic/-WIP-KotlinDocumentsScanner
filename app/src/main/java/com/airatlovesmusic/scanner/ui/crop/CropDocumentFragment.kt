package com.airatlovesmusic.scanner.ui.crop

import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.airatlovesmusic.scanner.R
import com.airatlovesmusic.scanner.entity.Corners
import com.airatlovesmusic.scanner.model.opencv.Loader
import com.airatlovesmusic.scanner.model.opencv.TransformDocumentImage
import kotlinx.android.synthetic.main.fragment_crop.*
import kotlinx.coroutines.launch
import org.opencv.core.Point
import java.io.FileDescriptor


class CropDocumentFragment: Fragment(R.layout.fragment_crop) {

    private val uri by lazy { requireArguments().getParcelable<Uri>(ARG_URI) as Uri }
    private val corners by lazy { requireArguments().getParcelable<Corners>(ARG_CORNERS) as Corners }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            whenCreated { Loader(requireContext()).load {} }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        iv_preview.setImageURI(uri)
        hud.post {
            hud.cropMode = true
            hud.onCornersDetected(corners)
        }
        btn_crop.setOnClickListener {
            iv_preview.setImageBitmap(getOriginalBitmap())
            val bitmap = TransformDocumentImage().getTranformedDocumentImage(
                iv_preview.drawable.toBitmap(),
                hud.getOpenCVPoints()
            )
            hud.onCornersNotDetected()
            iv_preview.setImageBitmap(bitmap)
        }
        btn_rescan.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun getOriginalBitmap(): Bitmap {
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

    companion object {
        fun create(uri: Uri, corners: Corners) = CropDocumentFragment().apply {
            arguments = bundleOf(
                ARG_URI to uri,
                ARG_CORNERS to corners
            )
        }
        const val ARG_URI = "bitmap"
        const val ARG_CORNERS = "corners"
    }

}