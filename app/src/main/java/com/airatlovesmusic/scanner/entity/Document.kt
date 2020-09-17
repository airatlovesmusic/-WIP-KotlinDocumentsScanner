package com.airatlovesmusic.scanner.entity

import java.util.*

/**
 * Created by Airat Khalilov on 17/09/2020.
 */

data class Document(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = Date().time
)