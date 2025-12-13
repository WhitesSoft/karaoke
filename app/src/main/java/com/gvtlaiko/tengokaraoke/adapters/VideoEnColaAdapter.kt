package com.gvtlaiko.tengokaraoke.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gvtlaiko.tengokaraoke.R
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
            itemView.isFocusable = false
            itemView.isFocusableInTouchMode = false
            itemView.isClickable = false

            with(binding) {
                tvIdVideo.text = item.id.videoId
                tvTitle.text = item.snippet.title
                tvLastMessage.text = item.snippet.channelTitle
                Glide.with(ivCard.context).load(item.snippet.thumbnails.high.url).into(ivCard)

                containerInfoPlay.setOnClickListener {
                    // Usamos bindingAdapterPosition en lugar del deprecated adapterPosition
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        onItemClick(item, bindingAdapterPosition)
                    }
                }

                containerInfoPlay.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start()
                        v.elevation = 8f
                        v.background = null
                    } else {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                        v.elevation = 0f
                        v.background = null
                    }
                }

                // --- ZONA 2: REMOVE (Botón de Basura) ---
                ivRemoveItem.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        onRemoveClick(item, bindingAdapterPosition)
                    }
                }

                // Animación de foco para el botón de borrar
                ivRemoveItem.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(150).start()
                    } else {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    }
                }
            }
        }
    }

}