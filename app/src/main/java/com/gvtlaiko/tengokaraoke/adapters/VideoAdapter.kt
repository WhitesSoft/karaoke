package com.gvtlaiko.tengokaraoke.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gvtlaiko.tengokaraoke.core.ItemDiffCallback
import com.gvtlaiko.tengokaraoke.data.models.response.Item
import com.gvtlaiko.tengokaraoke.databinding.CardItemBinding

class VideoAdapter(
    private val videos: MutableList<Item>,
    private val onClickListener: (Item) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        return VideoViewHolder(
            CardItemBinding.inflate(
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

    inner class VideoViewHolder(private val binding: CardItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun render(item: Item) {
            with(binding) {
                tvIdVideo.text = item.id.videoId
                tvTitle.text = item.snippet.title
                tvLastMessage.text = item.snippet.channelTitle
                Glide.with(ivCard.context).load(item.snippet.thumbnails.high.url).into(ivCard)
            }
            binding.root.setOnClickListener { onClickListener(item) }
        }
    }

}