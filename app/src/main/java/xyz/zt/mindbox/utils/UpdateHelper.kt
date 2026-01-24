package xyz.zt.mindbox.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import xyz.zt.mindbox.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateHelper {
    private const val GITHUB_API_URL = "https://api.github.com/repos/VictorVasquezZT2005/MindBox/releases/latest"

    const val CURRENT_VERSION = BuildConfig.VERSION_NAME

    suspend fun checkForUpdates(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val latestVersion = json.getString("tag_name").replace("v", "")

                    if (latestVersion != CURRENT_VERSION) latestVersion else null
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    fun openGitHub(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/VictorVasquezZT2005/MindBox/releases"))
        context.startActivity(intent)
    }
}