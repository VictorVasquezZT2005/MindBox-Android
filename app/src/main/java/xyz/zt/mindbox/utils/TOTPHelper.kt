package xyz.zt.mindbox.utils

import android.net.Uri
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow
import org.apache.commons.codec.binary.Base32

object TOTPHelper {

    fun generateCode(secretKey: String?): String {
        if (secretKey.isNullOrBlank()) return "000000"

        return try {
            val clean = secretKey.replace(" ", "").uppercase().trim()
            val data = (System.currentTimeMillis() / 1000 / 30)

            val bytes = Base32().decode(clean)

            val counter = ByteArray(8)
            var tempData = data
            for (i in 7 downTo 0) {
                counter[i] = (tempData and 0xFF).toByte()
                tempData = tempData shr 8
            }

            val keySpec = SecretKeySpec(bytes, "HmacSHA1")
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(keySpec)
            val hash = mac.doFinal(counter)

            val offset = hash[hash.size - 1].toInt() and 0x0F
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)

            val otp = binary % 10.0.pow(6.0).toInt()
            String.format(Locale.US, "%06d", otp)
        } catch (e: Exception) {
            e.printStackTrace()
            "000000"
        }
    }

    fun getProgress(): Float {
        val sec = (System.currentTimeMillis() / 1000) % 30
        return 1f - (sec.toFloat() / 30f)
    }

    fun parseQrCode(content: String): Triple<String, String, String>? {
        return try {
            val uri = Uri.parse(content)
            if (uri.scheme != "otpauth") return null

            val secret = uri.getQueryParameter("secret") ?: return null
            val issuer = uri.getQueryParameter("issuer")
                ?: uri.path?.substringBefore(":")?.removePrefix("/")
                ?: "Servicio"
            val account = uri.path?.substringAfter(":") ?: ""

            Triple(issuer, account, secret)
        } catch (_: Exception) {
            null
        }
    }
}