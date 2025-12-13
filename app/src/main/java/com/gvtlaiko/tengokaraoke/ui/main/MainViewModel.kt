package com.gvtlaiko.tengokaraoke.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gvtlaiko.tengokaraoke.core.UIState
import com.gvtlaiko.tengokaraoke.data.models.response.VideoResponse
import com.gvtlaiko.tengokaraoke.data.network.RetrofitModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
        "AIzaSyB6R_oAodzW9alx5RdBLW9_k0P1d-wLol4",
        "AIzaSyCUtBnOherlOYNJHQInGBfUuGyCRRN13Uw",
        "AIzaSyBLM5iG8jAG-f6fuej1lmfAFt8XBMFPQkc",
        "AIzaSyAXnIsPkra2u8k9E6gShl_FuultF5ym0bU",
        "AIzaSyDVOwBtzdHKBKOhxHLGcvWWwEFJGFfl0IQ"
    )

    fun getVideos(busquedaUsuario: String) {
        viewModelScope.launch {
            _uiStateData.value = UIState.Loading
            try {
                val currentKey = misApiKeys[currentKeyIndex]
                Log.i("MainActivity", "Usando Key [$currentKeyIndex]: $currentKey")
                currentKeyIndex = (currentKeyIndex + 1) % misApiKeys.size
                val call = retrofit.getHost().getVideos(busquedaUsuario, 50, currentKey)
                if (call.isSuccessful) {
                    call.body()?.let { it ->
                        _uiStateData.value = UIState.Success(it)
                    }
                } else {
                    _uiStateData.value =
                        UIState.Error("Error call: ${call.code()} - ${call.message()} - ${call.errorBody()} - ${call.message()}")
                }
            } catch (e: Exception) {
                _uiStateData.value = UIState.Error(e.localizedMessage ?: "Unknown error")
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

}