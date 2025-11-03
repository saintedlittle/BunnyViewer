package com.github.saintedlittle.bunnyviewer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform