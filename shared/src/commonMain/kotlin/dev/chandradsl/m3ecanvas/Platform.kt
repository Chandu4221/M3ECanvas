package dev.chandradsl.m3ecanvas

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform