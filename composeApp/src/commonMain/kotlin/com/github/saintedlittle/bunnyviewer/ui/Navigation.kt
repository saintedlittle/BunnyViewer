package com.github.saintedlittle.bunnyviewer.ui


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


sealed class Nav {
    data object Register : Nav()
    data object Login : Nav()
    data class Feed(val postId: Long? = null) : Nav()
}


class Navigation {
    private val _backstack = MutableStateFlow(listOf<Nav>())
    val backstack: StateFlow<List<Nav>> get() = _backstack
    val current: Nav get() = _backstack.value.lastOrNull() ?: Nav.Login


    fun goTo(dest: Nav) {
        _backstack.value += dest
    }
    fun replaceAll(dest: Nav) { _backstack.value = listOf(dest) }
    fun back(): Boolean = if (_backstack.value.size > 1) {
        _backstack.value = _backstack.value.dropLast(1); true
    } else false
}