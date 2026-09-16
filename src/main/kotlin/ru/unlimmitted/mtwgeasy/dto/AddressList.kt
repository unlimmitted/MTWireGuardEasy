package ru.unlimmitted.mtwgeasy.dto

data class AddressList(
    var id: String? = null,
    var address: String? = null,
    var disabled: Boolean = false,
    var comment: String? = null,
    var listName: String? = null,
)
