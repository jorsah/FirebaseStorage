package am.example.digitectask

import am.example.digitectask.model.ItemModel
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition


class ListAdapter(private val context: Context) :
    RecyclerView.Adapter<ListAdapter.ViewHolder>() {
    private val mList: MutableList<ItemModel> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemModel = mList[position]

        if (itemModel.url != null) {
            Glide.with(context).load(itemModel.url).into(holder.imageView)
        }
        with(holder) {
            imageView.visibility = View.VISIBLE
            Glide.with(context)
                .asBitmap()
                .load(itemModel.thumbnail)
                .override(300, 200)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        imageView.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
            name.text = itemModel.name
            size.text = itemModel.size.toString() + "mb"
        }

    }

    override fun getItemCount() = mList.size

    fun addItem(itemModel: ItemModel) {
        mList.add(0, itemModel)
        notifyDataSetChanged()
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.thumbnail_image)
        val name: TextView = itemView.findViewById(R.id.name)
        val size: TextView = itemView.findViewById(R.id.size)
    }
}
