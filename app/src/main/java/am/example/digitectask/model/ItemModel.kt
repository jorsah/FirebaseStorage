package am.example.digitectask.model

import android.graphics.Bitmap

data class ItemModel(
    val thumbnail: Bitmap? = null,
    val name: String,
    val size: Double,
    val url: String? = null
)