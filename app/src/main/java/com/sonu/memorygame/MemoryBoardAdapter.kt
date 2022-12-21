package com.sonu.memorygame

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.sonu.memorygame.databinding.ItemMemoryCardBinding
import com.sonu.memorygame.models.BoardSize
import com.sonu.memorygame.models.MemoryCard
import com.squareup.picasso.Picasso
import kotlin.math.min


class MemoryBoardAdapter(
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    private val onCardClicked: (position: Int) -> Unit,
) : RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val rvHeight = (parent.height / boardSize.getHeight()) - 2 * MARGIN_SIZE
        val rvWidth = (parent.width / boardSize.getWidth()) - 2 * MARGIN_SIZE
        val minSideLength = min(rvHeight, rvWidth)

        val view = ItemMemoryCardBinding.inflate(LayoutInflater.from(parent.context))
        val params = view.cardView.layoutParams as MarginLayoutParams
        params.height = minSideLength
        params.width = minSideLength
        params.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = boardSize.numCards

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(private val binding: ItemMemoryCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val imageButton = binding.imageButton
            val memoryCard = cards[position]
            if (memoryCard.isFaceUp) {
                if (memoryCard.imageUrl != null) {
                    Picasso.get().load(memoryCard.imageUrl).into(imageButton)
                } else {
                    imageButton.setImageResource(memoryCard.identifier)
                }
            } else {
                imageButton.setImageResource(R.drawable.bamboo)
            }
            imageButton.setOnClickListener {
                onCardClicked(position)
            }
            imageButton.alpha = if (memoryCard.isMatchFound) .4f else 1f
            val colorStateList = if (memoryCard.isMatchFound) ContextCompat.getColorStateList(
                imageButton.context,
                R.color.color_grey
            ) else null
            ViewCompat.setBackgroundTintList(imageButton, colorStateList)
        }
    }

    companion object {
        private const val MARGIN_SIZE = 10
        private const val TAG = "MemoryBoardAdapter"

    }

}