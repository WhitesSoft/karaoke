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

    private val _videoUrlState = MutableStateFlow<UIState<String>>(UIState.Empty)
    val videoUrlState: StateFlow<UIState<String>> = _videoUrlState

    fun getVideos(busquedaUsuario: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiStateData.value = UIState.Loading
            try {
                val serviceId = ServiceList.YouTube.serviceId
                val searchExtractor = NewPipe.getService(serviceId)
                    .getSearchExtractor(
                        busquedaUsuario,
                        listOf(YoutubeSearchQueryHandlerFactory.VIDEOS),
                        null
                    )

                searchExtractor.fetchPage() // primera pagina
                var currentPage = searchExtractor.initialPage

                // lista con los videos de la primera pagina
                val allNewPipeItems = currentPage.items.toMutableList()

                var paginasAdicionales = 2 // paginas a pedir

                while (paginasAdicionales > 0 && currentPage.hasNextPage()) {
                    try {
                        // peidmos la siguiente pagina
                        val nextPage = searchExtractor.getPage(currentPage.nextPage)

                        allNewPipeItems.addAll(nextPage.items)

                        currentPage = nextPage
                        paginasAdicionales--

                    } catch (e: Exception) {
                        e.printStackTrace()
                        break
                    }
                }

                // pasamos todos los videos
                val listaConvertida = allNewPipeItems.mapNotNull { item ->
                    if (item is StreamInfoItem) {
                        mapNewPipeItemToAppItem(item)
                    } else {
                        null
                    }
                }

                val response = VideoResponse(listaConvertida)
                _uiStateData.value = UIState.Success(response)

            } catch (e: Exception) {
                e.printStackTrace()
                _uiStateData.value = UIState.Error("Error NewPipe: ${e.localizedMessage}")
            }
        }
    }

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

    fun getStreamUrl(videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _videoUrlState.value = UIState.Loading
            try {
                val streamExtractor = NewPipe.getService(ServiceList.YouTube.serviceId)
                    .getStreamExtractor("https://www.youtube.com/watch?v=$videoId")

                streamExtractor.fetchPage()

                val stream = streamExtractor.videoStreams.lastOrNull {
                    it.resolution.contains("720") || it.resolution.contains("360")
                } ?: streamExtractor.videoStreams.lastOrNull()

                if (stream != null) {
                    _videoUrlState.value = UIState.Success(stream.url) as UIState<String>
                } else {
                    _videoUrlState.value = UIState.Error("No se encontró stream válido")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _videoUrlState.value = UIState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    private fun mapNewPipeItemToAppItem(streamItem: StreamInfoItem): Item {
        val videoIdClean = streamItem.url.replace("https://www.youtube.com/watch?v=", "")

        val idObj = Id(videoId = videoIdClean)

        val thumbUrl = streamItem.thumbnails.lastOrNull()?.url ?: ""
        val highThumb = Thumbnail(url = thumbUrl)
        val thumbnailsObj = Thumbnails(high = highThumb)

        val snippetObj = Snippet(
            title = streamItem.name,
            thumbnails = thumbnailsObj,
            channelTitle = streamItem.uploaderName,
            description = streamItem.shortDescription ?: ""
        )

        return Item(id = idObj, snippet = snippetObj)
    }

}
