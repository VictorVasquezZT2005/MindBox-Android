package xyz.zt.mindbox.utils

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Storage

object AppwriteHelper {
    private lateinit var client: Client
    lateinit var storage: Storage

    fun init(context: Context) {
        client = Client(context)
            .setEndpoint("https://sfo.cloud.appwrite.io/v1")
            .setProject("69bafc2400163f8e22ea")
            .setSelfSigned(true)

        storage = Storage(client)
    }
}