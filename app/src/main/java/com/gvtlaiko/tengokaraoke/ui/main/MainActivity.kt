package com.gvtlaiko.tengokaraoke.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.gvtlaiko.tengokaraoke.R
import com.gvtlaiko.tengokaraoke.adapters.GridSpacingItemDecoration
import com.gvtlaiko.tengokaraoke.adapters.SugerenciasAdapter
import com.gvtlaiko.tengokaraoke.adapters.VideoAdapter
import com.gvtlaiko.tengokaraoke.adapters.VideoEnColaAdapter
import com.gvtlaiko.tengokaraoke.core.UIState
import com.gvtlaiko.tengokaraoke.data.models.response.Item
import com.gvtlaiko.tengokaraoke.databinding.ActivityMainBinding
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var videoAdapterEnCola: VideoEnColaAdapter
    private val mainViewModel: MainViewModel by viewModels()
    private val listaVideos = mutableListOf<Item>()
    private val listaVideosEnCola = mutableListOf<Item>()
    private val TAG by lazy { "MainActivity" }
    private lateinit var youTubePlayerView: YouTubePlayerView
    private var youTubePlayer: YouTubePlayer? = null
    private var karaoke = true
    private var reproductorEnUso = false
    private val playerTracker = YouTubePlayerTracker()
    private var actualVideoId: String? = null
    private var actualVideo: Item? = null

    private lateinit var audioManager: AudioManager

    // --- CONFIGURACIÓN DE SEGURIDAD ---
    private val PREFS_NAME = "AppConfig"
    private val KEY_IS_UNLOCKED = "is_app_unlocked"

    private val APP_USER = "tengori"
    private val APP_PASSWORD = "1004"

    private var isPlayerFullscreen = false
    private var playerOriginalParent: FrameLayout? = null
    private var playerOriginalLayoutParams: FrameLayout.LayoutParams? = null

    private var isLoopingEnabled = false

    private lateinit var adapterVideosSugerencias: ArrayAdapter<String>
    private val sugerenciasVideosList = mutableListOf<String>()

    private val playbackSpeeds = listOf(
        PlayerConstants.PlaybackRate.RATE_0_25,
        PlayerConstants.PlaybackRate.RATE_0_5,
        PlayerConstants.PlaybackRate.RATE_1,
        PlayerConstants.PlaybackRate.RATE_1_5,
        PlayerConstants.PlaybackRate.RATE_2
    )
    private val speedLabels = listOf("0.25x", "0.5x", "1x", "1.5x", "2x")
    private var currentSpeedIndex = 2

    var searchJob: Job? = null


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        hideSystemUI()

        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isUnlocked = sharedPref.getBoolean(KEY_IS_UNLOCKED, false)

        if (!isUnlocked) {
            binding.lvContenedor?.isVisible = false
            showPasswordDialog(sharedPref)
        } else {
            binding.lvContenedor?.isVisible = true
            startAppComponents()
        }

    }

    private fun showPasswordDialog(sharedPref: SharedPreferences) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_login, null)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        val etUser = dialogView.findViewById<EditText>(R.id.etUser)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val btnLogin = dialogView.findViewById<Button>(R.id.btnLogin)
        val btnExit = dialogView.findViewById<Button>(R.id.btnExit)

        btnLogin.setOnClickListener {
            val user = etUser.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (user == APP_USER && password == APP_PASSWORD) {
                with(sharedPref.edit()) {
                    putBoolean(KEY_IS_UNLOCKED, true)
                    apply()
                }
                dialog.dismiss()
                startAppComponents()
            } else {
                etPassword.text.clear()
                etPassword.error = "Credenciales incorrectas"
                etPassword.requestFocus()

                // Opcional: Pequeña animación de error en el campo
                etPassword.animate()
                    .translationX(10f)
                    .setDuration(50)
                    .withEndAction {
                        etPassword.animate().translationX(0f).start()
                    }.start()
            }
        }

        btnExit.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnLogin.performClick()
                true
            } else {
                false
            }
        }
    }

    private fun startAppComponents() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        setupUI()
        setupRecycler()
        setupRecyclerVideosEnCola()
        setupYoutubePlayer()
        setupPlayerControles()
        observarListaVideos()
        observarSugerencias()
        setupTVNavigation()
        mainViewModel.getVideos("Musica en tendencia -shorts -tiktok")

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isPlayerFullscreen) {
                    exitCustomFullscreen()
                } else {
                    showExitConfirmationDialog()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun setupTVNavigation() {
        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()
                v.elevation = 10f
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                v.elevation = 0f
            }
        }

        binding.edtxtBusquedaUsuario?.apply {
            onFocusChangeListener = focusListener
            isFocusable = true
            isFocusableInTouchMode = true

            setOnClickListener { view ->
                view.requestFocus() // Aseguramos que tenga el foco
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                // SHOW_IMPLICIT suele funcionar mejor para mostrarlo programáticamente
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }

        }

        binding.ivSearch?.apply {
            onFocusChangeListener = focusListener
            isFocusable = true
        }
        binding.ivLowVideo?.apply {
            onFocusChangeListener = focusListener
            isFocusable = true
        }
        binding.ivFastVideo?.apply {
            onFocusChangeListener = focusListener
            isFocusable = true
        }
        binding.ivReplay?.apply {
            onFocusChangeListener = focusListener
            isFocusable = true
        }
        binding.ivFullscreen?.apply {
            onFocusChangeListener = focusListener
            isFocusable = true
        }
        binding.swKaraoke?.onFocusChangeListener = focusListener
        binding.edtxtBusquedaUsuario?.onFocusChangeListener = focusListener

        // Opcional: dar foco inicial a un elemento
        binding.ivLowVideo?.requestFocus()
    }

    private fun showExitConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_exit_app, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnCancel = dialogView.findViewById<View>(R.id.btnCancel)
        val btnExit = dialogView.findViewById<View>(R.id.btnExit)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnExit.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun setupYoutubePlayer() {

        lifecycle.addObserver(youTubePlayerView)

        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(player: YouTubePlayer) {
                youTubePlayer = player
                reproductorEnUso = false

                player.addListener(playerTracker)

                if (listaVideosEnCola.isNotEmpty()) {
                    iniciarReproduccionEnCola()
                }
            }

            override fun onVideoId(player: YouTubePlayer, videoId: String) {
                actualVideoId = videoId
            }

            override fun onStateChange(player: YouTubePlayer, state: PlayerConstants.PlayerState) {
                when (state) {
                    PlayerConstants.PlayerState.PLAYING,
                    PlayerConstants.PlayerState.PAUSED,
                    PlayerConstants.PlayerState.VIDEO_CUED -> {
                        reproductorEnUso = true
                        binding.llContenedorVideo?.isVisible = false
                        binding.youtubePlayerView.isVisible = true
                    }

                    PlayerConstants.PlayerState.ENDED,
                    PlayerConstants.PlayerState.UNKNOWN,
                    PlayerConstants.PlayerState.UNSTARTED -> reproductorEnUso =
                        false

                    else -> {}
                }

                if (state == PlayerConstants.PlayerState.ENDED) {
                    if (isLoopingEnabled) {
                        youTubePlayer?.seekTo(0f)
                        youTubePlayer?.play()
                    } else {
                        iniciarReproduccionEnCola()
                    }
                }
            }

            override fun onError(player: YouTubePlayer, error: PlayerConstants.PlayerError) {
                super.onError(player, error)
                Log.e(TAG, "YouTube Player Error: $error")
                reproductorEnUso = false
                binding.llContenedorVideo?.isVisible = true
                binding.youtubePlayerView.isVisible = false
                iniciarReproduccionEnCola()
            }
        })
    }

    private fun setupPlayerControles() {
        binding.ivLowVideo?.apply {
            isFocusable = true
            isClickable = true
            setOnClickListener {
                if (currentSpeedIndex > 0) {
                    currentSpeedIndex--
                    updatePlaybackSpeed()
                }
            }
        }

        binding.ivFastVideo?.apply {
            isFocusable = true
            isClickable = true
            setOnClickListener {
                if (currentSpeedIndex < playbackSpeeds.size - 1) {
                    currentSpeedIndex++
                    updatePlaybackSpeed()
                }
            }
        }

        binding.ivReplay?.apply {
            isFocusable = true
            isClickable = true
            setOnClickListener {
                youTubePlayer?.let { player ->
                    player.seekTo(0f)
                    player.play()
                }
            }
        }

        binding.ivFullscreen?.apply {
            isFocusable = true
            isClickable = true
            setOnClickListener {
                if (isPlayerFullscreen) {
                    exitCustomFullscreen()
                } else {
                    enterCustomFullscreen()
                }
            }
        }

        binding.ivSearch?.setOnClickListener {
            realizarBusqueda()
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    private fun enterCustomFullscreen() {
        playerOriginalParent = binding.frameContainer
        playerOriginalLayoutParams = youTubePlayerView.layoutParams as FrameLayout.LayoutParams

        // Ocultamos todo lo demás
        binding.lvContenedor?.visibility = View.GONE

        // Añadimos el player a la raiz
        playerOriginalParent?.removeView(youTubePlayerView)
        binding.main.addView(youTubePlayerView, ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        ))

        isPlayerFullscreen = true
        youTubePlayerView.requestFocus()
    }

    private fun exitCustomFullscreen() {
        binding.main.removeView(youTubePlayerView)
        playerOriginalParent?.addView(youTubePlayerView, playerOriginalLayoutParams)

        binding.lvContenedor?.visibility = View.VISIBLE

        playerOriginalParent = null
        isPlayerFullscreen = false

        // Devolver foco a algún botón
        binding.ivFullscreen?.requestFocus()
    }

    private fun updatePlaybackSpeed() {
        if (youTubePlayer == null) return

        val newSpeedEnum = playbackSpeeds[currentSpeedIndex]
        val newSpeedLabel = speedLabels[currentSpeedIndex]

        youTubePlayer?.setPlaybackRate(newSpeedEnum)
        binding.tvFastVideo?.text = newSpeedLabel
    }

    private fun realizarBusqueda(textoEspecifico: String? = null) {

        val textoBuscado = textoEspecifico ?: binding.edtxtBusquedaUsuario?.text.toString()

        if (textoBuscado.isNotEmpty()) {

            val query = if (karaoke) {
                "$textoBuscado karaoke"
            } else {
                textoBuscado
            }

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.edtxtBusquedaUsuario?.windowToken, 0)


            Log.i(TAG, "Iniciando búsqueda con query: $query")
            mainViewModel.getVideos(query)
        }
    }

    private fun iniciarReproduccionEnCola() {
        if (listaVideosEnCola.isEmpty()) {
            reproductorEnUso = false
            binding.llContenedorVideo?.isVisible = true
            binding.youtubePlayerView.isVisible = false
            return
        }

        val proximoVideo = listaVideosEnCola.first()
        actualVideo = proximoVideo

        youTubePlayer?.loadVideo(proximoVideo.id.videoId, 0f)

        listaVideosEnCola.removeAt(0)
        videoAdapterEnCola.notifyItemRemoved(0)
    }

    @SuppressLint("ResourceType", "UseCompatLoadingForColorStateLists")
    private fun setupUI() {

        adapterVideosSugerencias = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sugerenciasVideosList)
        binding.edtxtBusquedaUsuario?.setAdapter(adapterVideosSugerencias)
        setupSearchListener(binding.edtxtBusquedaUsuario!!)

        // En TV, cuando dan "Enter" en el teclado virtual
        binding.edtxtBusquedaUsuario?.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER) {

                realizarBusqueda()
                // Ocultar teclado
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(textView.windowToken, 0)

                // Mover el foco a los resultados automáticamente para mejor UX
                binding.rv.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }

        binding.edtxtBusquedaUsuario?.setOnItemClickListener { parent, view, position, id ->

            val itemSeleccionado = parent.getItemAtPosition(position) as String

            realizarBusqueda(itemSeleccionado)
            binding.edtxtBusquedaUsuario?.setText("")
            binding.edtxtBusquedaUsuario?.clearFocus()
        }

        binding.swKaraoke?.setOnCheckedChangeListener { _, isChecked ->
            karaoke = isChecked
            realizarBusqueda()
        }

        youTubePlayerView = binding.youtubePlayerView

    }

    private fun setupSearchListener(autoComplete: AutoCompleteTextView) {
        autoComplete.addTextChangedListener { text ->
            searchJob?.cancel()

            searchJob = lifecycleScope.launch {

                delay(500)

                if (!text.isNullOrBlank()) {
                    mainViewModel.getSugerencias(text.toString())
                }
            }
        }
    }

    private fun observarListaVideos() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.stateData.collect { state ->
                    when (state) {
                        is UIState.Empty -> {
                            Log.i("MainActivity", "empty")
                        }

                        is UIState.Error -> Log.i("MainActivity", state.error)
                        UIState.Loading -> {
                            Log.i("MainActivity", "Loading")
                            binding.lvContenedor?.isVisible = false
                            binding.pbLoading?.isVisible = true
                        }

                        is UIState.Success -> {
                            binding.lvContenedor?.isVisible = true
                            binding.pbLoading?.isVisible = false
                            Log.i("MainActivity", state.data.toString())

                            listaVideos.clear()
                            listaVideos.addAll(state.data.items)
                            videoAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    private fun observarSugerencias() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.sugerenciasState.collect { state ->
                    when (state) {
                        is UIState.Success -> {
                            val sugerencias = state.data

                            Log.i(TAG, "Sugerencias de YouTube: $sugerencias")

                            val nuevoAdapter = SugerenciasAdapter(
                                this@MainActivity,
                                sugerencias
                            )

                            binding.edtxtBusquedaUsuario?.let { autoComplete ->
                                autoComplete.setAdapter(nuevoAdapter)

                                autoComplete.dropDownWidth = android.view.ViewGroup.LayoutParams.MATCH_PARENT

                                if (autoComplete.hasFocus() && sugerencias.isNotEmpty()) {
                                    autoComplete.showDropDown()
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupRecycler() {

        videoAdapter = VideoAdapter(listaVideos) { item -> onClickListener(item) }

        binding.rv.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3)
            adapter = videoAdapter

            setHasFixedSize(true)
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.grid_item_spacing)

        val itemDecoration = GridSpacingItemDecoration(
            3,
            spacingInPixels,
            true
        )
        binding.rv.addItemDecoration(itemDecoration)

    }

    private fun onClickListener(item: Item) {
        listaVideosEnCola.add(item)
        videoAdapterEnCola.notifyItemInserted(listaVideosEnCola.size - 1)
        binding.rvEnCola?.scrollToPosition(listaVideosEnCola.size - 1)
    }

    private fun setupRecyclerVideosEnCola() {
        videoAdapterEnCola = VideoEnColaAdapter(
            listaVideosEnCola,
            onItemClick = { item, position ->
                onPlayVideoFromQueue(item, position)
            },
            onRemoveClick = { item, position ->
                onRemoveVideoFromQueue(item, position)
            }
        )

        binding.rvEnCola.apply {
            this?.layoutManager = LinearLayoutManager(this@MainActivity)
            this?.adapter = videoAdapterEnCola
        }
    }

    private fun onRemoveVideoFromQueue(item: Item, position: Int) {
        listaVideosEnCola.removeAt(position)
        videoAdapterEnCola.notifyItemRemoved(position)
        videoAdapterEnCola.notifyItemRangeChanged(position, listaVideosEnCola.size - position)
    }

    private fun onPlayVideoFromQueue(item: Item, position: Int) {
        binding.llContenedorVideo?.isVisible = false
        binding.youtubePlayerView.isVisible = true
        reproductorEnUso = true

        youTubePlayer?.loadVideo(item.id.videoId, 0f)
        actualVideo = item

        listaVideosEnCola.removeAt(position)
        videoAdapterEnCola.notifyItemRemoved(position)
        videoAdapterEnCola.notifyItemRangeChanged(position, listaVideosEnCola.size - position)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && isPlayerFullscreen) {
            exitCustomFullscreen()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        youTubePlayerView.release()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

}