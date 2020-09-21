package com.airatlovesmusic.scanner.ui.crop

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.airatlovesmusic.scanner.R
import com.airatlovesmusic.scanner.entity.Corners
import kotlinx.android.synthetic.main.fragment_crop.*

class CropImageFragment: Fragment(R.layout.fragment_crop) {

    private val preview by lazy { requireArguments().getParcelable<Bitmap>(ARG_BITMAP) as Bitmap }
    private val corners by lazy { requireArguments().getParcelable<Corners>(ARG_CORNERS) as Corners }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hud.cropMode = true
        iv_preview.setImageBitmap(preview)
        hud.post { hud.onCornersDetected(corners) }
    }

    companion object {
        fun create(bitmap: Bitmap, corners: Corners) = CropImageFragment().apply {
            arguments = bundleOf(
                ARG_BITMAP to bitmap,
                ARG_CORNERS to corners
            )
        }
        const val ARG_BITMAP = "bitmap"
        const val ARG_CORNERS = "corners"
    }

}