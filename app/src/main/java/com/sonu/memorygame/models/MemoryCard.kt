package com.sonu.memorygame.models

data class MemoryCard(
    val identifier: Int,
    val imageUrl: String? = null,
    var isFaceUp: Boolean = false,
    var isMatchFound: Boolean = false
)
