package com.gvtlaiko.tengokaraoke.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
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
import android.widget.TextView
import android.widget.Toast
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
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SonicAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var videoAdapterEnCola: VideoEnColaAdapter
    private val mainViewModel: MainViewModel by viewModels()
    private val listaVideos = mutableListOf<Item>()
    private val listaVideosEnCola = mutableListOf<Item>()
    private val TAG by lazy { "MainActivity" }
    private var karaoke = true
    private var actualVideo: Item? = null

    private var exoPlayer: ExoPlayer? = null

    // Pitch y Velocidad
    private var currentPitch: Float = 1.0f // 1.0 es normal. 1.1 es más agudo. 0.9 más grave.
    private var currentSpeed: Float = 1.0f

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

        NewPipe.init(
            DownloaderImpl.getInstance(),
            Localization.fromLocale(Locale.getDefault()),
            ContentCountry("US")
        )

        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
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

    private fun startAppComponents() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager


        setupUI()
        setupRecycler()
        setupRecyclerVideosEnCola()
        setupPlayerControles()
        observarListaVideos()
        observarSugerencias()
        observarStreamUrl() // newpipe
        setupTVNavigation()

        Log.i(TAG, "entra hasta aqui")

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
                view.requestFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
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
        binding.ivBajarTonalidad?.apply {
            onFocusChangeListener = focusListener
            isFocusable = true
        }
        binding.ivSubirTonalidad?.apply {
            onFocusChangeListener = focusListener
            isFocusable = true
        }
        binding.swKaraoke?.onFocusChangeListener = focusListener
        binding.edtxtBusquedaUsuario?.onFocusChangeListener = focusListener

        binding.ivLowVideo?.requestFocus()
    }

    private fun observarStreamUrl() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.videoUrlState.collect { state ->
                    when (state) {
                        is UIState.Loading -> {

                        }

                        is UIState.Success -> {
                            val url = state.data
                            playExoPlayer(url)
                        }

                        is UIState.Error -> {
                            Log.i(TAG, state.error)
                            Toast.makeText(
                                this@MainActivity,
                                "Error obteniendo video: ${state.error}",
                                Toast.LENGTH_SHORT
                            ).show()
                            iniciarReproduccionEnCola()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(UnstableApi::class)
    private fun playExoPlayer(url: String) {
        try {
            Log.i(TAG, "Iniciando ExoPlayer Oficial con URL: $url")

            // Liberamos el player anterior si existe
            exoPlayer?.release()

            // configuración de Sonic (Para que funcione el cambio de Tono/Velocidad)
            val renderersFactory = object : DefaultRenderersFactory(this) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean
                ): AudioSink? {
                    return DefaultAudioSink.Builder(this@MainActivity)
                        .setAudioProcessors(arrayOf(SonicAudioProcessor()))
                        .build()
                }
            }

            // configuración de Red (User Agent)
            val userAgent =
                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setAllowCrossProtocolRedirects(true)

            val mediaItem = MediaItem.fromUri(url)

            // creamos el player
            exoPlayer = ExoPlayer.Builder(this, renderersFactory)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .build()

            // Agregamos el listener al NUEVO player creado
            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        if (isLoopingEnabled) {
                            exoPlayer?.seekTo(0)
                            exoPlayer?.play()
                        } else {
                            // Cuando termina, llama al siguiente video
                            iniciarReproduccionEnCola()
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Error en ExoPlayer: ${error.message}")
                    Toast.makeText(
                        this@MainActivity,
                        "Error: saltando al siguiente",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Si falla, intentamos reproducir el siguiente para no trabar la app
                    iniciarReproduccionEnCola()
                }
            })

            // Restauramos velocidad y tono si ya estaban cambiados
            updatePitchAndSpeed()

            binding.playerView?.player = exoPlayer
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true

            // Manejo de visibilidad
            binding.playerView?.isVisible = true
            binding.llContenedorVideo?.isVisible = false

        } catch (e: Exception) {
            Log.e(TAG, "Error general: ${e.message}")
            binding.playerView?.isVisible = false
            binding.llContenedorVideo?.isVisible = true
            // Si falla la inicialización, pasamos al siguiente
            iniciarReproduccionEnCola()
        }
    }

    private fun setupPlayerControles() {

        binding.ivBajarTonalidad?.apply {
            isFocusable = true
            isClickable = true
            setOnClickListener {
                changePitch(-0.05f)
            }
        }

        binding.ivSubirTonalidad?.apply {
            isFocusable = true
            isClickable = true
            setOnClickListener {
                changePitch(0.05f)
            }
        }

        binding.ivBajarTonalidad?.apply {
            isFocusable = true
            isClickable = true
            setOnLongClickListener {
                resetPitch()
                true
            }
        }
        binding.ivSubirTonalidad?.apply {
            isFocusable = true
            isClickable = true
            setOnLongClickListener {
                resetPitch()
                true
            }
        }

        binding.ivReplay?.apply {
            isFocusable = true
            isClickable = true
            setOnClickListener {
                exoPlayer?.seekTo(0)
            }
        }

        binding.ivBucleVideo?.apply {
            isFocusable = true
            isClickable = true
            setOnClickListener {
                isLoopingEnabled = !isLoopingEnabled
                binding.ivBucleVideo?.setColorFilter(
                    if (isLoopingEnabled) getColor(R.color.primary_dark) else getColor(R.color.white)
                )
            }
        }

        binding.ivFastVideo?.apply {
            isFocusable = true
            isClickable = true
            setOnClickListener {
                if (currentSpeed < 2.0f)
                    currentSpeed += 0.20f
                updatePitchAndSpeed()
               actualizarIndicador(binding.tvFastVideo, currentSpeed)
            }
        }

        binding.ivLowVideo?.apply {
            isFocusable = true
            isClickable = true
            setOnClickListener {
                if (currentSpeed > 0.5f)
                    currentSpeed -= 0.20f
                updatePitchAndSpeed()
                actualizarIndicador(binding.tvFastVideo, currentSpeed)
            }
        }

        binding.ivFullscreen?.apply {
            isFocusable = true
            isClickable = true
            setOnClickListener {
                if (isPlayerFullscreen) exitCustomFullscreen() else enterCustomFullscreen()
            }
        }

        binding.ivSearch?.setOnClickListener { realizarBusqueda() }

    }

    private fun actualizarIndicador(textView: TextView?, valor: Float) {
        if (textView == null) return

        when {
            valor > 1.0f -> {
                textView.text = "→"
                textView.setTextColor(Color.RED) // O Color.parseColor("#00FF00")
            }

            valor < 1.0f -> {
                textView.text = "←"
                textView.setTextColor(Color.RED)   // O Color.parseColor("#FF0000")
            }

            else -> {
                textView.text = "·"
                textView.setTextColor(Color.WHITE) // Color normal
            }
        }
    }

     private fun obtenerSimbolo(valor: Float): String {
        return when {
            valor > 1.0f -> "→"
            valor < 1.0f -> "←"
            else -> "·"
        }
    }

    private fun resetPitch() {
        currentPitch = 1.0f
        actualizarIndicador(binding.tvTonalidadVideo, currentPitch)
        updatePitchAndSpeed()
    }

    private fun changePitch(delta: Float) {
        currentPitch += delta
        currentPitch = (currentPitch * 10).roundToInt() / 10.0f
        // limites (0.5 a 2.0) grave - ardilla
        currentPitch = currentPitch.coerceIn(0.5f, 2.0f)
        updatePitchAndSpeed()

        Log.i(TAG, "Nuevo Tono: $currentPitch")
        actualizarIndicador(binding.tvTonalidadVideo, currentPitch)
    }

    private fun updatePitchAndSpeed() {
        val params = PlaybackParameters(currentSpeed, currentPitch)
        exoPlayer?.playbackParameters = params
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
        playerOriginalParent =
            binding.frameContainer

        playerOriginalLayoutParams = binding.playerView?.layoutParams as FrameLayout.LayoutParams

        playerOriginalParent?.removeView(binding.playerView)

        val rootLayout = binding.main
        val fullScreenParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )

        rootLayout.addView(binding.playerView, fullScreenParams)

        binding.lvContenedor?.isVisible = false
        hideSystemUI()

        binding.ivFullscreen?.setImageResource(R.drawable.ic_close_fullscreen)
        isPlayerFullscreen = true
    }

    private fun exitCustomFullscreen() {
        binding.main.removeView(binding.playerView)

        playerOriginalParent?.addView(binding.playerView, playerOriginalLayoutParams)

        playerOriginalParent = null
        playerOriginalLayoutParams = null

        binding.lvContenedor?.isVisible = true
        showSystemUI()

        binding.ivFullscreen?.setImageResource(R.drawable.ic_open_with)
        isPlayerFullscreen = false
    }

    private fun showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
        }
    }

    private fun realizarBusqueda(textoEspecifico: String? = null) {

        val textoBuscado = textoEspecifico ?: binding.edtxtBusquedaUsuario?.text.toString()

        if (textoBuscado.isNotEmpty()) {

            val query = if (karaoke) {
                "$textoBuscado karaoke"
            } else {
                textoBuscado
            }

            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.edtxtBusquedaUsuario?.windowToken, 0)


            Log.i(TAG, "Iniciando búsqueda con query: $query")
            mainViewModel.getVideos(query)
        }
    }

    private fun iniciarReproduccionEnCola() {
        if (listaVideosEnCola.isEmpty()) {
            if (isPlayerFullscreen) {
                exitCustomFullscreen()
                binding.llContenedorVideo?.isVisible = true
                binding.playerView?.isVisible = false
            }
            binding.llContenedorVideo?.isVisible = true
            binding.playerView?.isVisible = false
            return
        }

        val proximoVideo = listaVideosEnCola.first()
        onPlayVideoFromQueue(proximoVideo, 0)
    }

    @SuppressLint("ResourceType", "UseCompatLoadingForColorStateLists")
    private fun setupUI() {

        adapterVideosSugerencias =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sugerenciasVideosList)
        binding.edtxtBusquedaUsuario?.setAdapter(adapterVideosSugerencias)
        setupSearchListener(binding.edtxtBusquedaUsuario!!)

        binding.edtxtBusquedaUsuario?.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER
            ) {

                realizarBusqueda()

                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(textView.windowToken, 0)

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

                        is UIState.Error -> Log.i("MainActivity", "${state.error} error")
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

                            binding.rv.scrollToPosition(0)
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

                                autoComplete.dropDownWidth =
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT

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
        Log.i(TAG, "Solicitando URL para: ${item.snippet.title}")

        actualVideo = item

        mainViewModel.getStreamUrl(item.id.videoId)

        listaVideosEnCola.removeAt(position)
        videoAdapterEnCola.notifyItemRemoved(position)
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }

}
