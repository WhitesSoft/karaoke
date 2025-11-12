package com.gvtlaiko.tengokaraoke.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.mediarouter.app.MediaRouteButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage
import com.gvtlaiko.tengokaraoke.R
import com.gvtlaiko.tengokaraoke.adapters.GridSpacingItemDecoration
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
    private var karaoke = false
    private var reproductorEnUso = false
    private val playerTracker = YouTubePlayerTracker()
    private var actualVideoId: String? = null
    private var actualVideo: Item? = null
    private var isMuted = false
    private lateinit var audioManager: AudioManager
    private lateinit var castContext: CastContext
    private var castSession: CastSession? = null


    private val volumeHandler = android.os.Handler(Looper.getMainLooper())
    private var isVolumeSessionActive = false


    private val VOLUME_SESSION_TIMEOUT = 2000L
    private val volumeSessionTimeoutRunnable = Runnable {
        isVolumeSessionActive = false
    }

    // Listener para saber cuando nos conectamos/desconectamos
    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            castSession = session
            invalidateOptionsMenu() // Actualiza el menú para mostrar el botón conectado
            Toast.makeText(this@MainActivity, "Conectado al Chromecast", Toast.LENGTH_SHORT).show()

            if (actualVideo != null && reproductorEnUso) {
                Toast.makeText(this@MainActivity, "Transfiriendo video a la TV...", Toast.LENGTH_SHORT).show()

                // Envía el video actual a la TV
                loadRemoteMedia(actualVideo!!)
            }
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            castSession = session
            invalidateOptionsMenu()
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            castSession = null
            invalidateOptionsMenu()
            Toast.makeText(this@MainActivity, "Desconectado del Chromecast", Toast.LENGTH_SHORT)
                .show()
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {}
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
    }


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

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        castContext = CastContext.getSharedInstance(this)


        setupUI()
        setupRecycler()
        setupRecyclerVideosEnCola()
        setupYoutubePlayer()
        setupPlayerControles()
        getData()
        iniciarCastPlayer()

        binding.wvYoutube?.settings?.userAgentString =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"

        binding.wvYoutube?.settings?.javaScriptEnabled = true
        binding.wvYoutube?.isVisible = false

    }

    private fun iniciarCastPlayer() {

        val mediaRouteButton = findViewById<MediaRouteButton>(R.id.ivShare)
        CastButtonFactory.setUpMediaRouteButton(applicationContext, mediaRouteButton)

    }

    // Dentro de MainActivity.kt
    private fun loadRemoteMedia(video: Item) {
        Log.i(TAG, "Cast: $video")
        if (castSession == null) {
            return // No estamos conectados, no hacemos nada
        }


        // Pausa el reproductor local
        youTubePlayer?.pause()

        // Construye el objeto MediaInfo que necesita el Chromecast
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        movieMetadata.putString(MediaMetadata.KEY_TITLE, video.snippet.title)
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, video.snippet.channelTitle)
        movieMetadata.addImage(WebImage(Uri.parse(video.snippet.thumbnails.high.url)))

        // ¡La magia está aquí! El ID de contenido es solo el videoId de YouTube.
        val mediaInfo = MediaInfo.Builder(video.id.videoId)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("videos/mp4")
            .setMetadata(movieMetadata)
            .build()

        // Carga el video en el reproductor remoto
        val remoteMediaClient = castSession!!.remoteMediaClient
        remoteMediaClient?.load(mediaInfo, true) // `true` para autoplay

        Toast.makeText(this, "Reproduciendo en TV: ${video.snippet.title}", Toast.LENGTH_LONG)
            .show()

        // Aquí deberías cambiar tu UI para mostrar un control remoto en lugar del reproductor
        // showRemoteControlsUI()
    }

    private fun adjustSystemVolume(direction: Int) {
        volumeHandler.removeCallbacks(volumeSessionTimeoutRunnable)

        val flags: Int
        if (isVolumeSessionActive) {
            flags = AudioManager.FLAG_PLAY_SOUND
        } else {
            isVolumeSessionActive = true
            flags = AudioManager.FLAG_SHOW_UI
        }

        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction, // Usa la dirección (subir o bajar) que nos pasaron
            flags
        )
        volumeHandler.postDelayed(volumeSessionTimeoutRunnable, VOLUME_SESSION_TIMEOUT)
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
//                if (listaVideosEnCola.isNotEmpty() && !reproductorEnUso) {
//                    iniciarReproduccionEnCola()
//                }
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
                    iniciarReproduccionEnCola()
                }
            }

            override fun onError(player: YouTubePlayer, error: PlayerConstants.PlayerError) {
                super.onError(player, error)
                Toast.makeText(
                    this@MainActivity,
                    "Error al cargar el video: $error",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "YouTube Player Error: $error")
                // Al haber un error, consideramos que el reproductor está libre e intentamos el siguiente
                reproductorEnUso = false
                binding.llContenedorVideo?.isVisible = true
                binding.youtubePlayerView.isVisible = false
                iniciarReproduccionEnCola()
            }
        })
    }

    private fun setupPlayerControles() {
        binding.ivNextVideo?.setOnClickListener {
            //Toast.makeText(this, "Reproduciendo el siguiente video...", Toast.LENGTH_SHORT).show()
            iniciarReproduccionEnCola() // Reutilizamos la lógica que ya funciona
        }

        binding.ivReplay?.setOnClickListener {
            youTubePlayer?.let { player ->
                player.seekTo(0f) // Vuelve al segundo 0
                player.play()     // Asegura que se reproduzca
                Toast.makeText(this, "Repitiendo video", Toast.LENGTH_SHORT).show()
            }
        }

//        binding.ivShare?.setOnClickListener {
//
//            if (castSession != null && castSession!!.isConnected) {
//                if (actualVideo != null)
//                    loadRemoteMedia(actualVideo!!)
//            }
//
////            if (actualVideoId == null) {
////                Toast.makeText(this, "No hay video para compartir", Toast.LENGTH_SHORT).show()
////                return@setOnClickListener
////            }
////            val videoUrl = "https://www.youtube.com/watch?v=$actualVideoId"
////            val shareIntent = Intent(Intent.ACTION_SEND).apply {
////                type = "text/plain"
////                putExtra(Intent.EXTRA_TEXT, videoUrl)
////            }
////            startActivity(Intent.createChooser(shareIntent, "Compartir video"))
//        }

        binding.ivMicroOff?.setOnClickListener {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_TOGGLE_MUTE, // Alterna entre mute/unmute
                AudioManager.FLAG_SHOW_UI
            )
//            youTubePlayer?.let { player ->
//                isMuted = !isMuted // Cambia el estado
//                if (isMuted) {
//                    // Guarda el volumen actual y luego silencia
//                    //ultimoNivelVolumen = playerTracker.volume
//                    player.setVolume(0)
//                    //binding?.ivMicroOff.setImageResource(R.drawable.ic_volume_on) // Cambia el ícono
//                    Toast.makeText(this, "Silenciado", Toast.LENGTH_SHORT).show()
//                } else {
//                    // Restaura el volumen anterior
//                    player.setVolume(actualVolumen)
////                    binding.ivMicroOff.setImageResource(R.drawable.ic_volume_off) // Restaura el ícono
//                    Toast.makeText(this, "Sonido activado", Toast.LENGTH_SHORT).show()
//                }
//            }
        }

        binding.ivVolumeDown?.setOnClickListener {
//            actualVolumen = (actualVolumen - 10).coerceIn(0, 100)
//            youTubePlayer?.setVolume(actualVolumen)
//            isMuted = actualVolumen == 0 // Si el volumen es 0, está silenciado
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
            // adjustSystemVolume(AudioManager.ADJUST_LOWER)
        }


        binding.ivVolumeUp?.setOnClickListener {
            //adjustSystemVolume(AudioManager.ADJUST_RAISE)
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            )
////            actualVolumen = (actualVolumen + 10).coerceIn(0, 100)
//            youTubePlayer?.setVolume(actualVolumen)
//            // Si subimos el volumen, ya no puede estar silenciado
//            if (isMuted && actualVolumen > 0) {
//                isMuted = false
//                binding.ivMicroOff?.setImageResource(R.drawable.ic_volume_off)
//            }
        }
    }

    private fun realizarBusqueda() {
        val textoBuscado = binding.edtxtBusquedaUsuario?.text.toString()

        if (textoBuscado.isNotEmpty()) {

            val query = if (karaoke) {
                "$textoBuscado karaoke"
            } else {
                textoBuscado
            }

            Log.i(TAG, "Iniciando búsqueda con query: $query")
            mainViewModel.getVideos(query, 35)
        }
    }

    private fun iniciarReproduccionEnCola() {
        if (listaVideosEnCola.isEmpty()) {
            Toast.makeText(this, "La cola de reproduccion está vacia", Toast.LENGTH_SHORT).show()
            reproductorEnUso = false
//            binding.llContenedorVideo?.isVisible = true
//            binding.youtubePlayerView.isVisible = false
            return
        }


        // 1. Toma el primer video de la cola (FIFO)
        val proximoVideo = listaVideosEnCola.first()
        actualVideo = proximoVideo

        // 2. Cárgalo en el reproductor
        youTubePlayer?.loadVideo(proximoVideo.id.videoId, 0f)

        binding.wvYoutube?.loadUrl("https://www.youtube.com/watch?v=${proximoVideo.id.videoId}")

        // 3. Elimínalo de la lista de datos
        listaVideosEnCola.removeAt(0)

        // 4. Notifica al adaptador que el ítem en la posición 0 fue eliminado
        videoAdapterEnCola.notifyItemRemoved(0)
    }


    @SuppressLint("ResourceType", "UseCompatLoadingForColorStateLists")
    private fun setupUI() {
        binding.tilBusquedaUsuario?.setBoxStrokeColorStateList(resources.getColorStateList(R.drawable.selector_outline))

        binding.edtxtBusquedaUsuario?.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                realizarBusqueda()

                // Oculta el teclado
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(textView.windowToken, 0)

                return@setOnEditorActionListener true
            }
            false
        }

        binding.swKaraoke?.setOnCheckedChangeListener { _, isChecked ->
            karaoke = isChecked
            realizarBusqueda()
        }

//        val youTubePlayerView: YouTubePlayerView? = binding.youtubePlayerView
//        lifecycle.addObserver(youTubePlayerView)
        //val youTubePlayerView: YouTubePlayerView? = binding.youtubePlayerView

        youTubePlayerView = binding.youtubePlayerView
        // binding.youtubePlayerView.let { playerView ->
//        lifecycle.addObserver(youTubePlayerView)
//        //}
//        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
//            override fun onReady(youTubePlayer: YouTubePlayer) {
//                // Guardamos la referencia del reproductor para usarla después
//                this@MainActivity.youTubePlayer = youTubePlayer
//            }
//        })
    }

    private fun getData() {
        mainViewModel.getVideos("Videos populares de hoy", 35)
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

    private fun setupRecycler() {

        videoAdapter = VideoAdapter(listaVideos) { item -> onClickListener(item) }

        binding.rv.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3)
            adapter = videoAdapter
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
        Toast.makeText(this, item.snippet.title, Toast.LENGTH_LONG).show()

        listaVideosEnCola.add(item)
        videoAdapterEnCola.notifyItemInserted(listaVideosEnCola.size - 1)
        binding.rvEnCola?.scrollToPosition(listaVideosEnCola.size - 1)
        Toast.makeText(this, "'${item.snippet.title}' añadido a la cola", Toast.LENGTH_SHORT)
            .show()

        if (!reproductorEnUso && youTubePlayer != null) {
            iniciarReproduccionEnCola()
        }

    }

    private fun setupRecyclerVideosEnCola() {
        videoAdapterEnCola =
            VideoEnColaAdapter(listaVideosEnCola) { item -> onClickListenerVideosEnCola(item) }

        binding.rvEnCola.apply {
            this?.layoutManager = LinearLayoutManager(this@MainActivity)
            this?.adapter = videoAdapterEnCola
        }
    }

    private fun onClickListenerVideosEnCola(item: Item) {
        Toast.makeText(this, item.snippet.title, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        youTubePlayerView.release()
    }

    override fun onResume() {
        super.onResume()
        castContext.sessionManager.addSessionManagerListener(
            sessionManagerListener,
            CastSession::class.java
        )
    }

    override fun onPause() {
        super.onPause()
        castContext.sessionManager.removeSessionManagerListener(
            sessionManagerListener,
            CastSession::class.java
        )
    }

}
