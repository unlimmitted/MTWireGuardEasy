package ru.unlimmitted.mtwgeasy.dto

data class MikroTikInfo(
    var routerBoard: String? = null,
    var version: String? = null,
    var interfaces: List<WgInterface> = emptyList(),
)
