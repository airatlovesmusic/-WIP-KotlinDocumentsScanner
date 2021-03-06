package com.airatlovesmusic.scanner.model.opencv

import android.content.Context
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import java.lang.ref.WeakReference

enum class OpenCvStatus {
    LOADED, ERROR
}

class Loader(context: Context) {
    private val reference = WeakReference<Context>(context)

    private var onLoad: ((OpenCvStatus) -> Unit)? = null
    private val mLoaderCallback = object : BaseLoaderCallback(context.applicationContext) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    onLoad?.invoke(OpenCvStatus.LOADED)
                }
                else -> {
                    super.onManagerConnected(status)
                    onLoad?.invoke(OpenCvStatus.ERROR)
                }
            }
        }
    }

    fun load(callback: (OpenCvStatus) -> Unit) = reference.get()?.let {
        onLoad = callback
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(
                OpenCVLoader.OPENCV_VERSION,
                it.applicationContext,
                mLoaderCallback
            )
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }
}