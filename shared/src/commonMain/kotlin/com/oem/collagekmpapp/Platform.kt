package com.oem.collagekmpapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform