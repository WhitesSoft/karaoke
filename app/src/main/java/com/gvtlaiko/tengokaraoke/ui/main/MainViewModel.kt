package com.gvtlaiko.tengokaraoke.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gvtlaiko.tengokaraoke.core.UIState
import com.gvtlaiko.tengokaraoke.data.models.response.Id
import com.gvtlaiko.tengokaraoke.data.models.response.Item
import com.gvtlaiko.tengokaraoke.data.models.response.Snippet
import com.gvtlaiko.tengokaraoke.data.models.response.Thumbnail
import com.gvtlaiko.tengokaraoke.data.models.response.Thumbnails
import com.gvtlaiko.tengokaraoke.data.models.response.VideoResponse
import com.gvtlaiko.tengokaraoke.data.network.RetrofitModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class MainViewModel : ViewModel() {

    private val retrofit by lazy { RetrofitModule }

    private val _uiStateData = MutableStateFlow<UIState<VideoResponse>>(UIState.Loading)
    val stateData: StateFlow<UIState<VideoResponse>> = _uiStateData

    private val _sugerenciasState = MutableStateFlow<UIState<List<String>>>(UIState.Empty)
    val sugerenciasState: StateFlow<UIState<List<String>>> = _sugerenciasState

    private var currentKeyIndex = 0

    val misApiKeys = listOf(
        "AIzaSyBFaVSE-n5rgORlvwn8iMTSKPgCKB9c1R0",
        "AIzaSyBKOWMJQtywrNVD_K9EI2ekwSiyfGhobsc",
//        "AIzaSyB6R_oAodzW9alx5RdBLW9_k0P1d-wLol4",
//        "AIzaSyCUtBnOherlOYNJHQInGBfUuGyCRRN13Uw",
//        "AIzaSyBLM5iG8jAG-f6fuej1lmfAFt8XBMFPQkc",
//        "AIzaSyAXnIsPkra2u8k9E6gShl_FuultF5ym0bU",
//        "AIzaSyDVOwBtzdHKBKOhxHLGcvWWwEFJGFfl0IQ"
        // tengori75
        "AIzaSyD6l00QsdFZe3IWHyUqqIXK_3e7VaGdvY0",
        "AIzaSyB1pS5LqYdSU-myoS4Z4jCtW9MUiw-9moQ",
        "AIzaSyAqKAWqScLP7O6fW0KBibI92WmH7azLaBo",
        "AIzaSyC3KKvmkc_fTSAPtznNz0LStIlfBnsAs3o",
        "AIzaSyBztdjnhpmwmRGVFjoEcPPW4XxBwxUo_Sk",
        "AIzaSyDxwtiH-WRd-MzcIhX7PimU5VUHqtIdQCk",
        // tengori333
        "AIzaSyBd1DZ_0XCYAlYBjZp9AxLsTqp3H810lMM",
        "AIzaSyAyRlPClR4ytY5sRsOX9IYfhMZxDOuu29I",
        "AIzaSyAyRlPClR4ytY5sRsOX9IYfhMZxDOuu29I",
        "AIzaSyDdyUgMtBTSyJsgu09B8tP4t4zB1qwjzgE",
        "AIzaSyBoUG36uimfaZscysjION_8wvH14SEEkcY",
        // tengori44
        "AIzaSyDTsGUyQWkKn6fBhvYMZnmo1WV_dzCs4Kk",
        "AIzaSyA4EX3JxNSJM54dpV3UMhhBTI5VcLw51XU",
        "AIzaSyBp81HsL3y29hBOTKaM34OV-c3Tks5mwP8",
        "AIzaSyD4SGLDNHGHUkXdO-lEqeL7aCnm2kgOu80",
        "AIzaSyAMHNqf20FB6TlOkCUYis0__voYjqDbwYc",
        "AIzaSyCF5FZ3kXVEbLIvxvcktEmQM43C-LHggGE",
        "AIzaSyBOVt9NCJqQDYa5OmIWlcStOt89oLsGcMg",
        // tengori222
        "AIzaSyDOtA5imVYgLKxHwORlF101_xZ-B7BMiBU"
    )

    fun getVideos(busquedaUsuario: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiStateData.value = UIState.Loading
            try {
                // 1. Usamos NewPipe para buscar
                val serviceId = ServiceList.YouTube.serviceId
                val searchExtractor = NewPipe.getService(serviceId)
                    .getSearchExtractor(
                        busquedaUsuario,
                        listOf(YoutubeSearchQueryHandlerFactory.VIDEOS),
                        null
                    )

                searchExtractor.fetchPage()
                val newPipeItems = searchExtractor.initialPage.items

                // 2. Convertimos los resultados de NewPipe a tu clase 'Item'
                // para que el Adapter no se rompa.
                val listaConvertida = newPipeItems.mapNotNull { item ->
                    if (item is StreamInfoItem) {
                        mapNewPipeItemToAppItem(item)
                    } else {
                        null
                    }
                }

                // 3. Empaquetamos en tu VideoResponse y enviamos
                // Asumo que VideoResponse tiene un constructor que acepta la lista
                // Si VideoResponse es de la librería de Google, tendrás que crear una clase wrapper propia.
                val response = VideoResponse(listaConvertida)
                _uiStateData.value = UIState.Success(response)

            } catch (e: Exception) {
                e.printStackTrace()
                _uiStateData.value = UIState.Error("Error NewPipe: ${e.localizedMessage}")
            }
        }
    }


//    fun getVideos(busquedaUsuario: String) {
//        viewModelScope.launch {
//            _uiStateData.value = UIState.Loading
//            try {
//                val currentKey = misApiKeys[currentKeyIndex]
//                Log.i("MainActivity", "Usando Key [$currentKeyIndex]: $currentKey")
//                currentKeyIndex = (currentKeyIndex + 1) % misApiKeys.size
//                val call = retrofit.getHost().getVideos(busquedaUsuario, 50, currentKey)
//                if (call.isSuccessful) {
//                    call.body()?.let { it ->
//                        _uiStateData.value = UIState.Success(it)
//                    }
//                } else {
//                    _uiStateData.value =
//                        UIState.Error("Error call: ${call.code()} - ${call.message()} - ${call.errorBody()} - ${call.message()}")
//                }
//            } catch (e: Exception) {
//                _uiStateData.value = UIState.Error(e.localizedMessage ?: "Unknown error")
//            }
//        }
//    }

    fun getSugerencias(query: String) {
        viewModelScope.launch {
            try {

                val responseBody = retrofit.getHost().getAutocomplete(query = query)
                val jsonString = responseBody.string()

                val jsonArray = org.json.JSONArray(jsonString)
                val suggestionsArray = jsonArray.getJSONArray(1)

                val cleanSuggestions = mutableListOf<String>()
                for (i in 0 until suggestionsArray.length()) {
                    cleanSuggestions.add(suggestionsArray.getString(i))
                }

                _sugerenciasState.value = UIState.Success(cleanSuggestions)

            } catch (e: Exception) {
                Log.e("ViewModel", "Error sugerencias: ${e.message}")
                _sugerenciasState.value = UIState.Empty
            }
        }
    }

    private fun mapNewPipeItemToAppItem(streamItem: StreamInfoItem): Item {
        // Extraemos el ID limpio (NewPipe da la URL completa)
        val videoIdClean = streamItem.url.replace("https://www.youtube.com/watch?v=", "")

        // Construimos tu objeto Id
        val idObj = Id(videoId = videoIdClean)

        // Construimos el objeto Thumbnails (ajusta según tu clase)
        val thumbUrl = streamItem.thumbnails.lastOrNull()?.url ?: ""
        val highThumb = Thumbnail(url = thumbUrl)
        val thumbnailsObj = Thumbnails(high = highThumb)

        // Construimos el Snippet
        val snippetObj = Snippet(
            title = streamItem.name,
            thumbnails = thumbnailsObj,
            channelTitle = streamItem.uploaderName,
            description = streamItem.shortDescription ?: ""
        )

        // Retornamos tu Item completo
        return Item(id = idObj, snippet = snippetObj)
    }

}