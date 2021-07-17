package com.masai.uber.model.eventbus

data class DriverRequestReceived(
    val key: String?,
    val pickUpLocation: String?,
    val startAddress: String?,
    val endAddress: String?
)