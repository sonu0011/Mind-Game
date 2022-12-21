package com.sonu.memorygame.utils

import android.view.View
import android.widget.ProgressBar

fun toggleProgressBar(progressBar: ProgressBar, visibility: Int = View.GONE) {
    progressBar.visibility = visibility
}
