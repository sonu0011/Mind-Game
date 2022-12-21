package com.sonu.memorygame.models

import com.google.firebase.firestore.PropertyName

data class UserImageList(@PropertyName("images") var images: List<String>? = null)

