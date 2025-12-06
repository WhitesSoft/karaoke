package com.gvtlaiko.tengokaraoke.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gvtlaiko.tengokaraoke.core.ItemDiffCallback
import com.gvtlaiko.tengokaraoke.data.models.response.Item
import com.gvtlaiko.tengokaraoke.databinding.CardItemBinding
import com.gvtlaiko.tengokaraoke.databinding.CardItemVideoColaBinding

class VideoEnColaAdapter(
    private val videos: MutableList<Item>,
    private val onItemClick: (Item, Int) -> Unit,
    private val onRemoveClick: (Item, Int) -> Unit
) : RecyclerView.Adapter<VideoEnColaAdapter.VideoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        return VideoViewHolder(
            CardItemVideoColaBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val item = videos[position]
        holder.render(item)
    }

    override fun getItemCount(): Int = videos.size

    inner class VideoViewHolder(private val binding: CardItemVideoColaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Listener para el ícono de eliminar
            binding.ivRemoveItem.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveClick(videos[position], position)
                }
            }

            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(videos[position], position)
                }
            }
        }

        fun render(item: Item) {
            with(binding) {
                tvIdVideo.text = item.id.videoId
                tvTitle.text = item.snippet.title
                tvLastMessage.text = item.snippet.channelTitle
                Glide.with(ivCard.context).load(item.snippet.thumbnails.high.url).into(ivCard)
            }
        }
    }

//    fun actualizarVideos(nuevosVideos: List<Item>) {
//        val diffCallback = ItemDiffCallback(this.videos, nuevosVideos)
//        val diffResult = DiffUtil.calculateDiff(diffCallback)
//
//        this.videos.clear()
//        this.videos.addAll(nuevosVideos)
//
//        diffResult.dispatchUpdatesTo(this)
//    }

}