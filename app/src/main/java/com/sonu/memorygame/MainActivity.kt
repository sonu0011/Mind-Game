package com.sonu.memorygame

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sonu.memorygame.databinding.ActivityMainBinding
import com.sonu.memorygame.databinding.DialogBoardSizeBinding
import com.sonu.memorygame.databinding.DialogDownloadGameBinding
import com.sonu.memorygame.models.BoardSize
import com.sonu.memorygame.models.MemoryGame
import com.sonu.memorygame.models.UserImageList
import com.sonu.memorygame.utils.COLLECTION_GAMES
import com.sonu.memorygame.utils.EXTRA_BOARD_SIZE
import com.sonu.memorygame.utils.EXTRA_GAME_NAME
import com.sonu.memorygame.utils.toggleProgressBar
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {
    private var customGameName: String? = null
    private lateinit var memoryGame: MemoryGame
    private lateinit var memoryBoardAdapter: MemoryBoardAdapter
    private lateinit var binding: ActivityMainBinding
    private var boardSize = BoardSize.EASY
    private val db = Firebase.firestore
    private var gameImagesUrl: List<String>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setupBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                if (!memoryGame.haveWonGame() && memoryGame.getNumMoves() > 0) {
                    showAlertDialog(
                        "Quit your current game?", null
                    ) {
                        setupBoard()
                    }
                } else {
                    setupBoard()
                }
                return true
            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_create_own_game -> {
                showNewCreateOwnGameDialog()
                return true
            }
            R.id.mi_download_game -> {
                showDownloadGameDialog()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showDownloadGameDialog() {
        val downloadDialog = DialogDownloadGameBinding.inflate(LayoutInflater.from(this))
        val gameNameEditText = downloadDialog.edGameName
        showAlertDialog("Download game", downloadDialog.root) {
            val gameName = gameNameEditText.text.toString()
            if (gameName.isNotBlank()) {
                Log.i(TAG, "showDownloadGameDialog: gamename $gameName")
                downloadGame(gameName)
            }
        }
    }

    private fun showNewCreateOwnGameDialog() {
        val boardSizeView = DialogBoardSizeBinding.inflate(LayoutInflater.from(this))
        val radioGroupSize = boardSizeView.radioGroupSize

        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Create your own game", boardSizeView.root) {
            val selectedBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            val intent = Intent(this, CreateGameActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, selectedBoardSize)
            startActivityForResult(intent, CODE_CREATE_GAME)
        }
    }

    private fun showNewSizeDialog() {
        val boardSizeView = DialogBoardSizeBinding.inflate(LayoutInflater.from(this))
        val radioGroupSize = boardSizeView.radioGroupSize

        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Choose new size", boardSizeView.root) {
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameImagesUrl = null
            customGameName = null
            setupBoard()
        }
    }

    private fun showAlertDialog(
        title: String, view: View?, positiveClickListener: View.OnClickListener
    ) {
        AlertDialog.Builder(this).setTitle(title).setView(view).setNegativeButton("Cancel", null)
            .setPositiveButton("OK") { _, _ ->
                positiveClickListener.onClick(null)
            }.show()
    }

    private fun setupBoard() {
        supportActionBar?.title = customGameName ?: getString(R.string.app_name)
        val tvNumMoves = binding.tvNumMoves
        val tvNumPairs = binding.tvNumPairs
        memoryGame = MemoryGame(boardSize, gameImagesUrl)
        when (boardSize) {
            BoardSize.EASY -> {
                tvNumMoves.text = "Easy: 4 x 2"
                tvNumPairs.text = "Pairs: 0/4"
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "Medium: 6 x 3"
                tvNumPairs.text = "Pairs: 0/9"
            }
            BoardSize.HARD -> {
                tvNumMoves.text = "Hard: 6 x 4"
                tvNumPairs.text = "Pairs: 0/12"
            }
        }

        binding.tvNumPairs.setTextColor(
            ContextCompat.getColor(this, R.color.color_progress_none)
        )
        memoryBoardAdapter = MemoryBoardAdapter(
            boardSize, memoryGame.cards
        ) { position: Int ->
            updateGameWithFlip(position)
        }
        binding.rvMemoryBoard.apply {
            layoutManager = GridLayoutManager(this@MainActivity, boardSize.getWidth())
            setHasFixedSize(true)
            adapter = memoryBoardAdapter
        }
    }

    private fun updateGameWithFlip(position: Int) {
        //Error checking
        if (memoryGame.haveWonGame()) {
            Snackbar.make(binding.rootLayout, "You already won!", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (memoryGame.isCardFaceUp(position)) {
            Snackbar.make(binding.rootLayout, "Invalid move!", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (memoryGame.flipCard(position)) {
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(
                    this, R.color.color_progress_full,
                )
            ) as Int
            binding.tvNumPairs.setTextColor(color)
            "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getPairs()}".also {
                binding.tvNumPairs.text = it
            }
            if (memoryGame.haveWonGame()) {
                Snackbar.make(
                    binding.rootLayout,
                    "Congratulations you have won the game!",
                    Snackbar.LENGTH_SHORT
                ).show()
                CommonConfetti.rainingConfetti(
                    binding.rootLayout,
                    intArrayOf(Color.MAGENTA, Color.GREEN, Color.YELLOW)
                ).oneShot()
            }

        }
        "Moves: ${memoryGame.getNumMoves()}".also { binding.tvNumMoves.text = it }
        memoryBoardAdapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) return
        val customGameName = data.getStringExtra(EXTRA_GAME_NAME)
        downloadGame(customGameName)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun downloadGame(mCustomGameName: String?) {
        if (mCustomGameName == null) return
        toggleProgressBar(binding.mainProgressBar, View.VISIBLE)
        db.collection(COLLECTION_GAMES).document(mCustomGameName).get()
            .addOnSuccessListener { game ->
                toggleProgressBar(binding.mainProgressBar)
                val userImageList = game.toObject(UserImageList::class.java)
                if (userImageList?.images == null) {
                    Log.e(TAG, "Invalid custom game data from Firebase")
                    Snackbar.make(
                        binding.rootLayout,
                        "Sorry, we couldn't find any such game, '$mCustomGameName'",
                        Snackbar.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }
                customGameName = mCustomGameName
                gameImagesUrl = userImageList.images

                // Pre-fetch the images for faster loading
                for (imageUrl in userImageList.images!!) {
                    Picasso.get().load(imageUrl).fetch()
                }
                Snackbar.make(
                    binding.rootLayout,
                    "You're now playing $customGameName",
                    Snackbar.LENGTH_LONG
                ).show()
                boardSize = BoardSize.getByValue(gameImagesUrl?.size ?: DEFAULT_NUM_CARDS)
                setupBoard()
            }
            .addOnFailureListener {
                toggleProgressBar(binding.mainProgressBar)
                Snackbar.make(
                    binding.rootLayout,
                    "Sorry, we couldn't find any such game, '$mCustomGameName'",
                    Snackbar.LENGTH_LONG
                ).show()
            }
    }

    companion object {
        private const val CODE_CREATE_GAME = 123
        private const val DEFAULT_NUM_CARDS = 4
        private const val TAG = "MainActivity"
    }
}