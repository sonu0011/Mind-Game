package com.sonu.memorygame

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.sonu.memorygame.databinding.ActivityCreateGameBinding
import com.sonu.memorygame.models.BoardSize
import com.sonu.memorygame.utils.*
import java.io.ByteArrayOutputStream

class CreateGameActivity : AppCompatActivity() {

    companion object {
        private const val READ_PHOTOS_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
        private const val READ_EXTERNAL_PHOTO_CODE = 456
        private const val PICK_PHOTO_CODE = 123
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14
        private const val TAG = "CreateGameActivity"
    }

    private lateinit var binding: ActivityCreateGameBinding
    private lateinit var imagePickerAdapter: ImagePickerAdapter
    private lateinit var boardSize: BoardSize
    private val chosenImageUris = mutableListOf<Uri>()
    private var numImagesRequired = -1
    private val storage = Firebase.storage
    private val db = Firebase.firestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGameBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        numImagesRequired = boardSize.getPairs()
        supportActionBar?.title = "Choose pics (0 / $numImagesRequired)"
        binding.etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        binding.etGameName.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    binding.btnSave.isEnabled = shouldEnableSaveButton()
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
        )

        binding.btnSave.setOnClickListener {
            saveDataToFirebase()
        }
        imagePickerAdapter = ImagePickerAdapter(chosenImageUris, boardSize, onImageClicked = {
            val isPermissionGranted = isPermissionGranted(this, READ_PHOTOS_PERMISSION)
            if (isPermissionGranted) {
                launchIntentForImages()
            } else {
                requestPermission(this, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTO_CODE)
            }
        })
        binding.rvImagePicker.apply {
            adapter = imagePickerAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@CreateGameActivity, boardSize.getWidth())
        }
    }

    private fun saveDataToFirebase() {
        Log.i(TAG, "Going to save data to Firebase ${chosenImageUris.size}")
        val customGameName = binding.etGameName.text.toString().trim()
        updateButtonAndProgressBarState(visibility = View.VISIBLE)
        db.collection(COLLECTION_GAMES).document(customGameName).get()
            .addOnSuccessListener { document ->
                if (document != null && document.data != null) {
                    AlertDialog.Builder(this)
                        .setTitle("Name taken")
                        .setMessage("A game already exists with the name $customGameName'. Please choose another")
                        .setPositiveButton("OK", null)
                        .show()
                    updateButtonAndProgressBarState(true)
                } else {
                    handleImageUploading(customGameName)
                }
            }
    }

    private fun handleImageUploading(customGameName: String) {
        val uploadedImageUrls = mutableListOf<String>()
        var didEncounterError = false
        for ((index, photoUri) in chosenImageUris.withIndex()) {
            val imageByteArray = getImageByteArray(photoUri)
            val filePath = "images/$customGameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoRef = storage.reference.child(filePath)
            photoRef.putBytes(imageByteArray)
                .continueWithTask { photoUploadTask ->
                    Log.i(TAG, "uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                    photoRef.downloadUrl
                }.addOnCompleteListener { downloadUrlTask ->
                    if (!downloadUrlTask.isSuccessful) {
                        Log.e(TAG, "Exception with Firebase storage", downloadUrlTask.exception)
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if (didEncounterError) {
                        updateButtonAndProgressBarState(true, View.GONE)
                        return@addOnCompleteListener
                    }
//                    binding.progressBar.progress =
//                        uploadedImageUrls.size * 100 / chosenImageUris.size
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrls.add(downloadUrl)
                    Log.i(
                        TAG,
                        "Finished uploading $downloadUrl, Num uploaded: ${uploadedImageUrls.size}"
                    )
                    if (uploadedImageUrls.size == chosenImageUris.size) {
                        handleAllImagesUploaded(customGameName, uploadedImageUrls)
                    }
                }
                .addOnFailureListener {
                    updateButtonAndProgressBarState(true, View.GONE)
                    return@addOnFailureListener
                }
        }
    }

    private fun updateButtonAndProgressBarState(
        isButtonEnabled: Boolean = false,
        visibility: Int = View.GONE
    ) {
        with(binding) {
            pgCreateGame.visibility = visibility
            btnSave.isEnabled = isButtonEnabled
        }
    }

    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        // upload these set of images to Firestore
        db.collection(COLLECTION_GAMES).document(gameName)
            .set(mapOf("images" to imageUrls))
            .addOnCompleteListener { gameCreationTask ->
                updateButtonAndProgressBarState(true)
                if (!gameCreationTask.isSuccessful) {
                    Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
                    Toast.makeText(this, "Failed game creation", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                Log.i(TAG, "Successfully created game $gameName")
                AlertDialog.Builder(this)
                    .setTitle("Upload complete! Let's play your game '$gameName'")
                    .setPositiveButton("OK") { _, _ ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME, gameName)
                        setResult(Activity.RESULT_OK, resultData)
                        finish()
                    }.show()
            }
            .addOnFailureListener {
                updateButtonAndProgressBarState(true)
            }
    }


    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == READ_EXTERNAL_PHOTO_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchIntentForImages()
            } else {
                Toast.makeText(
                    this,
                    "In order to create a custom game, you need to provide access to your photos",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null) {
            return
        }
        val imageUri = data.data
        val clipUri = data.clipData
        if (clipUri != null) {
            val imagesCount = clipUri.itemCount
            for (i in 0 until imagesCount) {
                val clipItem = clipUri.getItemAt(i)
                if (numImagesRequired > i) {
                    chosenImageUris.add(clipItem.uri)
                }
            }
        } else if (imageUri != null) {
            chosenImageUris.add(imageUri)
        }
        imagePickerAdapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics (${chosenImageUris.size} / $numImagesRequired)"
        binding.btnSave.isEnabled = shouldEnableSaveButton()
    }

    private fun shouldEnableSaveButton(): Boolean {
        // Check if we should enable save button or not
        if (chosenImageUris.size != numImagesRequired) return false
        if (binding.etGameName.text.isBlank() || binding.etGameName.text.length < MIN_GAME_NAME_LENGTH) return false
        return true
    }

    private fun launchIntentForImages() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Choose pics"), PICK_PHOTO_CODE)
    }
}