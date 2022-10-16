package am.example.digitectask.utils

import am.example.digitectask.model.VideoSize
import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns

fun Double.mbToBytes(): Int {
    return (this * 1048576).toInt()
}

fun Int.bytesToMb(): Double {
    return (this / 1048576.0 * 100.0).toInt() / 100.0
}

fun Uri.getVideoSize(context: Context): VideoSize {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, this)
    val width =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
            ?: 0
    val height =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
            ?: 0
    retriever.release()
    return VideoSize(width, height)
}

@SuppressLint("Range")
fun Uri.getFileName(context: Context): String? {
    var result: String? = null
    if (this.scheme == "content") {
        val cursor: Cursor? = context.contentResolver.query(this, null, null, null, null)
        cursor.use {
            if (it != null && it.moveToFirst() ) {
                result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
    }
    if (result == null) {
        result = this.path
        val cut = result!!.lastIndexOf('/')
        if (cut != -1) {
            result = result!!.substring(cut + 1)
        }
    }
    return result
}


fun Uri.isVideo(context: Context): Boolean {
    return context.contentResolver.getType(this)?.startsWith("video") ?: false
}
