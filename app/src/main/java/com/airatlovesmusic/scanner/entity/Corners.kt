package com.airatlovesmusic.scanner.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Corners(
    val corners: List<Point>,
    val size: Size
) : Parcelable

@Parcelize
data class Point(
    var x: Double = 0.0,
    var y: Double = 0.0
) : Parcelable

@Parcelize
data class Size(
    val width: Double = 0.0,
    val height: Double = 0.0
) : Parcelable
