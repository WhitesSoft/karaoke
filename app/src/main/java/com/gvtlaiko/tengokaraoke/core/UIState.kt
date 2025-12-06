package com.gvtlaiko.tengokaraoke.core

sealed class UIState<out T> {
    object Loading : UIState<Nothing>()
    object Empty : UIState<Nothing>()
    data class Success<out T>(val data: T) : UIState<T>()
//    data class Empty(val empty: Boolean = true) : UIState<Nothing>()
    data class Error(val error: String) : UIState<Nothing>()
}