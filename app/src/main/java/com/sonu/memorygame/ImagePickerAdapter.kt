package com.sonu.memorygame

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sonu.memorygame.databinding.CardImgeBinding
import com.sonu.memorygame.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(
    private val imageUris: List<Uri>,
    private val boardSize: BoardSize,
    private val onImageClicked: () -> Unit,
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = CardImgeBinding.inflate(LayoutInflater.from(parent.context))
        val cardWidth = parent.width / boardSize.getWidth()
        val cardHeight = parent.height / boardSize.getHeight()
        val cardSideLength = min(cardWidth, cardHeight)
        val layoutParams = view.ivCustomImage.layoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return boardSize.getPairs()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < imageUris.size) {
            holder.bind(imageUris[position])
        } else {
            holder.bind()
        }
    }

    inner class ViewHolder(private val binding: CardImgeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.ivCustomImage.setOnClickListener {
                // Should launch intent for user to select photos
                onImageClicked()
            }
        }

        fun bind(uri: Uri) {
            Log.e("TAG", "bind: uri $uri")
            binding.ivCustomImage.setImageURI(uri)
            binding.ivCustomImage.setOnClickListener(null)
        }
    }
}
