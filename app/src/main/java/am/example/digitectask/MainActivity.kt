package am.example.digitectask

import am.example.digitectask.databinding.ActivityMainBinding
import am.example.digitectask.model.ItemModel
import am.example.digitectask.utils.MediaUtils
import am.example.digitectask.utils.bytesToMb
import am.example.digitectask.utils.getFileName
import am.example.digitectask.utils.isVideo
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ListAdapter
    private lateinit var mediaHelper: MediaUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadListFromFireBase(FirebaseStorage.getInstance().reference)
        initRecyclerView()
        mediaHelper = MediaUtils(applicationContext)

        val pickMultipleMedia =
            registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
                lifecycleScope.launch {
                    showProgressBar(true)
                    uris.forEach { uri ->
                        selectAndUploadFile(uri)
                    }
                    val videoList = uris.filter { uri -> uri.isVideo(this@MainActivity) }
                    if (videoList.isNotEmpty())
                        mediaHelper.videoCompress(
                            videoList,
                            listener
                        )
                }
            }

        binding.button.setOnClickListener {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
    }

    private val listener = object : CompressionListener {
        override fun onProgress(index: Int, percent: Float) {
            runOnUiThread {}
        }

        override fun onStart(index: Int) {}

        override fun onSuccess(index: Int, size: Long, path: String?) {
            lifecycleScope.launch {
                mediaHelper.uploadFileToBackend(Uri.fromFile(File(path!!)), ::showProgressBar)
            }
        }

        override fun onFailure(index: Int, failureMessage: String) {}
        override fun onCancelled(index: Int) {}
    }

    private fun loadListFromFireBase(storageReference: StorageReference) {
        storageReference.child("Files").listAll().addOnSuccessListener { result ->
            lifecycleScope.launch(Dispatchers.IO) {
                showProgressBar(true)
                result.items.forEach {
                    withContext(Dispatchers.Main) {
                        adapter.addItem(
                            ItemModel(
                                name = it.name,
                                url = mediaHelper.getMedia(it),
                                size = mediaHelper.getSize(it)
                            )
                        )
                    }
                }
                showProgressBar(false)
            }
        }
    }

    private suspend fun selectAndUploadFile(uri: Uri) {
        if (uri.isVideo(this)) {
            withContext(Dispatchers.Main) {
                adapter.addItem(
                    ItemModel(
                        thumbnail = mediaHelper.getThumbnail(uri),
                        name = uri.getFileName(this@MainActivity) ?: "",
                        size = mediaHelper.getVideoSize(uri).bytesToMb()
                    )
                )
            }
        } else {
            val src = mediaHelper.compressImage(uri)
            mediaHelper.uploadFileToBackend(uri, ::showProgressBar)
            withContext(Dispatchers.Main) {
                adapter.addItem(
                    ItemModel(
                        thumbnail = src.first,
                        name = uri.getFileName(this@MainActivity) ?: "",
                        size = src.second.bytesToMb()
                    )
                )
            }
        }
    }

    private fun initRecyclerView() {
        adapter = ListAdapter(applicationContext)
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
    }

    private suspend fun showProgressBar(show: Boolean) {
        withContext(Dispatchers.Main) {
            binding.progress.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

}

