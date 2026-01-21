package xyz.zt.mindbox.data.model

import java.util.UUID

data class Password(
    // Al darles un valor por defecto "=", Firebase ya puede usarlos
    val id: String = UUID.randomUUID().toString(),
    val serviceName: String = "",
    val accountEmail: String = "",
    val secretKey: String = ""
)