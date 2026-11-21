package com.oem.collagekmpapp.model

data class Slot(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class CollageLayout(
    val id: String,
    val name: String,
    val slotCount: Int,
    val description: String,
    val slots: List<Slot>
)
