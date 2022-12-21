package com.sonu.memorygame.models

import com.sonu.memorygame.utils.DEFAULT_ICONS

class MemoryGame(private val boardSize: BoardSize, private val gameImagesUrl: List<String>?) {


    val cards: List<MemoryCard>
    var numPairsFound = 0
    var lastFlippedPosition: Int? = null
    private var numMoves = 0

    init {
        cards = if (gameImagesUrl == null) {
            val defaultIcons = DEFAULT_ICONS.shuffled().take(boardSize.getPairs())
            val randomizedIcons = (defaultIcons + defaultIcons).shuffled()
            randomizedIcons.map { MemoryCard(it) }
        } else {
            val randomImages = (gameImagesUrl + gameImagesUrl).shuffled()
            val imagesUrl =
                randomImages.map { MemoryCard(identifier = it.hashCode(), imageUrl = it) }
            imagesUrl
        }
    }

    fun flipCard(position: Int): Boolean {
        numMoves++
        //prev 0 flips => restore card + flip the card
        //prev 1 flip => check for pair match
        //prev 2 flips => restore card + flip the card

        var isMatched = false
        // 0 or 2 prev flip the card
        if (lastFlippedPosition == null) {
            restoreCards()
            lastFlippedPosition = position
        } else {
            // 1 prev flip the card
            isMatched = checkMatch(lastFlippedPosition!!, position)
            lastFlippedPosition = null
        }
        val card = cards[position]
        card.isFaceUp = !card.isFaceUp

        return isMatched
    }

    private fun restoreCards() {
        for (card in cards) {
            if (!card.isMatchFound) {
                card.isFaceUp = false
            }
        }
    }

    private fun checkMatch(prevFlippedPosition: Int, currentPosition: Int): Boolean {
        return if (cards[prevFlippedPosition].identifier != cards[currentPosition].identifier) {
            false
        } else {
            cards[prevFlippedPosition].isMatchFound = true
            cards[currentPosition].isMatchFound = true
            numPairsFound++
            true
        }
    }

    fun haveWonGame(): Boolean {
        return numPairsFound == boardSize.getPairs()
    }

    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    fun getNumMoves() = numMoves / 2
}