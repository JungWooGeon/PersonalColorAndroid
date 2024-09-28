package com.gun.personalcolor.state

sealed class AdState {
    data object Idle : AdState()
    data object Loaded : AdState()
    data class Error(val message: String) : AdState()
}