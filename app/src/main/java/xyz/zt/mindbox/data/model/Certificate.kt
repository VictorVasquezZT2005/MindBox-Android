package xyz.zt.mindbox.data.model

import java.util.UUID

data class Certificate(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val platform: String = "Otro",
    val date: String = "",
    val issueDate: String = "",
    val folio: String? = null,
    val credlyId: String? = null,
    val score: String? = null,
    val notes: String? = null,
    val pdfUrl: String? = null
)