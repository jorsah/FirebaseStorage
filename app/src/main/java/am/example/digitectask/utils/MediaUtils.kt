package am.example.digitectask.utils

import am.example.digitectask.model.VideoSize
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.StorageConfiguration
import com.google.android.gms.tasks.Task
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MediaUtils(private val context: Context) {

    fun getVideoSize(uri: Uri): Int {
        val fileDescriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
        val fileSize = fileDescriptor!!.length
        fileDescriptor.close()
        return fileSize.toInt()
    }

    suspend fun getThumbnail(uri: Uri): Bitmap? {
        val mMMR = MediaMetadataRetriever()
        withContext(Dispatchers.IO) {
            mMMR.setDataSource(context, uri)
        }
        return mMMR.frameAtTime
    }

    suspend fun compressImage(uri: Uri): Pair<Bitmap?, Int> {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val image = ImageDecoder.decodeBitmap(source)
        val baos = ByteArrayOutputStream()
        val isBm: ByteArrayInputStream
        withContext(Dispatchers.IO) {
            image.compress(
                Bitmap.CompressFormat.JPEG,
                100,
                baos
            )
            var options = 90
            while (baos.toByteArray().size > (1.5).mbToBytes()) {  //Loop if compressed picture is greater than 1.5Mb, than to compression
                baos.reset()
                image.compress(
                    Bitmap.CompressFormat.JPEG,
                    options,
                    baos
                )
                options -= 10
            }
            isBm = ByteArrayInputStream(baos.toByteArray())
        }

        return Pair(BitmapFactory.decodeStream(isBm, null, null), baos.toByteArray().size)
    }

    suspend fun uploadFileToBackend(
        uri: Uri,
        setVisibilityCallback: suspend (show: Boolean) -> Unit
    ) {
        setVisibilityCallback(true)
        val fileType = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(context.contentResolver.getType(uri)) ?: ".mp4"
        val reference: StorageReference = FirebaseStorage.getInstance()
            .getReference("Files/" + System.currentTimeMillis() + "." + fileType)

        reference.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (uriTask.isSuccessful) {
                    val map: HashMap<String, String> = HashMap()
                    map["fileLink"] = uriTask.result.toString()
                    FirebaseDatabase.getInstance().getReference("File")
                        .child("" + System.currentTimeMillis()).setValue(map)
                }
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Failed " + e.message, Toast.LENGTH_SHORT).show()
            }
            .await()
        setVisibilityCallback(false)

    }

    suspend fun getSize(storageReference: StorageReference): Double {
        var fileSize = 0.0
        storageReference.metadata.addOnCompleteListener { metadata ->
            fileSize = metadata.result.sizeBytes.toInt().bytesToMb()
        }.await()
        return fileSize
    }


    suspend fun getMedia(storageReference: StorageReference): String {
        var fileSize = ""
        storageReference.downloadUrl.addOnSuccessListener { uri ->
            fileSize = uri.toString()
        }.await()
        return fileSize
    }

    suspend fun videoCompress(list: List<Uri>, listener: CompressionListener) {

        val size = if (list.isNotEmpty() && list[0].getVideoSize(context).isCompressable()) {
            VideoSize(1280, 780)
        } else list[0].getVideoSize(context)

        withContext(Dispatchers.IO) {
            VideoCompressor.start(
                context = context, // => This is required
                uris = list, // => Source can be provided as content uris
                isStreamable = true,
                storageConfiguration = StorageConfiguration(
                    saveAt = Environment.DIRECTORY_MOVIES, // => the directory to save the compressed video(s). Will be ignored if isExternal = false.
                    isExternal = false, // => false means save at app-specific file directory. Default is true.
                    fileName = null // => an optional value for a custom video name.
                ),
                configureWith = Configuration(
                    quality = VideoQuality.MEDIUM,
                    isMinBitrateCheckEnabled = true,
                    videoBitrate = 3677198, /*Int, ignore, or null*/
                    disableAudio = false, /*Boolean, or ignore*/
                    keepOriginalResolution = false, /*Boolean, or ignore*/
                    videoWidth = size.width.toDouble(), /*Double, ignore, or null*/
                    videoHeight = size.height.toDouble(),/*Double, ignore, or null*/
                ),
                listener = listener
            )
        }
    }

}
