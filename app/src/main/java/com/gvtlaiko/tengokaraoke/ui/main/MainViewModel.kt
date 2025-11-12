package com.gvtlaiko.tengokaraoke.ui.main

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

    fun getVideos(busquedaUsuario: String, totalVideos: Int) {
        viewModelScope.launch {
            _uiStateData.value = UIState.Loading
            try {
                val call = retrofit.getHost().getVideos(busquedaUsuario, totalVideos)
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

}