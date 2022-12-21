package com.sonu.memorygame.models

enum class BoardSize(val numCards: Int) {
    EASY(8),
    MEDIUM(18),
    HARD(24);

    companion object {
        fun getByValue(value: Int): BoardSize {
            return when (value) {
                4 -> EASY
                9 -> MEDIUM
                else -> HARD
            }
        }
    }

    fun getWidth(): Int {
        return when (this) {
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4
        }
    }

    fun getHeight(): Int {
        return numCards / this.getWidth()
    }

    fun getPairs(): Int {
        return numCards / 2
    }
}